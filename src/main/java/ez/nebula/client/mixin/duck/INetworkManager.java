package ez.nebula.client.mixin.duck;

import net.minecraft.network.Packet;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface INetworkManager
{
    void nebula$sendPacketInstantly(final Packet packet);
}
