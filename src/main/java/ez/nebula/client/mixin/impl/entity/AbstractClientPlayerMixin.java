package ez.nebula.client.mixin.impl.entity;

import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin
{
    @Redirect(method = "getSkinUrl", at = @At(value = "INVOKE", target = "Ljava/lang/String;format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
    private static String redirect$getSkinUrl(String string, Object[] objects)
    {
        return String.format("https://minotar.net/skin/%s.png", objects);
    }
}
