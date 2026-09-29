package ez.nebula.client.impl.gui.clickgui.component.value.color;

import ez.nebula.client.impl.gui.clickgui.component.value.NumberSettingComponent;
import ez.nebula.client.util.render.gui.Render2D;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;

public final class AlphaSliderColorComponent extends NumberSettingComponent
{
    private static final ResourceLocation RGB_GRADIENT_LOCATION = new ResourceLocation(
            "nebula",
            "texture/clickgui/transparency.png");
    private static final double SLIDER_HEIGHT = 5.0;

    private final ColorSettingComponent parent;

    public AlphaSliderColorComponent(final ColorSettingComponent parent)
    {
        super(null);
        this.parent = parent;
    }

    @Override
    protected void drawSlider()
    {
        Render2D.texture(RGB_GRADIENT_LOCATION, getX(), y, (int) width, (int) getHeight());
        final double position = (parent.getSetting().getValue().getAlpha() / 255.0f) * getWidth();
        Render2D.rectangle(getX() + position - 2.5, y, 5, getHeight(), Color.white.getRGB());
    }

    @Override
    protected void drawText()
    {
        // purposefully empty
    }

    @Override
    protected void setValue(int mouseX)
    {
        if (mouseX < getX())
        {
            mouseX = (int) getX();
        }

        if (mouseX > getX() + getWidth())
        {
            mouseX = (int) (getX() + getWidth());
        }

        parent.gradientColorComponent.updateTransparency((float) ((mouseX - getX()) / getWidth()));
    }

    @Override
    public double getHeight()
    {
        return SLIDER_HEIGHT;
    }
}
