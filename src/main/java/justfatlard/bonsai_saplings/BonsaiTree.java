package justfatlard.bonsai_saplings;

import java.util.List;
import java.util.Set;

import com.mojang.math.Transformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
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
 * actually made of blocks can be smaller than one. Each leaf also gets a smaller solid block
 * inside it, so that its holes show the dark of a canopy rather than whatever is behind the pot.
 *
 * <p>The displays are also the record. Nothing stores which pots have trees in them - the trees
 * sitting in them do - so there is no second copy to fall out of step when a pot is broken by
 * something nobody was watching. A creeper topiary is a creeper standing at the same spot and
 * carrying the same tag, so everything that finds a tree finds it too.
 */
public final class BonsaiTree {
	private BonsaiTree() {}

	/** On every piece of every bonsai, tree or creeper, so a pot can find what stands in it. */
	static final String TAG = "bonsai_tree";
	/** Which form this one is wearing, so shears can pick a different one. */
	private static final String SHAPE_TAG = "bonsai_shape_";
	static final String TURN_TAG = "bonsai_turn_";
	/** The solid block behind a leaf: part of the tree, but not a leaf when reading the tree back. */
	private static final String BACKING_TAG = "bonsai_backing";

	/**
	 * Which potted block the tree grew from, so the sapling can be handed back.
	 *
	 * <p>The pot itself is empty while a tree stands in it. The sapling used to stay in the pot
	 * as the potted block, and the potted block draws a sapling, so every tree had a sapling
	 * poking up through its trunk. Now the tree is the only record of what was planted.
	 */
	private static final String POTTED_TAG = "bonsai_potted_";
	/** Which item a pot-less kind was planted from: a chorus flower, a handful of grass, a seed. */
	private static final String ITEM_TAG = "bonsai_item_";

	/** Small enough to sit in a pot, big enough that the trunk still reads as a trunk. */
	private static final float SCALE = 0.16F;
	/** The backing sits just inside its leaf, so the leaf's own faces are always in front. */
	private static final float BACKING_SCALE = SCALE * 0.9F;
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
	/**
	 * The same, in a cachepot: a decorated pot's neck rises to a quarter block over the top of the
	 * block and opens six pixels across, and the tree is sunk a little into it.
	 */
	private static final double CACHEPOT_SOIL = 1.15;
	/** On every piece of a tree standing in a cachepot, which is how a piece a block up finds its pot. */
	static final String CACHEPOT_TAG = "bonsai_cachepot";

	/** Near enough to be furniture, far enough not to be scenery. */
	private static final float VIEW_RANGE = 0.4F;

	/** Put a tree in this pot, replacing whatever was in it. */
	public static void plant(ServerLevel level, BlockPos pot, BonsaiSpecies species,
			BonsaiShape shape, int quarterTurns, Block potted) {
		clear(level, pot);

		boolean cachepot = Cachepot.isVessel(level.getBlockState(pot));
		Vec3 middle = cachepot ? cachepotMiddleOf(pot) : middleOf(pot);
		Block stairs = BetterLeaves.stairsFor(species.leaves());
		Set<Long> occupied = stairs == null ? null : BetterLeaves.occupied(shape.cells());
		for (BonsaiShape.Cell cell : shape.cells()) {
			BlockState drawn = cell.wood() ? species.wood() : species.leaves();
			if (stairs != null && !cell.wood()) {
				BlockState edge = BetterLeaves.edge(stairs, species.leaves(), occupied, cell, quarterTurns);
				if (edge != null) drawn = edge;
			}
			Display.BlockDisplay piece = piece(level, middle, drawn, placeOf(cell, quarterTurns, SCALE));
			piece.addTag(SHAPE_TAG + shape.name());
			piece.addTag(TURN_TAG + quarterTurns);
			if (cachepot) piece.addTag(CACHEPOT_TAG);
			level.addFreshEntity(piece);

			// A stair is already mostly its own backing, and a cube behind it would poke out of
			// the half that is not there.
			if (drawn == species.leaves() && species.backing() != null) {
				Display.BlockDisplay backing =
					piece(level, middle, species.backing(), placeOf(cell, quarterTurns, BACKING_SCALE));
				backing.addTag(BACKING_TAG);
				if (cachepot) backing.addTag(CACHEPOT_TAG);
				level.addFreshEntity(backing);
			}
		}
	}

