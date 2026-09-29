package ez.nebula.client.mixin.impl.render;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.player.EventRaytrace;
import ez.nebula.client.api.listener.event.render.EventCameraDistance;
import ez.nebula.client.api.listener.event.render.EventGamma;
import ez.nebula.client.api.listener.event.render.EventPerspective;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.impl.module.render.NoRenderModule;
import ez.nebula.client.mixin.duck.IEntityRenderer;
import ez.nebula.client.util.render.gui.ProjectionUtil;
import ez.nebula.client.util.render.world.EntityCulling;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/25/26
 */
@Mixin(value = EntityRenderer.class)
public abstract class EntityRendererMixin implements IEntityRenderer
{
    @Shadow protected abstract void orientCamera(float par1);

    @Shadow private Minecraft mc;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V", shift = At.Shift.AFTER, remap = false))
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

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void hook$(float v, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.hurtCameraSetting.getValue())
        {
            info.cancel();
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 2, remap = false))
    private void redirect$glTranslatef$2(float x, float y, float z)
    {
        final EventCameraDistance event = new EventCameraDistance(-z);
        EventBus.dispatch(event);
        GL11.glTranslatef(x, y, (float) -event.getCameraDistance());
    }

    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void redirect$setupCameraTransform$gluPerspective(float fovy, float aspect, float zNear, float zFar)
    {
        final EventPerspective event = new EventPerspective(fovy, aspect, zNear, zFar);
        EventBus.dispatch(event);
        Project.gluPerspective(event.getFov(), event.getAspect(), event.getzNear(), event.getzFar());
    }

    @ModifyVariable(method = "setupCameraTransform", at = @At("STORE"), ordinal = 2)
    private float modifyVariable$setupCameraTransform$f2(float f2)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.nauseaSetting.getValue())
        {
            return 0.0f;
        }
        return f2;
    }

    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;rayTrace(DF)Lnet/minecraft/util/MovingObjectPosition;"))
    private MovingObjectPosition redirect$getMouseOver$rayTrace(EntityLivingBase instance, double v, float p_70614_1_)
    {
        final MovingObjectPosition result = instance.rayTrace(v, p_70614_1_);
        final EventRaytrace event = new EventRaytrace(mc.renderViewEntity, result, p_70614_1_);
        return EventBus.dispatch(event) ? event.getResult() : result;
    }

    @Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;gammaSetting:F", opcode = Opcodes.GETFIELD))
    private float redirect$updateLightmap$gammaSetting(GameSettings instance)
    {
        final EventGamma event = new EventGamma(instance.gammaSetting);
        EventBus.dispatch(event);
        return event.getGamma();
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"))
    private Material redirect$setupFog$NoRenderHook(Block instance)
    {
        final Material material = instance.getMaterial();
        if (material == Material.water && NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.waterSetting.getValue())
        {
            return null;
        }
        if (material == Material.lava && NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.lavaSetting.getValue())
        {
            return null;
        }
        return material;
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getRespiration(Lnet/minecraft/entity/EntityLivingBase;)I"))
    private int redirect$setupFog$respirationEnchantmentFix(EntityLivingBase p_77501_0_)
    {
        // fix: servers with 32k armor will fuck this up...
        return Math.min(EnchantmentHelper.getRespiration(p_77501_0_), 3);
    }

    @Override
    public void nebula$orientCamera(float f)
    {
        orientCamera(f);
    }
}
