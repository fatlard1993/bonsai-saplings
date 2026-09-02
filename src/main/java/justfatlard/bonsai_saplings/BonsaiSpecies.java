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
public record BonsaiSpecies(BlockState wood, BlockState leaves) {
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
			Blocks.MOSS_BLOCK.defaultBlockState());
	}

	/** The odd ones out: a propagule is a mangrove, and an azalea grows on ordinary oak. */
	private static final Map<String, String[]> IRREGULAR = Map.of(
		"potted_mangrove_propagule", new String[] {"mangrove_log", "mangrove_leaves"},
		"potted_azalea_bush", new String[] {"oak_log", "azalea_leaves"},
		"potted_flowering_azalea_bush", new String[] {"oak_log", "flowering_azalea_leaves"});

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
				parts = new String[] {species + "_log", species + "_leaves"};
			}

			Block wood = byName(parts[0]);
			Block leaves = byName(parts[1]);
			if (wood == null || leaves == null) continue;

			table.put(block, new BonsaiSpecies(wood.defaultBlockState(), leaves.defaultBlockState()));
		}
		return table;
	}

	private static Block byName(String name) {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name));

		return block == null || block == Blocks.AIR ? null : block;
	}
}
