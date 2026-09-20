package ez.nebula.client.api.player.movement;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventMoveUpdate;
import ez.nebula.client.api.manager.IManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;

/**
 * @author xgraza
 * @since 6/2/26
 */
public final class MovementManager implements IManager
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    private MovementInput currentInput, savedInput;

    private double moveSpeed, motionSpeed;

    @Subscribe
    private final EventListener<EventMoveUpdate> moveUpdateEventListener = event ->
    {
        final double deltaX = MC.thePlayer.posX - MC.thePlayer.lastTickPosX;
        final double deltaZ = MC.thePlayer.posZ - MC.thePlayer.lastTickPosZ;
        moveSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        motionSpeed = Math.sqrt(MC.thePlayer.motionX * MC.thePlayer.motionX
                + MC.thePlayer.motionZ * MC.thePlayer.motionZ);
    };

    @Override
    public void init()
    {
        EventBus.subscribe(this);
    }

    public void sneak(final boolean sneaking)
    {
        if (currentInput == null)
        {
            return;
        }
        currentInput.sneak = sneaking;
    }

    public boolean isSneaking()
    {
        return currentInput != null && currentInput.sneak;
    }

    public void jump(final boolean jumping)
    {
        if (currentInput == null)
        {
            return;
        }
        currentInput.jump = jumping;
    }

    public boolean isJumping()
    {
        return currentInput != null && currentInput.jump;
    }

    public void move(final float[] movement)
    {
        move(movement[0], movement[1]);
    }

    public void move(final float forward, final float strafe)
    {
        if (currentInput == null)
        {
            return;
        }
        currentInput.moveStrafe = strafe;
        currentInput.moveForward = forward;
    }

    public void override()
    {
        if (savedInput != null)
        {
            return;
        }
        savedInput = MC.thePlayer.movementInput;
        currentInput = new ControlledMovementInput();
        MC.thePlayer.movementInput = currentInput;
    }

    public void restore()
    {
        if (savedInput == null)
        {
            return;
        }
        MC.thePlayer.movementInput = savedInput;
        savedInput = currentInput = null;
    }

    public double getMoveSpeed()
    {
        return moveSpeed;
    }

    public double getMotionSpeed()
    {
        return motionSpeed;
    }
}
