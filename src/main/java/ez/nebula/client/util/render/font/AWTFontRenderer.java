package ez.nebula.client.util.render.font;

import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.util.render.RenderUtil;
import ez.nebula.client.util.render.gui.Render2D;
import net.minecraft.client.renderer.OpenGlHelper;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL;

/**
 * @author xgraza
 * @since 03/01/25
 */
public final class AWTFontRenderer
{
    public static boolean DYNAMIC_FONT_RESIZING = true;

    private static final char COLOR_CONTROL_CHAR = '§';

    private final AWTFont normal, bold, italic, boldItalic;
    private final int[] colorCodes = new int[32];

    public AWTFontRenderer(Font font, final int size)
    {
        font = font.deriveFont((float) size);
        normal = new AWTFont(font.deriveFont(Font.PLAIN));
        bold = new AWTFont(font.deriveFont(Font.BOLD));
        italic = new AWTFont(font.deriveFont(Font.ITALIC));
        boldItalic = new AWTFont(font.deriveFont(Font.BOLD + Font.ITALIC));
        generateColorCodes();
    }

    public List<String> wrapText(final String text, final double maxWidth, final boolean byWord)
    {
        final List<String> textList = new ArrayList<>();
        Collections.addAll(textList, text.split(byWord ? " " : ""));
        final List<String> wrappedTextList = new ArrayList<>();

        String line = "";
        for (int i = 0; i < textList.size(); ++i)
        {
            final String item = textList.get(i);
            if ("\n".equals(item))
            {
                wrappedTextList.add(line);
                wrappedTextList.add("");
                line = "";
                continue;
            }
            line += item;
            if (byWord)
            {
                line += " ";
            }
            if (getStringWidth(line) > maxWidth
                    // look ahead one word/char to ensure we don't go past our limit
                    || (i + 1 <= textList.size() - 1
                    && getStringWidth(line + textList.get(i + 1)) > maxWidth))
            {
                wrappedTextList.add(line);
                line = "";
            }
        }
        wrappedTextList.add(line);
        return wrappedTextList;
    }

    public void drawStringShadow(final String input, final double x, final double y, final int color)
    {
        drawString(input, x, y, color, true);
        drawString(input, x, y, color, false);
    }

    public void drawString(final String input, double x, double y, int color, final boolean shadow)
    {
        if ((color & -67108864) == 0)
        {
            color |= -16777216;
        }
        if (shadow)
        {
            color = (color & 16579836) >> 2 | color & -16777216;
        }

        final int defaultColor = color;
        int textColor = defaultColor;

        glPushMatrix();

        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        RenderUtil.setGLColor(textColor);

        boolean bld = false;
        boolean ital = false;
        boolean strikethrough = false;
        boolean underline = false;

        AWTFont font = normal;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_RESCALE_NORMAL);
        bindFontTexture(font);

        double posX = x;
        double posY = y;

        if (shadow)
        {
            posX += 0.5;
            posY += 0.5;
        }

        glTranslated(posX, posY, 0);
        double scale = 0.5 / (DYNAMIC_FONT_RESIZING ? Render2D.getGUIScaleFactor() : 1.0);
        glScaled(scale, scale, scale);

        double offsetX = 0;
        double offsetY = 0;

