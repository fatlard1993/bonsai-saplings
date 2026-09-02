package justfatlard.bonsai_saplings;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * A creeper, grown rather than hatched.
 *
 * <p>A creeper has always been half topiary: a green thing on four legs that turns up in a garden
 * uninvited. Putting the egg in a pot instead of on the ground gets you the shape without the
 * consequences.
 *
 * <p>An empty pot only. A pot with something in it is somebody's tree, and quietly replacing it
 * with a creeper is not what anybody holding a spawn egg over it meant.
 */
public final class CreeperBonsai {
	private CreeperBonsai() {}

	private static final Identifier EGG = Identifier.withDefaultNamespace("creeper_spawn_egg");

	public static void register() {
		UseBlockCallback.EVENT.register(CreeperBonsai::onUseBlock);
	}

	private static InteractionResult onUseBlock(net.minecraft.world.entity.player.Player player,
			Level level, net.minecraft.world.InteractionHand hand,
			net.minecraft.world.phys.BlockHitResult hit) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(egg())) return InteractionResult.PASS;

		BlockPos pot = hit.getBlockPos();
		if (!serverLevel.getBlockState(pot).is(Blocks.FLOWER_POT)) return InteractionResult.PASS;

		// Taken before the egg gets its turn, which is the whole reason this is a callback and
		// not a hook on the pot: an egg that reaches its own use has already spawned a creeper.
		BonsaiTree.plant(serverLevel, pot, BonsaiSpecies.creeper(), BonsaiShape.CREEPER,
			serverLevel.getRandom().nextInt(4));
		stack.consume(1, player);

		serverLevel.playSound(null, pot, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	/** Looked up rather than imported: Items has no field for a spawn egg on every version. */
	private static Item egg() {
		return BuiltInRegistries.ITEM.getValue(EGG);
	}
}
