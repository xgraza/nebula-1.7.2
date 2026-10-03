package ez.nebula.client.mixin.common.entity;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.game.EventPostUpdate;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.player.EventFastUpdate;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.listener.event.player.EventMoveUpdate;
import ez.nebula.client.mixin.duck.IEntityClientPlayerMP;
import ez.nebula.client.mixin.duck.IEntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.Session;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = EntityClientPlayerMP.class)
public abstract class EntityClientPlayerMPMixin extends EntityPlayerSP implements IEntityClientPlayerMP
{
    @Shadow private boolean wasSneaking;
    @Shadow private boolean wasSprinting;
    @Shadow @Final public NetHandlerPlayClient sendQueue;
    @Shadow private double oldPosX;
    @Shadow private double oldMinY;
    @Shadow private double oldPosZ;
    @Shadow private int ticksSinceMovePacket;
    @Shadow private float oldRotationYaw;
    @Shadow private float oldRotationPitch;
    @Shadow private boolean wasOnGround;
    @Unique private int groundTicks, airTicks;

    public EntityClientPlayerMPMixin(Minecraft par1Minecraft, World par2World, Session par3Session, int par4)
    {
        super(par1Minecraft, par2World, par3Session, par4);
    }

    @Override
    public void moveEntity(double par1, double p_70091_3_, double par3)
    {
        final EventMove event = new EventMove(par1, p_70091_3_, par3);
        if (EventBus.dispatch(event))
        {
            return;
        }
        super.moveEntity(event.getX(), event.getY(), event.getZ());
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdate()V", shift = At.Shift.BEFORE))
    private void hook$onUpdate$updateEvent(final CallbackInfo info)
    {
        EventBus.dispatch(new EventUpdate());
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdate()V", shift = At.Shift.AFTER))
    private void hook$onUpdate$updatePost(final CallbackInfo info)
    {
        if (onGround)
        {
            airTicks = 0;
            ++groundTicks;
        } else
        {
            groundTicks = 0;
            ++airTicks;
        }
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;sendMotionUpdates()V", shift = At.Shift.AFTER))
    private void hook$onUpdate$physicsCalc(final CallbackInfo info)
    {
        final EventFastUpdate event = new EventFastUpdate();
        if (EventBus.dispatch(event) && event.getUpdates() > 0)
        {
            for (int tick = 0; tick < event.getUpdates(); ++tick)
            {
                int oldItemInUse = getItemInUseCount();
                int oldHurtTime = hurtTime;
                float oldPSwingProgress = prevSwingProgress;
                float oldSwingProgress = swingProgress;
                int oldSwingProgressInt = swingProgressInt;
                boolean oldSwingInProgress = isSwingInProgress;
                float oldYaw = rotationYaw;
                float oldPYaw = prevRotationYaw;
                float oldYawOff = renderYawOffset;
                float oldPYawOff = prevRenderYawOffset;
                float oldYawHead = rotationYawHead;
                float oldPYawHead = prevRotationYawHead;
                float oldCamYaw = cameraYaw;
                float oldPCamYaw = prevCameraYaw;
                float oldRArmYaw = renderArmYaw;
                float oldPRArmYaw = prevRenderArmYaw;
                float oldRenderArmP = renderArmPitch;
                float oldPRenderArmP = prevRenderArmPitch;
                float oldDistWalkedM = distanceWalkedModified;
                float oldPDistWalkedM = prevDistanceWalkedModified;
                float oldLimbSwingAmount = limbSwingAmount;
                float oldPLimbSwingAmount = prevLimbSwingAmount;
                float oldLimbSwing = limbSwing;
                EventBus.dispatch(new EventUpdate());
                super.onUpdate();
                ((IEntityPlayer)this).nebula$setItemInUseCount(oldItemInUse);
                hurtTime = oldHurtTime;
                prevSwingProgress = oldPSwingProgress;
                swingProgress = oldSwingProgress;
                swingProgressInt = oldSwingProgressInt;
                isSwingInProgress = oldSwingInProgress;
                rotationYaw = oldYaw;
                prevRotationYaw = oldPYaw;
                renderYawOffset = oldYawOff;
                prevRenderYawOffset = oldPYawOff;
                rotationYawHead = oldYawHead;
                prevRotationYawHead = oldPYawHead;
                cameraYaw = oldCamYaw;
                prevCameraYaw = oldPCamYaw;
                renderArmYaw = oldRArmYaw;
                prevRenderArmYaw = oldPRArmYaw;
                renderArmPitch = oldRenderArmP;
                prevRenderArmPitch = oldPRenderArmP;
                distanceWalkedModified = oldDistWalkedM;
                prevDistanceWalkedModified = oldPDistWalkedM;
                limbSwingAmount = oldLimbSwingAmount;
                prevLimbSwingAmount = oldPLimbSwingAmount;
                limbSwing = oldLimbSwing;
                sendMotionUpdates();
                EventBus.dispatch(new EventPostUpdate());
            }
        }
    }

    /**
     * @author xgraza
     * @reason EventMoveUpdate
     * Send updated motion and position information to the server
     */
    @Overwrite
    public void sendMotionUpdates()
    {
        final EventMoveUpdate event = new EventMoveUpdate(posX, boundingBox.minY, posY, posZ, rotationYaw, rotationPitch, onGround);
        if (EventBus.dispatch(event))
        {
            return;
        }

        if (isSprinting() != wasSprinting)
        {
            sendQueue.addToSendQueue(new C0BPacketEntityAction(
                    this, isSprinting() ? 4 : 5));
            wasSprinting = isSprinting();
        }

        if (isSneaking() != wasSneaking)
        {
            sendQueue.addToSendQueue(new C0BPacketEntityAction(
                    this, isSneaking() ? 1 : 2));
            wasSneaking = isSneaking();
        }

        double diffX = event.getX() - oldPosX;
        double diffY = event.getY() - oldMinY;
        double diffZ = event.getZ() - oldPosZ;
        boolean moved = diffX * diffX + diffY * diffY + diffZ * diffZ > event.getMinMove() || ticksSinceMovePacket >= 20;

        float diffYaw = event.getYaw() - oldRotationYaw;
        float diffPitch = event.getPitch() - oldRotationPitch;
        boolean rotated = diffYaw != 0.0f || diffPitch != 0.0f;

        if (ridingEntity != null)
        {
            sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(
                    motionX, -999.0, -999.0, motionZ,
                    event.getYaw(), event.getPitch(), event.isOnGround()));
            moved = false;
        } else if (moved && rotated)
        {
            sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(
                    event.getX(), event.getY(), event.getStance(), event.getZ(), event.getYaw(), event.getPitch(), event.isOnGround()));
        } else if (moved)
        {
            sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    event.getX(), event.getY(), event.getStance(), event.getZ(), event.isOnGround()));
        } else if (rotated)
        {
            sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
                    event.getYaw(), event.getPitch(), event.isOnGround()));
        } else
        {
            sendQueue.addToSendQueue(new C03PacketPlayer(event.isOnGround()));
        }

        ++ticksSinceMovePacket;
        wasOnGround = onGround;

        if (moved)
        {
            oldPosX = event.getX();
            oldMinY = event.getY();
            oldPosZ = event.getZ();
            ticksSinceMovePacket = 0;
        }

        if (rotated)
        {
            oldRotationYaw = event.getYaw();
            oldRotationPitch = event.getPitch();
        }

        // dispatch the end result
        EventBus.dispatch(new EventMoveUpdate.Post(oldPosX, oldMinY, event.getStance(), oldPosZ, oldRotationYaw, oldRotationPitch, wasOnGround));
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void hook$onUpdate$postUpdateEvent(final CallbackInfo info)
    {
        EventBus.dispatch(new EventPostUpdate());
    }

    @Override
    public void nebula$setWasSneaking(boolean bl)
    {
        wasSneaking = bl;
    }

    @Override
    public void nebula$setWasSprinting(boolean bl)
    {
        wasSprinting = bl;
    }

    @Override
    public boolean nebula$getWasSprinting()
    {
        return wasSprinting;
    }

    @Override
    public int nebula$getGroundTicks()
    {
        return groundTicks;
    }

    @Override
    public int nebula$getAirTicks()
    {
        return airTicks;
    }
}
