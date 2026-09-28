package ez.nebula.client.impl.module.render;

import ez.nebula.client.api.DebugFeature;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.listener.event.world.EventChangeWorld;
import ez.nebula.client.api.listener.event.world.EventUnloadChunk;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.world.BlockSearcher;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author xgraza
 * @since 6/4/26
 */
@DebugFeature
@ModuleManifest(name = "Finder",
        description = "Highlights selected blocks",
        category = ModuleCategory.RENDER)
public final class FinderModule extends Module
{
    @ModuleInstance
    public static FinderModule INSTANCE;
    private static final List<Block> BLOCK_LIST = new ArrayList<>();

//    static
//    {
//        BLOCK_LIST.add(Blocks.crafting_table);
//        BLOCK_LIST.add(Blocks.chest);
//        BLOCK_LIST.add(Blocks.trapped_chest);
//        BLOCK_LIST.add(Blocks.ender_chest);
//        BLOCK_LIST.add(Blocks.bed);
//    }

    private final BlockSearcher searcher = new BlockSearcher("Finder", this::onBlockSearched);
    private final Set<BlockPos> posSet = new ConcurrentSet<>();

    private final NumberSetting<Integer> searchRangeSetting = numberBuilder("Search Range", 5)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many chunks to search for blocks")
            .onValueChanged(searcher::setSearchRange)
            .build();

    @Override
    public void onEnable()
    {
        super.onEnable();
        searcher.setSearchRange(searchRangeSetting.getValue());
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        searcher.setSearching(false);
        posSet.clear();
    }

    @Subscribe(priority = IEventPriorities.HIGHEST)
    private final EventListener<EventRender3D> updateEventListener = event ->
    {
        if (MC.thePlayer.ticksExisted > 5)
        {
            searcher.setSearching(true);
        }
        if (posSet.isEmpty() || !searcher.isSearching())
        {
            return;
        }
        MC.mcProfiler.startSection("finder");
        for (final BlockPos pos : posSet)
        {
            final AxisAlignedBB bb = IAxisAlignedBB.create(pos);
            Render3D.filledAABB(bb, QuadMask.ALL_FACES, HUDModule.INSTANCE.primaryColorSetting.getValueInt(80));
            Render3D.outlinedAABB(bb, 1.5f, QuadMask.ALL_FACES, HUDModule.INSTANCE.primaryColorSetting.getValueInt());
        }
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S23PacketBlockChange)
        {
            final S23PacketBlockChange packet = event.getPacket();
            final BlockPos pos = new BlockPos(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f());
            if (posSet.contains(pos))
            {
                final Block block = packet.func_148880_c();
                if (BLOCK_LIST.contains(block))
                {
                    posSet.add(pos);
                } else
                {
                    posSet.remove(pos);
                }
            }
        }
    };

    @Subscribe
    private final EventListener<EventChangeWorld> changeWorldEventListener = event ->
    {
        searcher.setSearching(false);
        posSet.clear();
    };

    @Subscribe
    private final EventListener<EventUnloadChunk> unloadChunkEventListener = event ->
    {
        final Chunk chunk = event.getChunk();
        final int chunkX = chunk.xPosition * 16;
        final int chunkZ = chunk.zPosition * 16;
        final int maxChunkX = chunkX + 16;
        final int maxChunkZ = chunkZ + 16;
        posSet.removeIf((pos) -> pos.getX() <= maxChunkX && pos.getZ() <= maxChunkZ && pos.getX() >= chunkX && pos.getZ() >= chunkZ);
    };

    private void onBlockSearched(final BlockSearcher.SearchedBlock searchedBlock)
    {
        if (BLOCK_LIST.contains(searchedBlock.getBlock()))
        {
            posSet.add(new BlockPos(searchedBlock.getX(), searchedBlock.getY(), searchedBlock.getZ()));
        }
    }

    public static void addBlock(final Block block)
    {
        if (BLOCK_LIST.contains(block))
        {
            return;
        }
        BLOCK_LIST.add(block);
        INSTANCE.posSet.clear();
    }

    public static void removeBlock(final Block block)
    {
        BLOCK_LIST.remove(block);
        INSTANCE.posSet.clear();
    }
}
