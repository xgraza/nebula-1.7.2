package ez.nebula.client.impl.module.player;

import com.google.common.collect.Lists;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventContainerAction;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

import java.util.List;

/**
 * @author xgraza
 * @since 03/24/25
 */
@ModuleManifest(name = "AntiRevert",
        description = "Prevents illegal items from getting reverted",
        category = ModuleCategory.PLAYER)
public final class AntiRevertModule extends Module
{
    @ModuleInstance
    public static AntiRevertModule INSTANCE;

    // Some items such as Inf Tnt cannot be placed within 1500 blocks of 0, 0
    private static final int ALFHEIM_ILLEGAL_PLACE_RANGE = 1500;
    private static final List<Block> EPEARL_NO_LIKE_BLOCK_LIST = Lists.newArrayList(
            Blocks.tnt
    );

    private final Setting<Boolean> interactSetting = builder("Interact", true)
            .setDescription("If to block interactions with the infinite item")
            .build();
    private final Setting<Boolean> antiDropSetting = builder("Anti Distribute", true)
            .setDescription("If to prevent distribution of illegal items")
            .build();

    @Subscribe
    private final EventListener<EventContainerAction> containerActionEventListener = event ->
    {
        if (event.getSlot() == null || event.getSlot().getStack() == null)
        {
            return;
        }
        if (event.getAction() == 4
                && antiDropSetting.getValue()
                && ItemUtil.isIllegal(event.getSlot().getStack()))
        {
            event.setCanceled(true);
            notifyWarn("Prevented distribution of illegal item", 5000L);
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C02PacketUseEntity)
        {
            if (ItemUtil.isInfinite(MC.thePlayer.getHeldItem()) && interactSetting.getValue())
            {
                event.setCanceled(true);
            }
        } else if (event.getPacket() instanceof C07PacketPlayerDigging)
        {
            final C07PacketPlayerDigging packet = event.getPacket();
            if ((packet.func_149506_g() == 3 || packet.func_149506_g() == 4)
                    && antiDropSetting.getValue()
                    && ItemUtil.isIllegal(MC.thePlayer.getHeldItem()))
            {
                event.setCanceled(true);
                notifyWarn("Prevented distribution of illegal item", 5000L);
            }
        }
    };
}
