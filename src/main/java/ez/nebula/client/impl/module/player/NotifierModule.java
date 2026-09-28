package ez.nebula.client.impl.module.player;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.world.EventAddEntity;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.api.tray.SystemNotifications;
import ez.nebula.client.impl.module.world.FakePlayerModule;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.minecraft.player.EntityUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.Display;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xgraza
 * @since 3/16/26
 */
@ModuleManifest(name = "Notifier",
        description = "Notifies you of things happening in game around you",
        category = ModuleCategory.PLAYER)
public final class NotifierModule extends Module
{
    private static final int ENDER_PEARL_SPAWN_TYPE = 65;

    private final Setting<Boolean> visualRangeSetting = builder("Visual Range", false)
            .setDescription("If to notify you when another player enters your render distance")
            .build();
    private final Setting<Boolean> takeDamageSetting = builder("Take Damage", false)
            .setDescription("If to notify you when you have taken damage when not focused on the game")
            .build();
    private final Setting<Boolean> pearlsSetting = builder("Pearls", false)
            .setDescription("If to notify you when a pearl is thrown")
            .build();
    private final Setting<Double> pearlDelaySetting = numberBuilder("Pearl Delay", 1.5)
            .setMin(0.0)
            .setMax(5.0)
            .setScale(0.05)
            .setDescription("How long in seconds before notifying about another pearl thrown from the same player")
            .setVisibility((value) -> pearlsSetting.getValue())
            .build();
    private final Setting<Boolean> messageSetting = builder("Message", false)
            .setDescription("If to notify you if another player messages you in game while not focused on the game")
            .build();

    private final Map<Integer, Timer> playerPearlTimerMap = new ConcurrentHashMap<>();
    private float previousHealth = -1.0f;

    @Override
    public void onDisable()
    {
        super.onDisable();
        previousHealth = -1.0f;
        playerPearlTimerMap.clear();
    }

    @Subscribe
    private final EventListener<EventAddEntity> addEntityEventListener = event ->
    {
        if (event.getEntity() instanceof EntityPlayer
                && visualRangeSetting.getValue()
                && !event.isPreviousExisting())
        {
            final EntityPlayer player = (EntityPlayer) event.getEntity();
            if (Nebula.FRIENDS.has(player)
                    || player.getEntityId() == FreecamModule.CAMERA_ENTITY_ID
                    || player.getEntityId() == FakePlayerModule.INSTANCE.getFakePlayerEntityID())
            {
                return;
            }
            notify(String.format("Player %s entered your visual range", player.getCommandSenderName()));
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S0EPacketSpawnObject && pearlsSetting.getValue())
        {
            final S0EPacketSpawnObject packet = event.getPacket();
            if (packet.func_148993_l() != ENDER_PEARL_SPAWN_TYPE)
            {
                return;
            }

            double x = packet.func_148997_d() / 32.0;
            double y = packet.func_148998_e() / 32.0;
            double z = packet.func_148994_f() / 32.0;

            // find closest entity to pearl
            final EntityPlayer thrownByPlayer = MC.theWorld.getClosestPlayer(x, y, z, -1);
            if (thrownByPlayer != null)
            {
                if (pearlDelaySetting.getValue() > 0.0)
                {
                    final Timer timer = playerPearlTimerMap.get(thrownByPlayer.getEntityId());
                    if (timer != null && !timer.hasElapsed((long) (pearlDelaySetting.getValue() * 1000.0)))
                    {
                        return;
                    }
                    playerPearlTimerMap.computeIfAbsent(
                            thrownByPlayer.getEntityId(), (__) -> new Timer());
                }

                notify(String.format("A pearl was thrown by %s at XYZ: %.1f, %.1f, %.1f",
                        thrownByPlayer.getCommandSenderName(), x, y, z));
            } else
            {
                notify(String.format("A pearl was thrown at XYZ: %.1f, %.1f, %.1f", x, y, z));
            }
        }
        if (event.getPacket() instanceof S02PacketChat && messageSetting.getValue())
        {
            final S02PacketChat packet = event.getPacket();
            final String raw = packet.func_148915_c().getUnformattedText();
            if (!raw.startsWith("<") && raw.contains(" whispers: ") && !Display.isActive())
            {
                final String[] parts = raw.trim().split(" ");
                final String username = parts[0];
                notify(username + " sent you a private message!");
            }
        }
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (MC.thePlayer.getHealth() > 0.0f)
        {
            final float playerHealth = EntityUtil.getHealth(MC.thePlayer);
            if (takeDamageSetting.getValue() && previousHealth != -1.0f && previousHealth > playerHealth && !Display.isActive())
            {
                notify(String.format("You have taken damage! Health: %.1f", playerHealth));
            }
            previousHealth = playerHealth;
        }
    };

    @Subscribe
    private final EventListener<EventChangeWorld> changeWorldEventListener = event ->
    {
        playerPearlTimerMap.clear();
        previousHealth = -1.0f;
    };

    private void notify(final String text)
    {
        if (!Display.isActive())
        {
            SystemNotifications.info("Nebula", EnumChatFormatting.getTextWithoutFormattingCodes(text));
        } else
        {
            notifyWarn(text, 7500L);
        }
    }
}
