package ez.nebula.client.mixin.impl.render.gui;

import ez.nebula.client.impl.module.render.NoRenderModule;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class GuiInGameMixin
{
    @Inject(method = "func_130015_b", at = @At("HEAD"), cancellable = true)
    private void hook$func_130015_b$NoRenderHook(float v, int p_130015_1_, int par1, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.portalSetting.getValue())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderPumpkinBlur", at = @At("HEAD"), cancellable = true)
    private void hook$renderPumpkinBlur$NoRenderHook(int i, int p_73836_1_, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.pumpkinSetting.getValue())
        {
            info.cancel();
        }
    }
}
