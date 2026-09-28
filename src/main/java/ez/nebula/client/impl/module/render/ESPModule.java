package ez.nebula.client.impl.module.render;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.render.EventRender2D;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.player.FreecamModule;
import ez.nebula.client.util.minecraft.player.EntityUtil;
import ez.nebula.client.util.render.ColorUtil;
import ez.nebula.client.util.render.RenderUtil;
import ez.nebula.client.util.render.gui.ProjectionUtil;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 09/04/2025
 */
@ModuleManifest(name = "ESP",
        description = "Renders an overlay on entities/objects in the world to give you a \"6th sense\"",
        category = ModuleCategory.RENDER)
public final class ESPModule extends Module
{
    private final EnumSetting<Mode> modeSetting = enumBuilder("Mode", Mode.BOX)
            .setDescription("How to render the sixth sense")
            .build();

    private final Setting<Boolean> labelsSetting = builder("Labels", true)
            .setVisibility((value) -> modeSetting.getValue() == Mode.CS_GO)
            .build();

    private final NumberSetting<Float> lineWidthSetting = numberBuilder("Line Width", 1.5f)
            .setMin(0.5f)
            .setMax(5.0f)
            .setScale(0.1f)
            .setDescription("The width of the render line")
            .build();
    private final NumberSetting<Float> opacitySetting = numberBuilder("Opacity", 0.0f)
            .setMin(0.0f)
            .setMax(1.0f)
            .setScale(0.05f)
            .setDescription("The transparency of the render")
            .setVisibility((value) -> modeSetting.getValue() != Mode.CS_GO)
            .build();

    // entities
    private final Setting<Boolean> playersSetting = builder("Players", true)
            .setDescription("If to render player entities")
            .build();
    private final Setting<Boolean> hostileSetting = builder("Hostile", true)
            .setDescription("If to render hostile mobs")
            .build();
    private final Setting<Boolean> passiveSetting = builder("Passive", true)
            .setDescription("If to render passive mobs")
            .build();

    // static entities
    private final Setting<Boolean> itemFramesSetting = builder("Item Frames", false)
            .setDescription("If to render item frames")
            .build();
    private final Setting<Boolean> droppedItemsSetting = builder("Dropped Items", false)
            .setDescription("If to render dropped items")
            .build();

    // tile entities
    private final Setting<Boolean> chestsSetting = builder("Chests", true)
            .setDescription("If to render chests")
            .build();
    private final Setting<Boolean> endPortalsSetting = builder("End Portals", false)
            .setDescription("If to render end portal blocks")
            .setVisibility((value) -> modeSetting.getValue() == Mode.SHADER)
            .build();
    private final Setting<Boolean> skullsSetting = builder("Skulls", false)
            .setDescription("If to render heads")
            .setVisibility((value) -> modeSetting.getValue() == Mode.SHADER)
            .build();
    private final Setting<Boolean> redstoneSetting = builder(
            "Redstone Materials", false)
            .setDescription("If to render redstone materials (i.e. comparators, repeaters)")
            .setVisibility((value) -> modeSetting.getValue() == Mode.SHADER)
            .build();
    private final Setting<Boolean> signsSetting = builder("Signs", false)
            .setDescription("If to render signs")
            .setVisibility((value) -> modeSetting.getValue() == Mode.SHADER)
            .build();

    private final Map<Integer, float[][]> projected = new ConcurrentHashMap<>();
    private final List<Object> renderTargetList = new CopyOnWriteArrayList<>();

    private Framebuffer fb;
    private int fbHeight, fbWidth, fbScale;

