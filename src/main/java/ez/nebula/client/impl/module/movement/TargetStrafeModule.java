package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.util.minecraft.player.MoveUtil;
import ez.nebula.client.util.render.RenderUtil;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 3/16/26
 */
@ModuleManifest(name = "TargetStrafe",
        description = "Moves in a circle around your KillAura target to make it harder for them to attack you back",
        category = ModuleCategory.MOVEMENT)
public final class TargetStrafeModule extends Module
{
    private final NumberSetting<Float> rangeSetting = numberBuilder("Range", 4.2f)
            .setMin(1.0f)
            .setMax(6.0f)
            .setScale(0.1f)
            .setDescription("The range to strafe around your target")
            .build();
    private final NumberSetting<Double> reductionSetting = numberBuilder("Speed Reduction", 0.0)
            .setMin(0.0)
            .setMax(1.0)
            .setScale(0.01)
            .setDescription("What percentage to reduce strafe speed to prevent lagbacks")
            .build();
    private final NumberSetting<Double> advanceSetting = numberBuilder("Advance", 1.0)
            .setMin(0.0)
            .setMax(1.0)
            .setScale(0.1)
            .setDescription("The amount of blocks to move towards the target when holding W")
            .build();
    private final Setting<Boolean> jumpBackoutSetting = builder("Jump to Backout", true)
            .setDescription("If to allow holding space as a way to exit the strafe lock")
            .build();
    private final Setting<Boolean> autoMoveSetting = builder("Auto Move", false)
            .setDescription("If to automatically move when strafing around a target")
            .build();
    private final Setting<Boolean> renderSetting = builder("Render", true)
            .setDescription("If to render the strafe circle around the target")
            .build();

    private boolean strafeDirection = true;

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (!renderSetting.getValue() || isBlocked())
        {
            return;
        }
        MC.mcProfiler.startSection("targetStrafe");
        final EntityLivingBase target = KillAuraModule.INSTANCE.getTarget();

        glPushMatrix();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 0, 1);
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(2.5f);

        RenderUtil.setGLColor(HUDModule.INSTANCE.getBaseColor(10));

        final double x = target.prevPosX + (target.posX - target.prevPosX) * event.getPartialTicks();
        final double y = target.prevPosY + (target.posY - target.prevPosY) * event.getPartialTicks();
        final double z = target.prevPosZ + (target.posZ - target.prevPosZ) * event.getPartialTicks();

        glBegin(GL_LINE_LOOP);
        {
            final double radius = getStrafeRange();
            for (double angle = 0.0; angle <= 360.0; angle += 1.0)
            {
                final double rad = Math.toRadians(angle);
                glVertex3d(x + (Math.sin(rad) * radius) - RenderManager.renderPosX,
                        y - RenderManager.renderPosY,
                        z - (Math.cos(rad) * radius) - RenderManager.renderPosZ);
            }
        }
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();

        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdateInput.Post> postEventListener = event ->
    {
        if (!isBlocked() && autoMoveSetting.getValue() && event.getInput().equals(MC.thePlayer.movementInput))
        {
            event.getInput().jump = false;
            event.getInput().sneak = false;
            event.getInput().moveForward = 1;
        }
    };

    @Subscribe(priority = IEventPriorities.LOW)
    private final EventListener<EventMove> moveEventListener = event ->
    {
        if (isBlocked())
        {
            return;
        }

        if (MC.thePlayer.isCollidedHorizontally)
        {
            strafeDirection = !strafeDirection;
        } else
        {
            final float strafe = MC.thePlayer.movementInput.moveStrafe;
            if (strafe != 0.0f)
            {
                strafeDirection = strafe > 0.0f;
            }
        }

        final EntityLivingBase target = KillAuraModule.INSTANCE.getTarget();
        final double moveSpeed = Math.sqrt(event.getX() * event.getX() + event.getZ() * event.getZ())
                * (1.0 - reductionSetting.getValue());

        double degree = Math.atan2(MC.thePlayer.posZ - target.posZ, MC.thePlayer.posX - target.posX);
        degree += (moveSpeed / MC.thePlayer.getDistanceToEntity(target)) * (strafeDirection ? 1 : -1);

        // target strafe will tweak if strafe range is > than ka range
        final double dist = getStrafeRange();
        double x = target.posX + dist * Math.cos(degree);
        double z = target.posZ + dist * Math.sin(degree);

        float yaw = (float) (Math.toDegrees(Math.atan2(z - MC.thePlayer.posZ, x - MC.thePlayer.posX)) - 90.0);

        double rad = Math.toRadians(yaw);
        event.setX(moveSpeed * -Math.sin(rad));
        event.setZ(moveSpeed * Math.cos(rad));
    };

    private double getStrafeRange()
    {
        double range = Math.min(rangeSetting.getValue(), KillAuraModule.INSTANCE.rangeSetting.getValue());
        if (MC.gameSettings.keyBindForward.getIsKeyPressed() && MC.renderViewEntity.equals(MC.thePlayer))
        {
            range -= advanceSetting.getValue();
        }
        return Math.max(range, 1) - 0.5;
    }

    private boolean isBlocked()
    {
        return !KillAuraModule.INSTANCE.isAttacking()
                || !SpeedModule.INSTANCE.isToggled()
                || (!MoveUtil.isMoving() && !autoMoveSetting.getValue())
                || MC.thePlayer.movementInput.moveForward < 0 // allow to backout of the target strafe
                || (jumpBackoutSetting.getValue() && MC.thePlayer.movementInput.jump); // additionally, holding jump backs out
    }
}
