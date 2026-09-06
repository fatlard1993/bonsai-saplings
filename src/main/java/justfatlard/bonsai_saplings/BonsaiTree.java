package justfatlard.bonsai_saplings;

import java.util.List;
import java.util.Set;

import com.mojang.math.Transformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
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
	private static final String TURN_TAG = "bonsai_turn_";

	/**
	 * Which potted block the tree grew from, so the sapling can be handed back.
	 *
	 * <p>The pot itself is empty while a tree stands in it. The sapling used to stay in the pot
	 * as the potted block, and the potted block draws a sapling, so every tree had a sapling
	 * poking up through its trunk. Now the tree is the only record of what was planted.
	 */
	private static final String POTTED_TAG = "bonsai_potted_";

	/** Small enough to sit in a pot, big enough that the trunk still reads as a trunk. */
	private static final float SCALE = 0.16F;
	/**
	 * Where the tree's base sits: a little below the top of the soil in a flower pot.
	 *
	 * <p>The soil is four pixels deep and the rim six, and the tree used to stand on the rim, two
	 * pixels above the dirt with daylight under its trunk. A trunk sunk most of a pixel into the
	 * soil reads as planted. Every piece of a tree is placed at this height, and a pot finds its
	 * tree by looking here, so trees planted at the rim by an earlier build are still within
	 * reach of the search.
	 */
	private static final double SOIL = 0.20;

	/** Near enough to be furniture, far enough not to be scenery. */
	private static final float VIEW_RANGE = 0.4F;

	/** Put a tree in this pot, replacing whatever was in it. */
	public static void plant(ServerLevel level, BlockPos pot, BonsaiSpecies species,
			BonsaiShape shape, int quarterTurns, Block potted) {
		clear(level, pot);

		Vec3 middle = middleOf(pot);
		Block stairs = BetterLeaves.stairsFor(species.leaves());
		Set<Long> occupied = stairs == null ? null : BetterLeaves.occupied(shape.cells());
		for (BonsaiShape.Cell cell : shape.cells()) {
			Display.BlockDisplay piece =
				new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);

			BlockState drawn = cell.wood() ? species.wood() : species.leaves();
			if (stairs != null && !cell.wood()) {
				BlockState edge = BetterLeaves.edge(stairs, species.leaves(), occupied, cell, quarterTurns);
				if (edge != null) drawn = edge;
			}
			piece.setPos(middle);
			piece.setBlockState(drawn);
			piece.setViewRange(VIEW_RANGE);
			piece.setBillboardConstraints(Display.BillboardConstraints.FIXED);
			piece.setTransformation(placeOf(cell, quarterTurns));
			piece.addTag(TAG);
			piece.addTag(SHAPE_TAG + shape.name());
			piece.addTag(TURN_TAG + quarterTurns);

			level.addFreshEntity(piece);
		}
	}

	/** Cut it back and let it come up as the next thing: a quarter turn on, or the next form. */
	public static void cycle(ServerLevel level, BlockPos pot) {
		Block potted = pottedIn(level, pot);
		BonsaiSpecies species = BonsaiSpecies.of(potted);
		if (species == null) return;
		BonsaiShape.Next next = BonsaiShape.next(species.kind(), shapeIn(level, pot), turnIn(level, pot));
		plant(level, pot, species, next.shape(), next.turn(), potted);
	}

	/** Which way the tree in this pot is turned, in quarter turns; zero for one that never said. */
	private static int turnIn(ServerLevel level, BlockPos pot) {
		for (Display.BlockDisplay piece : piecesIn(level, pot)) {
			for (String tag : piece.entityTags()) {
				if (!tag.startsWith(TURN_TAG)) continue;
				try {
					return Integer.parseInt(tag.substring(TURN_TAG.length())) & 3;
				} catch (NumberFormatException ignored) {
					return 0;
				}
			}
		}
		return 0;
	}

	/** Tell a tree planted by an older build which sapling it grew from. */
	public static void remember(ServerLevel level, BlockPos pot, Block potted) {
		String tag = POTTED_TAG + BuiltInRegistries.BLOCK.getKey(potted).getPath();
		for (Display.BlockDisplay piece : piecesIn(level, pot)) {
			piece.addTag(tag);
		}
	}

	/** The potted block this pot's tree grew from, or null when there is no tree or no record. */
	public static Block pottedIn(ServerLevel level, BlockPos pot) {
		for (Display.BlockDisplay piece : piecesIn(level, pot)) {
			for (String tag : piece.entityTags()) {
				if (!tag.startsWith(POTTED_TAG)) continue;
				Block block = BuiltInRegistries.BLOCK.getValue(
					Identifier.withDefaultNamespace(tag.substring(POTTED_TAG.length())));
				return block == Blocks.AIR ? null : block;
			}
		}
		return null;
	}

	/**
	 * What a tree with no record was grown from, read off the tree itself.
	 *
	 * <p>A tree planted before the record existed still knows its species: its foliage is a
	 * particular leaf block, and every leaf block belongs to one sapling. Leaves before wood,
	 * because an azalea grows on oak and only its leaves tell it from an oak.
	 */
	public static Block inferPotted(ServerLevel level, BlockPos pot) {
		Block byWood = null;
		for (Display.BlockDisplay piece : piecesIn(level, pot)) {
			Block block = ((justfatlard.bonsai_saplings.mixin.BlockDisplayAccessor) piece).bonsai$blockState().getBlock();
			Block potted = BonsaiSpecies.pottedForLeaves(block);
			if (potted != null) return potted;
			if (byWood == null) byWood = BonsaiSpecies.pottedForWood(block);
		}
		return byWood;
	}

	/** Whether a tree, as opposed to nothing or a creeper, is standing in this pot. */
	public static boolean isTree(ServerLevel level, BlockPos pot) {
		return has(level, pot) && !isCreeper(level, pot);
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
	/** Whether this entity is a piece of some bonsai, whichever pot it stands in. */
	public static boolean isPiece(net.minecraft.world.entity.Entity entity) {
		return entity instanceof Display.BlockDisplay piece && piece.entityTags().contains(TAG);
	}

	/** The pot a piece stands in: every piece is placed at the pot's own middle, so this is exact. */
	public static BlockPos potOf(net.minecraft.world.entity.Entity piece) {
		return BlockPos.containing(piece.position());
	}

	private static List<Display.BlockDisplay> piecesIn(ServerLevel level, BlockPos pot) {
		Vec3 middle = middleOf(pot);

		return level.getEntitiesOfClass(Display.BlockDisplay.class,
			new AABB(middle, middle).inflate(0.2),
			piece -> piece.entityTags().contains(TAG));
	}

	private static Vec3 middleOf(BlockPos pot) {
		return new Vec3(pot.getX() + 0.5, pot.getY() + SOIL, pot.getZ() + 0.5);
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
