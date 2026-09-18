package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventMove;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author xgraza
 * @since 9/16/26
 */
@ModuleManifest(name = "ReverseStep",
        description = "Makes you fall faster down blocks",
        category = ModuleCategory.MOVEMENT)
public final class ReverseStepModule extends Module
{
    private final NumberSetting<Double> blocksSetting = numberBuilder("Blocks", 2.0)
            .setMin(0.5)
            .setMax(20.0)
            .setScale(0.5)
            .setDescription("The maximum amount of blocks to fall")
            .build();
    private final NumberSetting<Double> downforceSetting = numberBuilder("Downforce", 1.0)
            .setMin(1.0)
            .setMax(20.0)
            .setScale(0.5)
            .setDescription("The speed to force downwards")
            .build();

    @Subscribe
    private final EventListener<EventMove> moveEventListener = event ->
    {
        if (!MC.thePlayer.onGround
                || MC.thePlayer.isInWater()
                || PlayerUtil.isAboveWater()
                || PlayerUtil.isPhased()
                || MC.thePlayer.isInWeb
                || MC.thePlayer.isOnLadder()
                || MC.gameSettings.keyBindJump.pressed)
        {
            return;
        }
        for (double y = 0.0; y < blocksSetting.getValue() + 0.5; y += 0.01)
        {
            final AxisAlignedBB bb = MC.thePlayer.boundingBox.copy().offset(0.0, -y, 0.0);
            if (!MC.theWorld.func_147461_a(bb).isEmpty() && bb.minY > 0.0)
            {
                event.setY(MC.thePlayer.motionY = -downforceSetting.getValue());
                break;
            }
        }
    };
}
