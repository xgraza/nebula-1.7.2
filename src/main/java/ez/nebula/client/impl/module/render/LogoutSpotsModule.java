package ez.nebula.client.impl.module.render;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.ColorSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xgraza
 * @since 6/24/26
 */
@ModuleManifest(name = "LogoutSpots",
        description = "Renders a player's logout spot",
        category = ModuleCategory.RENDER)
public final class LogoutSpotsModule extends Module
{
    private final NumberSetting<Float> lineWidthSetting = numberBuilder("Line Width", 1.5f)
            .setMin(0.5f)
            .setMax(5.0f)
            .setScale(0.1f)
            .setDescription("The width of the logged out player's bounding box outline")
            .build();
    private final Setting<Boolean> logFriendsSetting = builder("Log Friends", true)
            .setDescription("If to log your friend's logout spots")
            .build();

    private final Map<String, LogoutSpot> logoutSpotMap = new ConcurrentHashMap<>();

    @Override
    public void onDisable()
    {
        super.onDisable();
        logoutSpotMap.clear();
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (logoutSpotMap.isEmpty())
        {
            return;
        }
        MC.mcProfiler.startSection("logoutSpots");
        final ColorSetting cs = (ColorSetting) HUDModule.INSTANCE.primaryColorSetting;
        for (final String username : logoutSpotMap.keySet())
        {
            final LogoutSpot spot = logoutSpotMap.get(username);
            if (spot.dimension != MC.thePlayer.dimension)
            {
                continue;
            }

            final AxisAlignedBB bb = spot.bb;
            Render3D.outlinedAABB(bb, lineWidthSetting.getValue(), QuadMask.ALL_FACES, cs.getValueInt());
            Render3D.filledAABB(bb, QuadMask.ALL_FACES, cs.getValueInt(120));

            double x = (bb.minX + bb.maxX) / 2.0;
            double y = bb.maxY + 0.2;
            double z = (bb.minZ + bb.maxZ) / 2.0;

            Render3D.billboard(x, y, z, 0.2, () ->
            {
                final String text1 = username + "'s logout spot";
                final String text2 = String.format("XYZ: %.2f, %.2f, %.2f", x, bb.minY, z);

                int textWidth1 = MC.fontRenderer.getStringWidth(text1);
                int textWidth2 = MC.fontRenderer.getStringWidth(text2);

                double textWidth = Math.max(textWidth1, textWidth2) / 2.0;
                int textHeight = (MC.fontRenderer.FONT_HEIGHT + 1) * 2;
                Render2D.rectangle(-(textWidth + 2), -(textHeight + 1), (textWidth + 2) * 2, textHeight + 4, 0x95000000);
                MC.fontRenderer.drawStringWithShadow(text1, (int) -(textWidth1 / 2.0), -textHeight + 2, -1);
                MC.fontRenderer.drawStringWithShadow(text2, (int) -(textWidth2 / 2.0), -textHeight + MC.fontRenderer.FONT_HEIGHT + 3, -1);
            });
        }
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S38PacketPlayerListItem)
        {
            final S38PacketPlayerListItem packet = event.getPacket();
            final String username = packet.func_149122_c();
            if (MC.thePlayer != null && MC.thePlayer.getCommandSenderName().equals(username))
            {
                return;
            }
            if (!packet.func_149121_d())
            {
                for (final Object o : MC.theWorld.playerEntities)
                {
                    if (!(o instanceof EntityPlayer))
                    {
                        continue;
                    }
                    final EntityPlayer player = (EntityPlayer)o;
                    if (player.getCommandSenderName().equals(username))
                    {
                        if (!logFriendsSetting.getValue() && Nebula.FRIENDS.has(player))
                        {
                            continue;
                        }

                        final AxisAlignedBB bb = player.boundingBox.copy();
                        logoutSpotMap.put(username, new LogoutSpot(bb, player.dimension));
                        notifyInfo(String.format("%s logged out at XYZ: %.2f, %.2f, %.2f",
                                username, player.posX, bb.minY, player.posZ), 7500L);
                        break;
                    }
                }
            } else
            {
                final LogoutSpot spot = logoutSpotMap.remove(username);
                if (spot == null)
                {
                    return;
                }
                final AxisAlignedBB bb = spot.bb;
                final Vec3 pos = Vec3.createVectorHelper(
                        (bb.minX + bb.maxX) / 2.0, bb.minY, (bb.minZ + bb.maxZ) / 2.0);
                notifyInfo(String.format("%s logged back in at XYZ: %.2f, %.2f, %.2f",
                        username, pos.xCoord, pos.yCoord, pos.zCoord), 7500L);
            }
        }
    };

    private static final class LogoutSpot
    {
        private final AxisAlignedBB bb;
        private final int dimension;

        public LogoutSpot(AxisAlignedBB bb, int dimension)
        {
            this.bb = bb;
            this.dimension = dimension;
        }
    }
}
