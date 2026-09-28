package ez.nebula.client.mixin.duck;

import net.minecraft.client.gui.GuiPlayerInfo;

import java.util.Map;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface INetHandlerPlayClient
{
    Map<String, GuiPlayerInfo> nebula$getPlayerInfoMap();

    boolean nebula$getDoneLoadingTerrain();

    void nebula$setDoneLoadingTerrain(boolean bl);
}
