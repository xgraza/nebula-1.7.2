package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;

/**
 * @author xgraza
 * @since 03/10/25
 */
@ModuleManifest(name = "AutoWalk",
        description = "Automatically walks for you",
        category = ModuleCategory.MOVEMENT)
public final class AutoWalkModule extends Module
{
    private final Setting<Boolean> stopOnSneakSetting = builder("Stop on Sneak", true)
            .setDescription("If to stop walking when the sneak key is down")
            .build();
    private final Setting<Boolean> stopOnBackSetting = builder("Stop on Backwards", true)
            .setDescription("If to stop walking when the walk backwards key is down")
            .build();

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null && !MC.gameSettings.keyBindForward.getIsKeyPressed())
        {
            MC.thePlayer.movementInput.moveForward = 0.0f;
        }
    }

    @Subscribe
    private final EventListener<EventUpdateInput.Post> postUpdateInputEventListener = event ->
    {
        if (event.getInput().equals(MC.thePlayer.movementInput))
        {
            event.getInput().moveForward = allowWalk() ? 1.0f : 0.0f;
        }
    };

    private boolean allowWalk()
    {
        return (!stopOnSneakSetting.getValue() || !MC.gameSettings.keyBindSneak.getIsKeyPressed()) &&
                (!stopOnBackSetting.getValue() || !MC.gameSettings.keyBindBack.getIsKeyPressed());
    }
}
