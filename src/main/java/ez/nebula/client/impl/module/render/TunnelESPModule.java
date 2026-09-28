package ez.nebula.client.impl.module.render;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.api.listener.event.world.EventUnloadChunk;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.world.BlockSearcher;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author xgraza
 * @since 6/13/26
 */
@ModuleManifest(name = "TunnelESP",
        description = "Renders possible tunnels dug in the terrain",
        category = ModuleCategory.RENDER)
public final class TunnelESPModule extends Module
{
    private final BlockSearcher searcher = new BlockSearcher("TunnelESP", this::onBlockSearched);
    private final Set<BlockPos> tunnelList = new ConcurrentSet<>();

    private final NumberSetting<Integer> searchRangeSetting = numberBuilder("Search Range", 5)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many chunks to search for tunnels")
            .onValueChanged(searcher::setSearchRange)
            .build();
    private final NumberSetting<Integer> minTunnelLengthSetting = numberBuilder("Min Length", 5)
            .setMin(1)
            .setMax(10)
            .setScale(1)
            .setDescription("The minimum tunnel length")
            .build();
    private final NumberSetting<Integer> maxTunnelLengthSetting = numberBuilder("Max Length", 64)
            .setMin(20)
            .setMax(256)
            .setScale(1)
            .setDescription("The max tunnel length")
            .build();
    private final NumberSetting<Integer> minSurroundSetting = numberBuilder("Surrounding Blocks", 4)
            .setMin(4)
            .setMax(6)
            .setScale(1)
            .setDescription("The minimum amount of blocks to surround a potential tunnel")
            .build();

    @Override
    public void onEnable()
    {
        super.onEnable();
        searcher.setSearchRange(searchRangeSetting.getValue());
        searcher.setSearching(true);
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        searcher.setSearching(false);
        tunnelList.clear();
    }

    @Subscribe(priority = IEventPriorities.HIGHEST)
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (MC.thePlayer.ticksExisted > 5)
        {
            searcher.setSearching(true);
        }
        if (tunnelList.isEmpty())
        {
            return;
        }
        MC.mcProfiler.startSection("tunnelESP");
        for (final BlockPos pos : tunnelList)
        {
            Render3D.filledAABB(IAxisAlignedBB.create(pos), QuadMask.ALL_FACES, HUDModule.INSTANCE.primaryColorSetting.getValueInt(60));
        }
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventChangeWorld> changeWorldEventListener = event ->
    {
        searcher.setSearching(false);
        tunnelList.clear();
    };

    @Subscribe
    private final EventListener<EventUnloadChunk> unloadChunkEventListener = event ->
    {
        // remove out of chunk tunnels
        final Chunk chunk = event.getChunk();
        final int chunkX = chunk.xPosition * 16;
        final int chunkZ = chunk.zPosition * 16;
        final int maxChunkX = chunkX + 16;
        final int maxChunkZ = chunkZ + 16;
        tunnelList.removeIf((pos) -> pos.getX() <= maxChunkX && pos.getZ() <= maxChunkZ && pos.getX() >= chunkX && pos.getZ() >= chunkZ);
    };

    private void onBlockSearched(final BlockSearcher.SearchedBlock searchedBlock)
    {
        final BlockPos lower = new BlockPos(searchedBlock.getX(), searchedBlock.getY(), searchedBlock.getZ());
        final BlockPos upper = lower.up();
        directionLoop: for (final EnumFacing facing : EnumFacing.values())
        {
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN)
            {
                continue;
            }

            final List<BlockPos> tunnelBlockList = new ArrayList<>();

            int tunnelLength = 0;
            while (maxTunnelLengthSetting.getValue() >= tunnelLength)
            {
                final BlockPos lowerOffset = lower.offset(facing, tunnelLength);
                final BlockPos upperOffset = upper.offset(facing, tunnelLength);

                // if the next facings are not walkable (or tunnel ends here)
                if (!isSurrounded(lowerOffset, upperOffset, facing))
                {
                    if (tunnelLength < minTunnelLengthSetting.getValue())
                    {
                        // look into the next direction
                        continue directionLoop;
                    }
                    break;
                }
                tunnelBlockList.add(lowerOffset);
                tunnelBlockList.add(upperOffset);
                tunnelLength++;
            }

            if (!tunnelBlockList.isEmpty())
            {
                tunnelList.addAll(tunnelBlockList);
            }
        }
    }

    private boolean isSurrounded(final BlockPos lower, final BlockPos upper, final EnumFacing direction)
    {
        final List<EnumFacing> searchDirections = new ArrayList<>();

        if (direction == EnumFacing.NORTH || direction == EnumFacing.SOUTH)
        {
            searchDirections.add(EnumFacing.EAST);
            searchDirections.add(EnumFacing.WEST);
        } else if (direction == EnumFacing.EAST || direction == EnumFacing.WEST)
        {
            searchDirections.add(EnumFacing.NORTH);
            searchDirections.add(EnumFacing.SOUTH);
        }

        if (searchDirections.isEmpty())
        {
            return false;
        }

        // if the blocks we're searching arent even air, we wont bother
        if (!isValidWalkthrough(lower) || !isValidWalkthrough(upper))
        {
            return false;
        }

        int blockedOff = 0;

        // if the block under the lowest block not air?
        if (!BlockUtil.isReplaceable(lower.down()))
        {
            ++blockedOff;
        }

        // if the block above the highest block not air?
        if (!BlockUtil.isReplaceable(upper.up()))
        {
            ++blockedOff;
        }

        for (final EnumFacing facing : searchDirections)
        {
            final BlockPos lowerOffset = lower.offset(facing);
            if (!BlockUtil.isReplaceable(lowerOffset))
            {
                ++blockedOff;
            }

            final BlockPos upperOffset = upper.offset(facing);
            if (!BlockUtil.isReplaceable(upperOffset))
            {
                ++blockedOff;
            }
        }
        return blockedOff >= minSurroundSetting.getValue();
    }

    private boolean isValidWalkthrough(final BlockPos pos)
    {
        final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
        return !block.getMaterial().isSolid() && !(block instanceof BlockSlab) && !(block instanceof BlockStairs);
    }
}
