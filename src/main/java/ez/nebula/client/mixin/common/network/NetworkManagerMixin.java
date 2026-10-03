package ez.nebula.client.mixin.common.network;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.impl.module.exploit.NoPacketKickModule;
import ez.nebula.client.mixin.duck.INetworkManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.NetworkManager.attrKeyConnectionState;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = NetworkManager.class)
public abstract class NetworkManagerMixin implements INetworkManager
{
    @Shadow private Channel channel;

    @Shadow private INetHandler netHandler;

    @Shadow public abstract void setConnectionState(EnumConnectionState p_150723_1_);

    @Shadow @Final private static Logger logger;

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void hook$exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable p_exceptionCaught_1_, CallbackInfo info)
    {
        if (NoPacketKickModule.INSTANCE.isToggled())
        {
            logger.error("Internal exception:", p_exceptionCaught_1_);
            info.cancel();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void hook$channelRead0(ChannelHandlerContext channelHandlerContext, Packet p_150728_1_, CallbackInfo info)
    {
        if (channel.isOpen() && EventBus.dispatch(new EventPacket.Inbound(netHandler, p_150728_1_)))
        {
            info.cancel();
        }
    }

    @Inject(method = "dispatchPacket", at = @At("HEAD"), cancellable = true)
    private void hook$dispatchPacket(Packet packet, GenericFutureListener[] p_150732_1_, CallbackInfo info)
    {
        if (EventBus.dispatch(new EventPacket.Outbound(packet)))
        {
            info.cancel();
        }
    }

    /**
     * Just a copy-paste of the internal dispatchPacket
     * @param packet
     */
    @Override
    public void nebula$sendPacketInstantly(Packet packet)
    {
        final EnumConnectionState packetState = EnumConnectionState.func_150752_a(packet);
        final EnumConnectionState currentState = (EnumConnectionState) this.channel.attr(attrKeyConnectionState).get();

        if (currentState != packetState)
        {
            logger.debug("Disabling auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop())
        {
            if (packetState != currentState)
            {
                setConnectionState(packetState);
            }

            this.channel.writeAndFlush(packet)
                    .addListeners(new GenericFutureListener[0])
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else
        {
            this.channel.eventLoop().execute(() ->
            {
                if (packetState != currentState)
                {
                    setConnectionState(packetState);
                }

                channel.writeAndFlush(packet)
                        .addListeners(new GenericFutureListener[0])
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        }
    }
}
