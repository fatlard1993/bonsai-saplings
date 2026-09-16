package justfatlard.bonsai_saplings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import justfatlard.bonsai_saplings.CreeperBonsai;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/**
 * A potted creeper is not a monster nearby.
 *
 * <p>Every monster within a few blocks of a bed refuses the player their sleep, and the whole
 * point of a creeper in a pot is to have one on the windowsill.
 */
@Mixin(Monster.class)
public class MonsterMixin {

	@Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
	private void bonsai$sleepBesideTheTopiary(ServerLevel level, Player player,
			CallbackInfoReturnable<Boolean> cir) {
		if (CreeperBonsai.isTopiary((Monster) (Object) this)) cir.setReturnValue(false);
	}
}
