package ez.nebula.client;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import ez.nebula.client.util.render.font.Fonts;
import net.minecraft.client.Minecraft;

/**
 * @author xgraza
 * @since 9/23/26
 */
@Mod(modid = "nebula_client",
        name = "Nebula",
        version = BuildConfig.VERSION)
public final class NebulaMod
{
    @Mod.EventHandler
    public void postInit(final FMLPostInitializationEvent event)
    {
        Fonts.initFonts();
        Nebula.init(Minecraft.getMinecraft().mcDataDir);
    }
}
