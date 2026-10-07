package ez.nebula.client.mixin.common.render.entity;

import ez.nebula.client.impl.module.render.ChamsModule;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin
{
    @Unique private boolean nebula$renderedChams;

    @Shadow protected abstract void renderModel(EntityLivingBase par1EntityLivingBase, float par2, float par3, float par4, float par5, float par6, float par7);

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", ordinal = 1, shift = At.Shift.BEFORE, remap = false))
    private void hook$doRender$ChamsPre(EntityLivingBase entityLivingBase, double p_130000_1_, double par1EntityLivingBase, double p_130000_2_, float par2, float p_130000_4_, CallbackInfo ci)
    {
        if (ChamsModule.INSTANCE.isToggled() && ChamsModule.INSTANCE.isEntityValid(entityLivingBase))
        {
            nebula$renderedChams = true;
            ChamsModule.INSTANCE.preEntityRender();
        }
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V"))
    private void redirect$doRender$renderModel$ChamsPost(RendererLivingEntity instance, EntityLivingBase entityLivingBase, float p_77036_1_, float par1EntityLivingBase, float p_77036_2_, float par2, float p_77036_3_, float par3)
    {
        if (nebula$renderedChams && ChamsModule.INSTANCE.postEntityRender())
        {
            renderModel(entityLivingBase, p_77036_1_, par1EntityLivingBase, p_77036_2_, par2, p_77036_3_, par3);
        }
        nebula$renderedChams = false;
    }
}
