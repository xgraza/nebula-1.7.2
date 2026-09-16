package ez.nebula.client.api.manager.hud2;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventTick;
import ez.nebula.client.api.manager.ITypedManager;
import ez.nebula.client.impl.config.HUD2Config;
import ez.nebula.client.impl.hud2.*;
import ez.nebula.client.util.render.gui.Render2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xgraza
 * @since 9/6/26
 */
public final class HUDElementManager implements ITypedManager<HUDElement>
{
    static final Logger LOGGER = LogManager.getLogger("HUD");

    private final List<HUDElement> elementList = new ArrayList<>();
    private final HUD2Config config = new HUD2Config(this);

    public double prevHeight = -1, prevWidth = -1;
    public int prevScale = -1;

    @Subscribe
    private final EventListener<EventTick> tickEventListener = event ->
    {
        if (Render2D.RESOLUTION == null || !config.isLoaded())
        {
            return;
        }
        final int scale = Render2D.RESOLUTION.getScaleFactor();
        final double width = Render2D.RESOLUTION.getScaledWidth_double() * scale;
        final double height = Render2D.RESOLUTION.getScaledHeight_double() * scale;
        if (prevHeight != -1 && prevWidth != -1 && prevScale != -1 && (width != prevWidth || height != prevHeight) && scale == prevScale)
        {
            final double scaleX = width / prevWidth;
            final double scaleY = height / prevHeight;
            for (final HUDElement element : elementList)
            {
                element.setX(element.getX() * scaleX);
                element.setY(element.getY() * scaleY);
            }
        }
        prevWidth = width;
        prevHeight = height;
        prevScale = scale;
    };

    @Override
    public void init()
    {
        register(new ArmorHUDElement());
        register(new CoordinatesHUDElement());
        register(new FPSHUDElement());
        register(new HealthHUDElement());
        register(new SpeedHUDElement());
        register(new TPSHUDElement());
        register(new WatermarkHUDElement());

        LOGGER.info("Registered {} HUD elements!", elementList.size());

        elementList.forEach(HUDElement::discoverSettings);

        Nebula.CONFIGS.register(config);
        EventBus.subscribe(this); // automatic scaling
    }
    
    public void register(final HUDElement element)
    {
        elementList.add(element);
    }

    @Override
    public List<HUDElement> getAll()
    {
        return elementList;
    }
}
