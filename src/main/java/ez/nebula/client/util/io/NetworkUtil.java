package ez.nebula.client.util.io;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author xgraza
 * @since 03/24/25
 */
public final class NetworkUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static int getLatency(final EntityPlayer player)
    {
        String playerName = player.getCommandSenderName();
        if (playerName == null || playerName.isEmpty())
        {
            return 0;
        }
        playerName = EnumChatFormatting.getTextWithoutFormattingCodes(playerName);
        for (final Object obj : MC.thePlayer.sendQueue.playerInfoList)
        {
            if (!(obj instanceof GuiPlayerInfo))
            {
                continue;
            }
            final GuiPlayerInfo playerInfo = (GuiPlayerInfo) obj;
            final String name = EnumChatFormatting.getTextWithoutFormattingCodes(playerInfo.name);
            if (name.equals(playerName))
            {
                return playerInfo.responseTime;
            }
        }
        return 0;
    }

    public static String queryString(final String[]... params)
    {
        return Arrays.stream(params).map((arr) ->
        {
            String value = "";
            if (arr.length == 2)
            {
                value = "=" + arr[1];
            }
            return encodeUri(arr[0]) + value;
        }).collect(Collectors.joining("&"));
    }

    public static String encodeUri(final String t)
    {
        try
        {
            return URLEncoder.encode(t, StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException e)
        {
            return t;
        }
    }
}
