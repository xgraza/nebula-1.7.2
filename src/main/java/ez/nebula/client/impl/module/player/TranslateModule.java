package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.listener.event.world.EventPlace;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IS02PacketChat;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.render.font.Fonts;
import ez.nebula.client.util.render.world.Render3D;
import ez.nebula.client.util.text.FormattingUtil;
import ez.nebula.client.util.text.translation.GoogleTranslateService;
import ez.nebula.client.util.text.translation.Language;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.src.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xgraza
 * @since 03/24/25
 */
@ModuleManifest(name = "Translate",
        description = "Gives you the option to translate text in the game chat",
        category = ModuleCategory.PLAYER)
public final class TranslateModule extends Module
{
    @ModuleInstance
    public static TranslateModule INSTANCE;

    private final EnumSetting<Language> targetSetting = enumBuilder("Target", Language.ENGLISH)
            .setDescription("The language to translate to")
            .build();
    private final Setting<Boolean> signsSetting = builder("Signs", false)
            .setDescription("If to translate signs on a right click")
            .build();

    private final Map<BlockPos, String> signPosTranslateMap = new HashMap<>();

    @Override
    public void onEnable()
    {
        super.onEnable();
        signPosTranslateMap.clear();
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (!signsSetting.getValue())
        {
            return;
        }
        final MovingObjectPosition result = MC.objectMouseOver;
        if (result == null || result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
        {
            return;
        }

        final BlockPos pos = new BlockPos(result.blockX, result.blockY, result.blockZ);
        final String translatedText = signPosTranslateMap.get(pos);
        if (translatedText == null || translatedText.isEmpty())
        {
            return;
        }

        MC.mcProfiler.startSection("translate_sign_hover");

        Render3D.billboard(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.25, () ->
        {
            final List<String> lines = Fonts.POPPINS.wrapText(translatedText, 120, true);
            Collections.reverse(lines); // wtf google translate
            double posY = ((Fonts.POPPINS.getFontHeight() + 1) * lines.size()) / 2.0;
            for (final String line : lines)
            {
                final double length = Fonts.POPPINS.getStringWidth(line);
                Fonts.POPPINS.drawStringShadow(line, -(length / 2), -posY, -1);
                posY += Fonts.POPPINS.getFontHeight() + 1;
            }
        });

        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S02PacketChat)
        {
            final S02PacketChat packet = event.getPacket();
            final IChatComponent component = packet.func_148915_c();
            if (!component.getUnformattedText().startsWith("<"))
            {
                return;
            }
            final ChatComponentText c = new ChatComponentText(component.getFormattedText());
            c.setChatStyle(new ChatStyle()
                    .setChatClickEvent(new ClickEvent(null, "NEBULA_TRANSLATE"))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText("Click to translate")
                                    .setChatStyle(new ChatStyle()
                                            .setColor(EnumChatFormatting.BLUE)))));
            ((IS02PacketChat)packet).nebula$setMessage(c);
        }
    };

    @Subscribe
    private final EventListener<EventPlace> placeEventListener = event ->
    {
        if (!signsSetting.getValue())
        {
            return;
        }
        final BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
        if (signPosTranslateMap.containsKey(pos))
        {
            return;
        }
        final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
        if (block instanceof BlockSign)
        {
            final TileEntity tileEntity = MC.theWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (!(tileEntity instanceof TileEntitySign))
            {
                return;
            }
            final TileEntitySign tileEntitySign = (TileEntitySign)tileEntity;
            signPosTranslateMap.put(pos, "Translating...");
            GoogleTranslateService.INSTANCE.translate(
                    targetSetting.getValue(), Language.AUTO, String.join(" ", tileEntitySign.signText),
                    (source, text) -> signPosTranslateMap.put(pos, text));
        }
    };

    public void handleTranslate(final IChatComponent component)
    {
        final String raw = StringUtils.stripControlCodes(component.getUnformattedText());
        final String playerName = FormattingUtil.parseUsernameFromChat(raw, "Player");
        final String unformatted = raw.replaceFirst(FormattingUtil.PLAYER_TAG_REGEX.pattern(), "").trim();
        GoogleTranslateService.INSTANCE.translate(
                targetSetting.getValue(), Language.AUTO, unformatted,
                (source, text) ->
                {
                    final ChatComponentText c = new ChatComponentText("");
                    c.appendSibling(new ChatComponentText("[from " + source.getLocale() + "]")
                            .setChatStyle(new ChatStyle()
                                    .setColor(EnumChatFormatting.BLUE)));
                    c.appendText(" ");
                    c.appendText("<" + playerName + ">");
                    c.appendText(" ");
                    c.appendText(text);
                    MC.ingameGUI.getChatGUI().printChatMessage(c);
                });
    }
}
