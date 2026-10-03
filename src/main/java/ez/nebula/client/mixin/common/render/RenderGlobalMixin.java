package ez.nebula.client.mixin.common.render;

import ez.nebula.client.impl.module.render.NoRenderModule;
import ez.nebula.client.util.render.world.EntityCulling;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class RenderGlobalMixin
{
    @Inject(method = "loadRenderers", at = @At("HEAD"))
    private void hook$loadRenders$EntityCulling(CallbackInfo info)
    {
        EntityCulling.reset();
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;shouldRenderInPass(I)Z", ordinal = 1))
    private boolean redirect$renderEntities$EntityCulling(Entity instance, int pass)
    {
        if (instance == null)
        {
            return false;
        }
        EntityCulling.queryEntity(instance);
        return instance.shouldRenderInPass(pass) && EntityCulling.shouldRenderEntity(instance);
    }

    @Redirect(method = "doSpawnParticle", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean redirect$doSpawnParticle$NoRenderHook(String instance, Object o)
    {
        if (o instanceof String)
        {
            final String str = (String) o;
            if ((str.equals("hugeexplosion") || str.equals("largeexplode"))
                    && NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.explosionsSetting.getValue())
            {
                return false;
            }
        }
        return instance.equals(o);
    }
}
