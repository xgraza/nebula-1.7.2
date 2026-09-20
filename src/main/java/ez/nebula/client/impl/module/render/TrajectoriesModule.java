package ez.nebula.client.impl.module.render;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.render.RenderUtil;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 03/02/25
 */
@SuppressWarnings("unchecked")
@ModuleManifest(name = "Trajectories",
        description = "Renders the predicted path of a projectile (may be slightly inaccurate due to Minecraft randomness)",
        category = ModuleCategory.RENDER)
public final class TrajectoriesModule extends Module
{
    private final NumberSetting<Float> lineWidthSetting = numberBuilder("Line Width", 1.5f)
            .setMin(0.5f)
            .setMax(5.0f)
            .setScale(0.1f)
            .setDescription("What width the boundary should be rendered with")
            .build();
    private final Setting<Boolean> renderTailSetting = builder("Render Trail", true)
            .setDescription("If to render the trajectory path")
            .build();
    private final NumberSetting<Double> landingRadiusSetting = numberBuilder("Landing Radius", 0.5)
            .setMin(0.1)
            .setMax(1.5)
            .setScale(0.1)
            .setDescription("The radius to render around the landing position of the trajectory")
            .build();

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        final TrajectoryResult result = calculateTrajectory(MC.thePlayer, event.getPartialTicks());
        if (result == null)
        {
            return;
        }

        MC.mcProfiler.startSection("trajectories");

