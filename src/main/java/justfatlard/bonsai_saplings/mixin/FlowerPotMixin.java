package justfatlard.bonsai_saplings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import justfatlard.bonsai_saplings.Bonsai;
import justfatlard.bonsai_saplings.BonsaiTree;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Notices a pot's contents changing, in either direction, and makes a pot with a tree in it
 * behave like the full pot it is.
 *
 * <p>The change is noticed at the tail, because what matters is what the pot holds afterwards:
 * the same method both fills an empty pot and empties a full one, and reading the block once it
 * has settled answers both without having to work out which just happened.
 *
 * <p>The behaving happens at the head. A bonsai stands in an empty pot block - the sapling is
 * taken out of the pot so it stops drawing through the trunk - and vanilla reads an empty pot as
 * something to put a plant in. So a plant offered to a pot with a tree is refused the way a full
 * pot refuses it, and an empty hand takes the tree out and gets the sapling back, the way an
 * empty hand empties any other pot.
 */
@Mixin(FlowerPotBlock.class)
public class FlowerPotMixin {

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void bonsai$fullOfTree(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (!Bonsai.isBonsaiPot(serverLevel, pos)) return;

		Block content = Block.byItem(stack.getItem());
		if (FlowerPotAccessor.pottedByContent().containsKey(content)) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void bonsai$takeTheTree(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (!Bonsai.isBonsaiPot(serverLevel, pos)) return;

		ItemStack sapling = Bonsai.saplingIn(serverLevel, pos);
		if (!sapling.isEmpty() && !player.addItem(sapling)) player.drop(sapling, false, net.minecraft.util.Prediction.PREDICTED);

		BonsaiTree.clear(serverLevel, pos);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Inject(method = "useItemOn", at = @At("TAIL"))
	private void bonsai$potChanged(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (level instanceof ServerLevel serverLevel) Bonsai.settle(serverLevel, pos);
	}
}
