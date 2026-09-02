package justfatlard.bonsai_saplings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import justfatlard.bonsai_saplings.Bonsai;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Notices a pot's contents changing, in either direction.
 *
 * <p>At the tail rather than at the head, because what matters is what the pot holds afterwards:
 * the same method both fills an empty pot and empties a full one, and reading the block once it
 * has settled answers both without having to work out which just happened.
 */
@Mixin(FlowerPotBlock.class)
public class FlowerPotMixin {

	@Inject(method = "useItemOn", at = @At("TAIL"))
	private void bonsai$potChanged(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (level instanceof ServerLevel serverLevel) Bonsai.settle(serverLevel, pos);
	}
}
