package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xgraza
 * @since 04/02/25
 */
@ModuleManifest(name = "AntiGhostBlock",
        description = "Prevents ghost blocks from spawning from placing or breaking",
        category = ModuleCategory.WORLD)
public final class AntiGhostBlockModule extends Module
{
    private static final int FALLING_BLOCK_ID = 70;

    private final Setting<Boolean> placeSetting = builder("Place", true)
            .setDescription("If to handle checking for ghost blocks on place")
            .build();
    private final Setting<Boolean> breakSetting = builder("Break", true)
            .setDescription("If to handle checking for ghost blocks on break")
            .build();
    private final Setting<Boolean> packetSetting = builder("Packet", true)
            .setDescription("If to send a packet when placing/breaking a block to server confirm")
            .build();
    private final NumberSetting<Double> confirmTimeSetting = numberBuilder("Confirm Time", 0.5)
            .setMin(0.1)
            .setMax(5.0)
            .setScale(0.1)
            .setDescription("How much time in seconds to wait before flagging a block as a ghost block")
            .build();
    private final Setting<Boolean> debugRenderSetting = builder("Debug Render", false)
            .setDescription("If to render all queued blocks")
            .build();

    private final Map<BlockPos, Long> placeConfirmBlockPosMap = new ConcurrentHashMap<>();
    private final Map<BlockPos, OriginalBlockData> breakConfirmBlockPosMap = new ConcurrentHashMap<>();

