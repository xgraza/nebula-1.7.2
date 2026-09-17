package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.util.math.Timer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.util.List;

/**
 * @author aesthetical
 * @since 04/29/23
 * @apiNote Ported from Nebula 3.0.0 on 8/25/26
 */
@ModuleManifest(name = "AutoRape",
        description = "Automatically does unspeakable things to the person in front of you",
        category = ModuleCategory.PLAYER)
public final class AutoRapeModule extends Module
{
    private static final double OFFSET = 0.05;

    private final NumberSetting<Integer> strokesSetting = numberBuilder("Strokes", 10)
            .setMin(1)
            .setMax(60)
            .setScale(1)
            .setDescription("How many strokes to give in a second")
            .build();
    private final NumberSetting<Float> differenceSetting = numberBuilder("Difference", 40.0f)
            .setMin(1.0f)
            .setMax(90.0f)
            .setScale(0.1f)
            .setDescription("The difference of yaw")
            .build();

    private final Timer timer = new Timer();
    private boolean override, sneaking;

    @Override
    public void onDisable()
    {
        super.onDisable();
        override = false;
        sneaking = false;
    }

    @Subscribe
    private final EventListener<EventUpdateInput.Post> postEventListener = event ->
    {
        if (override)
        {
            event.getInput().sneak = sneaking;
        }
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        final List<EntityPlayer> entities = MC.theWorld.playerEntities;
        if (entities.isEmpty())
        {
            return;
        }

        EntityPlayer player = null;
        for (final EntityPlayer entity : entities)
        {
            // if the entity is null/dead (no hitbox) or is ourselves, continue
            if (entity == null || entity.isDead || entity.equals(MC.thePlayer))
            {
                continue;
            }

            // if we are intersecting with their hitbox, we have a target
            if (entity.boundingBox.copy().expand(OFFSET, OFFSET, OFFSET).intersectsWith(MC.thePlayer.boundingBox))
            {
                player = entity;
                break;
            }
        }
        if (player == null)
        {
            if (override)
            {
                sneaking = false;
            }
            override = false;
            return;
        }

        final float deltaYaw = MathHelper.wrapAngleTo180_float(player.renderYawOffset)
                - MathHelper.wrapAngleTo180_float(MC.thePlayer.rotationYaw);
        if (deltaYaw > differenceSetting.getValue() || deltaYaw < -differenceSetting.getValue())
        {
            if (override)
            {
                sneaking = false;
            }
            override = false;
            return;
        }

        if (timer.hasElapsed(1000L / strokesSetting.getValue(), true))
        {
            override = true;
            sneaking = !sneaking;
        }
    };
}
