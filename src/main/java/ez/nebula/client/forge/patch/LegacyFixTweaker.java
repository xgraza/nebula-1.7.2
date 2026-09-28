package ez.nebula.client.forge.patch;

import com.google.common.base.Throwables;
import com.google.common.primitives.Ints;
import cpw.mods.fml.common.launcher.FMLInjectionAndSortingTweaker;
import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * @author LegacyJavaFixer
 * https://github.com/MinecraftForge/LegacyJavaFixer
 */
@SuppressWarnings("unused")
public final class LegacyFixTweaker implements ITweaker
{
    private static final Logger LOGGER = LogManager.getLogger("Tweak Fix");

    public LegacyFixTweaker() throws Throwable
    {
        ListIterator<ITweaker> itr = ((List<ITweaker>)Launch.blackboard.get("Tweaks")).listIterator();
        ITweaker replacement = new SortReplacement();
        while (itr.hasNext())
        {
            ITweaker t = itr.next();
            FMLRelaunchLog.log.info("[LegacyJavaFixer] Tweaker: " + t);
            if (t instanceof FMLInjectionAndSortingTweaker)
            {
                itr.set(replacement);
                FMLRelaunchLog.info("[LegacyJavaFixer] Replacing tweaker %s with %s", t, replacement);
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
        private final Class<?> wrapperCls = Class.forName("cpw.mods.fml.relauncher.CoreModManager$FMLPluginWrapper", false, SortReplacement.class.getClassLoader());
        private final Field wrapperField = this.wrapperCls.getDeclaredField("sortIndex");
        private final Map<String, Integer> tweakSorting = ReflectionHelper.getPrivateValue(CoreModManager.class, null, new String[]{"tweakSorting"});

        public SortReplacement() throws Throwable
        {
            this.wrapperField.setAccessible(true);
        }

        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader)
        {
            if (!this.hasRun)
            {
                LOGGER.info("Replacing sort");
                this.sort();
                URL is = FMLInjectionAndSortingTweaker.class.getResource("/cpw/mods/fml/common/launcher/TerminalTweaker.class");
                if (is != null)
                {
                    LOGGER.info("Detected TerminalTweaker");
                    List newTweaks = (List) Launch.blackboard.get("TweakClasses");
                    newTweaks.add("cpw.mods.fml.common.launcher.TerminalTweaker");
                }
            }
            this.hasRun = true;
        }

        private void sort()
        {
            List tweakers = (List) Launch.blackboard.get("Tweaks");
            ITweaker[] toSort = (ITweaker[]) tweakers.toArray(new ITweaker[tweakers.size()]);
            Arrays.sort(toSort, new Comparator<ITweaker>()
            {
                @Override
                public int compare(ITweaker o1, ITweaker o2)
                {
                    return Ints.saturatedCast((long)this.getIndex(o1) - (long)this.getIndex(o2));
                }

                private int getIndex(ITweaker t)
                {
                    try
                    {
                        if (t instanceof SortReplacement)
                        {
                            return Integer.MIN_VALUE;
                        }
                        if (SortReplacement.this.wrapperCls.isInstance(t))
                        {
                            return SortReplacement.this.wrapperField.getInt(t);
                        }
                        if (SortReplacement.this.tweakSorting.containsKey(t.getClass().getName()))
                        {
                            return SortReplacement.this.tweakSorting.get(t.getClass().getName());
                        }
                    }
                    catch (Throwable throwable)
                    {
                        Throwables.propagate(throwable);
                    }
                    return 0;
                }
            });

            for (int j = 0; j < toSort.length; ++j)
            {
                tweakers.set(j, toSort[j]);
            }
        }

        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
        {
        }

        public String[] getLaunchArguments()
        {
            return new String[0];
        }

        public String getLaunchTarget()
        {
            return "";
        }
    }
}
