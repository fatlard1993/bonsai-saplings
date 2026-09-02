package justfatlard.bonsai_saplings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
		BonsaiSpecies species = BonsaiSpecies.of(state.getBlock());

		if (species == null) {
			BonsaiTree.clear(level, pot);
			return;
		}

		if (BonsaiTree.has(level, pot)) return;

		BonsaiTree.plant(level, pot, species,
			BonsaiShape.ALL.get(level.getRandom().nextInt(BonsaiShape.ALL.size())),
			level.getRandom().nextInt(4));
	}
}
