package ez.nebula.client.impl.module.movement;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventFastUpdate;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.listener.event.player.EventMoveUpdate;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.MoveUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.src.BlockPos;

import java.util.List;

/**
 * @author xgraza
 * @since 03/26/25
 */
@ModuleManifest(name = "Speed",
        description = "Allows you to move faster",
        category = ModuleCategory.MOVEMENT)
public final class SpeedModule extends Module
{
    @ModuleInstance
    public static SpeedModule INSTANCE;

    private final EnumSetting<Mode> modeSetting = enumBuilder("Mode", Mode.STRAFE)
            .setDescription("The method to use when speeding up")
            .build();

    private final NumberSetting<Integer> setbackTicksSetting = numberBuilder("Setback Ticks", 10)
            .setMin(0)
            .setMax(50)
            .setScale(1)
            .setDescription("How long in ticks to handle AntiCheat set backs for")
            .build();

    // strafe/yport
    private final Setting<Boolean> timerSetting = builder("Timer", false)
            .setDescription("If to use timer to speed up the cheat even more")
            .setVisibility((value) -> modeSetting.getValue() == Mode.STRAFE || modeSetting.getValue() == Mode.Y_PORT)
            .build();

    // physics calc
    private final NumberSetting<Integer> iterationsSetting = numberBuilder("Iterations", 1)
            .setMin(1)
            .setMax(50)
            .setScale(1)
            .setDescription("How many times to re-update the local player")
            .setVisibility((value) -> modeSetting.getValue() == Mode.PHYSICS_CALC)
            .build();
    private final Setting<Boolean> handleSetbackSetting = builder("Handle Setbacks", false)
            .setDescription("If to stop using Physics Calc when an AntiCheat setback is received")
            .setVisibility((value) -> modeSetting.getValue() == Mode.PHYSICS_CALC)
            .build();
    private final Setting<Boolean> protectionSetting = builder("Protection", false)
            .setDescription("IF to stop using Physics Calc when you are in danger of dying from hunger")
            .setVisibility((value) -> modeSetting.getValue() == Mode.PHYSICS_CALC)
            .build();

    private final NumberSetting<Double> vanillaSpeedSetting = numberBuilder("Speed", 0.3)
            .setMin(0.1)
            .setMax(5.0)
            .setScale(0.05)
            .setDescription("How fast the vanilla speed cheat should go")
            .setVisibility((value) -> modeSetting.getValue() == Mode.VANILLA)
            .build();

    private int ticksSinceSetback, strafeStage;
    private double tickMoveSpeed, speed;
    private boolean boost;

    @Override
    public void onDisable()
    {
        super.onDisable();
        MC.timer.timerSpeed = 1.0f;
        boost = false;
        ticksSinceSetback = 0;
        strafeStage = 0;
        tickMoveSpeed = 0.0;
        speed = 0.0;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if ((modeSetting.getValue() != Mode.STRAFE && modeSetting.getValue() != Mode.Y_PORT)
                || --ticksSinceSetback > 0
                || !timerSetting.getValue())
        {
            MC.timer.timerSpeed = 1.0f;
        }

        if (modeSetting.getValue() == Mode.Y_PORT)
        {
            if (ticksSinceSetback > 0)
            {
                MC.timer.timerSpeed = 1.0f;
                return;
            }

            MC.thePlayer.setSprinting(true);
            double moveSpeed = getBaseGroundSpeed();

            if (MC.thePlayer.onGround && MoveUtil.isMoving())
            {
                MC.thePlayer.jump();

                if (timerSetting.getValue())
                {
                    if (MC.thePlayer.ticksExisted % 10 == 0)
                    {
                        MC.timer.timerSpeed = 1.35f;
                    } else
                    {
                        MC.timer.timerSpeed = boost ? 1.088f : 1.077f;
                    }

                    moveSpeed *= boost ? 1.23 : 1.14;
                } else
                {
                    MC.timer.timerSpeed = 1.0f;
                    moveSpeed *= boost ? 1.25 : 1.16;
                }
            } else
            {
                boost = !boost;
                if (MoveUtil.isMoving() && !MC.thePlayer.isCollidedHorizontally)
                {
                    MC.thePlayer.motionY = -4.0;
                }
                MC.timer.timerSpeed = 1.0f;
            }

            MoveUtil.setSpeed(null, MoveUtil.isMoving() ? moveSpeed : 0.0);
        }
    };

