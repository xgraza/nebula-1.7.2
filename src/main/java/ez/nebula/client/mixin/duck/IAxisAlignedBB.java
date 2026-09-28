package ez.nebula.client.mixin.duck;

import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IAxisAlignedBB
{
    Vec3 nebula$getCenter();

    static AxisAlignedBB create(final Vec3 vec, final double offset)
    {
        return AxisAlignedBB.getBoundingBox(vec.xCoord, vec.yCoord, vec.zCoord,
                vec.xCoord + offset, vec.yCoord + offset, vec.zCoord + offset);
    }

    static AxisAlignedBB create(final BlockPos pos)
    {
        return AxisAlignedBB.getBoundingBox(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