        glPushMatrix();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 0, 1);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(lineWidthSetting.getValue());

        RenderUtil.setGLColor(HUDModule.INSTANCE.getBaseColor(0));

        if (renderTailSetting.getValue())
        {
            glBegin(GL_LINE_STRIP);
            {
                for (final Vec3 vec3 : result.getTrail())
                {
                    glVertex3d(vec3.xCoord - RenderManager.renderPosX,
                            vec3.yCoord - RenderManager.renderPosY,
                            vec3.zCoord - RenderManager.renderPosZ);
                }
            }
            glEnd();
        }

        final Vec3 hitVec = result.getLanding() == null || result.getLanding().hitVec == null
                ? result.getTrail().get(result.getTrail().size() - 1)
                : result.getLanding().hitVec;

        glTranslated(hitVec.xCoord - RenderManager.renderPosX,
                hitVec.yCoord - RenderManager.renderPosY,
                hitVec.zCoord - RenderManager.renderPosZ);
        if (result.getLanding() != null)
        {
            final int hitSide = result.getLanding().sideHit;
            if (hitSide == 2 || hitSide == 3) {
                glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
            } else if (hitSide == 4 || hitSide == 5) {
                glRotatef(-90.0f, 0.0f, 0.0f, -1.0f);
            }
        }

        glBegin(GL_LINE_STRIP);
        {
            final double r = landingRadiusSetting.getValue();
            for (double angle = 0.0; angle <= 360.0; angle += 0.5)
            {
                double rad = Math.toRadians(angle);
                glVertex3d((Math.sin(rad) * r),
                        0,
                        (Math.cos(rad) * r));
            }
        }
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();

        MC.mcProfiler.endSection();
    };

    private TrajectoryResult calculateTrajectory(final EntityPlayer player,
                                                 final float partialTicks)
    {
        final ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemBow
                || (stack.getItem() instanceof ItemEnderPearl)
                || stack.getItem() instanceof ItemSnowball
                || stack.getItem() instanceof ItemEgg
                || stack.getItem() instanceof ItemExpBottle
                || (stack.getItem() instanceof ItemPotion
                && ItemPotion.isSplash(stack.getItemDamage()))))
        {
            return null;
        }

        final TrajectoryResult trajectoryResult = new TrajectoryResult();

        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        final float[] angles = Nebula.ROTATIONS.getServerAngles();
        final float yaw = angles[0];
        final float pitch = angles[1];

        x -= Math.cos(yaw / 180.0f * Math.PI) * 0.16f;
        y += player.getEyeHeight() - 0.10000000149011612D;
        z -= Math.sin(yaw / 180.0f * Math.PI) * 0.16f;

        if (MC.gameSettings.thirdPersonView != 0)
        {
            trajectoryResult.addTrail(Vec3.createVectorHelper(x, y, z));
        }

        final double size = stack.getItem() instanceof ItemBow ? 0.3 : 0.25;

        float velocity = 0.0f;
        float pitchOffset = 0.0f;
        if (stack.getItem() instanceof ItemEnderPearl
                || stack.getItem() instanceof ItemEgg
                || stack.getItem() instanceof ItemSnowball)
        {
            velocity = 1.5f;
        } else if (stack.getItem() instanceof ItemExpBottle)
        {
            velocity = 0.7f;
            pitchOffset = -20.0f;
        } else if (stack.getItem() instanceof ItemPotion)
        {
            velocity = 0.5f;
            pitchOffset = -20.0f;
        } else if (stack.getItem() instanceof ItemBow)
        {
            velocity = getArrowVelocity(stack.getMaxItemUseDuration() - player.getItemInUseCount()) * 3.0f;
        }

        double motionX = -Math.sin(yaw / 180.0f * Math.PI) * Math.cos(pitch / 180.0f * Math.PI) * 0.4f;
        double motionZ = Math.cos(yaw / 180.0f * Math.PI) * Math.cos(pitch / 180.0f * Math.PI) * 0.4f;
        double motionY = -Math.sin((pitch + pitchOffset) / 180.0f * Math.PI) * 0.4f;

        final double distance = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);

        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;

        double lastDist = 0.0;

        MovingObjectPosition finalResult = null;

        while (y > 0.0)
        {
            final Vec3 pos = Vec3.createVectorHelper(x, y, z);
            final Vec3 motion = pos.addVector(motionX, motionY, motionZ);

            final MovingObjectPosition result = MC.theWorld.rayTraceBlocks(pos, motion);
            if (result != null && !result.typeOfHit.equals(MovingObjectPosition.MovingObjectType.MISS))
            {
                finalResult = result;
                break;
            }

            final List<Entity> entitiesColliding = MC.theWorld.getEntitiesWithinAABB(Entity.class,
                    new AxisAlignedBB(
                            x - size, y - size, z - size,
                            x + size, y + size, z + size)
                            .addCoord(motionX, motionY, motionZ)
                            .expand(1.0, 1.0, 1.0));
            if (!entitiesColliding.isEmpty())
            {
                for (final Entity entity : entitiesColliding)
                {
                    if (!entity.canBeCollidedWith() || entity.equals(MC.thePlayer) || entity.equals(MC.renderViewEntity))
                    {
                        continue;
                    }
                    final AxisAlignedBB aabb = entity.boundingBox.copy().expand(0.3, 0.3, 0.3);
                    final MovingObjectPosition interceptedRaytrace = aabb.calculateIntercept(pos, motion);
                    if (interceptedRaytrace != null)
                    {
                        final double hitVecDistance = pos.distanceTo(interceptedRaytrace.hitVec);
                        if (hitVecDistance < lastDist || lastDist == 0.0)
                        {
                            lastDist = hitVecDistance;
                            finalResult = interceptedRaytrace;
                            break;
                        }
                    }
                }
            }

            x += motionX;
            y += motionY;
            z += motionZ;

            trajectoryResult.addTrail(Vec3.createVectorHelper(x, y, z));

            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;

            if (stack.getItem() instanceof ItemExpBottle)
            {
                motionY -= 0.07;
            } else if (stack.getItem() instanceof ItemBow
                    || stack.getItem() instanceof ItemPotion)
            {
                motionY -= 0.05;
            } else
            {
                motionY -= 0.03;
            }
        }

        if (finalResult != null)
        {
            trajectoryResult.addTrail(finalResult.hitVec);
        }

        trajectoryResult.setLanding(finalResult);
        return trajectoryResult;
    }

    private float getArrowVelocity(final int charge)
    {
        float f = (float) charge / 20.0f;
        f = (f * f + f * 2.0f) / 3.0f;
        if (f > 1.0f)
        {
            f = 1.0f;
        }
        return f;
    }

    private static class TrajectoryResult
    {
        private final List<Vec3> trail = new LinkedList<>();
        private MovingObjectPosition landing;

        public void addTrail(final Vec3 vec3)
        {
            trail.add(vec3);
        }

        public List<Vec3> getTrail()
        {
            return trail;
        }

        public MovingObjectPosition getLanding()
        {
            return landing;
        }

        public void setLanding(MovingObjectPosition landing)
        {
            this.landing = landing;
        }
    }
}
