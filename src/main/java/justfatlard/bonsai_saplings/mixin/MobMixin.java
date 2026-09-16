package justfatlard.bonsai_saplings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import justfatlard.bonsai_saplings.BonsaiTree;
import justfatlard.bonsai_saplings.CreeperBonsai;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a potted creeper a piece of furniture.
 *
 * <p>Two things vanilla does to every mob would undo it. Peaceful difficulty sweeps monsters away
 * each tick, and a topiary that vanishes when the difficulty changes is a topiary somebody loses.
 * And a right-click on a mob is where flint and steel light a creeper, a lead clips on, and a name
 * tag names it - none of which a hedge should answer to. The clicks it does answer are the pot's
 * own, because the creeper stands over the pot and that is where most clicks at the pot land: an
 * empty hand takes it out and hands the egg back, and sneaking shears turn it.
 */
@Mixin(Mob.class)
public class MobMixin {

	@Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
	private void bonsai$keepTheTopiary(CallbackInfo ci) {
		if (CreeperBonsai.isTopiary((Mob) (Object) this)) ci.cancel();
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void bonsai$handsOffTheTopiary(Player player, InteractionHand hand, Vec3 at,
			CallbackInfoReturnable<InteractionResult> cir) {
		Mob self = (Mob) (Object) this;
		if (!CreeperBonsai.isTopiary(self)) return;

		if (self.level() instanceof ServerLevel level) {
			net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
			if (stack.isEmpty()) {
				BonsaiTree.clear(level, BonsaiTree.potOf(self));
				net.minecraft.world.item.ItemStack egg = CreeperBonsai.eggStack();
				if (!player.addItem(egg)) player.drop(egg, false, net.minecraft.util.Prediction.PREDICTED);
				cir.setReturnValue(InteractionResult.SUCCESS);
				return;
			}
			if (stack.is(net.minecraft.world.item.Items.SHEARS) && player.isSecondaryUseActive()) {
				CreeperBonsai.turn(level, BonsaiTree.potOf(self));
				cir.setReturnValue(InteractionResult.SUCCESS);
				return;
			}
		}
		cir.setReturnValue(InteractionResult.PASS);
	}
}
