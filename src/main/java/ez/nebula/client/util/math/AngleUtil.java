package ez.nebula.client.util.math;

import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.TreeMap;

public final class AngleUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static MovingObjectPosition raytrace(final double reach, final float yaw, final float pitch)
    {
        Vec3 var4 = MC.thePlayer.getPosition(1.0f);
        Vec3 var5 = getLookVec(yaw, pitch);
        Vec3 var6 = var4.addVector(var5.xCoord * reach, var5.yCoord * reach, var5.zCoord * reach);
        return MC.theWorld.func_147447_a(var4, var6, false, false, true);
    }

    public static Vec3 getLookVec(final float yaw, final float pitch)
    {
        float var2 = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float var3 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float var4 = -MathHelper.cos(-pitch * 0.017453292F);
        float var5 = MathHelper.sin(-pitch * 0.017453292F);
        return MC.theWorld.getWorldVec3Pool().getVecFromPool(var3 * var4, var5, var2 * var4);
    }

    public static EnumFacing getVisibleFace(final BlockPos pos, final double reach)
    {
        final TreeMap<Double, EnumFacing> faceMap = getVisibleFaces(pos, reach, true);
        if (faceMap.isEmpty())
        {
            return null;
        }
        return faceMap.lastEntry().getValue();
//        final MovingObjectPosition result = raytrace(6.5, angles[0], angles[1]);
//        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
//        {
//            return EnumFacing.faceList[result.sideHit];
//        }
//        // TODO: get best visible faces
//        return BlockUtil.getOpposite(PlayerUtil.getFacing());
    }

    public static TreeMap<Double, EnumFacing> getVisibleFaces(final BlockPos pos, final double reach, final boolean strict)
    {
        Vec3 var4 = MC.thePlayer.getPosition(1.0f);

        final TreeMap<Double, EnumFacing> faceMap = new TreeMap<>();
        for (final EnumFacing facing : EnumFacing.values())
        {
            final BlockPos neighbor = pos.offset(facing);
            if (BlockUtil.isReplaceable(neighbor))
            {
                if (neighbor.getY() > var4.yCoord)
                {
                    continue;
                }

                float[] angles = anglesToBlock(neighbor, ((IEnumFacing)(Object)facing).nebula$getOpposite(), 1.0f);
                Vec3 var5 = getLookVec(angles[0], angles[1]);
                Vec3 var6 = var4.addVector(var5.xCoord * reach, var5.yCoord * reach, var5.zCoord * reach);
                final double distance = var6.distanceTo(Vec3.createVectorHelper(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5));
                // ChatUtil.sendNebula("dist: %.1f", distance);
                if (strict && distance > reach)
                {
                    continue;
                }
                faceMap.put(distance, facing);
            }
        }
        return faceMap;
    }

    public static float[] entityAngles(final Entity entity, final double yOffset, final float partialTicks)
    {
        final Vec3 eyes = MC.thePlayer.getPosition(partialTicks);
        final Vec3 pos = MathUtil.lerpEntity(entity, partialTicks)
                .addVector(0, yOffset, 0);
        final Vec3 vec = pos.subtract(eyes);

        final double dist = vec.lengthVector();
        float yaw = (float) (Math.toDegrees(Math.atan2(vec.zCoord, vec.xCoord)) + 90.0f);
        float pitch = (float) Math.toDegrees(Math.atan2(vec.yCoord, dist));

        return new float[] { yaw, pitch };
    }

    public static float[] anglesToBlock(final BlockPos pos, final EnumFacing face, final float partialTicks)
    {
        final Vec3 eyes = MC.thePlayer.getPosition(partialTicks);

        int offsetX = face.getFrontOffsetX();
        int offsetY = face.getFrontOffsetY();
        int offsetZ = face.getFrontOffsetZ();

        double deltaX = (Math.floor(eyes.xCoord) + 0.5)
                - ((double) pos.getX() + 0.5 - (offsetX * 0.5));
        double deltaZ = (Math.floor(eyes.zCoord) + 0.5)
                - ((double) pos.getZ() + 0.5 - (offsetZ * 0.5));

        double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) + 90.0f);
        float pitch = (float) Math.toDegrees(Math.atan2(
                eyes.yCoord - (pos.getY() - 0.5 - (offsetY * 0.5)), dist));

        return new float[]{ yaw, pitch };
    }
}
