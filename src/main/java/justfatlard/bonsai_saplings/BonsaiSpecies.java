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
public record BonsaiSpecies(BlockState wood, BlockState leaves, BlockState backing, String kind) {
	private static Map<Block, BonsaiSpecies> known;

	/**
	 * The solid block that fills in behind each leaf, named by the leaves it stands behind.
	 *
	 * <p>A leaf block is mostly holes, and at a sixth of a block the front and back faces of a
	 * leaf sit so close that their holes line up and the wall behind shows straight through.
	 * Birch, with the holiest texture and a white trunk behind it, came out looking bleached.
	 * A full-size canopy hides its holes with the shaded leaves behind them, so each leaf gets
	 * a slightly smaller opaque block inside it in the colour of that shade: a dull dark green
	 * for the green leaves, and the nearest plain block for the ones that are not green.
	 */
	private static final Map<String, String> BACKING = Map.of(
		"azalea_leaves", "moss_block",
		"flowering_azalea_leaves", "moss_block",
		"cherry_leaves", "pink_wool",
		"pale_oak_leaves", "light_gray_concrete",
		"yellow_poplar_leaves", "yellow_terracotta",
		"orange_poplar_leaves", "orange_terracotta",
		"red_poplar_leaves", "red_concrete");
	private static final String GREEN_BACKING = "green_concrete";
	/**
	 * Foliage that needs nothing behind it: a mushroom cap or a wart block is solid already,
	 * and a bamboo stalk is a plant with air round it, where a block behind would show.
	 */
	private static final java.util.Set<String> NO_BACKING = java.util.Set.of(
		"bamboo", "red_mushroom_block", "brown_mushroom_block", "nether_wart_block", "warped_wart_block",
		"cactus", "dead_bush", "chorus_flower", "short_grass",
		"wheat", "beetroots", "melon_stem", "pumpkin_stem", "torchflower_crop", "pitcher_crop",
		"sunflower", "lilac", "rose_bush", "peony", "pitcher_plant",
		"vine", "weeping_vines_plant", "cave_vines_plant");
	/**
	 * Foliage drawn at a particular growth rather than freshly planted: a start in a tray is a
	 * seedling, not a seed and not a crop. The age each crop shows its first true leaves at.
	 */
	private static final Map<String, Integer> SPROUT_AGE = Map.of(
		"wheat", 2, "beetroots", 1, "melon_stem", 2, "pumpkin_stem", 2, "torchflower_crop", 1, "pitcher_crop", 1);
	/**
	 * Things that grow in a pot without a potted block of their own: nothing in the game pots a
	 * chorus flower, a handful of grass or a seed, so these are planted straight into an empty
	 * pot and the tree remembers the item it came from. Keyed by the item, as [wood, foliage, kind].
	 */
	private static final Map<String, String[]> POTLESS = Map.ofEntries(
		Map.entry("chorus_flower", new String[] {"chorus_plant", "chorus_flower", "chorus"}),
		Map.entry("short_grass", new String[] {"moss_block", "short_grass", "grass"}),
		Map.entry("wheat_seeds", new String[] {"farmland", "wheat", "starts"}),
		Map.entry("beetroot_seeds", new String[] {"farmland", "beetroots", "starts"}),
		Map.entry("melon_seeds", new String[] {"farmland", "melon_stem", "starts"}),
		Map.entry("pumpkin_seeds", new String[] {"farmland", "pumpkin_stem", "starts"}),
		Map.entry("torchflower_seeds", new String[] {"farmland", "torchflower_crop", "starts"}),
		Map.entry("pitcher_pod", new String[] {"farmland", "pitcher_crop", "starts"}),
		// The tall flowers, which no pot in the game takes: a flower is its two halves, the
		// stalk as the wood and the bloom as the foliage, so a taller stem is more stalk under
		// the same bloom.
		Map.entry("sunflower", new String[] {"sunflower", "sunflower", "tall_flower"}),
		Map.entry("lilac", new String[] {"lilac", "lilac", "tall_flower"}),
		Map.entry("rose_bush", new String[] {"rose_bush", "rose_bush", "tall_flower"}),
		Map.entry("peony", new String[] {"peony", "peony", "tall_flower"}),
		Map.entry("pitcher_plant", new String[] {"pitcher_plant", "pitcher_plant", "tall_flower"}),
		// Trailing plants, grown the way a pothos is: a mound in the pot and strands spilling
		// over the rim. Vines on a bed of moss, weeping vines on crimson nylium, and cave vines,
		// planted from glow berries, carrying their berries.
		Map.entry("vine", new String[] {"moss_block", "vine", "trailing"}),
		Map.entry("weeping_vines", new String[] {"crimson_nylium", "weeping_vines_plant", "trailing"}),
		Map.entry("glow_berries", new String[] {"moss_block", "cave_vines_plant", "trailing"}));
	private static Map<net.minecraft.world.item.Item, BonsaiSpecies> potless;

