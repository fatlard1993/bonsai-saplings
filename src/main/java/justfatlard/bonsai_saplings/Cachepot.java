package justfatlard.bonsai_saplings;

import justfatlard.bonsai_saplings.mixin.FlowerPotAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A flower pot set inside a decorated pot: a cachepot, the way a potted plant is dressed up
 * indoors, and a bigger home for a bonsai.
 *
 * <p>The flower pot goes in the vanilla way - a decorated pot takes one stack of anything - and
 * from then on the decorated pot is planted as a flower pot is, with the same saplings and the
 * same potless plants. What grows there has room the flower pot never had: the two larger forms
 * of every kind are grown here, and only here. The flower pot stays inside through all of it;
 * taking the plant out hands the plant back.
 */
public final class Cachepot {
	private Cachepot() {}

	public static boolean isVessel(BlockState state) {
		return state.is(Blocks.DECORATED_POT);
	}

	/**
	 * Whether a tree in this pot has a cachepot's room: it is a cachepot, or a flower pot standing
	 * on a decorated pot. From outside they look alike, since a flower pot inside a decorated pot is
	 * not drawn at all.
	 */
	public static boolean growsLarge(ServerLevel level, BlockPos pot) {
		return isVessel(level.getBlockState(pot)) || isVessel(level.getBlockState(pot.below()));
	}

	/** Whether this decorated pot has a flower pot in it, which is what makes it a cachepot. */
	public static boolean holdsPot(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof DecoratedPotBlockEntity pot && pot.getTheItem().is(Items.FLOWER_POT);
	}

	/** A click on a decorated pot. PASS for anything that is not cachepot business, so the pot does what it always did. */
	public static InteractionResult use(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
		if (!holdsPot(level, pos)) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);

		if (BonsaiTree.has(level, pos)) {
			if (stack.is(Items.SHEARS) && player.isSecondaryUseActive()) {
				BonsaiTree.cycle(level, pos);
				stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
				level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.4F);
				return InteractionResult.SUCCESS;
			}
			if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
				ItemStack plant = Bonsai.saplingIn(level, pos);
				if (!plant.isEmpty() && !player.addItem(plant)) player.drop(plant, false, net.minecraft.util.Prediction.PREDICTED);
				BonsaiTree.clear(level, pos);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		if (stack.isEmpty()) return InteractionResult.PASS;
		Block potted = FlowerPotAccessor.pottedByContent().get(Block.byItem(stack.getItem()));
		BonsaiSpecies species = potted == null ? null : BonsaiSpecies.of(potted);
		BonsaiSpecies potless = species == null ? BonsaiSpecies.ofItem(stack.getItem()) : null;
		if (species == null && potless == null) return InteractionResult.PASS;

		java.util.List<BonsaiShape> forms = BonsaiShape.large((species != null ? species : potless).kind());
		BonsaiShape shape = forms.get(level.getRandom().nextInt(forms.size()));
		int turn = level.getRandom().nextInt(4);
		if (species != null) {
			BonsaiTree.plant(level, pos, species, shape, turn, potted);
			BonsaiTree.remember(level, pos, potted);
		} else {
			BonsaiTree.plantFromItem(level, pos, stack.getItem(), potless, shape, turn);
		}
		if (!player.getAbilities().instabuild) stack.shrink(1);
		level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}
}
