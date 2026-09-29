package ez.nebula.client.mixin.impl.entity;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.world.EventBlockSlipperiness;
import ez.nebula.client.mixin.duck.IEntityLivingBase;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(EntityLivingBase.class)
public class EntityLivingBaseMixin implements IEntityLivingBase
{
    @Unique private float renderPitch, prevRenderPitch;

    @Shadow private int jumpTicks;

    @Inject(method = "onEntityUpdate", at = @At("TAIL"))
    private void hook$onEntityUpdate$tail(CallbackInfo info)
    {
        prevRenderPitch = renderPitch;
    }

    // TODO: i think this is not going to work very well
    @ModifyVariable(method = "moveEntityWithHeading", at = @At("STORE"), ordinal = 3)
    private float modifyVariable$moveEntityWithHeading$f3(float f3)
    {
        final EntityLivingBase e = (EntityLivingBase) (Object) this;
        float f2 = 0.91f;
        if (e.onGround)
        {
            final Block block = e.worldObj.getBlock(MathHelper.floor_double(e.posX), MathHelper.floor_double(e.boundingBox.minY) - 1, MathHelper.floor_double(e.posZ));
            f2 = block.slipperiness * 0.91F;
            final EventBlockSlipperiness event = new EventBlockSlipperiness(e, block, f2);
            EventBus.dispatch(event);
            f2 = event.getSlipperiness();
        }
        return 0.16277136F / (f2 * f2 * f2);
    }

    @Redirect(method = "moveEntityWithHeading", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;motionX:D", opcode = Opcodes.PUTFIELD, ordinal = 4))
    private void redirect$moveEntityWithHeading$motionX$slipperinessFactor(EntityLivingBase e, double value)
    {
        if (e.onGround)
        {
            final Block block = e.worldObj.getBlock(MathHelper.floor_double(e.posX), MathHelper.floor_double(e.boundingBox.minY) - 1, MathHelper.floor_double(e.posZ));
            float f2 = block.slipperiness * 0.91F;
            final EventBlockSlipperiness event = new EventBlockSlipperiness(e, block, f2);
            EventBus.dispatch(event);
            e.motionX *= event.getSlipperiness();
        } else
        {
            e.motionX = value;
        }
    }

    @Redirect(method = "moveEntityWithHeading", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;motionZ:D", opcode = Opcodes.PUTFIELD, ordinal = 4))
    private void redirect$moveEntityWithHeading$motionZ$slipperinessFactor(EntityLivingBase e, double value)
    {
        if (e.onGround)
        {
            final Block block = e.worldObj.getBlock(MathHelper.floor_double(e.posX), MathHelper.floor_double(e.boundingBox.minY) - 1, MathHelper.floor_double(e.posZ));
            float f2 = block.slipperiness * 0.91F;
            final EventBlockSlipperiness event = new EventBlockSlipperiness(e, block, f2);
            EventBus.dispatch(event);
            e.motionZ *= event.getSlipperiness();
        } else
        {
            e.motionZ = value;
        }
    }

    @Override
    public void nebula$setJumpTicks(int i)
    {
        jumpTicks = i;
    }

    @Override
    public float nebula$getRenderPitch()
    {
        return renderPitch;
    }

    @Override
    public void nebula$setRenderPitch(float f)
    {
        renderPitch = f;
    }

    @Override
    public float nebula$getPrevRenderPitch()
    {
        return prevRenderPitch;
    }

    @Override
    public void nebula$setPrevRenderPitch(float f)
    {
        prevRenderPitch = f;
    }
}
