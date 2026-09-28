package ez.nebula.client.api.player.server;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventMoveUpdate;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.api.manager.IManager;
import ez.nebula.client.mixin.duck.IEntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author xgraza
 * @since 03/22/25
 */
public final class RotationManager implements IManager
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    /**
     * Client variable, if to make sure rotations are within bounds before sending to the server
     */
    private static final boolean ROTATE_PROTECTION = true;

    private final float[] serverAngles = new float[2];
    private final float[] spoofedAngles = { Float.NaN, Float.NaN };
    private int spoofPrority = -1;

    private final Queue<Rotation> queuedRotations = new ConcurrentLinkedQueue<>();
    private Rotation polledRot;
    private boolean didPushQueued;

    @Subscribe
    private final EventListener<EventMoveUpdate> moveUpdateEventListener = event ->
    {
        if (!queuedRotations.isEmpty() && polledRot == null)
        {
            polledRot = queuedRotations.poll();
        }

        if (polledRot != null)
        {
            didPushQueued = polledRot.priority >= spoofPrority && isRotationValid(polledRot.yaw, polledRot.pitch);
            if (didPushQueued)
            {
                event.setYaw(polledRot.yaw);
                event.setPitch(polledRot.pitch);
            }
        } else
        {
            didPushQueued = false;
        }

        if (isRotationValid(spoofedAngles) && !didPushQueued)
        {
            event.setYaw(spoofedAngles[0]);
            event.setPitch(spoofedAngles[1]);
            setInvalid(spoofedAngles);
        }
        setRenderAngles();
    };

    @Subscribe
    private final EventListener<EventMoveUpdate.Post> postEventListener = event ->
    {
        if (didPushQueued && polledRot != null)
        {
            didPushQueued = false;
            // if the C03 was sent and our server angles actually reflect these requested angles
            if (polledRot.yaw == serverAngles[0] && polledRot.pitch == serverAngles[1])
            {
                polledRot.invoke();
                polledRot = null;
            }
        }
    };

    @Subscribe
    private final EventListener<EventChangeWorld> changeWorldEventListener = event ->
    {
        setInvalid(spoofedAngles);
        queuedRotations.clear();
        polledRot = null;
    };

    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C03PacketPlayer)
        {
            final C03PacketPlayer packet = event.getPacket();
            if (packet.func_149463_k())
            {
                serverAngles[0] = packet.func_149462_g();
                serverAngles[1] = packet.func_149470_h();
            }
        }
    };

    @Override
    public void init()
    {
        EventBus.subscribe(this);
    }

    /**
     * Checks if a rotation can override another currently spoofed rotation
     * @param priority the requesting priority
     * @return if the requesting priority can override any current spoofed rotations
     */
    public boolean canTakePrecedent(final int priority)
    {
        return spoofPrority == -1 || priority > spoofPrority;
    }

    /**
     * Queues a rotation to be spoofed with a callback once sent
     * @param angles the float[] angles[0] - yaw, angles[1] - pitch
     * @param priority the requesting priority
     * @param callback the callback providing the queued {@link Rotation}
     */
    public void queue(final float[] angles, final int priority, final Consumer<Rotation> callback)
    {
        queue(angles[0], angles[1], priority, callback);
    }

    /**
     * Queues a rotation to be spoofed with a callback once sent
     * @param yaw the y rotation
     * @param pitch the x rotation
     * @param priority the requesting priority
     * @param callback the callback providing the queued {@link Rotation}
     */
    public void queue(final float yaw, final float pitch, final int priority, final Consumer<Rotation> callback)
    {
        queuedRotations.add(new Rotation(yaw, pitch, priority, callback));
    }

    /**
     * Spoofs angles
     * @param yaw the y rotation
     * @param pitch the x rotation
     * @param priority the requesting priority
     * @return if the rotation spoof was accepted based on the requesting priority hierarchy
     */
    public boolean spoof(final float yaw, final float pitch, final int priority)
    {
        if (priority != -1 && spoofPrority > priority)
        {
            return false;
        }
        spoofPrority = priority;
        spoofedAngles[0] = yaw;
        spoofedAngles[1] = pitch;
        return true;
    }

    /**
     * Invalidates current rotations
     * @param angles the float[] of angles
     */
    private void setInvalid(final float[] angles)
    {
        angles[0] = Float.NaN;
        angles[1] = Float.NaN;
        spoofPrority = -1;
    }

    /**
     * Checks if a rotation is valid
     * @param yaw the y rotation
     * @param pitch the x rotation
     * @return if this rotation is a valid rotation
     * @apiNote if {@link RotationManager#ROTATE_PROTECTION} is on, it will limit pitch to 90/-90
     */
    private boolean isRotationValid(final float yaw, final float pitch)
    {
        if (ROTATE_PROTECTION && Math.abs(pitch) > 90.0f)
        {
            return false;
        }
        return !Float.isNaN(yaw) && !Float.isNaN(pitch);
    }

    /**
     * Checks if a rotation is valid
     * @param angles the float[] angles[0] - yaw, angles[1] - pitch
     * @return if this rotation is a valid rotation
     * @apiNote if {@link RotationManager#ROTATE_PROTECTION} is on, it will limit pitch to 90/-90
     */
    private boolean isRotationValid(final float[] angles)
    {
        return isRotationValid(angles[0], angles[1]);
    }

    /**
     * @return if there are spoofed angles
     */
    public boolean isSpoofing()
    {
        return isRotationValid(spoofedAngles);
    }

    /**
     * Gets a look {@link Vec3} for the raytracing
     * @param yaw the y rotation
     * @param pitch the x rotation
     * @return the {@link Vec3}
     */
    public Vec3 getLook(float yaw, float pitch)
    {
        float var2 = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float var3 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float var4 = -MathHelper.cos(-pitch * 0.017453292F);
        float var5 = MathHelper.sin(-pitch * 0.017453292F);
        return MC.theWorld.getWorldVec3Pool().getVecFromPool(var3 * var4, var5, var2 * var4);
    }

    public float[] getServerAngles()
    {
        return serverAngles;
    }

    /**
     * @see {@link net.minecraft.entity.EntityLivingBase#func_110146_f}
     */
    private void setRenderAngles()
    {
        MC.thePlayer.rotationYawHead = serverAngles[0];
        ((IEntityLivingBase)MC.thePlayer).nebula$setRenderPitch(serverAngles[1]);

        // see EntityLivingBase#func_110146_f
        float yaw = MC.thePlayer.renderYawOffset;
        double deltaX = MC.thePlayer.posX - MC.thePlayer.prevPosX;
        double deltaZ = MC.thePlayer.posZ - MC.thePlayer.prevPosZ;
        float distance = (float) (deltaX * deltaX + deltaZ * deltaZ);

        if (distance > 0.0025000002F)
        {
            yaw = (float) Math.atan2(deltaZ, deltaX) * 180.0F / (float) Math.PI - 90.0F;
        }

        if (MC.thePlayer.swingProgress > 0.0F)
        {
            yaw = serverAngles[0];
        }

        float var3 = MathHelper.wrapAngleTo180_float(yaw - MC.thePlayer.renderYawOffset);
        MC.thePlayer.renderYawOffset += var3 * 0.3F;
        float var4 = MathHelper.wrapAngleTo180_float(serverAngles[0] - MC.thePlayer.renderYawOffset);

        if (var4 < -75.0F)
        {
            var4 = -75.0F;
        }

        if (var4 >= 75.0F)
        {
            var4 = 75.0F;
        }

        MC.thePlayer.renderYawOffset = serverAngles[0] - var4;

        if (var4 * var4 > 2500.0F)
        {
            MC.thePlayer.renderYawOffset += var4 * 0.2F;
        }
    }

    public static final class Rotation
    {
        private final float yaw, pitch;
        private final int priority;
        private final Consumer<Rotation> callback;

        public Rotation(float yaw, float pitch, int priority)
        {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.callback = null;
        }

        public Rotation(float yaw, float pitch, int priority, Consumer<Rotation> callback)
        {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.callback = callback;
        }

        void invoke()
        {
            if (callback != null)
            {
                callback.accept(this);
            }
        }

        public float getYaw()
        {
            return yaw;
        }

        public float getPitch()
        {
            return pitch;
        }

        public int getPriority()
        {
            return priority;
        }
    }
}
