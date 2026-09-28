package ez.nebula.client.util.minecraft.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

/**
 * @author xgraza
 * @since 03/24/25
 */
public final class PlayerUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static final EnumFacing[] FACINGS = {
            EnumFacing.SOUTH,
            EnumFacing.WEST,
            EnumFacing.NORTH,
            EnumFacing.EAST };

    public static EnumFacing getFacing()
    {
        int var25 = MathHelper.floor_double(MC.thePlayer.rotationYaw / 90.0D + 0.5D) & 3;
        return FACINGS[var25 % FACINGS.length];
    }

    public static BlockPos getOrigin(final EntityLivingBase entity)
    {
        return new BlockPos(MathHelper.floor_double(entity.posX),
                MathHelper.floor_double(entity.boundingBox.minY),
                MathHelper.floor_double(entity.posZ));
    }

    public static BlockPos getOrigin(final double posY)
    {
        return new BlockPos(MathHelper.floor_double(MC.thePlayer.posX),
                (int) posY,
                MathHelper.floor_double(MC.thePlayer.posZ));
    }

    public static BlockPos getOrigin()
    {
        return new BlockPos(MathHelper.floor_double(MC.thePlayer.posX),
                MathHelper.floor_double(MC.thePlayer.boundingBox.minY),
                MathHelper.floor_double(MC.thePlayer.posZ));
    }

    public static boolean isAboveWater()
    {
        if (MC.thePlayer.isInWater())
        {
            return false;
        }
        for (double y = 0.0; y <= 1.0; y += 0.1)
        {
            final Block block = MC.theWorld.getBlock(
                    (int) Math.floor(MC.thePlayer.posX),
                    (int) Math.floor(MC.thePlayer.boundingBox.minY - y),
                    (int) Math.floor(MC.thePlayer.posZ));
            if (block instanceof BlockLiquid)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isPhased()
    {
        return !MC.theWorld.getCollidingBoundingBoxes(MC.thePlayer, MC.thePlayer.boundingBox.copy().expand(-0.0625, -0.0625, -0.0625)).isEmpty();
    }

    public static boolean isPlayerCollided(final BlockPos pos)
    {
        return isPlayerCollided(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isPlayerCollided(final int x, final int y, final int z)
    {
        final AxisAlignedBB aabb = MC.thePlayer.boundingBox.copy().expand(0.0625, 0, 0.0625);
        final AxisAlignedBB aabb2 = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        return aabb.intersectsWith(aabb2);
    }
}
