package justfatlard.bonsai_saplings;

import java.util.List;

import com.mojang.math.Transformation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The little tree standing in the pot.
 *
 * <p>Block displays, one per block of the form, because a display carries a full transformation
 * and a block does not: the whole point is a tree at a fraction of block size, and nothing that is
 * actually made of blocks can be smaller than one.
 *
 * <p>The displays are also the record. Nothing stores which pots have trees in them - the trees
 * sitting in them do - so there is no second copy to fall out of step when a pot is broken by
 * something nobody was watching.
 */
public final class BonsaiTree {
	private BonsaiTree() {}

	private static final String TAG = "bonsai_tree";
	/** Which form this one is wearing, so shears can pick a different one. */
	private static final String SHAPE_TAG = "bonsai_shape_";

	/** Small enough to sit in a pot, big enough that the trunk still reads as a trunk. */
	private static final float SCALE = 0.16F;
	/** The rim of a flower pot, which is where soil ends and tree begins. */
	private static final double RIM = 0.375;

	/** Near enough to be furniture, far enough not to be scenery. */
	private static final float VIEW_RANGE = 0.4F;

	/** Put a tree in this pot, replacing whatever was in it. */
	public static void plant(ServerLevel level, BlockPos pot, BonsaiSpecies species,
			BonsaiShape shape, int quarterTurns) {
		clear(level, pot);

		Vec3 middle = middleOf(pot);
		for (BonsaiShape.Cell cell : shape.cells()) {
			Display.BlockDisplay piece =
				new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);

			piece.setPos(middle);
			piece.setBlockState(cell.wood() ? species.wood() : species.leaves());
			piece.setViewRange(VIEW_RANGE);
			piece.setBillboardConstraints(Display.BillboardConstraints.FIXED);
			piece.setTransformation(placeOf(cell, quarterTurns));
			piece.addTag(TAG);
			piece.addTag(SHAPE_TAG + shape.name());

			level.addFreshEntity(piece);
		}
	}

	/** Cut it back and let it come up as something else. */
	public static void reshape(ServerLevel level, BlockPos pot, BonsaiSpecies species,
			RandomSource random) {
		BonsaiShape next = BonsaiShape.otherThan(random, shapeIn(level, pot));

		plant(level, pot, species, next, random.nextInt(4));
	}

	public static void clear(ServerLevel level, BlockPos pot) {
		for (Display.BlockDisplay piece : piecesIn(level, pot)) {
			piece.discard();
		}
	}

	public static boolean has(ServerLevel level, BlockPos pot) {
		return !piecesIn(level, pot).isEmpty();
	}

	/** Whether what is standing in this pot is a creeper rather than a tree. */
	public static boolean isCreeper(ServerLevel level, BlockPos pot) {
		return BonsaiShape.CREEPER.name().equals(shapeIn(level, pot));
	}

	/** The form currently in this pot, or null when there is nothing in it. */
	private static String shapeIn(ServerLevel level, BlockPos pot) {
		for (Display.BlockDisplay piece : piecesIn(level, pot)) {
			for (String tag : piece.entityTags()) {
				if (tag.startsWith(SHAPE_TAG)) return tag.substring(SHAPE_TAG.length());
			}
		}
		return null;
	}

	/**
	 * The pieces belonging to this pot.
	 *
	 * <p>Searched tightly. A cascade hangs below the rim and a windswept leans well off centre,
	 * so the box has to cover the whole form - but two pots on neighbouring blocks must not
	 * collect each other's branches, which is why the pieces all sit at the pot's own position
	 * and carry their offset in the transformation instead of in their coordinates.
	 */
	private static List<Display.BlockDisplay> piecesIn(ServerLevel level, BlockPos pot) {
		Vec3 middle = middleOf(pot);

		return level.getEntitiesOfClass(Display.BlockDisplay.class,
			new AABB(middle, middle).inflate(0.2),
			piece -> piece.entityTags().contains(TAG));
	}

	private static Vec3 middleOf(BlockPos pot) {
		return new Vec3(pot.getX() + 0.5, pot.getY() + RIM, pot.getZ() + 0.5);
	}

	/**
	 * Where one block of the form sits, relative to the pot.
	 *
	 * <p>Carried in the transformation rather than the entity position on purpose: every piece of
	 * one tree then shares a position, which is what lets a pot find its own tree and only its
	 * own, however far a branch reaches.
	 */
	private static Transformation placeOf(BonsaiShape.Cell cell, int quarterTurns) {
		int x = cell.x();
		int z = cell.z();

		// Turned about the trunk, so the same six forms do not all face the same way.
		for (int turn = 0; turn < quarterTurns; turn++) {
			int spun = x;
			x = -z;
			z = spun;
		}

		// A display's own model hangs off its origin corner, so half a block of the scaled size
		// comes back off to stand the piece on the middle of the pot rather than beside it.
		return new Transformation(
			new org.joml.Vector3f(
				(x * SCALE) - SCALE / 2F,
				cell.y() * SCALE,
				(z * SCALE) - SCALE / 2F),
			null,
			new org.joml.Vector3f(SCALE, SCALE, SCALE),
			null);
	}
}
