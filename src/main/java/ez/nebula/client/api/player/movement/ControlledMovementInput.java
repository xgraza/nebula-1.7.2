package ez.nebula.client.api.player.movement;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import net.minecraft.util.MovementInput;

/**
 * @author xgraza
 * @since 6/2/26
 */
final class ControlledMovementInput extends MovementInput
{
    @Override
    public void updatePlayerMoveState()
    {
        final EventUpdateInput.Post event = new EventUpdateInput.Post(this);
        EventBus.dispatch(event);
        if (sneak && event.isModifySneaking())
        {
            moveStrafe *= 0.3f;
            moveForward *= 0.3f;
        }
    }
}
