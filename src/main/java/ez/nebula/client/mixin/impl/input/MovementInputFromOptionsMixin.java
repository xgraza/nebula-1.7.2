package ez.nebula.client.mixin.impl.input;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public class MovementInputFromOptionsMixin
{
    @Inject(method = "updatePlayerMoveState", at = @At("HEAD"), cancellable = true)
    private void hook$updatePlayerMoveState$updateInput(CallbackInfo info)
    {
        MovementInput i = (MovementInput) (Object) this;
        if (EventBus.dispatch(new EventUpdateInput(i)))
        {
            i.moveForward = 0;
            i.moveStrafe = 0;
            i.jump = false;
            i.sneak = false;
            EventBus.dispatch(new EventUpdateInput.Post(i));
            info.cancel();
        }
    }

    @Redirect(method = "updatePlayerMoveState", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInputFromOptions;sneak:Z", opcode = Opcodes.GETFIELD))
    private boolean redirect$updatePlayerMoveState$sneak(MovementInputFromOptions instance)
    {
        MovementInput i = (MovementInput) (Object) this;
        final EventUpdateInput.Post event = new EventUpdateInput.Post(i);
        EventBus.dispatch(event);
        return i.sneak && event.isModifySneaking();
    }
}
