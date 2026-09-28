package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.player.EventMoveUpdate;
import ez.nebula.client.api.listener.event.world.EventModifyBoundBox;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.impl.module.exploit.NoHungerModule;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author xgraza
 * @since 03/01/25
 */
@ModuleManifest(name = "Jesus",
        description = "Allows you to walk on top of water and lava, as Jesus did",
        category = ModuleCategory.MOVEMENT)
public final class JesusModule extends Module
{
    @ModuleInstance
    public static JesusModule INSTANCE;

    private static final AxisAlignedBB LIQUID_FULL_AABB = AxisAlignedBB.getBoundingBox(
            0, 0, 0, 1, 0.99, 1);

    private int waterTicks;
    private boolean attemptExit, didExit;

    @Override
    public void onDisable()
    {
        super.onDisable();
        attemptExit = false;
        didExit = false;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (MC.thePlayer.fallDistance > 3.0f)
        {
            return;
        }

        if (MC.thePlayer.isInWater())
        {
            MC.thePlayer.motionY = 0.11;
            attemptExit = true;
        } else
        {
            if (attemptExit)
            {
                MC.thePlayer.motionY = 0.3;
                didExit = true;
                attemptExit = false;
            }
        }
    };

    @Subscribe
    private final EventListener<EventMoveUpdate> moveUpdateEventListener = event ->
    {
        if (!PlayerUtil.isAboveWater() || MC.thePlayer.isInWater())
        {
            didExit = false;
            waterTicks = 0;
            return;
        }
        ++waterTicks;

        if (MC.thePlayer.ticksExisted % 2 == 0
                && waterTicks > (didExit ? 5 : 2)
                && !MC.gameSettings.keyBindJump.getIsKeyPressed())
        {
            event.setY(event.getY() + 0.01);
            event.setStance(event.getStance() + 0.01);
            event.setOnGround(NoHungerModule.INSTANCE.isToggled() && NoHungerModule.INSTANCE.groundSetting.getValue());
        }
    };

    @Subscribe
    private final EventListener<EventModifyBoundBox> modifyBoundBoxEventListener = event ->
    {
        if (MC.thePlayer == null
                || MC.thePlayer.fallDistance > 3.0f
                || MC.thePlayer.isInWater()
                || !PlayerUtil.isAboveWater()
                || attemptExit
                || event.getY() > MC.thePlayer.boundingBox.minY)
        {
            return;
        }
        final Block block = event.getWorld().getBlock(event.getX(), event.getY(), event.getZ());
        if (block instanceof BlockLiquid && MC.thePlayer.equals(event.getEntity()))
        {
            event.setAabb(LIQUID_FULL_AABB.copy().offset(event.getX(), event.getY(), event.getZ()));
        }
    };
}
