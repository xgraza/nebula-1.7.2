package ez.nebula.client.api.config;

import ez.nebula.client.api.manager.IManager;
import ez.nebula.client.impl.config.ClientSettingConfig;
import ez.nebula.client.impl.config.ModuleConfig;
import ez.nebula.client.util.io.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xgraza
 * @since 02/14/25
 */
public final class ConfigManager implements IManager
{
    private static final Logger LOGGER = LogManager.getLogger("Configs");
    
    private final List<IConfig> configList = new ArrayList<>();

    @Override
    public void init()
    {
        Runtime.getRuntime().addShutdownHook(new ConfigSaveThread(this));
        register(new ClientSettingConfig());
        try
        {
            load();
        } catch (final IOException e)
        {
            LOGGER.error("Failed to load configs!", e);
        }
        try
        {
            ModuleConfig.loadConfig("default");
        } catch (final IOException e)
        {
            LOGGER.error("Failed to load default module config!", e);
        }
    }

    public void register(final IConfig config)
    {
        configList.add(config);
    }

    public boolean save(final IConfig config)
    {
        final File file = config.getLocation();
        if (!file.getParentFile().exists())
        {
            if (!file.getParentFile().mkdir())
            {
                LOGGER.error("Failed to create parent directory {}", file);
                return false;
            }
            LOGGER.info("Created parent directory {}", file);
        }
        if (!file.exists())
        {
            try
            {
                if (!file.createNewFile())
                {
                    LOGGER.error("Failed to create file {}", file);
                }
            } catch (final IOException e)
            {
                LOGGER.error(e);
                return false;
            }
        }

        final String data = config.write();
        if (data == null || data.isEmpty())
        {
            LOGGER.warn("Save data for {} was empty", file);
            return false;
        }

        try
        {
            FileUtil.save(file, data);
        } catch (final IOException e)
        {
            LOGGER.error(e);
        }
        return true;
    }

    private void load() throws IOException
    {
        LOGGER.info("Attempting to load {} configs...", configList.size());
        int loaded = 0;
        for (final IConfig config : configList)
        {
            final File file = config.getLocation();
            if (!file.exists())
            {
                LOGGER.warn("Configuration file {} does not exist", file);
                continue;
            }
            final String data = FileUtil.read(file);
            if (!data.isEmpty())
            {
                try
                {
                    long endTime;
                    long startTime = System.nanoTime();
                    config.read(data);
                    endTime = System.nanoTime();
                    LOGGER.info("Loaded {} successfully in {}ms",
                            config.getClass().getSimpleName(), (endTime - startTime) / 1000000.0);
                    ++loaded;
                } catch (final Exception e)
                {
                    LOGGER.error("Failed to load a config", e);
                }
            } else
            {
                LOGGER.warn("{} had empty file contents", config);
            }
        }
        LOGGER.info("Loaded {}/{} configs successfully", loaded, configList.size());
    }

    public List<IConfig> getConfigs()
    {
        return configList;
    }
}
