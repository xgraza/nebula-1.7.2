package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventPostUpdate;
import ez.nebula.client.api.listener.event.player.*;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

/**
 * @author xgraza
 * @since 03/02/25
 */
@ModuleManifest(name = "NoSlow",
        description = "Negates vanilla slowdowns for eating, blocking, webs, blocks, and water",
        category = ModuleCategory.MOVEMENT)
public final class NoSlowModule extends Module
{
    private final EnumSetting<Mode> modeSetting = enumBuilder("Mode", Mode.VANILLA)
            .setDescription("The mode for preventing slowdowns")
            .build();
    private final Setting<Boolean> websSetting = builder("Webs", false)
            .setDescription("If to prevent slowdowns through webs")
            .build();
    private final Setting<Boolean> blocksSetting = builder("Blocks", false)
            .setDescription("If to prevent blocks from slowing you down")
            .build();
    private final Setting<Boolean> waterSetting = builder("Water", false)
            .setDescription("If to prevent water slowdowns")
            .build();

    private boolean bypass;

    @Override
    public void onDisable()
    {
        super.onDisable();
        bypass = false;
    }

    @Subscribe
    private final EventListener<EventMoveUpdate> moveUpdateEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.NCP && isBlocking() && event.isOnGround())
        {
            bypass = true;
            PacketUtil.send(new C07PacketPlayerDigging(
                    5, 0, 0, 0, 255));
        }
    };

    @Subscribe
    private final EventListener<EventPostUpdate> postUpdateEventListener = event ->
    {
        if (bypass)
        {
            bypass = false;
            PacketUtil.send(new C08PacketPlayerBlockPlacement(
                    -1, -1, -1, 255,
                    MC.thePlayer.getHeldItem(), 0.0F, 0.0F, 0.0F));
        }
    };

    @Subscribe
    private final EventListener<EventInWeb> updateEventListener = event ->
            event.setCanceled(websSetting.getValue() && event.getEntity().equals(MC.thePlayer));

    @Subscribe(priority = IEventPriorities.HIGHEST)
    private final EventListener<EventSprint> sprintEventListener = event ->
    {
        if (websSetting.getValue() && MC.thePlayer.isInWeb)
        {
            event.setSprinting(false);
            event.cancel();
        }
    };

    @Subscribe
    private final EventListener<EventItemSlowdown> itemSlowdownEventListener = event ->
    {
        event.getInput().moveForward *= 5.0f;
        event.getInput().moveStrafe *= 5.0f;
    };

    @Subscribe
    private final EventListener<EventPushFromBlocks> pushFromBlocksEventListener = event ->
    {
        if (blocksSetting.getValue())
        {
            event.setCanceled(true);
        }
    };

    @Subscribe
    private final EventListener<EventPushWater> pushWaterEventListener = event ->
    {
        if (waterSetting.getValue() && event.getEntity().equals(MC.thePlayer))
        {
            event.setCanceled(true);
        }
    };

    private boolean isBlocking()
    {
        if (KillAuraModule.INSTANCE.isToggled()
                && KillAuraModule.INSTANCE.isAttacking())
        {
            return KillAuraModule.INSTANCE.isBlocking();
        }
        return MC.thePlayer.isBlocking();
    }

    private enum Mode
    {
        VANILLA,
        NCP
                {
                    @Override public String toString()
                    {
                        return "NCP";
                    }
                }
    }
}
