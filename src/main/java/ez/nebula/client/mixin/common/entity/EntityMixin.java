package ez.nebula.client.mixin.common.entity;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.input.EventRotateCamera;
import ez.nebula.client.api.listener.event.player.EventSafeWalk;
import ez.nebula.client.mixin.duck.IEntity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = Entity.class)
public class EntityMixin implements IEntity
{
    @Shadow protected boolean isInWeb;
    @Shadow protected boolean inPortal;
    @Shadow private int fire;

    @Shadow public double motionY;
    @Shadow public double motionZ;
    @Shadow public double motionX;

//    @Redirect(method = "moveEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;isInWeb:Z", opcode = Opcodes.GETFIELD))
//    private boolean redirect$moveEntity$isInWeb(Entity instance)
//    {
//        return isInWeb && !EventBus.dispatch(new EventInWeb((Entity) (Object)this));
//    }

    @Shadow public float rotationPitch;

    @Shadow public float rotationYaw;

    @Shadow public float prevRotationPitch;

    @Shadow public float prevRotationYaw;

    @ModifyVariable(method = "moveEntity", at = @At("STORE"), ordinal = 0)
    private boolean modifyVar$moveEntity$flag(boolean a)
    {
        if ((Entity) (Object) this instanceof EntityPlayerSP && EventBus.dispatch(new EventSafeWalk()))
        {
            return true;
        }
        return a;
    }

    /**
     * @author xgraza
     * @reason i am not writing all of those mixins for this shit (ts)
     */
    @Overwrite
    public void setAngles(float par1, float par2)
    {
        float var3 = rotationPitch;
        float var4 = rotationYaw;
        float yaw = (float) ((double) this.rotationYaw + (double) par1 * 0.15D);
        float pitch = (float) ((double) this.rotationPitch - (double) par2 * 0.15D);

        if (pitch < -90.0F)
        {
            pitch = -90.0F;
        }

        if (pitch > 90.0F)
        {
            pitch = 90.0F;
        }

        if (EventBus.dispatch(new EventRotateCamera((Entity) (Object) this, yaw, pitch, par1, par2)))
        {
            return;
        }

        this.rotationYaw = yaw;
        this.rotationPitch = pitch;

        prevRotationPitch += this.rotationPitch - var3;
        prevRotationYaw += this.rotationYaw - var4;
    }

    @Override
    public boolean nebula$getIsInWeb()
    {
        return isInWeb;
    }

    @Override
    public boolean nebula$getInPoral()
    {
        return inPortal;
    }

    @Override
    public int nebula$getFire()
    {
        return fire;
    }
}
