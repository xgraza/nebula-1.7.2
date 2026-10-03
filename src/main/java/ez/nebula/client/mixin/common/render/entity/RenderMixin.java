package ez.nebula.client.mixin.common.render.entity;

import ez.nebula.client.impl.module.render.NametagsModule;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class RenderMixin
{
    @Inject(method = "func_147906_a", at = @At("HEAD"), cancellable = true)
    private void hook$(Entity p_147906_1_, String p_147906_2_, double p_147906_3_, double p_147906_5_, double p_147906_7_, int p_147906_9_, CallbackInfo info)
    {
        if (NametagsModule.INSTANCE.isToggled())
        {
            info.cancel();
        }
    }
}
