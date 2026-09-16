package ez.nebula.client.api.manager.hud;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.manager.ITypedManager;
import ez.nebula.client.impl.config.HUDConfig;
import ez.nebula.client.impl.hud.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author xgraza
 * @since 3/23/26
 */
public final class HUDManager implements ITypedManager<HUDElement>
{
    static final Logger LOGGER = LogManager.getLogger("HUD");

    private final List<HUDElement> hudElementList = new LinkedList<>();

    @Override
    public void init()
    {
        EventBus.subscribe(this);
        Nebula.CONFIGS.register(new HUDConfig(this));

        register(new ArmorStatusHUDElement());
        register(new ArraylistHUDElement());
        register(new CoordinatesHUDElement());
        register(new FPSHUDElement());
        register(new PotionStatusHUDElement());
        register(new ServerStatusHUDElement());
        register(new SpeedHUDElement());
        register(new TargetDisplayHUDElement());
        register(new TPSHUDElement());
        register(new WatermarkHUDElement());

        LOGGER.info("Registered {} HUD elements", hudElementList.size());

        hudElementList.forEach(HUDElement::discoverSettings);
        hudElementList.forEach(HUDElement::init);
    }
    
    public void register(final HUDElement element)
    {
        hudElementList.add(element);
    }

    @Override
    public List<HUDElement> getAll()
    {
        return hudElementList;
    }
}
