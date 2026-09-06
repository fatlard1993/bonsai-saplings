package justfatlard.bonsai_saplings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

/**
 * The canopy's edge in leaf stairs, when better-trees is here to lend them.
 *
 * <p>Better-trees rounds off every grown tree by turning the outermost leaves into stairs that
 * step down and outward, and a bonsai is a tree, so it gets the same treatment. The rule is
 * theirs: a leaf with one open side is a straight stair with its low step facing out, a leaf on
 * an outside corner is an outer stair, a leaf on the underside is turned over so the full half is
 * up, and anything too exposed or too enclosed to read as an edge stays a cube. Only the rule is
 * shared. Their blocks are looked up by name in the registry, so this mod compiles without them
 * and a pot in a world without them is all cubes, as it always was.
 */
final class BetterLeaves {
	private BetterLeaves() {}

	private static final String MOD_ID = "better-trees-justfatlard";

	private static final Direction[] HORIZONTALS = {
		Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	/** The stairs cut from these leaves, or null when better-trees is absent or has none for them. */
	static Block stairsFor(BlockState leaves) {
		if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return null;
		String path = BuiltInRegistries.BLOCK.getKey(leaves.getBlock()).getPath();
		if (!path.endsWith("_leaves")) return null;
		String stairs = path.substring(0, path.length() - "_leaves".length()) + "_leaf_stairs";
		return BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(MOD_ID, stairs)).orElse(null);
	}

	/** Where every cell of a form is, so a cell can ask what is beside it. */
	static Set<Long> occupied(List<BonsaiShape.Cell> cells) {
		Set<Long> set = new HashSet<>();
		for (BonsaiShape.Cell cell : cells) set.add(key(cell.x(), cell.y(), cell.z()));
		return set;
	}

	/**
	 * What this leaf cell should be drawn as: a stair on the edge, or null to stay a cube.
	 *
	 * <p>Worked out in the form's own frame and then turned with it, since the form is turned a
	 * quarter at a time about the trunk when it is planted and a stair must face the same way its
	 * neighbours went.
	 */
	static BlockState edge(Block stairs, BlockState leaves, Set<Long> occupied,
			BonsaiShape.Cell cell, int quarterTurns) {
		int open = 0;
		Direction primary = null;
		for (Direction side : HORIZONTALS) {
			if (isOpen(occupied, cell, side)) {
				open++;
				if (primary == null) primary = side;
			}
		}
		if (open == 0) return null;

		boolean openAbove = !occupied.contains(key(cell.x(), cell.y() + 1, cell.z()));
		boolean openBelow = !occupied.contains(key(cell.x(), cell.y() - 1, cell.z()));

		Direction facing;
		StairsShape shape;
		Half half;
		if (open > 2) {
			// The tip of a spray: a stair facing back in along its one attachment, if it has
			// one, as a cap; otherwise nothing to lean on and a cube.
			if (!openAbove || openBelow) return null;
			facing = Direction.NORTH;
			for (Direction side : HORIZONTALS) {
				if (!isOpen(occupied, cell, side)) { facing = side; break; }
			}
			shape = StairsShape.STRAIGHT;
			half = Half.BOTTOM;
		} else {
			if (open == 2 && isOpen(occupied, cell, primary.getOpposite())) return null;
			boolean left = isOpen(occupied, cell, primary.getCounterClockWise());
			boolean right = isOpen(occupied, cell, primary.getClockWise());
			if (left && right) return null;
			facing = primary.getOpposite();
			shape = left ? StairsShape.OUTER_LEFT : right ? StairsShape.OUTER_RIGHT : StairsShape.STRAIGHT;
			half = (!openAbove && openBelow) ? Half.TOP : Half.BOTTOM;
		}

		for (int turn = 0; turn < quarterTurns; turn++) facing = facing.getClockWise();

		BlockState state = stairs.defaultBlockState();
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		if (state.hasProperty(BlockStateProperties.HALF)) state = state.setValue(BlockStateProperties.HALF, half);
		if (state.hasProperty(BlockStateProperties.STAIRS_SHAPE)) state = state.setValue(BlockStateProperties.STAIRS_SHAPE, shape);
		if (state.hasProperty(BlockStateProperties.PERSISTENT) && leaves.hasProperty(BlockStateProperties.PERSISTENT)) {
			state = state.setValue(BlockStateProperties.PERSISTENT, leaves.getValue(BlockStateProperties.PERSISTENT));
		}
		return state;
	}

	private static boolean isOpen(Set<Long> occupied, BonsaiShape.Cell cell, Direction side) {
		return !occupied.contains(key(cell.x() + side.getStepX(), cell.y(), cell.z() + side.getStepZ()));
	}

	private static long key(int x, int y, int z) {
		return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (z + 512);
	}
}
