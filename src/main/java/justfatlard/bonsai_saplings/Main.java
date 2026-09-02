package justfatlard.bonsai_saplings;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Main implements ModInitializer {
	public static final String MOD_ID = "bonsai-saplings-justfatlard";

	@Override
	public void onInitialize() {
		// Shears cut it back, and it comes up as a different form. Taken here rather than left to
		// the block, because a pot has no answer for shears and would let them fall through to
		// whatever the player is standing on.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

			BlockPos pot = hit.getBlockPos();
			BonsaiSpecies species = BonsaiSpecies.of(serverLevel.getBlockState(pot).getBlock());
			if (species == null) return InteractionResult.PASS;

			// Any touch brings the pot up to date, which is the whole of the catching-up story: a
			// pot filled before this was installed, or one whose tree somebody removed, grows one
			// the first time a player reaches for it.
			//
			// This replaced a chunk-load sweep. That sweep spawned entities and searched for
			// entities while chunks were still loading, and wedged world startup so thoroughly
			// that the server never finished booting - twice, once before deferring the work off
			// the callback and once after, which is what settled it: the work does not belong
			// anywhere near chunk loading at all.
			Bonsai.settle(serverLevel, pot);

			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(Items.SHEARS)) return InteractionResult.PASS;
			if (!BonsaiTree.has(serverLevel, pot)) return InteractionResult.PASS;

			BonsaiTree.reshape(serverLevel, pot, species, serverLevel.getRandom());
			stack.hurtAndBreak(1, (net.minecraft.server.level.ServerPlayer) player,
				net.minecraft.world.entity.EquipmentSlot.MAINHAND);

			serverLevel.playSound(null, pot, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.4F);
			return InteractionResult.SUCCESS;
		});

		// A broken pot takes its tree with it. BEFORE, so the pot is still there to be recognised.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel) BonsaiTree.clear(serverLevel, pos);
			return true;
		});

		// Pots that were potted before this was installed, and any whose tree has gone missing.
		// Queued rather than run here. Planting means spawning entities and asking what entities
		// are already nearby, and doing either inside a chunk's own load - which is where world
		// startup spends its time, twenty-five chunks at once - wedged the server on "Preparing
		// spawn area". The queue runs it a moment later, with the chunk settled and its entities
		// present, which is the earliest point either question has a real answer anyway.
		CreeperBonsai.register();

		System.out.println("[" + MOD_ID + "] Loaded");
	}
}
