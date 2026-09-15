package ez.nebula.client.api.manager.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xgraza
 * @since 9/14/26
 */
public abstract class Plugin
{
    protected final Logger logger = LogManager.getLogger(getName());

    final List<String> loadedClassList = new ArrayList<>();

    public abstract void init();

    public void onUnload()
    {

    }

    public abstract String getName();
}
