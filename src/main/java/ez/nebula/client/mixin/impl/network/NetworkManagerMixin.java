package ez.nebula.client.mixin.impl.network;

import ez.nebula.client.mixin.duck.INetworkManager;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = NetworkManager.class)
public abstract class NetworkManagerMixin implements INetworkManager
{
    @Shadow protected abstract void dispatchPacket(Packet p_150732_1_, GenericFutureListener[] p_150732_2_);

    @Override
    public void nebula$sendPacketInstantly(Packet packet)
    {
        dispatchPacket(packet, new GenericFutureListener[0]);
    }
}
