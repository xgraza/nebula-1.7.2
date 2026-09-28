package ez.nebula.client.impl.module.combat;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0BPacketEntityAction;

/**
 * @author xgraza
 * @since 6/21/26
 */
@ModuleManifest(name = "WTap",
        description = "Makes entities gain extra knockback when you attack them",
        category = ModuleCategory.COMBAT)
public final class WTapModule extends Module
{
    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C02PacketUseEntity)
        {
            final C02PacketUseEntity packet = event.getPacket();
            final Entity entity = packet.func_149564_a(MC.theWorld);
            if (packet.func_149565_c() != C02PacketUseEntity.Action.ATTACK || !(entity instanceof EntityLivingBase))
            {
                return;
            }
            if (MC.thePlayer.isSprinting())
            {
                PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 5));
            }
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 4));
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 5));
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 4));
        }
    };
}
