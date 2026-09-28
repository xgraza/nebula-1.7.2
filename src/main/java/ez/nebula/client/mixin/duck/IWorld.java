package ez.nebula.client.mixin.duck;

import net.minecraft.block.Block;
import net.minecraft.src.BlockPos;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IWorld
{
    Block nebula$getBlock(final BlockPos pos);
}
