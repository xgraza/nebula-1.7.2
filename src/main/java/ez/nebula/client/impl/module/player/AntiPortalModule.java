package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.DebugFeature;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.listener.event.world.EventModifyBoundBox;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author xgraza
 * @since 9/16/26
 */
@DebugFeature
@ModuleManifest(name = "AntiPortal",
        description = "Prevents you from entering a portal",
        category = ModuleCategory.PLAYER)
public final class AntiPortalModule extends Module
{
    private static final double BB_OFFSET = 0.25;

    private boolean danger = false;

    @Override public void onDisable()
    {
        super.onDisable();
        danger = false;
    }

    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (!danger)
        {
            return;
        }
        if (event.getPacket() instanceof C03PacketPlayer
                || event.getPacket() instanceof C03PacketPlayer.C04PacketPlayerPosition
                || event.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook
                || event.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook)
        {
            ChatUtil.sendNebula("ruh roh");
            event.setCanceled(true);
        }
    };

    @Subscribe
    private final EventListener<EventMove> moveEventListener = event ->
    {
        danger = false;
    };

    @Subscribe(priority = IEventPriorities.HIGHEST, receiveCanceled = true)
    private final EventListener<EventModifyBoundBox> modifyBoundBoxEventListener = event ->
    {
        if (event.getEntity() == null || !event.getEntity().equals(MC.thePlayer))
        {
            return;
        }
        final Block block = MC.theWorld.getBlock(event.getX(), event.getY(), event.getZ());
        if (!(block instanceof BlockEndPortal))
        {
            return;
        }
        final AxisAlignedBB bb = MC.thePlayer.boundingBox.copy().expand(BB_OFFSET, BB_OFFSET, BB_OFFSET);
        danger = event.getAabb().intersectsWith(bb);
    };
}
