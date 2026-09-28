package ez.nebula.client.mixin.duck;

import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IEnumFacing
{
    EnumFacing nebula$getOpposite();

    BlockPos nebula$getFaceOffset();
}
