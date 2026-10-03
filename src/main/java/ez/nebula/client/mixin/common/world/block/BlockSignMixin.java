package ez.nebula.client.mixin.common.world.block;

import ez.nebula.client.mixin.duck.IBlockSign;
import net.minecraft.block.BlockSign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = BlockSign.class)
public class BlockSignMixin implements IBlockSign
{
    @Shadow private boolean field_149967_b;

    @Override
    public boolean nebula$isStanding()
    {
        return field_149967_b;
    }
}
