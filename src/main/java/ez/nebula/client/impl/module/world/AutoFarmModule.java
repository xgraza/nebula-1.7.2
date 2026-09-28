package ez.nebula.client.impl.module.world;

import com.google.common.collect.Lists;
import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import net.minecraft.block.*;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xgraza
 * @since 06/24/25
 */
@ModuleManifest(name = "AutoFarm",
        description = "Automatically harvests and replants crops (+ nether wart & sugar cane)",
        category = ModuleCategory.WORLD)
public final class AutoFarmModule extends Module
{
    private static final Map<Block, Item> CROP_BLOCK_TO_SEED = new HashMap<>();
    private static final Map<Class<? extends Block>, List<Block>> BASE_TO_CROP_BLOCK = new HashMap<>();

    private static final int FULL_GROWN_CROP_META = 7;
    private static final int FULL_GROWN_NETHERWART_META = 3;

    static
    {
        CROP_BLOCK_TO_SEED.put(Blocks.wheat, Items.wheat_seeds);
        CROP_BLOCK_TO_SEED.put(Blocks.melon_stem, Items.melon_seeds);
        CROP_BLOCK_TO_SEED.put(Blocks.pumpkin_stem, Items.pumpkin_seeds);
        CROP_BLOCK_TO_SEED.put(Blocks.potatoes, Items.potato);
        CROP_BLOCK_TO_SEED.put(Blocks.carrots, Items.carrot);
        BASE_TO_CROP_BLOCK.computeIfAbsent(BlockFarmland.class, (x) -> new ArrayList<>())
                .addAll(CROP_BLOCK_TO_SEED.keySet());

        CROP_BLOCK_TO_SEED.put(Blocks.reeds, Items.reeds);
        BASE_TO_CROP_BLOCK.put(BlockDirt.class, Lists.newArrayList(Blocks.reeds));
        BASE_TO_CROP_BLOCK.put(BlockGrass.class, Lists.newArrayList(Blocks.reeds));
        BASE_TO_CROP_BLOCK.put(BlockSand.class, Lists.newArrayList(Blocks.reeds));

        CROP_BLOCK_TO_SEED.put(Blocks.nether_wart, Items.nether_wart);
        BASE_TO_CROP_BLOCK.put(BlockSoulSand.class, Lists.newArrayList(Blocks.nether_wart));
    }
    
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.5)
            .setDescription("How far our to interact with crops")
            .build();
    
    private final Setting<Boolean> noPosionousSetting = builder("Throw Out Poisonous", true)
            .setDescription("If to automatically throw out poisonous potatoes when harvesting")
            .build();
    private final Setting<Boolean> autoHarvestSetting = builder("Auto Harvest", true)
            .setDescription("If to automatically harvest crops")
            .build();
    //    private final Setting<Boolean> pathToSetting = builder(
