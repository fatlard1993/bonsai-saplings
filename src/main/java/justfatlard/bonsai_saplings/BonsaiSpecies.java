package justfatlard.bonsai_saplings;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a potted sapling is made of once it has grown up a bit.
 *
 * <p>Derived from the potted block's own name rather than listed out, because the naming is the
 * one thing about a wood set that never varies: {@code potted_birch_sapling} wants
 * {@code birch_log} and {@code birch_leaves}, and a wood set added next version will too. The
 * three that do not follow the rule are named below, and a species that resolves to neither a log
 * nor leaves is simply not one this mod grows.
 */
public record BonsaiSpecies(BlockState wood, BlockState leaves, String kind) {
	private static Map<Block, BonsaiSpecies> known;

	/**
	 * What a creeper topiary is clipped out of.
	 *
	 * <p>Not a wood set and not derived from one: moss for the green of it, dark prismarine for
	 * the feet and the face, which is as close as vanilla gets to the black a creeper is patched
	 * with without reaching for a block nobody would grow anything in.
	 */
	public static BonsaiSpecies creeper() {
		return new BonsaiSpecies(
			Blocks.DARK_PRISMARINE.defaultBlockState(),
			Blocks.MOSS_BLOCK.defaultBlockState(), "creeper");
	}

	/**
	 * The odd ones out: a propagule is a mangrove, an azalea grows on ordinary oak, and a poplar
	 * has no leaves of its own name, only the three autumn colours it turns; it is potted in gold.
	 */
	private static final Map<String, String[]> IRREGULAR = Map.of(
		"potted_poplar_sapling", new String[] {"poplar_log", "yellow_poplar_leaves", "poplar"},
		"potted_mangrove_propagule", new String[] {"mangrove_log", "mangrove_leaves", "mangrove"},
		"potted_azalea_bush", new String[] {"oak_log", "azalea_leaves", "azalea"},
		"potted_flowering_azalea_bush", new String[] {"oak_log", "flowering_azalea_leaves", "flowering_azalea"});

	/** The potted block whose species wears these leaves, or null. */
	public static Block pottedForLeaves(Block leaves) {
		if (known == null) known = buildTable();
		for (Map.Entry<Block, BonsaiSpecies> entry : known.entrySet()) {
			if (entry.getValue().leaves().getBlock() == leaves) return entry.getKey();
		}
		return null;
	}

	/** The plain sapling whose species is grown on this wood, or null. Only a last resort: azaleas share oak. */
	public static Block pottedForWood(Block wood) {
		if (known == null) known = buildTable();
		Block found = null;
		for (Map.Entry<Block, BonsaiSpecies> entry : known.entrySet()) {
			if (entry.getValue().wood().getBlock() != wood) continue;
			String name = BuiltInRegistries.BLOCK.getKey(entry.getKey()).getPath();
			if (name.endsWith("_sapling")) return entry.getKey();
			if (found == null) found = entry.getKey();
		}
		return found;
	}

	/** The species this potted block grows into, or null if it is not one we do anything with. */
	public static BonsaiSpecies of(Block potted) {
		if (known == null) known = buildTable();

		return known.get(potted);
	}

	private static Map<Block, BonsaiSpecies> buildTable() {
		Map<Block, BonsaiSpecies> table = new HashMap<>();

		for (Block block : BuiltInRegistries.BLOCK) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			if (!name.startsWith("potted_")) continue;

			String[] parts = IRREGULAR.get(name);
			if (parts == null) {
				if (!name.endsWith("_sapling")) continue;

				String species = name.substring("potted_".length(), name.length() - "_sapling".length());
				parts = new String[] {species + "_log", species + "_leaves", species};
			}

			Block wood = byName(parts[0]);
			Block leaves = byName(parts[1]);
			if (wood == null || leaves == null) continue;

			table.put(block, new BonsaiSpecies(wood.defaultBlockState(), leaves.defaultBlockState(), parts[2]));
		}
		return table;
	}

	private static Block byName(String name) {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name));

		return block == null || block == Blocks.AIR ? null : block;
	}
}
