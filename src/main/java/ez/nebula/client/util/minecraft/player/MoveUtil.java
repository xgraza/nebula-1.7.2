package ez.nebula.client.util.minecraft.player;

import ez.nebula.client.api.listener.event.player.EventMove;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;

/**
 * @author xgraza
 * @since 03/17/25
 */
public final class MoveUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    private static final double[] NULL_VELOCITY = new double[2];
    public static final double NCP_BASE_SPEED = 0.2873f;

    public static double getPlayerMoveDistance()
    {
        final double deltaX = MC.thePlayer.posX - MC.thePlayer.lastTickPosX;
        final double deltaZ = MC.thePlayer.posZ - MC.thePlayer.lastTickPosZ;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    public static double getPlayerSpeed()
    {
        return Math.sqrt(MC.thePlayer.motionX * MC.thePlayer.motionX
                + MC.thePlayer.motionZ * MC.thePlayer.motionZ);
    }

    public static void setSpeed(final EventMove event, final double moveSpeed)
    {
        final double[] strafe = getStrafeMotion(moveSpeed);
        if (event != null)
        {
            event.setX(strafe[0]);
            event.setZ(strafe[1]);
        } else
        {
            MC.thePlayer.motionX = strafe[0];
            MC.thePlayer.motionZ = strafe[1];
        }
    }

    public static double[] getStrafeMotion(final float angle, final double moveSpeed)
    {
        if (moveSpeed <= 0.0)
        {
            return NULL_VELOCITY;
        }
        return new double[]{ -Math.sin(angle) * moveSpeed, Math.cos(angle) * moveSpeed };
    }

    public static double[] getStrafeMotion(final double moveSpeed)
    {
        final float angle = getDirectionRadians(MC.thePlayer, MC.thePlayer.rotationYaw);
        return getStrafeMotion(angle, moveSpeed);
    }

    public static float getDirectionYaw(final EntityPlayer player, float yaw)
    {
        // if we're moving backwards, reverse our yaw
        if (player.moveForward < 0.0f)
        {
            yaw -= 180.0f;
        }

        // this is for handling holding forward & strafing side to side at the same time
        float forward = player.moveForward * 0.5f;
        if (forward == 0.0f)
        {
            forward = 1.0f;
        }

        float strafe = player.moveStrafing;
        if (strafe > 0.0f)
        {
            yaw -= 90.0f * forward;
        } else if (strafe < 0.0f)
        {
            yaw += 90.0f * forward;
        }

        return yaw;
    }

    public static float getDirectionRadians(final EntityPlayer player, final float yaw)
    {
        return getDirectionYaw(player, yaw) * 0.017453292f;
    }

    public static float[] getXZMoveToVec(final Vec3 vector)
    {
        final Vec3 normalized = Vec3.createVectorHelper(vector.xCoord - MC.thePlayer.posX,
                vector.yCoord - MC.thePlayer.boundingBox.minY,
                vector.zCoord - MC.thePlayer.posZ).normalize();

        final double yaw = MC.thePlayer.rotationYaw * (Math.PI / 180.0f);
        double forward = normalized.dotProduct(Vec3.createVectorHelper(-Math.sin(yaw), 0, Math.cos(yaw)));
        double strafe = normalized.dotProduct(Vec3.createVectorHelper(Math.cos(yaw), 0, Math.sin(yaw)));

        final double length = Math.sqrt(forward + forward * strafe * strafe);
        if (length > 1.0)
        {
            forward /= length;
            strafe /= length;
        }

        return new float[]{ (float) forward, (float) strafe };
    }

    public static double getBaseNcpSpeed(final int minPotionTime)
    {
        double speed = NCP_BASE_SPEED;
        if (MC.thePlayer.isPotionActive(Potion.moveSpeed.id))
        {
            final PotionEffect effect = MC.thePlayer.getActivePotionEffect(Potion.moveSpeed);
            if (effect.getDuration() > minPotionTime * 20)
            {
                speed *= 1.0 + (0.2 * (effect.getAmplifier() + 1));
            }
        }
        if (MC.thePlayer.isPotionActive(Potion.moveSlowdown.id))
        {
            final PotionEffect effect = MC.thePlayer.getActivePotionEffect(Potion.moveSlowdown);
            if (effect.getDuration() > minPotionTime * 20)
            {
                speed /= 1.0 + (0.2 * (effect.getAmplifier() + 1));
            }
        }
        return speed;
    }

    public static double getJumpHeight(double base)
    {
        if (MC.thePlayer.isPotionActive(Potion.jump.id))
        {
            base += (MC.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1.0) * 0.1;
        }
        return base;
    }

    public static boolean isMoving()
    {
        return MC.thePlayer.movementInput.moveForward != 0.0f
                || MC.thePlayer.movementInput.moveStrafe != 0.0f;
    }
}
