package ez.nebula.client.mixin.impl.render.gui;

import ez.nebula.client.impl.module.render.HUDModule;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.Color;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = FontRenderer.class)
public class FontRendererMixin
{
    @Unique private static final String COLOR_CODE_INDEX_MAP = "0123456789abcdefklmnorz";

    @Shadow private float alpha;

    @Redirect(method = "renderStringAtPos", at = @At(value = "INVOKE", target = "Ljava/lang/String;indexOf(I)I"))
    private int a(String str, int i)
    {
        final int index = COLOR_CODE_INDEX_MAP.indexOf(i);
        if (index == 22)
        {
            Color color = new Color(HUDModule.INSTANCE.getBaseColor(10));
//            if (par2)
//            {
//                color = color.darker().darker().darker();
//            }
            final int var6 = color.getRGB();
            final float red = (float) (var6 >> 16 & 255) / 255.0F;
            final float blue = (float) (var6 & 255) / 255.0F;
            final float green = (float) (var6 >> 8 & 255) / 255.0F;
            GL11.glColor4f(red, green, blue, alpha);
        }
        return i;
    }
}
