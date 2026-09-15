package ez.nebula.client.api.manager.plugin;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.ITypedManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * @author xgraza
 * @since 9/14/26
 */
public final class PluginManager implements ITypedManager<Plugin>
{
    private static final Logger LOGGER = LogManager.getLogger("Plugins");

    private final CustomClassLoader classLoader = new CustomClassLoader(PluginManager.class.getClassLoader());
    private final List<Plugin> pluginList = new ArrayList<>();
    private File pluginDirectory;

    @Override
    public void init()
    {
        Thread.currentThread().setContextClassLoader(classLoader);
        pluginDirectory = new File(Nebula.NEBULA_ROOT, "plugins");
        scan();
    }

    /**
     * Registers a plugin
     * @param plugin the plugin instance
     */
    void register(final Plugin plugin)
    {
        pluginList.add(plugin);
    }

    /**
     * Unregisters a plugin
     * @param plugin the plugin instance
     */
    void unregister(final Plugin plugin)
    {
        pluginList.remove(plugin);
    }

    public void scan()
    {
        unload();

        if (!pluginDirectory.exists())
        {
            LOGGER.warn("{} folder does not exist", pluginDirectory.getAbsolutePath());
            return;
        }

        final File[] files = pluginDirectory.listFiles((f) -> f.getName().endsWith(".jar"));
        if (files == null || files.length == 0)
        {
            LOGGER.warn("No plugins found");
            return;
        }
        LOGGER.debug("Walking {} plugins", files.length);

        for (final File file : files)
        {
            try (final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(file.toPath()))))
            {
                LOGGER.info("Attempting to load plugin file {} ({} B)", file.getName(), Files.size(file.toPath()));

                final List<Class<?>> classes = classLoader.from(zis);
                if (classes.isEmpty())
                {
                    LOGGER.warn("No classes loaded for {}", file.getName());
                    return;
                }
                for (final Class<?> clazz : classes)
                {
                    if (!Plugin.class.isAssignableFrom(clazz))
                    {
                        continue;
                    }
                    LOGGER.debug("Found class {}", clazz);
                    final Plugin plugin = (Plugin) clazz.getConstructors()[0].newInstance();
                    classes.forEach((c) -> plugin.loadedClassList.add(c.getCanonicalName()));
                    register(plugin); // register before init so we can unload
                    LOGGER.info("Initializing plugin \"{}\"", plugin.getName());
                    plugin.init();
                    return; // only one Plugin class per plugin
                }
            } catch (final IOException | InvocationTargetException | IllegalAccessException | InstantiationException e)
            {
                LOGGER.error("Failed to load .jar file", e);
            }
        }
    }

    public void unload()
    {
        pluginList.forEach((plugin) ->
        {
            try
            {
                LOGGER.debug("Unloading plugin classes for {}", plugin);
                unload(plugin);
                LOGGER.info("Unloading plugin \"{}\"", plugin.getName());
                plugin.onUnload();
            } catch (Exception e)
            {
                LOGGER.error("Failed to unload plugin \"{}\"", plugin.getName());
                LOGGER.error(e);
            }
            unregister(plugin);
        });
        pluginList.clear();
    }

    public void unload(final Plugin plugin)
    {
        for (final String className : plugin.loadedClassList)
        {
            classLoader.define(className, new byte[0]);
        }
    }

    @Override
    public List<Plugin> getAll()
    {
        return pluginList;
    }
}