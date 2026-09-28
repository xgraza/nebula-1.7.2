package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IEntityLivingBase;
import ez.nebula.client.mixin.duck.IEntityPlayerSP;

/**
 * @author xgraza
 * @since 03/24/25
 */
@ModuleManifest(name = "NoJumpDelay",
        description = "Removes the vanilla jump delay",
        category = ModuleCategory.MOVEMENT)
public final class NoJumpDelayModule extends Module
{
    private final Setting<Boolean> horsesSetting = builder("Horses", false)
            .setDescription("If to remove the jump delay on horses")
            .build();

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        ((IEntityLivingBase)MC.thePlayer).nebula$setJumpTicks(0);

        if (horsesSetting.getValue() && MC.thePlayer.isRidingHorse())
        {
            ((IEntityPlayerSP)MC.thePlayer).nebula$setHorseJumpPowerCounter(9);
        }
    };
}