    @Override
    public void onDisable()
    {
        super.onDisable();
        placeConfirmBlockPosMap.clear();
        breakConfirmBlockPosMap.clear();
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (placeConfirmBlockPosMap.isEmpty() || !debugRenderSetting.getValue())
        {
            return;
        }
        MC.mcProfiler.startSection("antiGhostBlock");
        for (final BlockPos pos : placeConfirmBlockPosMap.keySet())
        {
            final AxisAlignedBB aabb = IAxisAlignedBB.create(pos);
            Render3D.filledAABB(aabb, QuadMask.ALL_FACES, 0xAB00FF00);
            Render3D.outlinedAABB(aabb, 1.5f, QuadMask.ALL_FACES, 0xAB00FF00);
        }
        for (final BlockPos pos : breakConfirmBlockPosMap.keySet())
        {
            final AxisAlignedBB aabb = IAxisAlignedBB.create(pos);
            Render3D.filledAABB(aabb, QuadMask.ALL_FACES, 0xAB00FF00);
            Render3D.outlinedAABB(aabb, 1.5f, QuadMask.ALL_FACES, 0xAB00FF00);
        }
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (placeSetting.getValue() && !placeConfirmBlockPosMap.isEmpty())
        {
            for (final BlockPos pos : placeConfirmBlockPosMap.keySet())
            {
                // fix random NPE thrown (inconsistency between Netty/Main thread?)
                final Long confirmTime = placeConfirmBlockPosMap.get(pos);
                if (confirmTime == null)
                {
                    continue;
                }

                final long timeMS = confirmTime + (long) (confirmTimeSetting.getValue() * 1000.0);
                if (System.currentTimeMillis() > timeMS)
                {
                    placeConfirmBlockPosMap.remove(pos);
                    MC.theWorld.setBlockToAir(pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }

        if (breakSetting.getValue() && !breakConfirmBlockPosMap.isEmpty())
        {
            for (final BlockPos pos : breakConfirmBlockPosMap.keySet())
            {
                final OriginalBlockData data = breakConfirmBlockPosMap.get(pos);
                if (data == null)
                {
                    continue;
                }
                final long timeMS = data.getTime() + (long) (confirmTimeSetting.getValue() * 1000.0);
                if (System.currentTimeMillis() > timeMS)
                {
                    breakConfirmBlockPosMap.remove(pos);
                    MC.theWorld.setBlock(pos.getX(), pos.getY(), pos.getZ(), data.getBlock());
                    MC.theWorld.setBlockMetadataWithNotify(pos.getX(), pos.getY(), pos.getZ(), data.getMeta(), 0);
                }
            }
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S22PacketMultiBlockChange)
        {
            // TODO
        } else if (event.getPacket() instanceof S23PacketBlockChange)
        {
            final S23PacketBlockChange packet = event.getPacket();
            final BlockPos pos = new BlockPos(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f());

            if (placeSetting.getValue())
            {
                // TODO: check if the block matches server-side?
                placeConfirmBlockPosMap.remove(pos);
            }

            if (breakSetting.getValue() && breakConfirmBlockPosMap.containsKey(pos))
            {
                final Block block = packet.func_148880_c();
                if (block == null || block instanceof BlockAir)
                {
                    breakConfirmBlockPosMap.remove(pos);
                }
            }
        } else if (event.getPacket() instanceof S0EPacketSpawnObject)
        {
            final S0EPacketSpawnObject packet = event.getPacket();
            if (packet.func_148993_l() != FALLING_BLOCK_ID)
            {
                return;
            }
            final BlockPos pos = new BlockPos((int) (packet.func_148997_d() / 32.0), (int) (packet.func_148998_e() / 32.0), (int) (packet.func_148994_f() / 32.0));
            final OriginalBlockData blockData = breakConfirmBlockPosMap.get(pos);
            if (blockData == null)
            {
                return;
            }
            final int blockId = packet.func_149009_m() & 65535;
            if (blockId == Block.getIdFromBlock(blockData.getBlock()))
            {
                breakConfirmBlockPosMap.remove(pos);
            }
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement && placeSetting.getValue())
        {
            final C08PacketPlayerBlockPlacement packet = event.getPacket();
            if (!isBlockPlacePacket(packet))
            {
                return;
            }

            final BlockPos pos = new BlockPos(packet.func_149576_c(), packet.func_149571_d(), packet.func_149570_e());
            final EnumFacing side = EnumFacing.values()[packet.func_149568_f()];

            if (packetSetting.getValue())
            {
                PacketUtil.send(new C07PacketPlayerDigging(1, pos.getX(), pos.getY(), pos.getZ(), side.ordinal()));
            }
            placeConfirmBlockPosMap.put(pos.offset(side), System.currentTimeMillis());
        }

        if (event.getPacket() instanceof C07PacketPlayerDigging && breakSetting.getValue())
        {
            final C07PacketPlayerDigging packet = event.getPacket();
            if (packet.func_149506_g() != 2) // STOP_BREAKING / FINISH
            {
                return;
            }
            final BlockPos pos = new BlockPos(packet.func_149505_c(), packet.func_149503_d(), packet.func_149502_e());
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
            if (block == null || block instanceof BlockAir)
            {
                return;
            }
            // TODO: retain metadata?
            breakConfirmBlockPosMap.put(pos, new OriginalBlockData(block, System.currentTimeMillis(), MC.theWorld.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ())));
        }
    };

    private boolean isBlockPlacePacket(final C08PacketPlayerBlockPlacement packet)
    {
        // ignore interact block packets
        if (packet.func_149576_c() == -1
                && packet.func_149571_d() == -1
                && packet.func_149570_e() == -1
                && packet.func_149568_f() == 255)
        {
            return false;
        }

        ItemStack itemStack = packet.func_149574_g();
        // resort to the item stack in the server hand
        if (itemStack == null)
        {
            itemStack = Nebula.INVENTORY.stack();
        }

        return itemStack != null && itemStack.getItem() instanceof ItemBlock;
    }

    private static final class OriginalBlockData
    {
        private final Block block;
        private final long time;
        private final int meta;

        public OriginalBlockData(Block block, long time, int meta)
        {
            this.block = block;
            this.time = time;
            this.meta = meta;
        }

        public Block getBlock()
        {
            return block;
        }

        public long getTime()
        {
            return time;
        }

        public int getMeta()
        {
            return meta;
        }
    }
}
