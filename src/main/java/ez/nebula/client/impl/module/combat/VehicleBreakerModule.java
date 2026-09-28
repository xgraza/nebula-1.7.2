package ez.nebula.client.impl.module.combat;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.network.play.client.C02PacketUseEntity;

/**
 * @author xgraza
 * @since 06/24/25
 */
@ModuleManifest(name = "VehicleBreaker",
        description = "Quickly breaks vehicles",
        category = ModuleCategory.COMBAT)
public final class VehicleBreakerModule extends Module
{
    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C02PacketUseEntity)
        {
            final C02PacketUseEntity packet = event.getPacket();
            final Entity entity = packet.func_149564_a(MC.theWorld);
            if (packet.func_149565_c() != C02PacketUseEntity.Action.ATTACK || !(entity instanceof EntityBoat))
            {
                return;
            }
            for (int i = 0; i < 10; ++i)
            {
                PacketUtil.sendInstant(event.getPacket());
            }
        }
    };
}
