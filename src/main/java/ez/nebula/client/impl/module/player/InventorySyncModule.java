package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.mixin.duck.IContainer;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

/**
 * @author xgraza
 * @since 03/26/25
 */
@ModuleManifest(name = "InventorySync",
        description = "Syncs inventory transactions with the server to prevent de-sync",
        category = ModuleCategory.PLAYER)
public final class InventorySyncModule extends Module
{
    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S32PacketConfirmTransaction)
        {
            final S32PacketConfirmTransaction packet = event.getPacket();
            final Container container = getContainer(packet.func_148889_c());
            if (container == null)
            {
                return;
            }
            final short transaction = packet.func_148890_d();
            final short currentTransaction = ((IContainer)container).nebula$getTransactionId();
            if (transaction + 1 <= currentTransaction || transaction > currentTransaction)
            {
                ((IContainer)container).nebula$setTransactionId((short) (transaction + 1));
            }
        }
    };

    private Container getContainer(final int windowId)
    {
        if (windowId == 0)
        {
            return MC.thePlayer.inventoryContainer;
        } else if (windowId == MC.thePlayer.openContainer.windowId)
        {
            return MC.thePlayer.openContainer;
        }
        return null;
    }
}
