package ez.nebula.client.forge;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.*;

/**
 * @author xgraza
 * @since 9/25/26
 */
@IFMLLoadingPlugin.MCVersion("1.7.2")
@IFMLLoadingPlugin.Name("NebulaMixinLoadingPlugin")
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE)
@SuppressWarnings("unused")
public final class NebulaMixinLoadingPlugin implements IFMLLoadingPlugin
{
    private static final Logger LOGGER = LogManager.getLogger("Nebula Mixin");

    public NebulaMixinLoadingPlugin() throws Throwable
    {
        LOGGER.info("Adding Transformer exclusions...");
        Launch.classLoader.addTransformerExclusion("ez.nebula.client.api");
        Launch.classLoader.addTransformerExclusion("ez.nebula.client.impl");
        Launch.classLoader.addTransformerExclusion("ez.nebula.client.util");
        Launch.classLoader.addTransformerExclusion("net.minecraft.src");

        LOGGER.info("Adding LegacyJavaFixer to TweakClasses");
        ((List) Launch.blackboard.get("TweakClasses")).add("ez.nebula.client.forge.patch.LegacyFixTweaker");

        LOGGER.info("Mixin init");
        MixinBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setOption(MixinEnvironment.Option.DEBUG_ALL, true);
        Mixins.addConfiguration("mixins.nebula.json");
    }

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{ "ez.nebula.client.forge.patch.ASMParserPatcher" };
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
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
