package ez.nebula.client.forge;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import ez.nebula.client.util.fml.FMLHelper;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author xgraza
 * @since 9/25/26
 */
@IFMLLoadingPlugin.MCVersion("1.7.2")
@IFMLLoadingPlugin.Name("NebulaFMLCoreModPlugin")
@SuppressWarnings("unused")
public final class NebulaFMLCoreModPlugin implements IFMLLoadingPlugin
{
    private static final Logger LOGGER = LogManager.getLogger("Nebula Coremod");

    static
    {
        System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
        System.setProperty("log4j2.formatMsgNoLookups", "true");
        System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "false");
        System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "false");
        LOGGER.info("Set system properties");
    }

    public NebulaFMLCoreModPlugin()
    {
        if (!FMLHelper.isDeobfEnv())
        {
            LOGGER.info("We are in a forge enviornment, we must add ourselves to the CL...");
            // retarded? yes. works? yes...
            try
            {
                final URL jarUrl = NebulaFMLCoreModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
                final ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
                if (sysClassLoader instanceof URLClassLoader)
                {
                    Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    addURL.setAccessible(true);
                    addURL.invoke(sysClassLoader, jarUrl);
                    LOGGER.info("Successfully added {} to System CL", jarUrl);
                } else
                {
                    LOGGER.error("Current CL is not URLClassLoader? Did launchwrapper already override?");
                }
            } catch (Exception e)
            {
                LOGGER.error("Failed to inject JAR into System CL", e);
            }
        }

        LOGGER.info("Adding MixinTweaker & LegacyFixTweaker to tweaks");
        ((List) Launch.blackboard.get("TweakClasses")).add("org.spongepowered.asm.launch.MixinTweaker");
        ((List) Launch.blackboard.get("TweakClasses")).add("ez.nebula.client.forge.tweak.NebulaTweaker");

        LOGGER.info("Adding/removing transformer & classloader exclusions");
        Launch.classLoader.addTransformerExclusion("ez.nebula.client.api");
        Launch.classLoader.addTransformerExclusion("ez.nebula.client.impl");
        Launch.classLoader.addTransformerExclusion("ez.nebula.client.util");
        Launch.classLoader.addTransformerExclusion("net.minecraft.src");
    }

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{ "ez.nebula.client.forge.asm.FMLPatcher" };
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        data.forEach((k, v) -> LOGGER.info("{} : {}", k, v));
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
