package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.player.EventStep;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IMinecraft;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * @author xgraza
 * @since 04/04/25
 */
@ModuleManifest(name = "Step",
        description = "Send extra packets to step up blocks without having to jump",
        category = ModuleCategory.MOVEMENT)
public final class StepModule extends Module
{
    private static final double[] STEP_PACKET_VALUES = {
            0.42, 0.7532, 1.0013,
            // 1.5
            1.16, 1.23, 1.2, 1.5
    };
    private static final float MIN_STEP_HEIGHT = 0.6f;

    private final NumberSetting<Float> stepHeightSetting = numberBuilder("Height", 1.0f)
            .setMin(MIN_STEP_HEIGHT)
            .setMax(1.5f)
            .setScale(0.1f)
            .setDescription("The max step height to use")
            .build();
    private final Setting<Boolean> timerSetting = builder("Timer", true)
            .setDescription("If to use timer")
            .build();

    private boolean timer;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null)
        {
            MC.thePlayer.stepHeight = 0.5f;
        }
        ((IMinecraft)MC).nebula$getTimer().timerSpeed = 1.0f;
        timer = false;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        MC.thePlayer.stepHeight = stepHeightSetting.getValue();
        if (timer && MC.thePlayer.onGround)
        {
            timer = false;
            ((IMinecraft)MC).nebula$getTimer().timerSpeed = 1.0f;
        }
    };

    @Subscribe
    private final EventListener<EventStep> stepEventListener = event ->
    {
        if (!event.getEntity().equals(MC.thePlayer) || !MC.thePlayer.onGround)
        {
            return;
        }

        final float offset = (float) (MC.thePlayer.boundingBox.minY
                - (MC.thePlayer.posY - MC.thePlayer.yOffset));
        if (offset < MIN_STEP_HEIGHT || offset > stepHeightSetting.getValue())
        {
            return;
        }

        double minY = MC.thePlayer.boundingBox.minY - 1.0;
        if (offset < 1.0f)
        {
            // what the hacky bullshit!!
            minY = (minY + 1) - offset;
        } else if (offset > 1.0f)
        {
            // well well well...
            minY = minY - (offset - 1.0);
        }
        final double stance = minY + (double) MC.thePlayer.yOffset - MC.thePlayer.ySize;

        int packets = STEP_PACKET_VALUES.length;
        if (offset <= 1.0f)
        {
            packets = 3;
        }

        timer = timerSetting.getValue();
        ((IMinecraft)MC).nebula$getTimer().timerSpeed = timer ? 1.0f / (packets + 1) : 1.0f;
        for (int i = 0; i < packets; ++i)
        {
            double packetHeight = STEP_PACKET_VALUES[i];
            if (offset < 1.0f)
            {
                packetHeight *= offset;
            }
            PacketUtil.send(new C03PacketPlayer.C04PacketPlayerPosition(
                    MC.thePlayer.posX,
                    minY + packetHeight,
                    stance + packetHeight,
                    MC.thePlayer.posZ,
                    false));
        }
    };
}