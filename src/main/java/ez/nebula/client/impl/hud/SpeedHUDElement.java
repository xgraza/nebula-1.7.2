package ez.nebula.client.impl.hud;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.hud.HUDElement;
import ez.nebula.client.api.manager.hud.trait.HUDManifest;
import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.util.render.font.Fonts;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

/**
 * @author xgraza
 * @since 05/21/26
 */
@HUDManifest(name = "Speed",
        description = "Displays your speed in BPS",
        x = 2, y = 2)
public final class SpeedHUDElement extends HUDElement
{
    @Override
    public void init()
    {
        super.init();
        setHeight(Fonts.POPPINS.getFontHeight() + (getPadding() * 2));
    }

    @Override
    public void render(final ScaledResolution res)
    {
        final double moveDelta = Nebula.MOVEMENT.getMoveSpeed();
        double speed = (moveDelta / 1000) / (0.05 / 3600);
        speed *= MC.timer.timerSpeed;
        speed /= 3.6;
        final String formatted = String.format("Speed: %s%.2f BPS",
                EnumChatFormatting.GRAY, speed);
        setWidth(Fonts.POPPINS.getStringWidth(formatted) + (getPadding() * 4));
        Fonts.POPPINS.drawStringShadow(formatted, getX() + getPadding(), getY() + getPadding(), HUDModule.INSTANCE.getBaseColor(10));
    }
}
