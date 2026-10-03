package ez.nebula.client.mixin.common.render.gui;

import ez.nebula.client.impl.module.render.NoRenderModule;
import net.minecraft.client.gui.achievement.GuiAchievement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiAchievement.class)
public class GuiAchievementMixin
{
    @Inject(method = "func_146254_a", at = @At("HEAD"), cancellable = true)
    private void hook$func_146254_a$NoRenderHook(CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.toastsSetting.getValue())
        {
            info.cancel();;
        }
    }
}