	private static Display.BlockDisplay piece(ServerLevel level, Vec3 middle, BlockState drawn,
			Transformation place) {
		Display.BlockDisplay piece = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
		piece.setPos(middle);
		piece.setBlockState(drawn);
		piece.setViewRange(VIEW_RANGE);
		piece.setBillboardConstraints(Display.BillboardConstraints.FIXED);
		piece.setTransformation(place);
		piece.addTag(TAG);
		return piece;
	}

	/** Cut it back and let it come up as the next thing: a quarter turn on, or the next form. */
	public static void cycle(ServerLevel level, BlockPos pot) {
		BonsaiSpecies species = speciesIn(level, pot);
		if (species == null) return;
		BonsaiShape.Next next = BonsaiShape.next(species.kind(), shapeIn(level, pot), turnIn(level, pot),
			Cachepot.growsLarge(level, pot));
		Block potted = pottedIn(level, pot);
		net.minecraft.world.item.Item item = itemIn(level, pot);
		// The record goes with the cut: re-planted without it, a tree forgot what it grew from, and
		// only a flower pot has anything to read it back off.
		if (potted != null) {
			plant(level, pot, species, next.shape(), next.turn(), potted);
			remember(level, pot, potted);
		} else if (item != null) {
			plantFromItem(level, pot, item, species, next.shape(), next.turn());
		}
	}

