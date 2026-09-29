package ez.nebula.client.mixin.impl.entity;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.player.*;
import ez.nebula.client.mixin.duck.IEntityPlayerSP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = EntityPlayerSP.class)
public class EntityPlayerSPMixin implements IEntityPlayerSP
{
    @Shadow private int horseJumpPowerCounter;

    @Shadow public MovementInput movementInput;

    @Shadow protected Minecraft mc;

    @Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;sprintToggleTimer:I", shift = At.Shift.AFTER, ordinal = 1, opcode = Opcodes.PUTFIELD))
    private void hook$onLivingUpdate$itemSlowdownEvent(CallbackInfo info)
    {
        EventBus.dispatch(new EventItemSlowdown(movementInput));
    }

    @Inject(method = "isSneaking", at = @At("RETURN"), cancellable = true)
    private void hook$isSneaking$eventSneak(CallbackInfoReturnable<Boolean> info)
    {
        if (((EntityPlayer) (Object) this).equals(mc.thePlayer))
        {
            final EventSneak event = new EventSneak(info.getReturnValue());
            EventBus.dispatch(event);
            info.setReturnValue(event.isState());
        }
    }

    @Inject(method = "func_145771_j", at = @At("HEAD"), cancellable = true)
    private void hook$func_145771_j$pushFromBlocksEvent(double x, double y, double z, CallbackInfoReturnable<Boolean> info)
    {
        if (EventBus.dispatch(new EventPushFromBlocks()))
        {
            info.setReturnValue(false);
        }
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;getIsKeyPressed()Z"))
    private boolean redirect$onLivingUpdate$getKeyIsPressed(KeyBinding instance)
    {
        final EventSprint event = new EventSprint();
        return EventBus.dispatch(event) ? event.isSprinting() : instance.getIsKeyPressed();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;moveForward:F", ordinal = 3, opcode = Opcodes.GETFIELD))
    private float redirect$onLivingUpdate$moveForward$4(MovementInput instance)
    {
        return EventBus.dispatch(new EventOmniSprint()) ? Float.MAX_VALUE : instance.moveForward;
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;moveForward:F", ordinal = 4, opcode = Opcodes.GETFIELD))
    private float redirect$onLivingUpdate$moveForward$5(MovementInput instance)
    {
        return EventBus.dispatch(new EventOmniSprint()) ? Float.MAX_VALUE : instance.moveForward;
    }

    @Override
    public void nebula$setHorseJumpPowerCounter(int i)
    {
        horseJumpPowerCounter = i;
    }
}
