package justfatlard.bonsai_saplings;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A creeper, grown rather than hatched.
 *
 * <p>A creeper has always been half topiary: a green thing on four legs that turns up in a garden
 * uninvited. Putting the egg in a pot instead of on the ground gets you the shape without the
 * consequences.
 *
 * <p>It is a real creeper, shrunk to pot size, because nothing built out of blocks at this scale
 * read as one. What makes it furniture rather than a mob is everything taken away from it: no
 * mind, no voice, no gravity, no way to hurt it, and no way to light it. The pieces of that are
 * set here; the ones vanilla would put back are held off in the mixins.
 *
 * <p>An empty pot only. A pot with something in it is somebody's tree, and quietly replacing it
 * with a creeper is not what anybody holding a spawn egg over it meant.
 */
public final class CreeperBonsai {
	private CreeperBonsai() {}

	private static final Identifier EGG = Identifier.withDefaultNamespace("creeper_spawn_egg");

	/** Marks the creeper as a topiary, on top of the tag every bonsai piece carries. */
	static final String TAG = "bonsai_creeper";

	/** A creeper is nearly two blocks tall; this brings it down to the height of the trees. */
	private static final double SCALE = 0.4;

	public static void register() {
		UseBlockCallback.EVENT.register(CreeperBonsai::onUseBlock);
	}

	private static InteractionResult onUseBlock(net.minecraft.world.entity.player.Player player,
			Level level, net.minecraft.world.InteractionHand hand,
			net.minecraft.world.phys.BlockHitResult hit) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(eggItem())) return InteractionResult.PASS;

		BlockPos pot = hit.getBlockPos();
		if (!serverLevel.getBlockState(pot).is(Blocks.FLOWER_POT)) return InteractionResult.PASS;
		// An empty pot is empty of trees too. A bonsai stands in an empty pot block now, and it
		// is somebody's tree.
		if (BonsaiTree.has(serverLevel, pot)) return InteractionResult.PASS;

		// Taken before the egg gets its turn, which is the whole reason this is a callback and
		// not a hook on the pot: an egg that reaches its own use has already spawned a creeper.
		if (!plant(serverLevel, pot, serverLevel.getRandom().nextInt(4))) return InteractionResult.PASS;
		stack.consume(1, player);

		serverLevel.playSound(null, pot, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	/** Stand a creeper in this pot, facing one of the four ways. */
	static boolean plant(ServerLevel level, BlockPos pot, int quarterTurns) {
		Creeper creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (creeper == null) return false;

		BonsaiTree.clear(level, pot);
		Vec3 middle = BonsaiTree.middleOf(pot);
		face(creeper, middle, quarterTurns);
		creeper.getAttribute(Attributes.SCALE).setBaseValue(SCALE);
		creeper.setNoAi(true);
		creeper.setSilent(true);
		creeper.setNoGravity(true);
		creeper.setPermanentlyInvulnerable(true);
		creeper.setPersistenceRequired();
		creeper.addTag(BonsaiTree.TAG);
		creeper.addTag(TAG);
		creeper.addTag(BonsaiTree.TURN_TAG + quarterTurns);
		return level.addFreshEntity(creeper);
	}

	/** Shears on a creeper cannot change what it is, so they turn it a quarter instead. */
	public static void turn(ServerLevel level, BlockPos pot) {
		Creeper creeper = BonsaiTree.creeperIn(level, pot);
		if (creeper == null) return;
		int next = (BonsaiTree.turnIn(level, pot) + 1) & 3;
		for (int turn = 0; turn < 4; turn++) creeper.removeTag(BonsaiTree.TURN_TAG + turn);
		creeper.addTag(BonsaiTree.TURN_TAG + next);
		face(creeper, BonsaiTree.middleOf(pot), next);
	}

	private static void face(Creeper creeper, Vec3 at, int quarterTurns) {
		float yaw = quarterTurns * 90F;
		creeper.snapTo(at.x, at.y, at.z, yaw, 0F);
		creeper.setYHeadRot(yaw);
		creeper.setYBodyRot(yaw);
	}

	/** Whether this entity is a potted creeper rather than the kind that walks up behind you. */
	public static boolean isTopiary(Entity entity) {
		return entity instanceof Creeper && entity.entityTags().contains(TAG);
	}

	/** The egg a potted creeper hands back when it is taken out of its pot. */
	public static ItemStack eggStack() {
		return new ItemStack(eggItem());
	}

	/** Looked up rather than imported: Items has no field for a spawn egg on every version. */
	private static Item eggItem() {
		return BuiltInRegistries.ITEM.getValue(EGG);
	}
}