	/** Which way the tree in this pot is turned, in quarter turns; zero for one that never said. */
	static int turnIn(ServerLevel level, BlockPos pot) {
		for (Entity piece : piecesIn(level, pot)) {
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
		for (Entity piece : piecesIn(level, pot)) {
			piece.addTag(tag);
		}
	}

	/** Plant a pot-less kind: the tree records the item, since no potted block ever will. */
	public static void plantFromItem(ServerLevel level, BlockPos pot, net.minecraft.world.item.Item item,
			BonsaiSpecies species, BonsaiShape shape, int quarterTurns) {
		plant(level, pot, species, shape, quarterTurns, null);
		String tag = ITEM_TAG + BuiltInRegistries.ITEM.getKey(item).getPath();
		for (Entity piece : piecesIn(level, pot)) piece.addTag(tag);
	}

	/** The item a pot-less kind in this pot was planted from, or null. */
	public static net.minecraft.world.item.Item itemIn(ServerLevel level, BlockPos pot) {
		for (Entity piece : piecesIn(level, pot)) {
			for (String tag : piece.entityTags()) {
				if (!tag.startsWith(ITEM_TAG)) continue;
				net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.getValue(
					Identifier.withDefaultNamespace(tag.substring(ITEM_TAG.length())));
				return item == net.minecraft.world.item.Items.AIR ? null : item;
			}
		}
		return null;
	}

	/** What stands in this pot, from its potted block or its item; null for a tree with no record. */
	public static BonsaiSpecies speciesIn(ServerLevel level, BlockPos pot) {
		Block potted = pottedIn(level, pot);
		if (potted != null) return BonsaiSpecies.of(potted);
		net.minecraft.world.item.Item item = itemIn(level, pot);
		return item == null ? null : BonsaiSpecies.ofItem(item);
	}

	/** The potted block this pot's tree grew from, or null when there is no tree or no record. */
	public static Block pottedIn(ServerLevel level, BlockPos pot) {
		for (Entity piece : piecesIn(level, pot)) {
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
	 * because an azalea grows on oak and only its leaves tell it from an oak. The backing
	 * blocks are skipped: green concrete is nobody's leaf.
	 */
	public static Block inferPotted(ServerLevel level, BlockPos pot) {
		Block byWood = null;
		for (Entity piece : piecesIn(level, pot)) {
			if (!(piece instanceof Display.BlockDisplay display)) continue;
			if (piece.entityTags().contains(BACKING_TAG)) continue;
			Block block = ((justfatlard.bonsai_saplings.mixin.BlockDisplayAccessor) display).bonsai$blockState().getBlock();
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
		for (Entity piece : piecesIn(level, pot)) {
			piece.discard();
		}
	}

	public static boolean has(ServerLevel level, BlockPos pot) {
		return !piecesIn(level, pot).isEmpty();
	}

	/** Whether what is standing in this pot is a creeper rather than a tree. */
	public static boolean isCreeper(ServerLevel level, BlockPos pot) {
		return creeperIn(level, pot) != null;
	}

	/** The creeper standing in this pot, or null. */
	public static Creeper creeperIn(ServerLevel level, BlockPos pot) {
		for (Entity piece : piecesIn(level, pot)) {
			if (piece instanceof Creeper creeper) return creeper;
		}
		return null;
	}

	/** The form currently in this pot, or null when there is nothing in it. */
	private static String shapeIn(ServerLevel level, BlockPos pot) {
		for (Entity piece : piecesIn(level, pot)) {
			for (String tag : piece.entityTags()) {
				if (tag.startsWith(SHAPE_TAG)) return tag.substring(SHAPE_TAG.length());
			}
		}
		return null;
	}

	/** Whether this entity is a piece of some bonsai, whichever pot it stands in. */
	public static boolean isPiece(Entity entity) {
		return entity instanceof Display.BlockDisplay piece && piece.entityTags().contains(TAG);
	}

	/**
	 * The pot a piece stands in: every piece is placed at its pot's own middle, so this is exact.
	 * A cachepot's tree stands up in the block above its pot, and says so.
	 */
	public static BlockPos potOf(Entity piece) {
		if (piece.entityTags().contains(CACHEPOT_TAG)) {
			return BlockPos.containing(piece.position().subtract(0, CACHEPOT_SOIL - SOIL, 0));
		}
		return BlockPos.containing(piece.position());
	}

	/**
	 * The pieces belonging to this pot.
	 *
	 * <p>Searched tightly. A cascade hangs below the rim and a windswept leans well off centre,
	 * so the box has to cover the whole form - but two pots on neighbouring blocks must not
	 * collect each other's branches, which is why the pieces all sit at the pot's own position
	 * and carry their offset in the transformation instead of in their coordinates.
	 */
	private static List<Entity> piecesIn(ServerLevel level, BlockPos pot) {
		// Tall enough for a pot's tree and a cachepot's, and then each piece asked which pot it
		// belongs to: a flower pot on top of a cachepot stands exactly where the cachepot's tree
		// does, and a box alone cannot tell them apart.
		double x = pot.getX() + 0.5, z = pot.getZ() + 0.5;
		return level.getEntitiesOfClass(Entity.class,
			new AABB(x - 0.2, pot.getY(), z - 0.2, x + 0.2, pot.getY() + CACHEPOT_SOIL + 0.2, z + 0.2),
			piece -> piece.entityTags().contains(TAG) && potOf(piece).equals(pot));
	}

	/** Where a tree's base sits: the middle of the pot, sunk a little into the soil. */
	static Vec3 middleOf(BlockPos pot) {
		return new Vec3(pot.getX() + 0.5, pot.getY() + SOIL, pot.getZ() + 0.5);
	}

	/** The same for a cachepot: in the decorated pot's neck. */
	static Vec3 cachepotMiddleOf(BlockPos pot) {
		return new Vec3(pot.getX() + 0.5, pot.getY() + CACHEPOT_SOIL, pot.getZ() + 0.5);
	}

	/**
	 * Where one block of the form sits, relative to the pot, drawn {@code size} across.
	 *
	 * <p>Carried in the transformation rather than the entity position on purpose: every piece of
	 * one tree then shares a position, which is what lets a pot find its own tree and only its
	 * own, however far a branch reaches.
	 */
	private static Transformation placeOf(BonsaiShape.Cell cell, int quarterTurns, float size) {
		int x = cell.x();
		int z = cell.z();

		// Turned about the trunk, so the same six forms do not all face the same way.
		for (int turn = 0; turn < quarterTurns; turn++) {
			int spun = x;
			x = -z;
			z = spun;
		}

		// A display's own model hangs off its origin corner, so half of the drawn size comes
		// back off to centre the piece on its cell rather than beside it; a backing, being
		// smaller than its cell, is also lifted so it sits in the middle of the leaf.
		float lift = (SCALE - size) / 2F;
		return new Transformation(
			new org.joml.Vector3f(
				(x * SCALE) - size / 2F,
				cell.y() * SCALE + lift,
				(z * SCALE) - size / 2F),
			null,
			new org.joml.Vector3f(size, size, size),
			null);
	}
}
