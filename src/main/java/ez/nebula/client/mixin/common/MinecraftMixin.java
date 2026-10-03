package ez.nebula.client.mixin.common;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.game.EventBindStopUse;
import ez.nebula.client.api.listener.event.game.EventDisplayGUI;
import ez.nebula.client.api.listener.event.game.EventTick;
import ez.nebula.client.api.listener.event.input.EventKey;
import ez.nebula.client.api.listener.event.input.EventMouse;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.mixin.duck.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/23/26
 */
@Mixin(value = Minecraft.class)
public final class MinecraftMixin implements IMinecraft
{
    @Shadow private int rightClickDelayTimer;
    @Shadow private Timer timer;
    @Shadow private static int debugFPS;
    @Shadow @Final @Mutable private Session session;
    @Shadow public WorldClient theWorld;
    @Shadow public EntityRenderer entityRenderer;
    @Shadow public GuiScreen currentScreen;

    @Unique private boolean nebula$update;
    @Unique private boolean displayEventCancelled;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void hook$runTick$tickEvent(final CallbackInfo info)
    {
        EventBus.dispatch(new EventTick());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;onTick(I)V", shift = At.Shift.AFTER, ordinal = 0))
    private void hook$runTick$mouseEvent(final CallbackInfo info)
    {
        EventBus.dispatch(new EventMouse(Mouse.getEventButton()));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;onTick(I)V", ordinal = 1))
    private void hook$runTick$keyEvent(final CallbackInfo info)
    {
        EventBus.dispatch(new EventKey(nebula$getKeyCode()));
    }

    /**
     * Override all Keyboard#getEventKey calls with {@link MinecraftMixin#nebula$getKeyCode()}
     * @return the corrected key code
     */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
    private int redirect$runTick$getEventKey()
    {
        return nebula$getKeyCode();
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;getIsKeyPressed()Z", ordinal = 0))
    private boolean redirect$runTick$itemUseGetIsKeyPressed(KeyBinding instance)
    {
        return instance.getIsKeyPressed() && !EventBus.dispatch(new EventBindStopUse());
    }

    /**
     * Removes useless Thread#yield call
     */
    @Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false))
    private void redirect$yield$runGameLoop()
    {
        // remove Thread.yield()
    }

    /**
     * Re-call Display#update
     */
    @Inject(method = "func_147120_f", at = @At("RETURN"))
    private void hook$func_147120_f(final CallbackInfo info)
    {
        if (nebula$update)
        {
            nebula$update = false;
            Display.update();
        }
    }

    /**
     * Remove the Display.update() call entirely from the top most part of the function
     */
    @Redirect(method = "func_147120_f", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", remap = false))
    private void redirect$func_147120_f$DisplayUpdate()
    {
        nebula$update = true;
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void hook$loadWorld$worldChangeEvent(WorldClient worldClient, String p_71353_1_, CallbackInfo info)
    {
        EventBus.dispatch(new EventChangeWorld());

        // clear old maps out - Sk1er improvement
        if (worldClient != theWorld)
        {
            entityRenderer.getMapItemRenderer().func_148249_a();
        }
    }

    @Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
    private void redirect$loadWorld$SystemGCCall()
    {
        // remove System.gc()
    }

    @ModifyVariable(method = "displayGuiScreen", at = @At("STORE"), argsOnly = true)
    private GuiScreen modifyVariable$displayGuiScreen$p_147108_1_(GuiScreen p_147108_1_)
    {
        final EventDisplayGUI event = new EventDisplayGUI(currentScreen, p_147108_1_);
        displayEventCancelled = EventBus.dispatch(event);
        return event.getPending();
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    private void hook$displayGuiScreen$displayGUIEvent(GuiScreen screen, CallbackInfo info)
    {
        if (displayEventCancelled)
        {
            info.cancel();
            displayEventCancelled = false;
        }
    }

    @Unique
    private int nebula$getKeyCode()
    {
        return Keyboard.getEventKey() <= 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
    }

    @Override
    public void nebula$setRightClickDelayTimer(int delay)
    {
        rightClickDelayTimer = delay;
    }

    @Override
    public Timer nebula$getTimer()
    {
        return timer;
    }

    @Override
    public float nebula$getDebugFPS()
    {
        return debugFPS;
    }

    @Override
    public void nebula$setSession(Session s)
    {
        session = s;
    }
}
