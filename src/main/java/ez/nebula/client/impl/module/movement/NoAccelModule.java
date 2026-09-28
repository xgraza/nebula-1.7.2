package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.MoveUtil;

/**
 * @author xgraza
 * @since 07/04/25
 */
@ModuleManifest(name = "NoAccel",
        description = "Makes movement more instantaneous",
        category = ModuleCategory.MOVEMENT)
public final class NoAccelModule extends Module
{
    private final Setting<Boolean> movingSetting = builder("Moving", false)
            .setDescription("If to force a static speed when moving")
            .build();

    @Subscribe
    private final EventListener<EventMove> moveEventListener = event ->
    {
        if (MoveUtil.isMoving())
        {
            if (!movingSetting.getValue()
                    || !MC.thePlayer.onGround
                    || MC.gameSettings.keyBindJump.getIsKeyPressed()
                    || SpeedModule.INSTANCE.isToggled())
            {
                return;
            }
            MoveUtil.setSpeed(event, MoveUtil.getBaseNcpSpeed(0));
            return;
        }
        MoveUtil.setSpeed(event, 0);
    };
}
