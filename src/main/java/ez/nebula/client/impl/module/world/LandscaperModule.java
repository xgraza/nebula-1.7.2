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
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.math.MathUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.block.*;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author xgraza
 * @since 05/03/25
 */
@ModuleManifest(name = "Landscaper",
        description = "Automatically breaks all foliage (i.e. grass, flowers, snow) blocks at your Y level",
        category = ModuleCategory.WORLD)
@RotationPriority(ModuleRotationPriorities.LANDSCAPER)
public final class LandscaperModule extends InteractionModule
{
    private final NumberSetting<Float> rangeSetting = numberBuilder("Range", 4.2f)
            .setMin(1.0f)
            .setMax(6.0f)
            .setScale(0.1f)
            .setDescription("The range to break foliage blocks in")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate to the block to break")
            .build();
    private final NumberSetting<Integer> blocksPerTickSetting = numberBuilder("Blocks per Tick", 2)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many blocks to break per tick")
            .setVisibility((value) -> !rotateSetting.getValue())
            .build();
    private final Setting<Boolean> snowSetting = builder("Shovel Snow", true)
            .setDescription("If to clear snow")
            .build();
    private final Setting<Boolean> saplingsSetting = builder("Saplings", false)
            .setDescription("If to destroy saplings")
            .build();

    private final List<BlockPos> breakList = new ArrayList<>();
    private BlockPos breakingBlockPos;
    private float[] angles;

    @Override
    public void onDisable()
    {
        super.onDisable();
        breakList.clear();
        breakingBlockPos = null;
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
        Nebula.INTERACTIONS.restore();
        angles = null;
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (breakingBlockPos == null)
        {
            return;
        }
        MC.mcProfiler.startSection("landscaper");
        if (rotateSetting.getValue())
        {
            angles = AngleUtil.anglesToBlock(breakingBlockPos, EnumFacing.UP, event.getPartialTicks());
        }
        Render3D.filledAABB(IAxisAlignedBB.create(breakingBlockPos), QuadMask.ALL_FACES, 0x8000FF00);
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        populateBreakQueue();
        if (breakList.isEmpty())
        {
            return;
        }

        if (rotateSetting.getValue())
        {
            if (breakingBlockPos == null && (breakingBlockPos = breakList.get(0)) == null)
            {
                Nebula.INVENTORY.sync();
                return;
            }
            if (!rotate(angles))
            {
                return;
            }
            if (breakBlock(breakingBlockPos, EnumFacing.UP, true))
            {
                breakingBlockPos = null;
            }
        } else
        {
            breakMultiPos(blocksPerTickSetting.getValue(), true, breakList);
            Nebula.INVENTORY.sync();
        }
    };

    private boolean isBlockValid(final BlockPos blockPos)
    {
        final Block block = ((IWorld)MC.theWorld).nebula$getBlock(blockPos);
        if (!snowSetting.getValue() && (block instanceof BlockSnow || block instanceof BlockSnowBlock))
        {
            return false;
        }
        if (!saplingsSetting.getValue() && block instanceof BlockSapling)
        {
            return false;
        }
        return block instanceof BlockFlower
                || block instanceof BlockDoublePlant
                || block instanceof BlockTallGrass
                || block instanceof BlockMushroom
                || block instanceof BlockDeadBush
                || block instanceof BlockSnow
                || block instanceof BlockSnowBlock
                || block instanceof BlockSapling;
    }

    private void populateBreakQueue()
    {
        breakList.clear();
        final BlockPos origin = PlayerUtil.getOrigin();
        for (final BlockPos offset : BlockUtil.RADIAL_BLOCK_MAP.get(rangeSetting.getValue().intValue()))
        {
            final BlockPos pos = origin.add(offset);
            if (isBlockValid(pos) && !breakList.contains(pos))
            {
                breakList.add(pos);
            }
        }
        breakList.sort(Comparator.comparingDouble(MathUtil::getDistanceFromPlayer));
    }
}
