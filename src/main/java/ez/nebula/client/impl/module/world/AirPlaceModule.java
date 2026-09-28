package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.DebugFeature;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * @author xgraza
 * @since 6/7/26
 */
@DebugFeature
@ModuleManifest(name = "AirPlace",
        description = "Allows you to place blocks mid-air",
        category = ModuleCategory.WORLD)
public final class AirPlaceModule extends Module
{
    private final NumberSetting<Double> delaySetting = numberBuilder("Delay", 0.1)
            .setMin(0.0)
            .setMax(5.0)
            .setScale(0.1)
            .setDescription("The delay in seconds between air placements")
            .build();
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(7.0)
            .setScale(0.5)
            .setDescription("How far to raytrace in air for")
            .build();

    private final Timer timer = new Timer();
    private MovingObjectPosition result;

    @Override
    public void onDisable()
    {
        super.onDisable();
        result = null;
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (result == null || result.sideHit == -1)
        {
            return;
        }
        MC.mcProfiler.startSection("airPlace");
        final BlockPos pos = new BlockPos(result.blockX, result.blockY, result.blockZ);
        final EnumFacing facing = EnumFacing.values()[result.sideHit];
        final int color = HUDModule.INSTANCE.primaryColorSetting.getValueInt(120);
        final AxisAlignedBB bb = IAxisAlignedBB.create(pos);
        Render3D.outlinedAABB(bb, 1.5f, QuadMask.mask(facing), color);
        Render3D.filledAABB(bb, QuadMask.mask(facing), color);
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        final ItemStack itemStack = MC.thePlayer.getHeldItem();
        if (MC.objectMouseOver == null || MC.objectMouseOver.sideHit == -1 || itemStack == null || !(itemStack.getItem() instanceof ItemBlock))
        {
            result = null;
            return;
        }

        final Vec3 start = MC.thePlayer.getPosition(1.0f);
        final Vec3 var5 = MC.thePlayer.getLook(1.0f);
        final Vec3 end = start.addVector(var5.xCoord * rangeSetting.getValue(),
                var5.yCoord * rangeSetting.getValue(),
                var5.zCoord * rangeSetting.getValue());
        result = MC.theWorld.func_147447_a(start, end, true, false, true);
        if (result == null)
        {
            return;
        }

        if (MC.gameSettings.keyBindUseItem.getIsKeyPressed()
                && result.sideHit != -1
                && timer.hasElapsed((long) (delaySetting.getValue() * 1000.0), true))
        {
            // TODO: NoCheatPlus bypass?
            Nebula.INTERACTIONS.rightClickBlock(result);
        }
    };
}
