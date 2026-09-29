package ez.nebula.client.mixin.impl.network;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.player.EventPlayerDeath;
import ez.nebula.client.mixin.duck.INetHandlerPlayClient;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = NetHandlerPlayClient.class)
public class NetHandlerPlayClientMixin implements INetHandlerPlayClient
{
    @Shadow private boolean doneLoadingTerrain;
    @Shadow private Map playerInfoMap;

    @Inject(method = "handleEntityMetadata", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hook$handleEntityMetadata$playerDeathEvent(S1CPacketEntityMetadata packet, CallbackInfo info, Entity entity)
    {
        if (!(entity instanceof EntityPlayer))
        {
            return;
        }
        for (final Object o : packet.func_149376_c())
        {
            if (!(o instanceof DataWatcher.WatchableObject))
            {
                continue;
            }
            final DataWatcher.WatchableObject object = (DataWatcher.WatchableObject) o;
            // 6 = health, object is 0.0f-1.0f (or max health)
            if (object.getDataValueId() == 6 && ((float) object.getObject()) == 0.0f)
            {
                EventBus.dispatch(new EventPlayerDeath((EntityPlayer) entity));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, GuiPlayerInfo> nebula$getPlayerInfoMap()
    {
        return playerInfoMap;
    }

    @Override
    public boolean nebula$getDoneLoadingTerrain()
    {
        return doneLoadingTerrain;
    }

    @Override
    public void nebula$setDoneLoadingTerrain(boolean bl)
    {
        doneLoadingTerrain = bl;
    }
}
