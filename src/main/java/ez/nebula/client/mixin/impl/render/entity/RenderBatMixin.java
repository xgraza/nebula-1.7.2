package ez.nebula.client.mixin.impl.render.entity;

import ez.nebula.client.impl.module.render.NoRenderModule;
import net.minecraft.client.renderer.entity.RenderBat;
import net.minecraft.entity.passive.EntityBat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderBat.class)
public class RenderBatMixin
{
    @Inject(method = "doRender(Lnet/minecraft/entity/passive/EntityBat;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void hook$(EntityBat p_82443_1_, double p_82443_2_, double p_82443_4_, double p_82443_6_, float p_82443_8_, float p_82443_9_, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.batsSetting.getValue())
        {
            info.cancel();
        }
    }
}