//            "Path To", false).setVisibility(autoHarvestSetting::getValue);
    private final Setting<Boolean> sugarCaneSetting = builder("Sugar Canes", true)
            .setDescription("If to harvest sugar cane")
            .build();
    private final NumberSetting<Integer> sugarCaneLengthSetting = numberBuilder("Sugar Cane Length", 1)
            .setMin(1)
            .setMax(3)
            .setScale(1)
            .setDescription("How tall a sugar cane should be before harvesting it")
            .setVisibility((value) -> sugarCaneSetting.getValue() && autoHarvestSetting.getValue())
            .build();
    private final Setting<Boolean> netherwartsSetting = builder("Nether Warts", true)
            .setDescription("If to harvest nether warts")
            .build();
    private final Setting<Boolean> packetScanSetting = builder("Packet Scan", false)
            .setDescription("If to scan packets for dried up or new crops")
            .build();

    private final Map<BlockPos, Block> plantTypeAtBlockMap = new ConcurrentHashMap<>();

    private int oldSlot = InventoryUtil.INVALID_SLOT;
    private BlockPos melonBreakPos;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null)
        {
            if (oldSlot != InventoryUtil.INVALID_SLOT)
            {
                MC.thePlayer.inventory.currentItem = oldSlot;
            }

        }

        oldSlot = InventoryUtil.INVALID_SLOT;
        melonBreakPos = null;
        plantTypeAtBlockMap.clear();
        Nebula.INTERACTIONS.restore();
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (melonBreakPos != null)
        {
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(melonBreakPos);
            if ((block == Blocks.melon_block || block == Blocks.pumpkin)
                    && !Nebula.INTERACTIONS.breakBlock(melonBreakPos, EnumFacing.DOWN))
            {
                return;
            }
            if (oldSlot != InventoryUtil.INVALID_SLOT)
            {
                MC.thePlayer.inventory.currentItem = oldSlot;
            }
            oldSlot = InventoryUtil.INVALID_SLOT;
            melonBreakPos = null;
        }

        cacheSurroundingFarmland();

        if (noPosionousSetting.getValue())
        {
            throwOutPoisonous();
        }

        if (plantTypeAtBlockMap.isEmpty())
        {
            Nebula.INTERACTIONS.restore();
            return;
        }

        for (final BlockPos pos : plantTypeAtBlockMap.keySet())
        {
            // keep in cache, but too far away to harvest / check...
            if (MC.thePlayer.getDistance(pos.getX(), pos.getY(), pos.getZ()) > rangeSetting.getValue())
            {
                continue;
            }

            final Block cropBlockType = plantTypeAtBlockMap.get(pos);
            if (cropBlockType == null
                    || isNotAllowedToPlaceCrop(pos, cropBlockType)
                    || !allowCrop(cropBlockType))
            {
                plantTypeAtBlockMap.remove(pos);
                continue;
            }

            final BlockPos cropBlockPos = pos.up();
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(cropBlockPos);

            if (block == Blocks.air)
            {
                if (plantSeed(pos, cropBlockType))
                {
                    return;
                }
                continue;
            }

            if (block == Blocks.reeds)
            {
                if (!isSugarCaneGrown(cropBlockPos))
                {
                    continue;
                }
            } else
            {
                final int meta = MC.theWorld.getBlockMetadata(cropBlockPos.getX(),
                        cropBlockPos.getY(), cropBlockPos.getZ());
                final int grownMeta = cropBlockType == Blocks.nether_wart
                        ? FULL_GROWN_NETHERWART_META
                        : FULL_GROWN_CROP_META;
                if (meta < grownMeta)
                {
                    continue;
                }
            }

            if (!autoHarvestSetting.getValue())
            {
                return;
            }

            if (cropBlockType == Blocks.melon_stem || cropBlockType == Blocks.pumpkin_stem)
            {
                final BlockPos melonPos = findGrownMelonCrop(cropBlockPos);
                if (melonPos == null)
                {
                    continue;
                }
                // melon & pumpkins both can be harvested with the same type of tool, just search for the best
                final int slot = InventoryUtil.getBestToolSlotFor(Blocks.pumpkin);
                if (slot != InventoryUtil.INVALID_SLOT)
                {
                    oldSlot = MC.thePlayer.inventory.currentItem;
                    MC.thePlayer.inventory.currentItem = slot;
                }
                // if we can instant remove it, dont bother bringing to the next tick...
                if (Nebula.INTERACTIONS.breakBlock(melonPos, EnumFacing.DOWN))
                {
                    MC.thePlayer.inventory.currentItem = oldSlot;
                    oldSlot = InventoryUtil.INVALID_SLOT;
                    return;
                }
                melonBreakPos = melonPos;
                return;
            }

            if (Nebula.INTERACTIONS.breakBlock(cropBlockPos, EnumFacing.UP))
            {
                plantSeed(pos, cropBlockType);
                return;
            }
        }

        Nebula.INTERACTIONS.restore();
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S23PacketBlockChange && packetScanSetting.getValue())
        {
            final S23PacketBlockChange packet = event.getPacket();
            final Block packetBlock = packet.func_148880_c();
            BlockPos pos = new BlockPos(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f());
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);

            if (block instanceof BlockFarmland && !(packetBlock instanceof BlockFarmland))
            {
                // ChatUtil.send("Farmland dried up :(");
                plantTypeAtBlockMap.remove(pos);
                return;
            }

            pos = pos.down();
            if (CROP_BLOCK_TO_SEED.containsKey(packetBlock)
                    && !plantTypeAtBlockMap.containsKey(pos)
                    && !(packetBlock instanceof BlockReed))
            {
                // ChatUtil.send("Caching new crop @ %s", pos);
                plantTypeAtBlockMap.put(pos, packet.func_148880_c());
            }
        }
    };

    private boolean isNotAllowedToPlaceCrop(final BlockPos pos, final Block cropBlock)
    {
        final boolean invalidBase = !BASE_TO_CROP_BLOCK.getOrDefault(
                ((IWorld)MC.theWorld).nebula$getBlock(pos).getClass(),
                Collections.emptyList()).contains(cropBlock);
        if (invalidBase)
        {
            return true;
        }
        if (cropBlock != Blocks.reeds)
        {
            return invalidBase;
        }
        for (final EnumFacing facing : EnumFacing.values())
        {
            if (facing == EnumFacing.UP)
            {
                continue;
            }
            final BlockPos neighbor = pos.offset(facing);
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(neighbor);
            if (block == Blocks.water || block == Blocks.flowing_water)
            {
                return false;
            }
        }
        return true;
    }

    private boolean isSugarCaneGrown(final BlockPos origin)
    {
        int height = 0;
        BlockPos pos = origin;
        while (((IWorld)MC.theWorld).nebula$getBlock(pos) == Blocks.reeds)
        {
            ++height;
            pos = pos.up();
        }
        return height >= sugarCaneLengthSetting.getValue();
    }

    private BlockPos findGrownMelonCrop(final BlockPos pos)
    {
        for (final EnumFacing facing : EnumFacing.values())
        {
            final BlockPos neighbor = pos.offset(facing);
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(neighbor);
            if (block == Blocks.melon_block || block == Blocks.pumpkin)
            {
                return neighbor;
            }
        }
        return null;
    }

    private void throwOutPoisonous()
    {
        final int slot = InventoryUtil.getSlot(0, 36,
                (stack) -> stack.getItem() == Items.poisonous_potato);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }
        InventoryUtil.windowClick(InventoryUtil.toPacketSlot(slot), InventoryUtil.ClickType.DROP_ALL);
    }

    private boolean plantSeed(final BlockPos pos, final Block type)
    {
        final int slot = InventoryUtil.getHotbarSlot(
                (stack) -> stack.getItem() == CROP_BLOCK_TO_SEED.get(type));
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return false;
        }
        Nebula.INVENTORY.spoof(slot);
        final boolean result = Nebula.INTERACTIONS.rightClickBlock(pos, EnumFacing.UP);
        Nebula.INVENTORY.sync();
        return result;
    }

    private void cacheSurroundingFarmland()
    {
        final BlockPos origin = PlayerUtil.getOrigin();
        final int r = rangeSetting.getValue().intValue();
        for (int y = -1; y < 2; ++y)
        {
            for (int x = -r; x <= r; ++x)
            {
                for (int z = -r; z <= r; ++z)
                {
                    final BlockPos pos = origin.add(x, y, z);
                    if (!plantTypeAtBlockMap.containsKey(pos))
                    {
                        final Block cropBlock = ((IWorld)MC.theWorld).nebula$getBlock(pos.up());
                        if (!allowCrop(cropBlock) || isNotAllowedToPlaceCrop(pos, cropBlock))
                        {
                            continue;
                        }

                        // ChatUtil.send("Cached %s at %s", cropBlock, pos);
                        plantTypeAtBlockMap.put(pos, cropBlock);
                    }
                }
            }
        }
    }

    private boolean allowCrop(final Block cropBlock)
    {
        if (cropBlock == Blocks.reeds && !sugarCaneSetting.getValue())
        {
            return false;
        }
        return cropBlock != Blocks.nether_wart || netherwartsSetting.getValue();
    }
}
