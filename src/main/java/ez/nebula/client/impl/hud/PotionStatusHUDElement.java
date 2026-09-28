package ez.nebula.client.impl.hud;

import ez.nebula.client.api.manager.hud.HUDElement;
import ez.nebula.client.api.manager.hud.trait.HUDManifest;
import ez.nebula.client.util.render.font.Fonts;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.text.FormattingUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 5/21/26
 */
@HUDManifest(name = "PotionStatus",
        description = "Shows active potion effects",
        x = 2, y = 2, width = 100, height = 100)
public final class PotionStatusHUDElement extends HUDElement
{
    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation(
            "textures/gui/container/inventory.png");

    @Override
    public void render(final ScaledResolution res)
    {
        double posY = getY() + getHeight() - (getPadding() * 2);
        for (final Object o : MC.thePlayer.getActivePotionEffects())
        {
            if (!(o instanceof PotionEffect))
            {
                continue;
            }
            final PotionEffect effect = (PotionEffect) o;

            final String formatted = String.format("%s %s%s: %s",
                    I18n.format(effect.getEffectName()),
                    FormattingUtil.formatRomanNumeral(effect.getAmplifier() + 1),
                    EnumChatFormatting.GRAY,
                    Potion.getDurationString(effect));
            final double textWidth = Fonts.POPPINS.getStringWidth(formatted);
            if (textWidth > getWidth())
            {
                setWidth(textWidth);
            }

            posY -= Fonts.POPPINS.getFontHeight() + getPadding();
            double posX = getX() + getWidth() - textWidth - (getPadding() * 2);

            final Potion potion = Potion.potionTypes[effect.getPotionID()];
            Fonts.POPPINS.drawStringShadow(formatted, posX, posY, potion.getLiquidColor());

            if (potion.hasStatusIcon())
            {
                glPushMatrix();
                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                glDisable(GL_LIGHTING);
                final int iconIndex = potion.getStatusIconIndex();
                MC.getTextureManager().bindTexture(CONTAINER_LOCATION);
                glTranslated(posX - (getPadding() * 2) - 9, posY + (getPadding() * 2), 0.0);
                glScaled(0.5, 0.5, 0.5);
                Render2D.drawTexturedModalRectX(0, 0, iconIndex % 8 * 18, 198 + iconIndex / 8 * 18, 18, 18);
                glPopMatrix();
            }
        }
    }
}
