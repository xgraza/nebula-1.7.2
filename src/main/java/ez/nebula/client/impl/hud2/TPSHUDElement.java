package ez.nebula.client.impl.hud2;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.hud2.trait.HUDManifest;
import ez.nebula.client.api.manager.hud2.type.TextHUDElement;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import net.minecraft.util.EnumChatFormatting;

/**
 * @author xgraza
 * @since 9/6/26
 */
@HUDManifest(value = "TPS", description = "Displays the ticks per second the server is executing")
public final class TPSHUDElement extends TextHUDElement
{
    private final Setting<Boolean> showCurrentSetting = builder("Show Current", false)
            .setDescription("If to show the current calculated TPS versus the average")
            .build();

    @Override
    public String text()
    {
        return ChatUtil.NEBULA_CLIENT_COLOR
                + "TPS: "
                + EnumChatFormatting.GRAY
                + String.format("%.1f", showCurrentSetting.getValue()
                    ? Nebula.SERVER.tps()
                    : Nebula.SERVER.averageTPS());
    }
}
