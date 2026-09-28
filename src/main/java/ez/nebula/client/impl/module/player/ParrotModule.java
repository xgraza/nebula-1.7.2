package ez.nebula.client.impl.module.player;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.text.FormattingUtil;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.StringUtils;

/**
 * @author xgraza
 * @since 8/21/26
 */
@ModuleManifest(name = "Parrot",
        description = "Repeat what other players say like a parrot",
        category = ModuleCategory.PLAYER)
public final class ParrotModule extends Module
{
    private final Setting<Boolean> friendsSetting = builder("Friends", false)
            .setDescription("If to repeat your friends")
            .build();

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (MC.thePlayer == null || MC.theWorld == null)
        {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat)
        {
            final S02PacketChat packet = event.getPacket();
            final String message = StringUtils.stripControlCodes(packet.func_148915_c().getUnformattedText());

            final String username = FormattingUtil.parseUsernameFromChat(message, null);
            if (username == null || username.isEmpty() || username.equalsIgnoreCase(MC.getSession().getUsername()))
            {
                return;
            }

            if (!friendsSetting.getValue() && Nebula.FRIENDS.has(username))
            {
                return;
            }

            MC.thePlayer.sendQueue.addToSendQueue(new C01PacketChatMessage(
                    ChatAllowedCharacters.filerAllowedCharacters(
                            message.replaceFirst(FormattingUtil.PLAYER_TAG_REGEX.pattern(), ""))));
        }
    };
}
