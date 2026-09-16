package justfatlard.bonsai_saplings.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A sapling planted in a cachepot grows one of the larger forms, and the shears reach both larger
 * forms from there. Planted the way a player plants: a click on the pot, through the server's own
 * use-item-on path and every callback in front of it.
 */
public final class CachepotForms implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");

			BlockPos pot = server.computeOnServer(s -> connection.getServerPlayer().blockPosition().east(2));
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				level.setBlockAndUpdate(pot.below(), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(pot, Blocks.DECORATED_POT.defaultBlockState());
				((DecoratedPotBlockEntity) level.getBlockEntity(pot)).setTheItem(new ItemStack(Items.FLOWER_POT));
			});

			InteractionResult planted = click(server, connection, pot, new ItemStack(Items.OAK_SAPLING), false);
			context.waitTicks(5);
			String grown = server.computeOnServer(s -> describe(s.overworld(), pot));
			check(planted.consumesAction(), "planting in the cachepot was not taken: " + planted + "; pot holds " + grown);
			check(grown.contains("cachepot"), "the tree is not a cachepot's: " + grown);
			check(grown.contains("oak_grand") || grown.contains("oak_twin"), "a cachepot grew a small form: " + grown);

			Set<String> forms = new LinkedHashSet<>();
			for (int cut = 0; cut < 24; cut++) {
				click(server, connection, pot, new ItemStack(Items.SHEARS), true);
				context.waitTick();
				forms.add(server.computeOnServer(s -> describe(s.overworld(), pot)));
			}
			String seen = String.join(" | ", forms);
			check(seen.contains("oak_grand") && seen.contains("oak_twin"), "the shears did not reach both larger forms: " + seen);

			// A flower pot standing on a decorated pot has the same room.
			BlockPos onTop = pot.east(3).above();
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				level.setBlockAndUpdate(onTop.below(2), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(onTop.below(), Blocks.DECORATED_POT.defaultBlockState());
				level.setBlockAndUpdate(onTop, Blocks.FLOWER_POT.defaultBlockState());
			});
			click(server, connection, onTop, new ItemStack(Items.OAK_SAPLING), false);
			context.waitTicks(10);
			click(server, connection, onTop, ItemStack.EMPTY, true);
			context.waitTicks(5);
			String raised = server.computeOnServer(s -> describe(s.overworld(), onTop));
			check(raised.contains("oak_grand") || raised.contains("oak_twin"),
				"a flower pot on a decorated pot grew a small form: " + raised);
		}
	}

	private static InteractionResult click(TestServerContext server, TestServerConnection connection, BlockPos pot,
										   ItemStack held, boolean sneaking) {
		return server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			player.setShiftKeyDown(sneaking);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pot).add(0, 0.4, 0), Direction.UP, pot, false);
			InteractionResult result = player.gameMode.useItemOn(player, s.overworld(), held, InteractionHand.MAIN_HAND, hit);
			player.setShiftKeyDown(false);
			return result;
		});
	}

	/** The form and pot tags of the tree standing in this pot, or "nothing". */
	private static String describe(ServerLevel level, BlockPos pot) {
		Set<String> tags = new LinkedHashSet<>();
		for (Entity piece : level.getEntitiesOfClass(Entity.class, new AABB(pot).inflate(0, 2, 0),
				e -> e.entityTags().contains("bonsai_tree"))) {
			for (String tag : piece.entityTags()) {
				if (tag.startsWith("bonsai_shape_")) tags.add(tag.substring("bonsai_shape_".length()));
				if (tag.equals("bonsai_cachepot")) tags.add("cachepot");
			}
		}
		return tags.isEmpty() ? "nothing" : String.join(",", tags);
	}

	private static void check(boolean holds, String otherwise) {
		if (!holds) throw new AssertionError(otherwise);
	}
}
