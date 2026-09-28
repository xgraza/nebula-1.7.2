package ez.nebula.client.impl.hud2;

import ez.nebula.client.api.manager.hud2.trait.HUDManifest;
import ez.nebula.client.api.manager.hud2.type.TextHUDElement;
import ez.nebula.client.mixin.duck.IMinecraft;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import ez.nebula.client.util.text.FormattingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

/**
 * @author xgraza
 * @since 9/6/26
 */
@HUDManifest(value = "FPS", description = "Displays the amount of frames per second rendered on screen")
public final class FPSHUDElement extends TextHUDElement
{
    @Override
    public String text()
    {
        return ChatUtil.NEBULA_CLIENT_COLOR
                + "FPS: "
                + EnumChatFormatting.GRAY
                + ((IMinecraft)MC).nebula$getDebugFPS()
                + (!MC.inGameHasFocus ? " [idle]" : "");
    }
}
