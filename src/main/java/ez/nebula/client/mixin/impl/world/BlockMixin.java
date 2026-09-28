package ez.nebula.client.mixin.impl.world;

import ez.nebula.client.mixin.duck.IBlock;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = Block.class)
public class BlockMixin implements IBlock
{
    @Shadow protected float blockHardness;

    @Override
    public float nebula$getBlockHardness()
    {
        return blockHardness;
    }
}