	/**
	 * The odd ones out: a propagule is a mangrove, an azalea grows on ordinary oak, and a poplar
	 * has no leaves of its own name, only the three autumn colours it turns; it is potted in gold.
	 */
	private static final Map<String, String[]> IRREGULAR = Map.ofEntries(
		Map.entry("potted_poplar_sapling", new String[] {"poplar_log", "yellow_poplar_leaves", "poplar"}),
		// Not trees, but potted and grown here all the same: bamboo as a grove of canes with
		// their fronds, the mushrooms as the huge ones on a stem, the fungi as the huge ones
		// of the nether under a cap of wart.
		Map.entry("potted_bamboo", new String[] {"bamboo_block", "bamboo", "bamboo"}),
		Map.entry("potted_red_mushroom", new String[] {"mushroom_stem", "red_mushroom_block", "red_mushroom"}),
		Map.entry("potted_brown_mushroom", new String[] {"mushroom_stem", "brown_mushroom_block", "brown_mushroom"}),
		Map.entry("potted_crimson_fungus", new String[] {"crimson_stem", "nether_wart_block", "crimson_fungus"}),
		Map.entry("potted_warped_fungus", new String[] {"warped_stem", "warped_wart_block", "warped_fungus"}),
		// A cactus grows into a saguaro, all cactus; a dead bush into deadwood, the bleached
		// trunk of the oldest bonsai style, with the bush itself as its few tufts.
		Map.entry("potted_cactus", new String[] {"cactus", "cactus", "cactus"}),
		Map.entry("potted_dead_bush", new String[] {"stripped_birch_log", "dead_bush", "dead_bush"}),
		Map.entry("potted_mangrove_propagule", new String[] {"mangrove_log", "mangrove_leaves", "mangrove"}),
		Map.entry("potted_azalea_bush", new String[] {"oak_log", "azalea_leaves", "azalea"}),
		Map.entry("potted_flowering_azalea_bush", new String[] {"oak_log", "flowering_azalea_leaves", "flowering_azalea"}));

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

	/** The species this item plants straight into an empty pot, or null for anything else. */
	public static BonsaiSpecies ofItem(net.minecraft.world.item.Item item) {
		if (potless == null) {
			Map<net.minecraft.world.item.Item, BonsaiSpecies> table = new HashMap<>();
			for (Map.Entry<String, String[]> entry : POTLESS.entrySet()) {
				net.minecraft.world.item.Item planted = BuiltInRegistries.ITEM.getValue(
					Identifier.withDefaultNamespace(entry.getKey()));
				BonsaiSpecies species = build(entry.getValue());
				if (planted != null && planted != net.minecraft.world.item.Items.AIR && species != null) {
					table.put(planted, species);
				}
			}
			potless = table;
		}
		return potless.get(item);
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

			BonsaiSpecies species = build(parts);
			if (species != null) table.put(block, species);
		}
		return table;
	}

	/** A species from its [wood, foliage, kind] names, or null when the game lacks either block. */
	private static BonsaiSpecies build(String[] parts) {
		Block wood = byName(parts[0]);
		Block leaves = byName(parts[1]);
		if (wood == null || leaves == null) return null;
		BlockState backing = null;
		if (!NO_BACKING.contains(parts[1])) {
			Block behind = byName(BACKING.getOrDefault(parts[1], GREEN_BACKING));
			backing = (behind == null ? Blocks.MOSS_BLOCK : behind).defaultBlockState();
		}
		BlockState foliage = leaves.defaultBlockState();
		// A vine draws only the faces it clings to. With every side and the top on, a cell of it
		// is a small leafy box, and a column of those is a strand.
		if (leaves == Blocks.VINE) {
			for (var face : new net.minecraft.world.level.block.state.properties.BooleanProperty[] {
					net.minecraft.world.level.block.VineBlock.NORTH, net.minecraft.world.level.block.VineBlock.SOUTH,
					net.minecraft.world.level.block.VineBlock.EAST, net.minecraft.world.level.block.VineBlock.WEST,
					net.minecraft.world.level.block.VineBlock.UP}) {
				foliage = foliage.setValue(face, true);
			}
		}
		// Glow berries are grown for the glow.
		if (foliage.hasProperty(net.minecraft.world.level.block.CaveVines.BERRIES)) {
			foliage = foliage.setValue(net.minecraft.world.level.block.CaveVines.BERRIES, true);
		}
		// A two-block plant is its own wood and foliage: the lower half, and the upper.
		if (wood == leaves && foliage.hasProperty(net.minecraft.world.level.block.DoublePlantBlock.HALF)) {
			foliage = foliage.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
		}
		// A cane's fronds are a property of the stalk, off by default.
		if (leaves == Blocks.BAMBOO) {
			foliage = foliage.setValue(net.minecraft.world.level.block.BambooStalkBlock.LEAVES,
				net.minecraft.world.level.block.state.properties.BambooLeaves.LARGE);
		}
		Integer sprout = SPROUT_AGE.get(parts[1]);
		if (sprout != null) {
			for (var property : foliage.getProperties()) {
				if (property.getName().equals("age")
						&& property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age
						&& age.getPossibleValues().contains(sprout)) {
					foliage = foliage.setValue(age, sprout);
				}
			}
		}
		return new BonsaiSpecies(wood.defaultBlockState(), foliage, backing, parts[2]);
	}

	private static Block byName(String name) {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name));

		return block == null || block == Blocks.AIR ? null : block;
	}
}
