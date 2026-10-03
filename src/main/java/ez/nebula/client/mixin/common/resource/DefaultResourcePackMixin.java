package ez.nebula.client.mixin.common.resource;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.DefaultResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(DefaultResourcePack.class)
public class DefaultResourcePackMixin
{
    @Shadow @Final @Mutable public static Set defaultResourceDomains;

    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void hook$clinit(CallbackInfo info)
    {
        defaultResourceDomains = ImmutableSet.of("minecraft", "nebula");
    }
}
