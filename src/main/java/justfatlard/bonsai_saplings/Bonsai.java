package justfatlard.bonsai_saplings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Keeping what is in a pot and what is standing in it saying the same thing. */
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
		// A creeper stands in a plain flower pot, so every check below would read it as a tree in
		// an empty pot and clear it away. It answers to nothing here: it was planted deliberately
		// and it leaves when the pot does.
		if (BonsaiTree.isCreeper(level, pot)) return;

		BlockState state = level.getBlockState(pot);
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
				java.util.List<BonsaiShape> forms = BonsaiShape.family(species.kind());
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
			if (BonsaiTree.pottedIn(level, pot) != null) return;
			Block inferred = BonsaiTree.inferPotted(level, pot);
			if (inferred != null) {
				BonsaiTree.remember(level, pot, inferred);
				return;
			}
		}

		BonsaiTree.clear(level, pot);
	}

	/** Whether this is an empty pot with a tree standing in it: a bonsai, as the player sees it. */
	public static boolean isBonsaiPot(ServerLevel level, BlockPos pot) {
		return level.getBlockState(pot).is(Blocks.FLOWER_POT) && BonsaiTree.isTree(level, pot);
	}

	/** The sapling this pot's tree grew from, or an empty stack when nothing recorded it. */
	public static net.minecraft.world.item.ItemStack saplingIn(ServerLevel level, BlockPos pot) {
		Block potted = BonsaiTree.pottedIn(level, pot);
		if (!(potted instanceof net.minecraft.world.level.block.FlowerPotBlock flowerPot)) {
			return net.minecraft.world.item.ItemStack.EMPTY;
		}
		return new net.minecraft.world.item.ItemStack(flowerPot.getPotted());
	}
}
