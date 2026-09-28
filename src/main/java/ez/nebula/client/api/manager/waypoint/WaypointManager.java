package ez.nebula.client.api.manager.waypoint;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.ITypedManager;
import ez.nebula.client.impl.config.WaypointConfig;
import ez.nebula.client.impl.module.render.NameProtectModule;
import ez.nebula.client.impl.module.render.WaypointsModule;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;

/**
 * @author xgraza
 * @since 6/12/26
 */
public final class WaypointManager implements ITypedManager<Waypoint>
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    private final List<Waypoint> waypointList = new ArrayList<>();

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        final List<Waypoint> serverWaypoints = getServerWaypoints();
        if (serverWaypoints.isEmpty() || !WaypointsModule.INSTANCE.isToggled())
        {
            return;
        }

        // TODO: this doesnt work if you super quickly press F2
        if (WaypointsModule.INSTANCE.antiScreenshotSetting.getValue() && MC.gameSettings.keyBindScreenshot.getIsKeyPressed())
        {
            return;
        }

        MC.mcProfiler.startSection("waypoints");

        for (final Waypoint waypoint : serverWaypoints)
        {
            final Vec3 vec = getWaypointPos(waypoint);
            Render3D.billboard(vec.xCoord, vec.yCoord, vec.zCoord, 0.2f, () ->
            {
                glEnable(GL_DEPTH_CLAMP);
                final double distance = Math.sqrt(MC.thePlayer.getDistanceSq(vec.xCoord, vec.yCoord, vec.zCoord));
                final String text1 = NameProtectModule.INSTANCE.protect(waypoint.getName());
                String text2 = String.format("%.1f block%s", distance, distance > 1.0 ? "s" : "");

                if (waypoint.getDimension() != MC.thePlayer.dimension)
                {
                    if (waypoint.getDimension() == -1)
                    {
                        text2 += " (Nether)";
                    } else if (waypoint.getDimension() == 0)
                    {
                        text2 += " (Overworld)";
                    }
                }

                int textWidth1 = MC.fontRenderer.getStringWidth(text1);
                int textWidth2 = MC.fontRenderer.getStringWidth(text2);

                double textWidth = Math.max(textWidth1, textWidth2) / 2.0;
                int textHeight = (MC.fontRenderer.FONT_HEIGHT + 1) * 2;
                if (!WaypointsModule.INSTANCE.showDistanceSetting.getValue())
                {
                    textHeight = MC.fontRenderer.FONT_HEIGHT + 1;
                }

                Render2D.rectangle(-(textWidth + 2), -(textHeight + 1), (textWidth + 2) * 2, textHeight + 4, 0x95000000);
                MC.fontRenderer.drawStringWithShadow(text1, (int) -(textWidth1 / 2.0), -textHeight + 2, -1);
                if (WaypointsModule.INSTANCE.showDistanceSetting.getValue())
                {
                    MC.fontRenderer.drawStringWithShadow(text2, (int) -(textWidth2 / 2.0), -textHeight + MC.fontRenderer.FONT_HEIGHT + 3, -1);
                }
                glDisable(GL_DEPTH_CLAMP);
            });
        }

        MC.mcProfiler.endSection();
    };

    private Vec3 getWaypointPos(final Waypoint waypoint)
    {
        double x = waypoint.getX();
        double y = waypoint.getY();
        double z = waypoint.getZ();
        if (waypoint.getDimension() != MC.thePlayer.dimension)
        {
            if (waypoint.getDimension() == 0)
            {
                x /= 8;
                z /= 8;
            } else if (waypoint.getDimension() == -1)
            {
                x *= 8;
                z *= 8;
            }
        }
        return Vec3.createVectorHelper(x, y, z);
    }

    @Override
    public void init()
    {
        EventBus.subscribe(this);
        Nebula.CONFIGS.register(new WaypointConfig(this));
    }

    public void clear()
    {
        waypointList.clear();
    }

    public void register(final Waypoint waypoint)
    {
        waypointList.add(waypoint);
    }

    public void unregister(final Waypoint waypoint)
    {
        waypointList.remove(waypoint);
    }

    public void unregister(final String serverIp, final String name)
    {
        waypointList.removeIf((waypoint) ->
                waypoint.getServerIP().contains(serverIp) && waypoint.getName().equals(name));
    }

    public boolean exists(final String serverIp, final String name)
    {
        return waypointList.stream()
                .anyMatch((waypoint) -> waypoint.getServerIP().contains(serverIp) && waypoint.getName().equals(name));
    }

    public Waypoint get(final String serverIp, final String name)
    {
        return waypointList.stream()
                .filter((waypoint) -> waypoint.getServerIP().contains(serverIp) && waypoint.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<Waypoint> getServerWaypoints()
    {
        if (waypointList.isEmpty())
        {
            return Collections.emptyList();
        }
        final String serverIP = Nebula.SERVER.ip();
        if (serverIP.equalsIgnoreCase("unknown"))
        {
            return Collections.emptyList();
        }
        return waypointList.stream()
                .filter((waypoint) -> waypoint.getServerIP().contains(serverIP))
                .collect(Collectors.toList());
    }

    @Override
    public List<Waypoint> getAll()
    {
        return waypointList;
    }
}
