package justfatlard.bonsai_saplings.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;

/** Which blocks a pot will take, which vanilla keeps to itself. */
@Mixin(FlowerPotBlock.class)
public interface FlowerPotAccessor {
	@Accessor("POTTED_BY_CONTENT")
	static Map<Block, Block> pottedByContent() {
		throw new AssertionError();
	}
}
