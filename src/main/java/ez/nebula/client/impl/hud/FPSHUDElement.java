package ez.nebula.client.impl.hud;

import ez.nebula.client.api.manager.hud.HUDElement;
import ez.nebula.client.api.manager.hud.trait.HUDManifest;
import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.mixin.duck.IMinecraft;
import ez.nebula.client.util.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

@HUDManifest(name = "FPS", description = "Shows the current game FPS", x = 2, y = 2)
public final class FPSHUDElement extends HUDElement
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
        final String formatted = String.format("FPS: %s%s", EnumChatFormatting.GRAY, ((IMinecraft)MC).nebula$getDebugFPS());
        setWidth(Fonts.POPPINS.getStringWidth(formatted) + (getPadding() * 4));
        Fonts.POPPINS.drawStringShadow(formatted, getX() + getPadding(), getY() + getPadding(), HUDModule.INSTANCE.getBaseColor(10));
    }
}