    @Subscribe
    private final EventListener<EventMove> moveEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.STRAFE)
        {
            if (MC.thePlayer.onGround || !MoveUtil.isMoving())
            {
                speed = getBaseGroundSpeed();
                strafeStage = 1;
            }

            if (ticksSinceSetback > 0)
            {
                strafeStage = 0;
            }

            if (timerSetting.getValue())
            {
                if (MoveUtil.isMoving())
                {
                    MC.timer.timerSpeed = boost ? 1.06f : 1.079f;
                } else
                {
                    MC.timer.timerSpeed = 1.0f;
                }
            }

            switch (strafeStage)
            {
                case 0:
                {
                    speed = getBaseGroundSpeed();
                    strafeStage = 1;
                    break;
                }
                case 1: // jump/accel
                {
                    if (MC.thePlayer.onGround && MoveUtil.isMoving())
                    {
                        final double motionY = MoveUtil.getJumpHeight(0.3995f);
                        MC.thePlayer.motionY = motionY;
                        event.setY(motionY);
                        final double jumpBoost = boost ? 1.459 : 1.426;
                        speed = (1.41 * getBaseGroundSpeed() - 0.01) * jumpBoost;
                        strafeStage = 2;
                    }
                    break;
                }
                case 2: // decelerate from big speed boost when jumping
                {
                    final double decel = boost ? 0.715 : 0.65;
                    final double playerSpeed = decel * (tickMoveSpeed - getBaseGroundSpeed());
                    speed = tickMoveSpeed - playerSpeed;
                    strafeStage = 3;
                    boost = !boost;
                    break;
                }
                case 3: // air friction (reduce speed while in air)
                {
                    final double airFriction = boost ? 159.077f : 149.077f;
                    speed = tickMoveSpeed - (tickMoveSpeed / airFriction);
                    final List collisionBoxes = MC.theWorld.getCollidingBoundingBoxes(MC.thePlayer,
                            MC.thePlayer.boundingBox.copy().offset(0, 0.2, 0));
                    if (!collisionBoxes.isEmpty())
                    {
                        strafeStage = 0;
                    }
                    break;
                }
            }

            MoveUtil.setSpeed(event, MoveUtil.isMoving() ? Math.max(speed, getBaseGroundSpeed()) : 0.0);
        } else if (modeSetting.getValue() == Mode.VANILLA)
        {
            double speed = ticksSinceSetback <= 0 ? getBaseGroundSpeed() : vanillaSpeedSetting.getValue();
            MoveUtil.setSpeed(event, MoveUtil.isMoving() ? speed : 0.0);
        }
    };

    @Subscribe
    private final EventListener<EventFastUpdate> fastUpdateEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.PHYSICS_CALC)
        {
            if (--ticksSinceSetback > 0 && handleSetbackSetting.getValue())
            {
                return;
            }
            if (protectionSetting.getValue() && MC.thePlayer.getFoodStats().getFoodLevel() <= 2)
            {
                return;
            }
            event.setUpdates(iterationsSetting.getValue());
            event.cancel();
        }
    };

    @Subscribe
    private final EventListener<EventMoveUpdate> moveUpdateEventListener = event ->
            tickMoveSpeed = Nebula.MOVEMENT.getMoveSpeed();

    @Subscribe(receiveCanceled = true)
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S08PacketPlayerPosLook)
        {
            ticksSinceSetback = setbackTicksSetting.getValue();
        }
    };

    @Override
    public boolean isActive()
    {
        return isToggled() && MoveUtil.isMoving();
    }

    private double getBaseGroundSpeed()
    {
        return getGroundFriction() * MoveUtil.getBaseNcpSpeed(2) - 0.01;
    }

    private double getGroundFriction()
    {
        final BlockPos pos = PlayerUtil.getOrigin().down();
        final Block block = MC.theWorld.getBlock(pos);
        if (block == Blocks.ice || block == Blocks.packed_ice)
        {
            return 1.7;
        }
        return 0.99f;
    }

    public enum Mode
    {
        STRAFE, Y_PORT, /*ON_GROUND,*/ PHYSICS_CALC, VANILLA
    }
}
