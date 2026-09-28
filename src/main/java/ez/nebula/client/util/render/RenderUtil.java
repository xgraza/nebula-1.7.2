package ez.nebula.client.util.render;

import ez.nebula.client.impl.module.render.ClickGUIModule;
import ez.nebula.client.impl.module.render.GlintModule;
import ez.nebula.client.mixin.duck.IRenderItem;
import ez.nebula.client.util.render.shader.Shader;
import ez.nebula.client.util.render.world.QuadMask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 03/01/25
 */
public final class RenderUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Tessellator TESSELLATOR = Tessellator.instance;

    private static final RenderItem RENDER_ITEM = new RenderItem();
    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation(
            "textures/misc/enchanted_item_glint.png");

    public static ScaledResolution GAME_RESOLUTION;
    public static Shader ROUNDED_RECTANGLE_SHADER,
            BLUR_SHADER,
            ESP_SHADER;

    public static void initShaders()
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
        ESP_SHADER = new Shader(
                "/assets/nebula/shader/vertex.vsh",
                "/assets/nebula/shader/esp.fsh",
                (s) ->
                {
                    s.createUniform("texture");
                    s.createUniform("texelSize");
                    s.createUniform("color");
                    s.createUniform("radius");
                    s.createUniform("opacity");
                });
    }

    public static void startScissor(final double x,
                                    final double y,
                                    final double width,
                                    final double height)
    {
//        final double scale = GAME_RESOLUTION.getScaleFactor();
//        glEnable(GL_SCISSOR_TEST);
//        glScissor((int) (x * scale),
//                (int) (((GAME_RESOLUTION.getScaledHeight_double() - y) * scale) - (height * scale)),
//                (int) (width * scale),
//                (int) (height * scale));
        final double effectiveScale = RenderUtil.getGUIScaleFactor() * GAME_RESOLUTION.getScaleFactor();

        final double scissorX = x * effectiveScale;
        final double scissorWidth = width * effectiveScale;
        final double scissorHeight = height * effectiveScale;

        final double rawDisplayHeight = GAME_RESOLUTION.getScaledHeight_double() * GAME_RESOLUTION.getScaleFactor();
        final double scissorY = rawDisplayHeight - ((y + height) * effectiveScale);

        glEnable(GL_SCISSOR_TEST);
        glScissor(
                (int) Math.ceil(scissorX),
                (int) Math.ceil(scissorY),
                (int) Math.ceil(scissorWidth),
                (int) Math.ceil(scissorHeight)
        );
    }

    public static void endScissor()
    {
        glDisable(GL_SCISSOR_TEST);
    }

    public static int calculateFaceMask(final EnumFacing... facings)
    {
        int mask = 0;
        for (final EnumFacing facing : facings)
        {
            mask |= QuadMask.mask(facing);
        }
        return mask;
    }

    public static boolean hasQuadMask(final int renderMask, final int face)
    {
        return (renderMask & face) != 0;
    }

    public static void renderFilledAABB(AxisAlignedBB aabb,
                                        final int renderMask,
                                        final int color)
    {
        glPushMatrix();

        glDisable(GL_TEXTURE_2D);

        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);

        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);

        glDisable(GL_CULL_FACE);

        aabb = aabb.copy().offset(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);

        if (hasQuadMask(renderMask, QuadMask.UP))
        {
            TESSELLATOR.startDrawingQuads();
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.SOUTH))
        {
            TESSELLATOR.startDrawingQuads();
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.NORTH))
        {
            TESSELLATOR.startDrawingQuads();
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.WEST))
        {
            TESSELLATOR.startDrawingQuads();
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.EAST))
        {
            TESSELLATOR.startDrawingQuads();
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.DOWN))
        {
            // top
            TESSELLATOR.startDrawingQuads();
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            TESSELLATOR.draw();
        }

        glEnable(GL_CULL_FACE);

        glEnable(GL_TEXTURE_2D);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);

        glPopMatrix();
    }

    public static void renderOutlinedAABB(AxisAlignedBB aabb,
                                          final float lineWidth,
                                          final int renderMask,
                                          final int color)
    {
        glPushMatrix();

        glDisable(GL_TEXTURE_2D);

        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);

        glLineWidth(lineWidth);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);

        glDisable(GL_CULL_FACE);

        aabb = aabb.copy().offset(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);

        if (hasQuadMask(renderMask, QuadMask.UP))
        {
            TESSELLATOR.startDrawing(GL_LINE_LOOP);
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.SOUTH))
        {
            TESSELLATOR.startDrawing(GL_LINE_LOOP);
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.NORTH))
        {
            TESSELLATOR.startDrawing(GL_LINE_LOOP);
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.WEST))
        {
            TESSELLATOR.startDrawing(GL_LINE_LOOP);
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.EAST))
        {
            TESSELLATOR.startDrawing(GL_LINE_LOOP);
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            TESSELLATOR.draw();
        }

        if (hasQuadMask(renderMask, QuadMask.DOWN))
        {
            // top
            TESSELLATOR.startDrawing(GL_LINE_LOOP);
            setTessellatorColor(color);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            TESSELLATOR.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            TESSELLATOR.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            TESSELLATOR.draw();
        }

        glEnable(GL_CULL_FACE);

        glEnable(GL_TEXTURE_2D);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);

        glLineWidth(1.0f);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);

        glPopMatrix();
    }

    public static void renderGLBillboard(final double x, final double y, final double z, final double size, final Runnable runnable)
    {
        glPushMatrix();

        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, -1100000.0f);

        RenderHelper.disableStandardItemLighting();
        glDisable(GL_LIGHTING);

        glTranslated(x - RenderManager.renderPosX, y - RenderManager.renderPosY, z - RenderManager.renderPosZ);
        glRotatef(-RenderManager.instance.playerViewY, 0.0f, 1.0f, 0.0f);
        glRotatef(RenderManager.instance.playerViewX,
                MC.gameSettings.thirdPersonView == 2
                        ? -1.0f
                        : 1.0f,
                0.0f, 0.0f);

        final double distance = MC.renderViewEntity.getDistance(x,
                y,
                z);
        final double scale = (size * Math.max(distance, 4.0)) / 50.0;
        glScaled(-scale, -scale, scale);

        glDisable(GL_DEPTH_TEST);

        runnable.run();

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);

        glPolygonOffset(1.0f, 1100000.0f);
        glDisable(GL_POLYGON_OFFSET_FILL);

        glEnable(GL_ALPHA_TEST);

        glPopMatrix();
    }

    public static void renderLine(double x1, double y1, double x2, double y2, float lineWidth, int color)
    {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(lineWidth);
        setGLColor(color);
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

    public static void renderRectangle(final double x,
                                       final double y,
                                       final double width,
                                       final double height,
                                       final int color)
    {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        setGLColor(color);
        TESSELLATOR.startDrawingQuads();
        TESSELLATOR.addVertex(x, y, 0.0);
        TESSELLATOR.addVertex(x, y + height, 0.0);
        TESSELLATOR.addVertex(x + width, y + height, 0.0);
        TESSELLATOR.addVertex(x + width, y, 0.0);
        TESSELLATOR.draw();
        glEnable(GL_TEXTURE_2D);
    }

    public static void render2DOutline(final double x,
                                       final double y,
                                       final double width,
                                       final double height,
                                       final float lineWidth,
                                       final int color)
    {
        glDisable(GL_TEXTURE_2D);
        setGLColor(color);

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

    public static void renderGradientRectangle(
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
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        glDisable(GL_TEXTURE_2D);

        glShadeModel(GL_SMOOTH);

        glBegin(GL_QUADS);
        {
            setGLColor(tr);
            glVertex2d(x + width, y);
            setGLColor(tl);
            glVertex2d(x, y);
            setGLColor(bl);
            glVertex2d(x, y + height);
            setGLColor(br);
            glVertex2d(x + width, y + height);
        }
        glEnd();

        glShadeModel(GL_FLAT);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);

        glPopMatrix();
    }

    public static void renderRoundedRectangle(final double x,
                                              final double y,
                                              final double width,
                                              final double height,
                                              final float radius,
                                              final int color)
    {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 0, 1);

        ROUNDED_RECTANGLE_SHADER.use(() ->
        {
            int scaleFactor = MC.gameSettings.guiScale;
            if (GAME_RESOLUTION != null)
            {
                scaleFactor = GAME_RESOLUTION.getScaleFactor();
            }

            ROUNDED_RECTANGLE_SHADER.set("rectSize", (float) (width * scaleFactor), (float) (height * scaleFactor));
            final float[] argb = getColorARGB(color);
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

    public static void renderTexture(ResourceLocation loc, double x, double y, int w, int h)
    {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        MC.getTextureManager().bindTexture(loc);

        glColor4f(1, 1, 1, 1);
        // this is from mc 1.8.9 code cause 1.7.2 fucking sucks

        float u = 0.0f;
        float v = 0.0f;

        float f = 1.0F / (float) w;
        float f1 = 1.0F / (float) h;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + h, 0.0D, u * f, (v + (float) h) * f1);
        tessellator.addVertexWithUV(x + w, y + h, 0.0D, (u + (float) w) * f, (v + (float) h) * f1);
        tessellator.addVertexWithUV(x + w, y, 0.0D, (u + (float) w) * f, v * f1);
        tessellator.addVertexWithUV(x, y, 0.0D, u * f, v * f1);
        tessellator.draw();

        glDisable(GL_BLEND);
        glPopMatrix();
    }

    public static void renderItemWithoutEffects(final ItemStack itemStack,
                                                final int posX,
                                                final int posY)
    {
        if (itemStack == null)
        {
            return;
        }

        glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        float zLevel = RENDER_ITEM.zLevel;
        RENDER_ITEM.zLevel = 0.0f;
        RENDER_ITEM.renderItemIntoGUI(
                MC.fontRenderer, MC.getTextureManager(), itemStack, posX, posY);
        RENDER_ITEM.zLevel = zLevel;

        RenderHelper.disableStandardItemLighting();
        glPopMatrix();
    }

    public static void renderItemWithGlint(final ItemStack itemStack,
                                           final int posX,
                                           final int posY)
    {
        if (itemStack == null)
        {
            return;
        }

        glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        RENDER_ITEM.renderItemIntoGUI(
                MC.fontRenderer, MC.getTextureManager(), itemStack, posX, posY);
        RENDER_ITEM.renderItemOverlayIntoGUI(
                MC.fontRenderer, MC.getTextureManager(), itemStack, posX, posY);

        if (itemStack.hasEffect())
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
            ((IRenderItem)RENDER_ITEM).nebula$renderGlint(posX * 431278612 + -26 * 32178161, posX - 2, -26 - 2, 20, 20);
            glDepthMask(true);
            glDisable(GL_ALPHA_TEST);
            glEnable(GL_LIGHTING);
            glDepthFunc(GL_LEQUAL);
            glDisable(GL_BLEND);
        }

        RenderHelper.disableStandardItemLighting();
        glPopMatrix();
    }

    public static float[] getColorARGB(final int color)
    {
        final float alpha = (color >> 24 & 0xff) / 255.0f;
        final float red = (color >> 16 & 0xff) / 255.0f;
        final float green = (color >> 8 & 0xff) / 255.0f;
        final float blue = (color & 0xff) / 255.0f;
        return new float[]{ alpha, red, green, blue };
    }

    public static void setTessellatorColor(final int color)
    {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        final float alpha = (float) (color >> 24 & 255) / 255.0F;
        TESSELLATOR.setColorRGBA_F(red, green, blue, alpha);
    }

    public static void setGLColor(final int color)
    {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        final float alpha = (float) (color >> 24 & 255) / 255.0F;
        glColor4f(red, green, blue, alpha);
    }

    public static void setGLColorOpaque(final int color)
    {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        glColor4f(red, green, blue, 1.0f);
    }

    public static int getScaleFactor()
    {
        return GAME_RESOLUTION == null ? 2 : GAME_RESOLUTION.getScaleFactor();
    }

    public static double getGUIScaleFactor()
    {
        if (ClickGUIModule.INSTANCE != null && ClickGUIModule.INSTANCE.guiScaleSetting.getValue() != 1.0)
        {
            return ClickGUIModule.INSTANCE.guiScaleSetting.getValue();
        }
        return 2.0 / getScaleFactor();
    }
}
