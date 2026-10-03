package ez.nebula.client.mixin.common.phys;

import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = AxisAlignedBB.class)
public class AxisAlignedBBMixin implements IAxisAlignedBB
{
    @Shadow public double minX;
    @Shadow public double maxX;
    @Shadow public double minY;
    @Shadow public double maxY;
    @Shadow public double minZ;
    @Shadow public double maxZ;

    @Override
    public Vec3 nebula$getCenter()
    {
        return Vec3.createVectorHelper(minX + (maxX - minX) * 0.5D,
                minY + (maxY - minY) * 0.5D, minZ + (maxZ - minZ) * 0.5D);
    }
}
