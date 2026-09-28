package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.impl.module.combat.AutoBedModule;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IBlock;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.MathUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author xgraza
 * @since 06/24/25
 */
@ModuleManifest(name = "Nuker",
        description = "Automatically breaks blocks around you to clear an area",
        category = ModuleCategory.WORLD)
@RotationPriority(ModuleRotationPriorities.NUKER)
public final class NukerModule extends InteractionModule
{
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.5)
            .setDescription("The range to break at")
            .build();
    private final NumberSetting<Integer> blocksSetting = numberBuilder("Blocks", 10)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many blocks to break in a tick")
            .build();
    private final Setting<Boolean> underSetting = builder("Break Under", false)
            .setDescription("If to break underneath you")
            .build();
    private final Setting<Boolean> keepYSetting = builder("Keep Y", true)
            .setDescription("If to keep your y position for what blocks to break")
            .build();

    private final List<BlockPos> breakList = new ArrayList<>();
    private BlockInfo info;
    private double posY = -1;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (info != null && MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
        info = null;
        breakList.clear();
        posY = -1;
        Nebula.INTERACTIONS.restore();
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        MC.mcProfiler.startSection("nuker");
        for (final BlockPos pos : breakList)
        {
            if (info != null && info.getPos().equals(pos))
            {
                continue;
            }
            final AxisAlignedBB bb = IAxisAlignedBB.create(pos);
            Render3D.filledAABB(bb, QuadMask.ALL_FACES, HUDModule.INSTANCE.primaryColorSetting.getValueInt(80));
            Render3D.outlinedAABB(bb, 1.5f, QuadMask.ALL_FACES, HUDModule.INSTANCE.primaryColorSetting.getValueInt());
        }
        if (info != null)
        {
            final AxisAlignedBB bb = IAxisAlignedBB.create(info.getPos());
            final int faceMask = QuadMask.mask(info.getFacing());
            Render3D.filledAABB(bb, faceMask, 0x80FF0000);
            Render3D.outlinedAABB(bb, 1.5f, faceMask, 0xFFFF0000);
        }
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (AutoBedModule.INSTANCE.isActive() || KillAuraModule.INSTANCE.isAttacking())
        {
            if (info != null)
            {
                Nebula.INVENTORY.sync();
            }
            info = null;
            return;
        }

        if (info != null)
        {
            if (MathUtil.getDistanceFromPlayer(info.getPos(), true) > rangeSetting.getValue())
            {
                Nebula.INVENTORY.sync();
                info = null;
            }

            Nebula.INTERACTIONS.override();
            if (info != null && !breakBlock(info, true))
            {
                return;
            }
            Nebula.INTERACTIONS.restore();
            info = null;
        }

        calculateBreakPositions();
        if (breakList.isEmpty())
        {
            Nebula.INTERACTIONS.restore();
            Nebula.INVENTORY.sync();
            info = null;
            return;
        }

        info = breakMultiPos(blocksSetting.getValue(), true, breakList);
    };

    private void calculateBreakPositions()
    {
        breakList.clear();

        if (!keepYSetting.getValue() || posY == -1)
        {
            posY = Math.round(MC.thePlayer.boundingBox.minY);
        }
        
        final BlockPos origin = PlayerUtil.getOrigin(posY);
        for (final BlockPos offset : BlockUtil.RADIAL_BLOCK_MAP.get(rangeSetting.getValue().intValue()))
        {
            if (!underSetting.getValue() && offset.getY() < 0)
            {
                continue;
            }
            final BlockPos pos = origin.add(offset);
            if (MathUtil.getDistanceFromPlayer(pos, true) > rangeSetting.getValue())
            {
                continue;
            }

            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
            final float blockHardness = ((IBlock)block).nebula$getBlockHardness();
            if (blockHardness == -1.0f || blockHardness == 100.0f || block.getMaterial() == Material.air)
            {
                continue;
            }

            breakList.add(pos);
        }

        if (!breakList.isEmpty())
        {
            breakList.sort(Comparator.comparingDouble(MathUtil::getDistanceFromPlayer));
        }
    }
}
