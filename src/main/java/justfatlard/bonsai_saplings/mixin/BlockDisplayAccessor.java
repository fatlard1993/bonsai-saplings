package justfatlard.bonsai_saplings.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * What block a display piece is showing, which vanilla keeps to itself.
 *
 * <p>An invoker on the getter rather than an accessor on a field: the state lives in the
 * entity's synched data, not in a field of its own, and naming a field that is not there
 * failed the mixin at apply and took the server down with it.
 */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
	@Invoker("getBlockState")
	BlockState bonsai$blockState();
}