    @Override
    public void onDisable()
    {
        super.onDisable();
        projected.clear();
        renderTargetList.clear();

        if (fb != null)
        {
            fb.framebufferClear();
        }
        fb = null;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        for (int entityId : projected.keySet())
        {
            projected.remove(entityId);
        }

        renderTargetList.clear();
        for (final Object o : MC.theWorld.loadedEntityList)
        {
            if (!(o instanceof Entity))
            {
                continue;
            }
            final Entity bEntity = (Entity)o;
            if (bEntity.isDead || bEntity.equals(MC.thePlayer) || bEntity.getEntityId() == FreecamModule.CAMERA_ENTITY_ID)
            {
                continue;
            }

            if ((playersSetting.getValue() && bEntity instanceof EntityPlayer)
                    || (hostileSetting.getValue() && EntityUtil.isEntityHostile(bEntity))
                    || (passiveSetting.getValue() && EntityUtil.isEntityPassive(bEntity))
                    || (bEntity instanceof EntityItemFrame && itemFramesSetting.getValue())
                    || (bEntity instanceof EntityItem && droppedItemsSetting.getValue()))
            {
                renderTargetList.add(bEntity);
            }
        }

        for (final Object o : MC.theWorld.loadedTileEntityList)
        {
            if (!(o instanceof TileEntity))
            {
                continue;
            }
            final TileEntity entity = (TileEntity)o;
            if (modeSetting.getValue() == Mode.CS_GO)
            {
                if (entity instanceof TileEntityChest && chestsSetting.getValue())
                {
                    renderTargetList.add(entity);
                }
                continue;
            }

            if (modeSetting.getValue() == Mode.SHADER)
            {
                if (((entity instanceof TileEntityChest
                            || entity instanceof TileEntityEnderChest)
                            && chestsSetting.getValue())
                        || (entity instanceof TileEntityEndPortal && endPortalsSetting.getValue())
                        || (entity instanceof TileEntitySkull && skullsSetting.getValue())
                        || (redstoneSetting.getValue()
                            && (entity instanceof TileEntityDispenser
                            || entity instanceof TileEntityComparator
                            || entity instanceof TileEntityDaylightDetector
                            || entity instanceof TileEntityPiston
                            || entity instanceof TileEntityHopper))
                        || (entity instanceof TileEntitySign && signsSetting.getValue()))
                {
                    renderTargetList.add(entity);
                }
            } else
            {
                if ((entity instanceof TileEntityChest || entity instanceof TileEntityEnderChest)
                        && chestsSetting.getValue())
                {
                    renderTargetList.add(entity);
                }
            }
        }
    };

    @Subscribe
    private final EventListener<EventRender2D> render2DEventListener = event ->
    {
        if (modeSetting.getValue().equals(Mode.CS_GO))
        {
            MC.mcProfiler.startSection("esp_csgo");
            renderCSGOESP();
            MC.mcProfiler.endSection();
        }
    };

    @Subscribe(priority = IEventPriorities.LOW)
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.SHADER)
        {
            MC.mcProfiler.startSection("esp_shader");
            renderShaderESP(event.getPartialTicks());
            MC.mcProfiler.endSection();
            return;
        }

        for (final Object entity : renderTargetList)
        {
            if (modeSetting.getValue() == Mode.CS_GO)
            {
                MC.mcProfiler.startSection("esp_csgo_projection");
                projectEntity(entity, event.getPartialTicks());
                MC.mcProfiler.endSection();
            } else if (modeSetting.getValue() == Mode.BOX)
            {
                MC.mcProfiler.startSection("esp_box");
                renderBoxESP(entity, event.getPartialTicks());
                MC.mcProfiler.endSection();
            }
        }
    };

    private void renderShaderESP(float tickDelta)
    {
        final ScaledResolution r = Render2D.RESOLUTION;
        if (r == null)
        {
            return;
        }
        glPushMatrix();
        glPushAttrib(GL_ALPHA_BITS);

        glDisable(GL_CULL_FACE);

//        final boolean renderShadows = Render.renderShadow;
//        Render.renderShadow = false;

        MC.mcProfiler.startSection("fb");
        if (fb != null)
        {
            fb.framebufferClear();

            if (r.getScaledHeight() != fbHeight || r.getScaledWidth() != fbWidth || r.getScaleFactor() != fbScale)
            {
                fbHeight = r.getScaledHeight();
                fbWidth = r.getScaledWidth();
                fbScale = r.getScaleFactor();

                fb.deleteFramebuffer();
                fb = new Framebuffer(MC.displayWidth, MC.displayWidth, true);
            }
        } else
        {
            fb = new Framebuffer(MC.displayWidth, MC.displayWidth, true);
        }
        MC.mcProfiler.endStartSection("static_render");
        fb.bindFramebuffer(false);

        for (final Object renderTarget : renderTargetList)
        {
            if (renderTarget instanceof Entity)
            {
                RenderManager.instance.renderEntityStatic((Entity) renderTarget, tickDelta, true);
            } else if (renderTarget instanceof TileEntity)
            {
                TileEntityRendererDispatcher.instance.renderTileEntity((TileEntity) renderTarget, tickDelta);
            }
        }
        MC.mcProfiler.endStartSection("bind_fb");

        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);
        glEnable(GL_ALPHA_TEST);
        glDepthMask(false);

        fb.unbindFramebuffer();
        MC.getFramebuffer().bindFramebuffer(true);

        RenderUtil.ESP_SHADER.use();
        RenderUtil.ESP_SHADER.set("texture", 0);
        RenderUtil.ESP_SHADER.set("texelSize", 1.0f / r.getScaledWidth(), 1.0f / r.getScaledHeight());
        Color c = HUDModule.INSTANCE.primaryColorSetting.getValue();
        RenderUtil.ESP_SHADER.set("color", c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1);
        RenderUtil.ESP_SHADER.set("radius", lineWidthSetting.getValue());
        RenderUtil.ESP_SHADER.set("opacity", opacitySetting.getValue());

        MC.entityRenderer.disableLightmap(0.0);
        RenderHelper.disableStandardItemLighting();

        MC.mcProfiler.endStartSection("overlay");

        MC.entityRenderer.setupOverlayRendering();
        glEnable(GL_TEXTURE_2D);

        glBindTexture(GL_TEXTURE_2D, fb.framebufferTexture);
        glBegin(GL_QUADS);
        {
            glTexCoord2d(0, 1);
            glVertex2d(0, 0);
            glTexCoord2d(0, 0);
            glVertex2d(0, r.getScaledHeight());
            glTexCoord2d(1, 0);
            glVertex2d(r.getScaledWidth(), r.getScaledHeight());
            glTexCoord2d(1, 1);
            glVertex2d(r.getScaledWidth(), 0);
        }
        glEnd();

        RenderUtil.ESP_SHADER.stop();

        MC.mcProfiler.endStartSection("finish");

        MC.entityRenderer.enableLightmap(0);

