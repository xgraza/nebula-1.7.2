package ez.nebula.client.mixin.impl.render;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.mixin.duck.IEntityRenderer;
import ez.nebula.client.util.render.gui.ProjectionUtil;
import ez.nebula.client.util.render.world.EntityCulling;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/25/26
 */
@Mixin(value = EntityRenderer.class)
public abstract class EntityRendererMixin implements IEntityRenderer
{
    @Shadow protected abstract void orientCamera(float par1);

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V", shift = At.Shift.AFTER))
    private void hook$renderWorld(float v, long p_78471_1_, CallbackInfo ci)
    {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.mcProfiler.endStartSection("nebulaRender3D");
        mc.mcProfiler.startSection("entityCulling");
        EntityCulling.checkCulling();
        mc.mcProfiler.endStartSection("projection");
        ProjectionUtil.updateProjection();
        mc.mcProfiler.endStartSection("dispatch");
        EventBus.dispatch(new EventRender3D(v));
        mc.mcProfiler.endSection();
        mc.mcProfiler.endSection();
    }

    @Override
    public void nebula$orientCamera(float f)
    {
        orientCamera(f);
    }
}
