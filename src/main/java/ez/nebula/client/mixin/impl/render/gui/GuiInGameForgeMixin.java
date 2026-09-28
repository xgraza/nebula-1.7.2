package ez.nebula.client.mixin.impl.render.gui;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.render.EventRender2D;
import ez.nebula.client.util.render.gui.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class)
public class GuiInGameForgeMixin
{
    @Shadow private ScaledResolution res;

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z", shift = At.Shift.BEFORE))
    private void hook$renderGameOverlay$getRes(float partialTicks, boolean hasScreen, int mouseX, int mouseY, CallbackInfo ci)
    {
        Render2D.RESOLUTION = res;
    }

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", shift = At.Shift.AFTER))
    private void hook$renderGameOverlay$render2DEvent(float partialTicks, boolean hasScreen, int mouseX, int mouseY, CallbackInfo ci)
    {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.mcProfiler.startSection("nebulaRender2D");
        EventBus.dispatch(new EventRender2D(res, partialTicks));
        mc.mcProfiler.endSection();
    }
}