        final char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; ++i)
        {
            final char ch = chars[i];
            if (ch == ' ')
            {
                offsetX += font.getSpaceWidth();
                continue;
            } else if (ch == '\n')
            {
                offsetX = 0;
                offsetY += font.getFontHeight();
                continue;
            } else if (ch == COLOR_CONTROL_CHAR)
            {
                if (i + 1 > chars.length)
                {
                    break;
                }
                final char colorControlChar = Character.toLowerCase(chars[i + 1]);
                switch (colorControlChar)
                {
                    case 'l':
                    {
                        bld = true;
                        if (ital)
                        {
                            font = boldItalic;
                        } else
                        {
                            font = bold;
                        }
                        bindFontTexture(font);
                        break;
                    }
                    case 'm':
                    {
                        strikethrough = true;
                        break;
                    }
                    case 'n':
                    {
                        underline = true;
                        break;
                    }
                    case 'o':
                    {
                        ital = true;
                        if (bld)
                        {
                            font = boldItalic;
                        } else
                        {
                            font = italic;
                        }
                        bindFontTexture(font);
                        break;
                    }
                    case 'r':
                    {
                        bld = false;
                        ital = false;
                        strikethrough = false;
                        underline = false;

                        textColor = defaultColor;
                        bindFontTexture(font = normal);
                        break;
                    }
                    case 'z':
                    {
                        Color c = new Color(HUDModule.INSTANCE.getBaseColor(10));
                        if (shadow)
                        {
                            c = c.darker().darker();
                        }
                        textColor = c.getRGB();
                        break;
                    }
                    default:
                    {
                        int colorCode = "0123456789abcdefklmnor".indexOf(colorControlChar);
                        if (colorCode < 16)
                        {
                            if (shadow)
                            {
                                colorCode += 16;
                            }
                            textColor = colorCodes[colorCode];
                        }
                        break;
                    }
                }
                ++i;
                continue;
            }

            final Glyph glyph = font.getGlyph(ch);
            if (glyph == null)
            {
                continue;
            }
            RenderUtil.setGLColorOpaque(textColor);
            font.drawChar(glyph, offsetX, offsetY);
            if (strikethrough)
            {

            }
            if (underline)
            {

            }
            offsetX += glyph.getAdvance();
        }

        glDisable(GL_RESCALE_NORMAL);
        glPopMatrix();
    }

    private void bindFontTexture(final AWTFont font)
    {
        glBindTexture(GL_TEXTURE_2D, font.getGlyphTexture());
        if (DYNAMIC_FONT_RESIZING)
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }
    }

    public double getFontHeight()
    {
        return (normal.getFontHeight() / 2.0) / (DYNAMIC_FONT_RESIZING ? Render2D.getGUIScaleFactor() : 1) - 1;
    }

    public double getStringWidth(final String input)
    {
        double width = 0.0;

        boolean bold = false;
        boolean italics = false;
        AWTFont font = normal;

        final char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; ++i)
        {
            final char ch = chars[i];
            if (ch == COLOR_CONTROL_CHAR)
            {
                final char colorCode = chars[i + 1];
                if (colorCode == 'l')
                {
                    bold = true;
                    if (italics)
                    {
                        font = boldItalic;
                    } else
                    {
                        font = this.bold;
                    }
                } else if (colorCode == 'o')
                {
                    italics = true;
                    if (bold)
                    {
                        font = boldItalic;
                    } else
                    {
                        font = this.italic;
                    }
                } else if (colorCode == 'r')
                {
                    bold = italics = false;
                    font = normal;
                }
                ++i; // lookahead once
                continue;
            }
            width += getCharWidth(ch, font);
        }
        return DYNAMIC_FONT_RESIZING ? width * (Render2D.getScaleFactor() / 4.0) : width / 2.0;
    }

    public double getCharWidth(final char ch)
    {
        return getCharWidth(ch, normal);
    }

    public double getCharWidth(final char ch, final AWTFont font)
    {
        if (ch == ' ')
        {
            return font.getSpaceWidth();
        }
        final Glyph glyph = font.getGlyph(ch);
        if (glyph != null)
        {
            return glyph.getAdvance();
        }
        return 0.0;
    }

    private void generateColorCodes()
    {
        for (int i = 0; i < 32; ++i)
        {
            int var6 = (i >> 3 & 1) * 85;
            int var7 = (i >> 2 & 1) * 170 + var6;
            int var8 = (i >> 1 & 1) * 170 + var6;
            int var9 = (i & 1) * 170 + var6;

            if (i == 6)
            {
                var7 += 85;
            }

            if (i >= 16)
            {
                var7 /= 4;
                var8 /= 4;
                var9 /= 4;
            }

            colorCodes[i] = (var7 & 255) << 16 | (var8 & 255) << 8 | var9 & 255;
        }
    }
}
