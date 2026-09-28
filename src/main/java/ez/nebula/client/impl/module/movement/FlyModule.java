package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IPlayerControllerMP;
import ez.nebula.client.util.minecraft.player.MoveUtil;
import ez.nebula.client.util.text.FormattingUtil;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.world.WorldSettings;

/**
 * @author xgraza
 * @since 3/14/26
 */
@ModuleManifest(name = "Fly",
        description = "Buzz buzz, I'm flying to infinity and beyond!",
        category = ModuleCategory.MOVEMENT)
public final class FlyModule extends Module
{
    private static final double ANTI_KICK_MOTION_Y = -0.0313;

    private final EnumSetting<Mode> modeSetting = enumBuilder("Mode", Mode.VANILLA)
            .setDescription("How to fly")
            .onValueChanged((mode) ->
            {
                if (mode != Mode.CREATIVE)
                {
                    revertCapabilities();
                }
            })
            .build();
    private final NumberSetting<Double> speedSetting = numberBuilder("Speed", 1.0)
            .setMin(0.1)
            .setMax(7.0)
            .setScale(0.05)
            .setDescription("How fast to fly")
            .build();
    private final Setting<Boolean> antiKickSetting = builder("Anti-Kick", false)
            .setDescription("If to gradually fall to prevent vanilla Minecraft floating kicks")
            .build();
    private final Setting<Boolean> doubleTapSpaceSetting = builder("Double Tap Space", false)
            .setDescription("If to allow vanilla double-tap-space to fly")
            .setVisibility((value) -> modeSetting.getValue() == Mode.CREATIVE)
            .build();

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (modeSetting.getValue() == Mode.CREATIVE)
        {
            revertCapabilities();
        }
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.CREATIVE)
        {
            MC.thePlayer.capabilities.allowFlying = true;
            MC.thePlayer.capabilities.setFlySpeed(speedSetting.getValue().floatValue() / 10.0f);
            if (!doubleTapSpaceSetting.getValue())
            {
                MC.thePlayer.capabilities.isFlying = true;
            }

            if (antiKickSetting.getValue())
            {
                MC.thePlayer.motionY = ANTI_KICK_MOTION_Y;
            }
        }
    };

    @Subscribe
    private final EventListener<EventMove> moveEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.VANILLA)
        {
            if (MoveUtil.isMoving())
            {
                MoveUtil.setSpeed(event, speedSetting.getValue());
            }

            if (MC.gameSettings.keyBindJump.getIsKeyPressed())
            {
                MC.thePlayer.motionY = speedSetting.getValue();
            } else if (MC.gameSettings.keyBindSneak.getIsKeyPressed())
            {
                MC.thePlayer.motionY = -speedSetting.getValue();
            } else
            {
                MC.thePlayer.motionY = antiKickSetting.getValue() ? ANTI_KICK_MOTION_Y : 0.0;
            }

            event.setY(MC.thePlayer.motionY);
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C13PacketPlayerAbilities && modeSetting.getValue() == Mode.CREATIVE)
        {
            final C13PacketPlayerAbilities packet = event.getPacket();
            if (((IPlayerControllerMP)MC.playerController).nebula$getCurrentGameType() != WorldSettings.GameType.CREATIVE)
            {
                packet.func_149493_d(false);
                packet.func_149483_b(false);
                packet.func_149491_c(false);
            }
            packet.func_149485_a(0.05f);
            packet.func_149492_b(0.1f);
        }
    };

    private void revertCapabilities()
    {
        if (MC.thePlayer != null && MC.thePlayer.capabilities != null)
        {
            MC.thePlayer.capabilities.isFlying = false;
            MC.thePlayer.capabilities.allowFlying = false;
            MC.thePlayer.capabilities.setFlySpeed(0.05f);
        }
    }

    @Override
    public String getMetadata()
    {
        return FormattingUtil.formatEnum(modeSetting.getValue());
    }

    private enum Mode
    {
        VANILLA, CREATIVE
    }
}