//        Render.renderShadow = renderShadows;

        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glPopAttrib();
        glPopMatrix();

        MC.entityRenderer.setupOverlayRendering();
        MC.mcProfiler.endSection();
    }

    private Framebuffer setupFB(final ScaledResolution res)
    {
        if (fb != null)
        {
            fb.framebufferClear();

            if (res.getScaledHeight() != fbHeight || res.getScaledWidth() != fbWidth || res.getScaleFactor() != fbScale)
            {
                fbHeight = res.getScaledHeight();
                fbWidth = res.getScaledWidth();
                fbScale = res.getScaleFactor();

                fb.deleteFramebuffer();
                return new Framebuffer(MC.displayWidth, MC.displayWidth, true);
            }
            return fb;
        } else
        {
            return (fb = new Framebuffer(MC.displayWidth, MC.displayWidth, true));
        }
    }

    private void renderCSGOESP()
    {
        for (final int entityID : projected.keySet())
        {
            Object gEntity = MC.theWorld.getEntityByID(entityID);
            if (gEntity == null)
            {
                for (final Object o : MC.theWorld.loadedTileEntityList)
                {
                    if (!(o instanceof TileEntity))
                    {
                        continue;
                    }
                    final TileEntity tileEntity = (TileEntity)o;
                    if (tileEntity.hashCode() == entityID)
                    {
                        gEntity = tileEntity;
                        break;
                    }
                }
            }

            if (gEntity instanceof EntityLivingBase)
            {
                final EntityLivingBase e = (EntityLivingBase) gEntity;
                if (e.isDead || e.getHealth() <= 0.0f)
                {
                    renderTargetList.remove(e);
                    projected.remove(entityID);
                    continue;
                }
            }

            if (gEntity instanceof EntityItemFrame
                    && ((EntityItemFrame) gEntity).getDisplayedItem() == null)
            {
                continue;
            }

            final float[][] projection = projected.get(entityID);
            final float[] top = projection[0], bottom = projection[1];

            double height = top[1] - bottom[1];
            double width = height * 0.3;

            glPushMatrix();

            glLineWidth(lineWidthSetting.getValue());
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_TEXTURE_2D);

            // TODO
            if (MC.gameSettings.guiScale == 2)
            {
                glScaled(0.5, 0.5, 0.5);
            } else if (MC.gameSettings.guiScale == 3)
            {
                glScaled(0.33f, 0.33f, 0.33f);
            }
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            glBegin(GL_LINES);
            {
                glVertex2d(top[0] - width, top[1]);
                glVertex2d(top[0] + width, top[1]);

                glVertex2d(top[0] - width, top[1]);
                glVertex2d(top[0] - width, bottom[1]);

                glVertex2d(top[0] + width, top[1]);
                glVertex2d(top[0] + width, bottom[1]);

                glVertex2d(top[0] - width, bottom[1]);
                glVertex2d(top[0] + width, bottom[1]);
            }
            glEnd();

            // heath bar

            if (gEntity instanceof EntityLivingBase)
            {
                final EntityLivingBase e = (EntityLivingBase) gEntity;
                final float healthPercent = (e.getHealth() + e.getAbsorptionAmount()) / 24.0f;
                glColor4f(
                        1.0f - healthPercent,
                        healthPercent,
                        0.0f,
                        1.0f
                );

                glBegin(GL_LINES);
                {
                    glVertex2d(top[0] + (height * 0.35), top[1]);
                    glVertex2d(top[0] + (height * 0.35), bottom[1]);
                }
                glEnd();
            }

            glEnable(GL_TEXTURE_2D);
            glEnable(GL_DEPTH_TEST);

            if (labelsSetting.getValue())
            {
                String text = null;
                if (gEntity instanceof EntityLivingBase)
                {
                    final EntityLivingBase e = (EntityLivingBase) gEntity;
                    text = e.getCommandSenderName() + EnumChatFormatting.RED + " " + e.getHealth() + "❤";
                } else if (gEntity instanceof TileEntity)
                {
                    final TileEntity e = (TileEntity) gEntity;
                    text = e.getBlockType().getLocalizedName();
                } else if (gEntity instanceof EntityItem)
                {
                    final EntityItem e = (EntityItem) gEntity;
                    text = e.getEntityItem().getDisplayName() + " x" + e.getEntityItem().stackSize;
                } else if (gEntity instanceof EntityItemFrame)
                {
                    final EntityItemFrame e = (EntityItemFrame) gEntity;
                    if (e.getDisplayedItem() != null)
                    {
                        text = e.getDisplayedItem().getDisplayName();
                    }
                }

                if (text != null)
                {
                    text = NameProtectModule.INSTANCE.protect(text);
                    final int textWidth = MC.fontRenderer.getStringWidth(text);
                    MC.fontRenderer.drawStringWithShadow(text, (int) (top[0] - (textWidth / 2.0f)), (int) top[1] - 10, -1);
                }
            }

            glLineWidth(1.0f);

            glPopMatrix();
        }
    }

    private void renderBoxESP(final Object entity, final float partialTicks)
    {
        AxisAlignedBB aabb = null;
        if (entity instanceof EntityLivingBase)
        {
            final EntityLivingBase e = (EntityLivingBase) entity;

            double x = (e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks);
            double y = (e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks);
            double z = (e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks);

            double o = e.width - 0.25;

            aabb = AxisAlignedBB.getBoundingBox(x - o, y - 0.2, z - o,
                    x + o,
                    y + e.height + 0.2,
                    z + o);
        } else if (entity instanceof TileEntity)
        {
            final TileEntity e = (TileEntity) entity;
            aabb = AxisAlignedBB.getBoundingBox(e.xCoord, e.yCoord, e.zCoord,
                    e.xCoord + 1,
                    e.yCoord + 1,
                    e.zCoord + 1);
        }

        if (aabb == null)
        {
            return;
        }

        final int color = getColor(entity);
        Render3D.filledAABB(aabb, QuadMask.ALL_FACES, ColorUtil.withAlpha(color, (int) (255.0f * opacitySetting.getValue())));
        Render3D.outlinedAABB(aabb, lineWidthSetting.getValue(), QuadMask.ALL_FACES, color);
    }

    private void projectEntity(final Object entity, final float partialTicks)
    {
        float[] top, bottom;
        int id;

        if (entity instanceof Entity)
        {
            final Entity e = (Entity) entity;
            double x = (e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks) - RenderManager.renderPosX;
            double y = (e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks) - RenderManager.renderPosY;
            double z = (e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks) - RenderManager.renderPosZ;

            top = ProjectionUtil.project(x, y + e.height + 0.2, z);
            bottom = ProjectionUtil.project(x, y - 0.2, z);
            id = e.getEntityId();
        } else if (entity instanceof TileEntity)
        {
            final TileEntity e = (TileEntity) entity;
            double x = e.xCoord - RenderManager.renderPosX;
            double y = e.yCoord - RenderManager.renderPosY;
            double z = e.zCoord - RenderManager.renderPosZ;
            top = ProjectionUtil.project(x, y + 1.2, z);
            bottom = ProjectionUtil.project(x, y - 0.2, z);
            id = e.hashCode();
        } else
        {
            return;
        }

        if (top[2] > 1 || bottom[2] > 1)
        {
            projected.remove(id);
            return;
        }

        projected.put(id, new float[][]{ top, bottom });
    }

    // amazing gavcode 3000
    private int getColor(final Object entity)
    {
        if (entity instanceof EntityLivingBase)
        {
            if (entity instanceof EntityPlayer)
            {
                final EntityPlayer player = (EntityPlayer) entity;
                if (Nebula.FRIENDS.has(player))
                {
                    return Color.cyan.getRGB();
                }
                return HUDModule.INSTANCE.getBaseColor(10);
            } else if (EntityUtil.isEntityPassive((Entity) entity))
            {
                return Color.green.getRGB();
            } else if (EntityUtil.isEntityHostile((Entity) entity))
            {
                return Color.red.getRGB();
            }
        } else if (entity instanceof TileEntity)
        {
            if (entity instanceof TileEntityEnderChest)
            {
                return Color.magenta.getRGB();
            } else if (entity instanceof TileEntityChest)
            {
                return Color.orange.getRGB();
            }
        }
        return HUDModule.INSTANCE.getBaseColor(10);
    }

    private enum Mode
    {
        BOX, CS_GO, SHADER
    }
}
