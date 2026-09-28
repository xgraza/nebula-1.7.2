package ez.nebula.client.mixin.impl.network;

import ez.nebula.client.mixin.duck.INetHandlerPlayClient;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
