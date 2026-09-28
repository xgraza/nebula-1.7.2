package ez.nebula.client.impl.module.render;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import net.minecraft.client.gui.GuiPlayerInfo;

import java.util.regex.Pattern;

/**
 * @author xgraza
 * @since 6/16/26
 */
@ModuleManifest(name = "NameProtect",
        description = "If to protect usernames from being shown",
        category = ModuleCategory.RENDER)
public final class NameProtectModule extends Module
{
    @ModuleInstance
    public static NameProtectModule INSTANCE;

    private final Setting<Boolean> allOnlinePlayersSetting = builder("All Online Players", false)
            .setDescription("If to protect all online players usernames")
            .build();
    private final Setting<String> selfAliasSetting = builder("Self Alias", "Nebula User")
            .setDescription("The alias to use for yourself")
            .build();

    public String protect(String text)
    {
        if (!INSTANCE.isToggled())
        {
            return text;
        }

        text = text.replaceAll("(?ui)" + Pattern.quote(MC.getSession().getUsername()), selfAliasSetting.getValue());

        int i = 1;
        for (final String friendName : Nebula.FRIENDS.getAll())
        {
            text = text.replaceAll("(?ui)" + Pattern.quote(friendName), "Friend " + i);
            ++i;
        }

        if (allOnlinePlayersSetting.getValue())
        {
            for (final Object o : MC.thePlayer.sendQueue.playerInfoList)
            {
                if (!(o instanceof GuiPlayerInfo))
                {
                    continue;
                }
                final GuiPlayerInfo info = (GuiPlayerInfo)o;
                final String name = info.name;
                if (!Nebula.FRIENDS.has(name))
                {
                    text = text.replaceAll("(?ui)" + Pattern.quote(name), "Player");
                }
            }
        }

        return text;
    }
}
