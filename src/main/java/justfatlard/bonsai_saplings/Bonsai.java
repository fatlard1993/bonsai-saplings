package justfatlard.bonsai_saplings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Keeping what is in a pot and what is standing in it saying the same thing. */
import net.minecraft.world.item.ItemStack;

public final class Bonsai {
	private Bonsai() {}

	/**
	 * Make the tree match the pot, whatever just happened to either.
	 *
	 * <p>One entry point for filling, emptying and breaking, because all three are the same
	 * question asked at different moments: is there a sapling in this pot, and is there a tree
	 * standing in it? Anything that can change either answer can call this and be right.
	 */
	public static void settle(ServerLevel level, BlockPos pot) {
		// A creeper stands in a plain flower pot with no sapling to record, so the checks below
		// would read it as a tree with no record and try to work out what it grew from. It answers
		// to nothing here: it was planted deliberately and it leaves when the pot does.
		if (BonsaiTree.isCreeper(level, pot)) return;

		BlockState state = level.getBlockState(pot);

		// A cachepot keeps its own record from the moment it is planted, so there is nothing to
		// catch up on. What can go wrong is the flower pot leaving it - a hopper under a decorated
		// pot takes what is inside - and without it the pot is a pot again: the plant is handed
		// back where it stands and the tree goes.
		if (Cachepot.isVessel(state)) {
			if (BonsaiTree.has(level, pot) && !Cachepot.holdsPot(level, pot)) {
				ItemStack plant = saplingIn(level, pot);
				if (!plant.isEmpty()) Block.popResource(level, pot.above(), plant);
				BonsaiTree.clear(level, pot);
			}
			return;
		}

		Block potted = state.getBlock();
		BonsaiSpecies species = BonsaiSpecies.of(potted);

		if (species != null) {
			// A sapling in the pot: grow it, and take the sapling itself out of sight. The
			// potted block draws a sapling, and a sapling poking up through the trunk is the one
			// thing that stops a bonsai reading as a tree. The tree keeps which sapling it was,
			// and hands it back when the pot is emptied or broken.
			if (BonsaiTree.has(level, pot)) {
				BonsaiTree.remember(level, pot, potted);
			} else {
				java.util.List<BonsaiShape> forms = Cachepot.growsLarge(level, pot)
					? BonsaiShape.large(species.kind()) : BonsaiShape.family(species.kind());
				BonsaiTree.plant(level, pot, species,
					forms.get(level.getRandom().nextInt(forms.size())),
					level.getRandom().nextInt(4), potted);
			}
			level.setBlock(pot, Blocks.FLOWER_POT.defaultBlockState(), Block.UPDATE_ALL);
			return;
		}

		// An empty pot with a tree that knows its sapling is the settled state of a bonsai. One
		// whose tree does not know - planted before the record existed - is told, from the tree's
		// own leaves. It used to be cleared as junk, which made the first touch of any kind the
		// end of every tree from the earlier build.
		if (state.is(Blocks.FLOWER_POT) && BonsaiTree.has(level, pot)) {
			if (BonsaiTree.pottedIn(level, pot) != null || BonsaiTree.itemIn(level, pot) != null) return;
			Block inferred = BonsaiTree.inferPotted(level, pot);
			if (inferred != null) {
				BonsaiTree.remember(level, pot, inferred);
				return;
			}
		}

		BonsaiTree.clear(level, pot);
	}

	/**
	 * Whether this is an empty pot with something standing in it, tree or creeper: a bonsai, as
	 * the player sees it, and a full pot as far as vanilla's pot behaviour is concerned.
	 */
	/**
	 * Plant something the game has no potted block for straight into an empty pot: a chorus
	 * flower, a handful of grass, a seed. True when it was, and the item has been spent.
	 */
	public static boolean plantItem(ServerLevel level, BlockPos pot, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.item.ItemStack stack) {
		if (!level.getBlockState(pot).is(Blocks.FLOWER_POT) || BonsaiTree.has(level, pot)) return false;
		BonsaiSpecies species = BonsaiSpecies.ofItem(stack.getItem());
		if (species == null) return false;
		java.util.List<BonsaiShape> forms = Cachepot.growsLarge(level, pot)
			? BonsaiShape.large(species.kind()) : BonsaiShape.family(species.kind());
		BonsaiTree.plantFromItem(level, pot, stack.getItem(), species,
			forms.get(level.getRandom().nextInt(forms.size())), level.getRandom().nextInt(4));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return true;
	}

	public static boolean isBonsaiPot(ServerLevel level, BlockPos pot) {
		return level.getBlockState(pot).is(Blocks.FLOWER_POT) && BonsaiTree.has(level, pot);
	}

	/**
	 * What this pot's occupant was planted from - the sapling, or the egg for a creeper - or an
	 * empty stack when nothing recorded it.
	 */
	public static net.minecraft.world.item.ItemStack saplingIn(ServerLevel level, BlockPos pot) {
		if (BonsaiTree.isCreeper(level, pot)) return CreeperBonsai.eggStack();
		Block potted = BonsaiTree.pottedIn(level, pot);
		if (potted instanceof net.minecraft.world.level.block.FlowerPotBlock flowerPot) {
			return new net.minecraft.world.item.ItemStack(flowerPot.getPotted());
		}
		net.minecraft.world.item.Item item = BonsaiTree.itemIn(level, pot);
		return item == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item);
	}
}
