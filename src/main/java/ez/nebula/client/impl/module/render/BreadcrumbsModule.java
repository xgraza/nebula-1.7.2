package ez.nebula.client.impl.module.render;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.ColorSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.INetHandlerPlayClient;
import ez.nebula.client.util.math.MathUtil;
import ez.nebula.client.util.render.RenderUtil;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 6/24/26
 */
@ModuleManifest(name = "Breadcrumbs",
        description = "Shows a trail of where an entity was",
        category = ModuleCategory.RENDER)
public final class BreadcrumbsModule extends Module
{
    private final Setting<Boolean> playersSetting = builder("Players", true)
            .setDescription("If to show breadcrumbs of other players")
            .build();
    private final Setting<Boolean> onlySelfSetting = builder("Only Self", true)
            .setDescription("If to only render a breadcrumb for yourself")
            .setVisibility((value) -> playersSetting.getValue())
            .build();
    private final Setting<Boolean> pearlsSetting = builder("Pearls", true)
            .setDescription("If to show breadcrumbs for thrown ender pearls")
            .build();
    private final Setting<Boolean> arrowsSetting = builder("Arrows", true)
            .setDescription("If to show breadcrumbs for arrows")
            .build();

    private final NumberSetting<Double> decayTimeSetting = numberBuilder("Decay Time", 3.5)
            .setMin(1.0)
            .setMax(10.0)
            .setScale(0.1)
            .setDescription("How long in seconds before a breadcrumb is invalidated")
            .build();
    private final Setting<Boolean> fadeSetting = builder("Fade", false)
            .setDescription("If to fade the breadcrumb before removing it")
            .build();
    private final NumberSetting<Double> fadeTimeSetting = numberBuilder("Fade Time", 1.5)
            .setMin(1.1)
            .setMax(10.0)
            .setScale(0.1)
            .setDescription("How long in seconds before the expire time before fading")
            .setVisibility((value) -> fadeSetting.getValue())
            .build();
    private final NumberSetting<Float> lineWidthSetting = numberBuilder("Line Width", 1.5f)
            .setMin(0.5f)
            .setMax(5.0f)
            .setScale(0.1f)
            .setDescription("The width of the breadcrumb trail")
            .build();

    private final Map<Integer, List<Breadcrumb>> entityBreadcrumbMap = new ConcurrentHashMap<>();

    @Override
    public void onDisable()
    {
        super.onDisable();
        entityBreadcrumbMap.clear();
    }

    @Subscribe
    private final EventListener<EventChangeWorld> changeWorldEventListener = event ->
    {
        entityBreadcrumbMap.clear();
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        MC.mcProfiler.startSection("breadCrumbs");
        for (final Object o : MC.theWorld.loadedEntityList)
        {
            if (!(o instanceof Entity))
            {
                continue;
            }
            final Entity entity = (Entity)o;
            if (isValidEntity(entity))
            {
                final List<Breadcrumb> breadcrumbList = entityBreadcrumbMap.computeIfAbsent(
                        entity.getEntityId(), (__) -> new CopyOnWriteArrayList<>());
                final Breadcrumb newBreadcrumb = new Breadcrumb(MathUtil.lerpEntity(entity, event.getPartialTicks()), System.currentTimeMillis());
                if (entity instanceof EntityPlayer)
                {
                    if (entity.equals(MC.thePlayer))
                    {
                        newBreadcrumb.vec.yCoord -= MC.thePlayer.yOffset;
                    }
                    newBreadcrumb.vec.yCoord += 0.2;
                }
                breadcrumbList.add(newBreadcrumb);
            }
        }

        entityBreadcrumbMap.values().forEach(this::renderBreadcrumbTrail);
        MC.mcProfiler.endSection();
    };

    private void renderBreadcrumbTrail(final List<Breadcrumb> breadcrumbList)
    {
        breadcrumbList.removeIf((breadcrumb) ->
                System.currentTimeMillis() - breadcrumb.createTime >= decayTimeSetting.getValue() * 1000.0);

        glPushMatrix();

        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(lineWidthSetting.getValue());
        glEnable(GL_ALPHA_TEST);
        glDisable(GL_DEPTH_TEST);

        final ColorSetting cs = (ColorSetting) HUDModule.INSTANCE.primaryColorSetting;

        glBegin(GL_LINE_STRIP);
        {
            for (final Breadcrumb breadcrumb : breadcrumbList)
            {
                int alpha = 255;

                // insane code!!!
                if (fadeSetting.getValue())
                {
                    double fadeTimeMS = fadeTimeSetting.getValue() * 1000.0;

                    long expireTime = breadcrumb.createTime + (long)(decayTimeSetting.getValue() * 1000.0);
                    long fadeTime = expireTime - (long) fadeTimeMS;
                    long time = System.currentTimeMillis();

                    alpha = (int) (((fadeTime - time) / fadeTimeMS) * 255.0);
                    if (alpha > 255)
                    {
                        alpha = 255;
                    }
                }

                // if invisible, don't render
                if (alpha <= 0)
                {
                    breadcrumbList.remove(breadcrumb);
                    continue;
                }

                RenderUtil.setGLColor(cs.getValueInt(alpha));
                final Vec3 vec = breadcrumb.vec.addVector(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);
                glVertex3d(vec.xCoord, vec.yCoord, vec.zCoord);
            }
        }
        glEnd();

        glEnable(GL_DEPTH_TEST);
        glLineWidth(1.0f);
        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    private boolean isValidEntity(final Entity entity)
    {
        if (!((INetHandlerPlayClient)MC.getNetHandler()).nebula$getDoneLoadingTerrain())
        {
            return false;
        }
        if (entity instanceof EntityPlayer)
        {
            if (!playersSetting.getValue())
            {
                return false;
            }
            if (onlySelfSetting.getValue() && !entity.equals(MC.thePlayer))
            {
                return false;
            }
            return true;
        }
        if (entity instanceof EntityEnderPearl)
        {
            return pearlsSetting.getValue();
        }
        if (entity instanceof EntityArrow)
        {
            return arrowsSetting.getValue();
        }
        return false;
    }

    private static final class Breadcrumb
    {
        private final Vec3 vec;
        private final long createTime;

        public Breadcrumb(Vec3 vec, long createTime)
        {
            this.vec = vec;
            this.createTime = createTime;
        }
    }
}
