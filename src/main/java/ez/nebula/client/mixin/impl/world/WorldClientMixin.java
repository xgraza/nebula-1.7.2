package ez.nebula.client.mixin.impl.world;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.world.EventAddEntity;
import ez.nebula.client.impl.module.render.NoRenderModule;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WorldClient.class)
public class WorldClientMixin
{
    @Inject(method = "addEntityToWorld", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hook$addEntityToWorld$addEntityEvent(int i, Entity p_73027_1_, CallbackInfo info, Entity entity)
    {
        EventBus.dispatch(new EventAddEntity(i, p_73027_1_, entity != null));
    }

    @Inject(method = "doVoidFogParticles", at = @At("HEAD"), cancellable = true)
    private void hook$doVoidFogParticles$NoRenderHook(int i, int p_73029_1_, int par1, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.voidParticlesSetting.getValue())
        {
            info.cancel();
        }
    }
}
