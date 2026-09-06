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

	/** A pot whose tree has just loaded, waiting for a quiet tick to be brought up to date. */
	private record PotToSettle(ServerLevel level, BlockPos pot) {}

	private static final java.util.Set<PotToSettle> queued = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static final java.util.Queue<PotToSettle> toSettle = new java.util.concurrent.ConcurrentLinkedQueue<>() {
		@Override
		public boolean add(PotToSettle pot) {
			// One entry per pot however many pieces the tree has.
			return queued.add(pot) && super.add(pot);
		}

		@Override
		public PotToSettle poll() {
			PotToSettle pot = super.poll();
			if (pot != null) queued.remove(pot);
			return pot;
		}
	};
	private static final int SETTLE_PER_TICK = 4;

	@Override
	public void onInitialize() {
		// Sneaking shears cut it back, and it comes up as a different form. Taken here rather
		// than left to the block, because a pot has no answer for shears and would let them fall
		// through to whatever the player is standing on. Sneaking, so that every plain click on the
		// pot is vanilla's: an empty hand empties it, a plant is refused, and shears held the
		// ordinary way do what any other item does.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

			BlockPos pot = hit.getBlockPos();
			boolean sapling = BonsaiSpecies.of(serverLevel.getBlockState(pot).getBlock()) != null;
			if (!sapling && !Bonsai.isBonsaiPot(serverLevel, pot)) return InteractionResult.PASS;

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
			if (!stack.is(Items.SHEARS) || !player.isSecondaryUseActive()) return InteractionResult.PASS;
			if (!BonsaiTree.isTree(serverLevel, pot)) return InteractionResult.PASS;

			BonsaiTree.cycle(serverLevel, pot);
			stack.hurtAndBreak(1, (net.minecraft.server.level.ServerPlayer) player,
				net.minecraft.world.entity.EquipmentSlot.MAINHAND);

			serverLevel.playSound(null, pot, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.4F);
			return InteractionResult.SUCCESS;
		});

		// A broken pot takes its tree with it, and gives the sapling back: the pot is empty, so
		// vanilla's drop is the pot alone. BEFORE, so the pot is still there to be recognised.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel serverLevel)) return true;
			if (Bonsai.isBonsaiPot(serverLevel, pos) && !player.isCreative()) {
				ItemStack sapling = Bonsai.saplingIn(serverLevel, pos);
				if (!sapling.isEmpty()) {
					net.minecraft.world.level.block.Block.popResource(serverLevel, pos, sapling);
				}
			}
			BonsaiTree.clear(serverLevel, pos);
			return true;
		});

		// Trees from earlier builds arrive in one of two states: the sapling still drawn up
		// through the trunk, or an empty pot under a tree with no record of its sapling. Nothing
		// put either right until somebody touched the pot. Now a tree's pieces arriving in the
		// world queue their pot, and a later tick settles it: the pot is emptied, the record
		// written from the tree's own leaves.
		//
		// Queued, never done on the spot. Settling means asking what entities stand at the pot
		// and rewriting the block, and doing either inside a chunk's own load - which is where
		// world startup spends its time, twenty-five chunks at once - wedged the server on
		// "Preparing spawn area", twice. A few pots a tick, once their chunk is properly in, is
		// the whole of the cost.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (BonsaiTree.isPiece(entity)) toSettle.add(new PotToSettle(level, BonsaiTree.potOf(entity)));
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (int done = 0; done < SETTLE_PER_TICK; done++) {
				PotToSettle next = toSettle.poll();
				if (next == null) return;
				if (!next.level().isLoaded(next.pot())) continue;
				Bonsai.settle(next.level(), next.pot());
			}
		});

		CreeperBonsai.register();

		System.out.println("[" + MOD_ID + "] Loaded");
	}
}
