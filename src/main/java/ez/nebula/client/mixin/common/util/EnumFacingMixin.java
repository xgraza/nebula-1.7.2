package ez.nebula.client.mixin.common.util;

import ez.nebula.client.mixin.duck.IEnumFacing;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/23/26
 */
@Mixin(value = EnumFacing.class)
public final class EnumFacingMixin implements IEnumFacing
{
    @Shadow @Final private int order_b;
    @Shadow @Final private int frontOffsetX;
    @Shadow @Final private int frontOffsetY;
    @Shadow @Final private int frontOffsetZ;

    @Override
    public EnumFacing nebula$getOpposite()
    {
        return EnumFacing.values()[order_b];
    }

    @Override public BlockPos nebula$getFaceOffset()
    {
        return new BlockPos(frontOffsetX, frontOffsetY, frontOffsetZ);
    }
}
