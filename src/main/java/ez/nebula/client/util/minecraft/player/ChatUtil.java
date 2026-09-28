package ez.nebula.client.util.minecraft.player;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author xgraza
 * @since 02/15/25
 */
public final class ChatUtil
{
    private static final Logger LOGGER = LogManager.getLogger("Nebula Chat");
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static final String NEBULA_CLIENT_COLOR = "\u00a7z";

    private static final String CHAT_PREFIX = String.format(
            "%s(Nebula):%s ",
            NEBULA_CLIENT_COLOR,
            EnumChatFormatting.RESET);

    public static final String DEBUG_PREFIX = String.format(
            "%s(DEBUG):%s ",
            EnumChatFormatting.YELLOW,
            EnumChatFormatting.RESET);
    public static final String VERBOSE_PREFIX = EnumChatFormatting.BLUE
            + "(%s): "
            + EnumChatFormatting.RESET;
    public static final String WORLD_DOWNLOADER_PREFIX = String.format(
            "%s(WDL):%s ",
            EnumChatFormatting.DARK_GREEN,
            EnumChatFormatting.RESET);

    public static void sendFormatted(final List<IChatComponent> componentList)
    {
        for (final IChatComponent component : componentList)
        {
            if (MC.ingameGUI == null || MC.thePlayer == null)
            {
                LOGGER.info(component);
            } else
            {
                MC.ingameGUI.getChatGUI().printChatMessage(component);
            }
        }
    }

    public static void sendFormatted(final String chatPrefix, final String content, final Object... format)
    {
        sendFormatted(format(createBaseChatComponent(chatPrefix), content, format));
    }

    public static void sendNebula(final String content, final Object... format)
    {
        sendFormatted(CHAT_PREFIX, content, format);
    }

    public static List<IChatComponent> format(final IChatComponent parent, String content, final Object... format)
    {
        content = content.replaceAll("(?i)&([0-9A-FK-ORZ])", "§$1");
        content = String.format(content, format);

        final List<IChatComponent> componentList = new LinkedList<>();
        final String[] lines = content.split("\n");
        for (final String line : lines)
        {
            final IChatComponent component = parent == null ? new ChatComponentText("") : parent;
            component.appendText(line);
            componentList.add(component);
        }

        return componentList;
    }

    private static IChatComponent createBaseChatComponent(final String chatPrefix)
    {
        return new ChatComponentText(chatPrefix).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));
    }
}
