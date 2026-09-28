package ez.nebula.client.api.player.server;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.IManager;
import ez.nebula.client.mixin.duck.IPlayerControllerMP;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author xgraza
 * @since 04/02/25
 */
public final class InventoryManager implements IManager
{
    private static final Logger LOGGER = LogManager.getLogger("Inventory");
    private static final Minecraft MC = Minecraft.getMinecraft();

    private int slot = -1;

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S09PacketHeldItemChange)
        {
            slot = ((S09PacketHeldItemChange) event.getPacket()).func_149385_c();
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C09PacketHeldItemChange)
        {
            final C09PacketHeldItemChange packet = event.getPacket();
            final int slotIndex = packet.func_149614_c();
            if (slotIndex > 8 || slotIndex < 0)
            {
                if (Nebula.DEBUG)
                {
                    LOGGER.warn("Something tried to set slot to {}", slotIndex);
                }
                event.cancel();
                return;
            }
            slot = slotIndex;
        }
    };

    @Override
    public void init()
    {
        EventBus.subscribe(this);
    }

    /**
     * Spoofs the server held item slot
     * @param index the item index 0-8
     */
    public void spoof(final int index)
    {
        spoof(index, false);
    }

    /**
     * Spoofs the server held item slot
     * @param index the item index 0-8
     * @param forced if to force the slot change if the index is invalid
     */
    public void spoof(final int index, final boolean forced)
    {
        if (slot == index && !forced)
        {
            return;
        }
        PacketUtil.send(new C09PacketHeldItemChange(slot = index));
    }

    /**
     * Sets the client-sided held item slot
     * @param index the item index 0-8
     */
    public void select(final int index)
    {
        if (MC.thePlayer == null)
        {
            return;
        }
        if (MC.thePlayer.inventory.currentItem != index)
        {
            spoof(index, true);
        }
        MC.thePlayer.inventory.currentItem = index;
        ((IPlayerControllerMP)MC.playerController).nebula$setCurrentPlayerItem(index);
    }

    /**
     * Synchronizes the server held item slot index with that of the client
     */
    public void sync()
    {
        if (slot != MC.thePlayer.inventory.currentItem)
        {
            PacketUtil.send(new C09PacketHeldItemChange(MC.thePlayer.inventory.currentItem));
        }
    }

    /**
     * @return the current spoofed client slot, or the current locally held item slot index
     */
    public int slot()
    {
        if (slot == -1 && MC.thePlayer != null)
        {
            slot = MC.thePlayer.inventory.currentItem;
        }
        return slot;
    }

    /**
     * @return the current spoofed client {@link ItemStack}, or the currently locally held {@link ItemStack}, or null
     */
    public ItemStack stack()
    {
        if (MC.thePlayer == null)
        {
            return null;
        }
        return MC.thePlayer.inventory.mainInventory[slot()];
    }
}
