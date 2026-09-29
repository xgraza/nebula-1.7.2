package ez.nebula.client.mixin.impl.render;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.render.EventGamma;
import net.minecraft.client.settings.GameSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author xgraza
 * @since 9/28/26
 * Optifine
 */
@Pseudo
@Mixin(targets = {"net.minecraft.src.CustomColorizer"}, remap = false)
public final class CustomColorizer
{
    @Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;gammaSetting:F", opcode = Opcodes.GETFIELD))
    private float redirect$updateLightmap$gammaSetting(GameSettings instance)
    {
        final EventGamma event = new EventGamma(instance.gammaSetting);
        EventBus.dispatch(event);
        return event.getGamma();
    }
}
