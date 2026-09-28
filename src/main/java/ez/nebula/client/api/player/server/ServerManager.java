package ez.nebula.client.api.player.server;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.api.manager.IManager;
import ez.nebula.client.util.io.NetworkUtil;
import ez.nebula.client.util.math.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.play.server.S03PacketTimeUpdate;

import java.util.Arrays;

/**
 * @author xgraza
 * @since 5/5/26
 */
public final class ServerManager implements IManager
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    private final Timer packetTimer = new Timer();

    private final double[] packetResponseTimes = new double[20];
    private long lastServerTimePacketMS = -1L;
    private int index = 0;

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        packetTimer.resetTime();
        if (!(event.getPacket() instanceof S03PacketTimeUpdate))
        {
            return;
        }
        final long time = System.currentTimeMillis();
        if (lastServerTimePacketMS == -1L)
        {
            lastServerTimePacketMS = time;
            return;
        }
        final double difference = time - lastServerTimePacketMS;
        lastServerTimePacketMS = time;
        final double tps = 20.0 / (difference / 1000.0);
        packetResponseTimes[index++ % packetResponseTimes.length] = Math.max(0.0, Math.min(tps, 20.0));
    };

    @Subscribe
    private final EventListener<EventChangeWorld> changeWorldEventListener = event ->
    {
        Arrays.fill(packetResponseTimes, 0);
        index = 0;
        lastServerTimePacketMS = -1L;
        packetTimer.resetTime();
    };

    @Override
    public void init()
    {
        EventBus.subscribe(this);
        Arrays.fill(packetResponseTimes, 0);
    }

    public String ip()
    {
        final ServerData serverData = MC.func_147104_D();
        if (serverData == null || serverData.serverIP == null)
        {
            if (MC.isSingleplayer())
            {
                return "SP";
            }
            return "Unknown";
        }
        return serverData.serverIP.split(":")[0];
    }

    public double timeSinceLastPacket()
    {
        return packetTimer.getTimeElapsedMS();
    }

    public double averageTPS()
    {
        double sum = 0.0;
        int amount = 0;
        for (final double timeDifference : packetResponseTimes)
        {
            if (timeDifference > 0.0)
            {
                sum += timeDifference;
                ++amount;
            }
        }
        return amount == 0 ? 20.0 : sum / amount;
    }

    public double tps()
    {
        return packetResponseTimes[Math.max(0, index - 1) % packetResponseTimes.length];
    }

    public double scaledLatency()
    {
        return (50L + NetworkUtil.getLatency(MC.thePlayer)) * (1.0 / Math.min(20.0, tps()));
    }
}
