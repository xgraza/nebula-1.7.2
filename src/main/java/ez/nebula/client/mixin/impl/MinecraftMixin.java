package ez.nebula.client.mixin.impl;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.input.EventKey;
import ez.nebula.client.api.listener.event.input.EventMouse;
import ez.nebula.client.mixin.duck.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    @Unique private boolean nebula$update;

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", shift = At.Shift.AFTER, remap = false))
    private void hook$runTick$mouseEvent(final CallbackInfo info)
    {
        if (Mouse.getEventButtonState())
        {
            EventBus.dispatch(new EventMouse(Mouse.getEventButton()));
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z", shift = At.Shift.AFTER, remap = false))
    private void hook$runTick$keyEvent(final CallbackInfo info)
    {
        if (Keyboard.getEventKeyState())
        {
            EventBus.dispatch(new EventKey(nebula$getKeyCode()));
        }
    }

    /**
     * Override all Keyboard#getEventKey calls with {@link MinecraftMixin#nebula$getKeyCode()}
     * @return the corrected key code
     */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
    private int redirect$getEventKey$runTick()
    {
        return nebula$getKeyCode();
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
    @Redirect(method = "func_147120_f", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V"))
    private void redirect$update$func_147120_f()
    {
        nebula$update = true;
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
