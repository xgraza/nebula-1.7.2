package ez.nebula.client.mixin.impl.world;

import ez.nebula.client.mixin.duck.IWorld;
import net.minecraft.block.Block;
import net.minecraft.src.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = World.class)
public abstract class WorldMixin implements IWorld
{
    @Shadow public abstract Block getBlock(int x, int y, int z);

    @Override
    public Block nebula$getBlock(BlockPos pos)
    {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }
}
