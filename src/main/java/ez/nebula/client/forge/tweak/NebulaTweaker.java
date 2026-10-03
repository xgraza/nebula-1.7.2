package ez.nebula.client.forge.tweak;

import com.google.common.base.Throwables;
import com.google.common.primitives.Ints;
import cpw.mods.fml.common.launcher.FMLInjectionAndSortingTweaker;
import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * @author LegacyJavaFixer, xgraza
 * https://github.com/MinecraftForge/LegacyJavaFixer
 */
@SuppressWarnings("unused")
public final class NebulaTweaker implements ITweaker
{
    private static final Logger LOGGER = LogManager.getLogger("Nebula Tweaker");

    public NebulaTweaker()
    {
        try
        {
            Class.forName("net.minecraftforge.legacyjavafixer.sort.LegacyJavaSortDummyLoadingPlugin");
            LOGGER.info("LegacyJavaFixer mod found, skipping bundled tweaker");
            return;
        } catch (ClassNotFoundException e)
        {
            LOGGER.info("LegacyJavaFixer mod not found!");
        }

        LOGGER.info("Running bundled https://github.com/MinecraftForge/LegacyJavaFixer");
        ListIterator<ITweaker> itr = ((List<ITweaker>)Launch.blackboard.get("Tweaks")).listIterator();
        while (itr.hasNext())
        {
            ITweaker t = itr.next();
            if (t instanceof FMLInjectionAndSortingTweaker)
            {
                LOGGER.info("Replacing tweaker {} with fixed sort replacement tweaker", t);
                itr.set(new SortReplacement());
            }
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
    {

    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader)
    {
        LOGGER.info("Mixin init");
        MixinBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setOption(MixinEnvironment.Option.DEBUG_ALL, true);
        Mixins.addConfiguration("mixins.nebula.json");
    }

    @Override
    public String getLaunchTarget()
    {
        return "";
    }

    @Override
    public String[] getLaunchArguments()
    {
        return new String[0];
    }

    public static class SortReplacement implements ITweaker
    {
        private boolean hasRun = false;
        Class<?> wrapperCls = null;
        Field wrapperField = null;
        Map<String, Integer> tweakSorting = null;

        SortReplacement()
        {
            try
            {
                wrapperCls = Class.forName("cpw.mods.fml.relauncher.CoreModManager$FMLPluginWrapper", false, SortReplacement.class.getClassLoader());
                wrapperField = wrapperCls.getDeclaredField("sortIndex");
                wrapperField.setAccessible(true);
                tweakSorting = ReflectionHelper.getPrivateValue(CoreModManager.class, null, "tweakSorting");
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to reflect needed properties", e);
            }
        }

        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader)
        {
            if (!hasRun)
            {
                @SuppressWarnings("unchecked")
                List<String> newTweaks = (List<String>) Launch.blackboard.get("TweakClasses");
                LOGGER.info("Replacing sort");
                sort();
                URL is = FMLInjectionAndSortingTweaker.class.getResource("/cpw/mods/fml/common/launcher/TerminalTweaker.class");
                if (is != null)
                {
                    LOGGER.info("Detected TerminalTweaker");
                    newTweaks.add("cpw.mods.fml.common.launcher.TerminalTweaker");
                }
            }
            hasRun = true;
        }
        //Copied from FML's fixed version.
        @SuppressWarnings("unchecked")
        private void sort()
        {
            List<ITweaker> tweakers = (List<ITweaker>) Launch.blackboard.get("Tweaks");
            // Basically a copy of Collections.sort pre 8u20, optimized as we know we're an array list.
            // Thanks unhelpful fixer of http://bugs.java.com/view_bug.do?bug_id=8032636
            ITweaker[] toSort = tweakers.toArray(new ITweaker[0]);
            Arrays.sort(toSort, new Comparator<ITweaker>()
            {
                @Override
                public int compare(ITweaker o1, ITweaker o2)
                {
                    return Ints.saturatedCast((long)getIndex(o1) - (long)getIndex(o2));
                }
                private int getIndex(ITweaker t)
                {
                    try
                    {
                        if (t instanceof SortReplacement) return Integer.MIN_VALUE;
                        if (wrapperCls.isInstance(t)) return wrapperField.getInt(t);
                        if (tweakSorting.containsKey(t.getClass().getName())) return tweakSorting.get(t.getClass().getName());
                    }
                    catch (Exception e)
                    {
                        Throwables.propagate(e);
                    }
                    return 0;
                }
            });
            // Basically a copy of Collections.sort, optimized as we know we're an array list.
            // Thanks unhelpful fixer of http://bugs.java.com/view_bug.do?bug_id=8032636
            for (int j = 0; j < toSort.length; j++) {
                tweakers.set(j, toSort[j]);
            }
        }

        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
        {

        }

        @Override
        public String[] getLaunchArguments()
        {
            return new String[0];
        }

        @Override
        public String getLaunchTarget()
        {
            return "";
        }
    }
}
