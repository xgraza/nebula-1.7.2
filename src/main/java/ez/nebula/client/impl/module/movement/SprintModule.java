package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventOmniSprint;
import ez.nebula.client.api.listener.event.player.EventSprint;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;

/**
 * @author xgraza
 * @since 02/14/25
 */
@ModuleManifest(name = "Sprint",
        description = "Force holds the sprint key for you",
        category = ModuleCategory.MOVEMENT)
public final class SprintModule extends Module
{
    private final Setting<Boolean> omniSprintSetting = builder("Omni-Sprint", false)
            .setDescription("If to allow full-speed sprint in all directions")
            .build();

    @Subscribe
    private final EventListener<EventSprint> sprintEventListener = event ->
    {
        event.setSprinting(true);
        event.cancel();
    };

    @Subscribe
    private final EventListener<EventOmniSprint> omniSprintEventListener = event ->
            event.setCanceled(omniSprintSetting.getValue() && MC.thePlayer.movementInput.moveForward != 0.0f);
}
