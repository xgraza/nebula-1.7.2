package ez.nebula.client.mixin.common.render.entity;

import ez.nebula.client.mixin.duck.IEntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public class RenderLivingMixin
{
    @Unique private EntityLivingBase renderEntity;
    @Unique private float partialTicks;

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private void hook$doRender(EntityLivingBase entityLivingBase, double p_130000_1_, double par1EntityLivingBase, double p_130000_2_, float par2, float p_130000_4_, CallbackInfo info)
    {
        renderEntity = entityLivingBase;
        partialTicks = p_130000_4_;
    }

    @ModifyVariable(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("STORE"), ordinal = 4)
    private float modifyVariable$doRender$pitch(float pitch)
    {
        if (renderEntity != null && renderEntity.equals(Minecraft.getMinecraft().thePlayer))
        {
            final IEntityLivingBase i = (IEntityLivingBase) renderEntity;
            return i.nebula$getPrevRenderPitch() + (i.nebula$getRenderPitch() - i.nebula$getPrevRenderPitch()) * partialTicks;
        }
        return pitch;
    }
}
