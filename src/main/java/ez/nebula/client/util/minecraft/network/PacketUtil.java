package ez.nebula.client.util.minecraft.network;

import ez.nebula.client.mixin.duck.INetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

public final class PacketUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static void send(final Packet packet)
    {
        MC.getNetHandler().addToSendQueue(packet);
    }

    public static void sendRepeated(final int times, final Packet packet)
    {
        if (times <= 0)
        {
            return;
        }
        for (int i = 0; i < times; ++i)
        {
            send(packet);
        }
    }

    public static void sendInstant(final Packet packet)
    {
        ((INetworkManager)MC.getNetHandler().getNetworkManager()).nebula$sendPacketInstantly(packet);
    }
}
