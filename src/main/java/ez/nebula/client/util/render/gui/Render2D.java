package ez.nebula.client.util.render.gui;

import ez.nebula.client.impl.module.render.ClickGUIModule;
import ez.nebula.client.impl.module.render.GlintModule;
import ez.nebula.client.mixin.duck.IRenderItem;
import ez.nebula.client.util.render.RenderUtil;
import ez.nebula.client.util.render.shader.Shader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 8/24/26
 */
public final class Render2D
{
    private static final Tessellator TESSELLATOR = Tessellator.instance;
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Shader ROUNDED_RECTANGLE_SHADER;

    private static final RenderItem RENDER_ITEM = new RenderItem();
    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation(
            "textures/misc/enchanted_item_glint.png");

    public static ScaledResolution RESOLUTION;

    static
    {
        ROUNDED_RECTANGLE_SHADER = new Shader(
                "/assets/nebula/shader/vertex.vsh",
                "/assets/nebula/shader/roundedrect.frag",
                (shader) ->
                {
                    shader.createUniform("rectSize");
                    shader.createUniform("color");
                    shader.createUniform("radius");
                    shader.createUniform("edgeSoftness");
                });
    }

    /**
     * Renders a 2D line
     * @param x1 the origin x coordinate
     * @param y1 the origin y coordinate
     * @param x2 the ending x coordinate
     * @param y2 the ending y coordinate
     * @param lineWidth the width of the line
     * @param color the color the line should be
     */
    public static void line(double x1, double y1, double x2, double y2, float lineWidth, int color)
    {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(lineWidth);
        RenderUtil.setGLColor(color);
        glBegin(GL_LINES);
        {
            glVertex2d(x1, y1);
            glVertex2d(x2, y2);
        }
        glEnd();
        glLineWidth(1.0f);
        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_TEXTURE_2D);
    }

    /**
     * Draws a rectangle
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param color the color of the rectangle
     */
    public static void rectangle(final double x, final double y, final double width, final double height, final int color)
    {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        RenderUtil.setGLColor(color);
        TESSELLATOR.startDrawingQuads();
        TESSELLATOR.addVertex(x, y, 0.0);
        TESSELLATOR.addVertex(x, y + height, 0.0);
        TESSELLATOR.addVertex(x + width, y + height, 0.0);
        TESSELLATOR.addVertex(x + width, y, 0.0);
        TESSELLATOR.draw();
        glEnable(GL_TEXTURE_2D);
    }

    /**
     * Draws a rectangle with rounded corners
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the rounded rectangle
     * @param height the height of the rounded rectangle
     * @param radius the radius of each corner
     * @param color the color of the rectangle
     */
    public static void roundedRectangle(final double x,
                                              final double y,
                                              final double width,
                                              final double height,
                                              final float radius,
                                              final int color)
    {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);

        ROUNDED_RECTANGLE_SHADER.use(() ->
        {
            int scaleFactor = MC.gameSettings.guiScale;
            if (RESOLUTION != null)
            {
                scaleFactor = RESOLUTION.getScaleFactor();
            }

            ROUNDED_RECTANGLE_SHADER.set("rectSize", (float) (width * scaleFactor), (float) (height * scaleFactor));
            final float[] argb = RenderUtil.getColorARGB(color);
            ROUNDED_RECTANGLE_SHADER.set("color", argb[1], argb[2], argb[3], argb[0]);
            ROUNDED_RECTANGLE_SHADER.set("radius", radius * scaleFactor);
            ROUNDED_RECTANGLE_SHADER.set("edgeSoftness", 1.0f);
        });

        glTranslated(0, 0, 1);

        glBegin(GL_QUADS);
        {
            glTexCoord2d(0, 0);
            glVertex2d(x, y);
            glTexCoord2d(0, 1);
            glVertex2d(x, y + height);
            glTexCoord2d(1, 1);
            glVertex2d(x + width, y + height);
            glTexCoord2d(1, 0);
            glVertex2d(x + width, y);
        }
        glEnd();

        ROUNDED_RECTANGLE_SHADER.stop();

        glDisable(GL_BLEND);
    }

    /**
     * Draws an outline of a rectangle
     * @apiNote ONLY the outline
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the outline
     * @param height the height of the outline
     * @param lineWidth the width of the outline
     * @param color the color of the outline
     */
    public static void rectangleOutline(final double x, final double y, final double width, final double height, final float lineWidth, final int color)
    {
        glDisable(GL_TEXTURE_2D);
        RenderUtil.setGLColor(color);

        glLineWidth(lineWidth);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        TESSELLATOR.startDrawing(GL_LINES);

        // top
        TESSELLATOR.addVertex(x, y, 0);
        TESSELLATOR.addVertex(x + width, y, 0);

        // left side
        TESSELLATOR.addVertex(x, y, 0);
        TESSELLATOR.addVertex(x, y + height, 0);

        // right side
        TESSELLATOR.addVertex(x + width, y, 0);
        TESSELLATOR.addVertex(x + width, y + height, 0);

        // bottom
        TESSELLATOR.addVertex(x, y + height, 0);
        TESSELLATOR.addVertex(x + width, y + height, 0);

        TESSELLATOR.draw();

        glLineWidth(1.0f);
        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
        glDisable(GL_LINE_SMOOTH);

        glEnable(GL_TEXTURE_2D);
    }

    /**
     * Draws a rectangle with a color gradient
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param tl the top left color
     * @param bl the bottom left color
     * @param tr the top right color
     * @param br the bottom right color
     */
    public static void gradientRectangle(
            final double x,
            final double y,
            final double width,
            final double height,
            final int tl,
            final int bl,
            final int tr,
            final int br)
    {
        glPushMatrix();

        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);
        glDisable(GL_TEXTURE_2D);

        glShadeModel(GL_SMOOTH);

        glBegin(GL_QUADS);
        {
            RenderUtil.setGLColor(tr);
            glVertex2d(x + width, y);
            RenderUtil.setGLColor(tl);
            glVertex2d(x, y);
            RenderUtil.setGLColor(bl);
            glVertex2d(x, y + height);
            RenderUtil.setGLColor(br);
            glVertex2d(x + width, y + height);
        }
        glEnd();

        glShadeModel(GL_FLAT);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);

        glPopMatrix();
    }

    /**
     * Draws a texture from a {@link ResourceLocation}
     * @param location the {@link ResourceLocation} of the asset
     * @param x the x coordinate
     * @param y the y coordinate
     * @param w the width of the drawn texture
     * @param h the height of the drawn texture
     */
    public static void texture(ResourceLocation location, double x, double y, int w, int h)
    {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        MC.getTextureManager().bindTexture(location);

        RenderUtil.setGLColor(0xFFFFFFFF);
        // this is from mc 1.8.9 code cause 1.7.2 fucking sucks

        final float u = 0.0f;
        final float v = 0.0f;

        final float scaledWidth = 1.0F / (float) w;
        final float scaledHeight = 1.0F / (float) h;
        TESSELLATOR.startDrawingQuads();
        TESSELLATOR.addVertexWithUV(x, y + h, 0.0D, u * scaledWidth, (v + (float) h) * scaledHeight);
        TESSELLATOR.addVertexWithUV(x + w, y + h, 0.0D, (u + (float) w) * scaledWidth, (v + (float) h) * scaledHeight);
        TESSELLATOR.addVertexWithUV(x + w, y, 0.0D, (u + (float) w) * scaledWidth, v * scaledHeight);
        TESSELLATOR.addVertexWithUV(x, y, 0.0D, u * scaledWidth, v * scaledHeight);
        TESSELLATOR.draw();

        glDisable(GL_BLEND);
        glPopMatrix();
    }

    /**
     * Renders the icon of an {@link ItemStack} without any effects
     * @param stack the {@link ItemStack}
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public static void itemNoEffects(final ItemStack stack, final int x, final int y)
    {
        if (stack == null)
        {
            return;
        }

        glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        float zLevel = RENDER_ITEM.zLevel;
        RENDER_ITEM.zLevel = 0.0f;
        RENDER_ITEM.renderItemIntoGUI(
                MC.fontRenderer, MC.getTextureManager(), stack, x, y);
        RENDER_ITEM.zLevel = zLevel;

        RenderHelper.disableStandardItemLighting();
        glPopMatrix();
    }

    /**
     * Renders the icon of an {@link ItemStack} with the glistening effect over it
     * @param stack the {@link ItemStack}
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public static void itemWithEffects(final ItemStack stack, final int x, final int y)
    {
        if (stack == null)
        {
            return;
        }

        glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        float zLevel = RENDER_ITEM.zLevel;
        RENDER_ITEM.zLevel = 0.0f;
        RENDER_ITEM.renderItemIntoGUI(
                MC.fontRenderer, MC.getTextureManager(), stack, x, y);
        RENDER_ITEM.renderItemOverlayIntoGUI(
                MC.fontRenderer, MC.getTextureManager(), stack, x, y);
        RENDER_ITEM.zLevel = zLevel;

        if (stack.hasEffect())
        {
            glEnable(GL_BLEND);
            glDepthFunc(GL_EQUAL);
            glDisable(GL_LIGHTING);
            glDepthMask(false);
            MC.getTextureManager().bindTexture(RES_ITEM_GLINT);
            glEnable(GL_ALPHA_TEST);
            if (GlintModule.INSTANCE.isToggled())
            {
                RenderUtil.setGLColorOpaque(GlintModule.INSTANCE.colorSetting.getValue().getRGB());
            } else
            {
                glColor4f(0.5F, 0.25F, 0.8F, 1.0F);
            }
            ((IRenderItem)RENDER_ITEM).nebula$renderGlint(x * 431278612 + -26 * 32178161, x - 2, -26 - 2, 20, 20);
            glDepthMask(true);
            glDisable(GL_ALPHA_TEST);
            glEnable(GL_LIGHTING);
            glDepthFunc(GL_LEQUAL);
            glDisable(GL_BLEND);
        }

        RenderHelper.disableStandardItemLighting();
        glPopMatrix();
    }

    public static void drawTexturedModalRectX(int par1, int par2, int par3, int par4, int par5, int par6)
    {
        float var7 = 0.00390625F;
        float var8 = 0.00390625F;
        Tessellator var9 = Tessellator.instance;
        var9.startDrawingQuads();
        var9.addVertexWithUV((double)(par1 + 0), (double)(par2 + par6), 0, (double)((float)(par3 + 0) * var7), (double)((float)(par4 + par6) * var8));
        var9.addVertexWithUV((double)(par1 + par5), (double)(par2 + par6), 0, (double)((float)(par3 + par5) * var7), (double)((float)(par4 + par6) * var8));
        var9.addVertexWithUV((double)(par1 + par5), (double)(par2 + 0), 0, (double)((float)(par3 + par5) * var7), (double)((float)(par4 + 0) * var8));
        var9.addVertexWithUV((double)(par1 + 0), (double)(par2 + 0), 0, (double)((float)(par3 + 0) * var7), (double)((float)(par4 + 0) * var8));
        var9.draw();
    }

    /**
     * Begins a scissor box
     * @param x the x coordinate of the scissor box
     * @param y the y coordinate of the scissor box
     * @param width the width of the scissor box
     * @param height the height of the scissor box
     */
    public static void startScissor(final double x,
                                    final double y,
                                    final double width,
                                    final double height)
    {
        final double effectiveScale = getGUIScaleFactor() * RESOLUTION.getScaleFactor();

        final double scissorX = x * effectiveScale;
        final double scissorWidth = width * effectiveScale;
        final double scissorHeight = height * effectiveScale;

        final double rawDisplayHeight = RESOLUTION.getScaledHeight_double() * RESOLUTION.getScaleFactor();
        final double scissorY = rawDisplayHeight - ((y + height) * effectiveScale);

        glEnable(GL_SCISSOR_TEST);
        glScissor(
                (int) Math.ceil(scissorX),
                (int) Math.ceil(scissorY),
                (int) Math.ceil(scissorWidth),
                (int) Math.ceil(scissorHeight)
        );
    }

    /**
     * Ends a scissor box
     */
    public static void endScissor()
    {
        glDisable(GL_SCISSOR_TEST);
    }

    /**
     * Gets the {@link ScaledResolution} scale factor, or defaulting to "Normal" (2)
     * @return the scale factor
     */
    public static int getScaleFactor()
    {
        return RESOLUTION == null ? 2 : RESOLUTION.getScaleFactor();
    }

    /**
     * Gets the GUI scale factor for use within glScaled or glScalef
     * @return the GUI scale factor
     */
    public static double getGUIScaleFactor()
    {
        if (ClickGUIModule.INSTANCE != null && ClickGUIModule.INSTANCE.guiScaleSetting.getValue() != 1.0)
        {
            return ClickGUIModule.INSTANCE.guiScaleSetting.getValue();
        }
        return 2.0 / getScaleFactor();
    }
}
