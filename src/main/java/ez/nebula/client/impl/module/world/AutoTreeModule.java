package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.MathUtil;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockWood;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDye;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author xgraza
 * @since 6/2/26
 */
@ModuleManifest(name = "AutoTree",
        description = "Automatically plants and/or bonemeals saplings",
        category = ModuleCategory.WORLD)
public final class AutoTreeModule extends InteractionModule
{
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.5)
            .setDescription("How far to plant or bonemeal saplings")
            .build();

    private final Setting<Boolean> plantSetting = builder("Plant", true)
            .setDescription("If to automatically plant saplings")
            .build();
    private final NumberSetting<Double> plantDelaySetting = numberBuilder("Plant Delay", 1.0)
            .setMin(0.0)
            .setMax(10.0)
            .setScale(0.1)
            .setDescription("How much time in seconds before trying to plant another sapling")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final NumberSetting<Integer> spacingSetting = numberBuilder("Space", 3)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("The spacing in blocks between placing saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final Setting<Boolean> oakSaplingSetting = builder("Oak Saplings", true)
            .setDescription("If to plant oak saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final Setting<Boolean> darkOakSaplingSetting = builder("Dark Oak Saplings", true)
            .setDescription("If to plant dark oak saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final Setting<Boolean> birchSaplingSetting = builder("Birch Saplings", true)
            .setDescription("If to plant birch saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final Setting<Boolean> spruceSaplingSetting = builder("Spruce Saplings", true)
            .setDescription("If to plant spruce saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final Setting<Boolean> acaciaSaplingSetting = builder("Acacia Saplings", true)
            .setDescription("If to plant acacia saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();
    private final Setting<Boolean> jungleSaplingSetting = builder("Jungle Saplings", true)
            .setDescription("If to plant jungle saplings")
            .setVisibility((value) -> plantSetting.getValue())
            .build();

    private final Setting<Boolean> bonemealSetting = builder("Bonemeal", false)
            .setDescription("If to automatically bonemeal saplings")
            .build();
    private final NumberSetting<Double> bonemealDelaySetting = numberBuilder("Bonemeal Delay", 1.0)
            .setMin(0.0)
            .setMax(10.0)
            .setScale(0.1)
            .setDescription("How much time in seconds before trying to bonemeal another sapling")
            .setVisibility((value) -> bonemealSetting.getValue())
            .build();
    private final NumberSetting<Integer> packetsSetting = numberBuilder("Packets", 5)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many times to bonemeal a sapling in a tick")
            .setVisibility((value) -> bonemealSetting.getValue())
            .build();

    private final List<BlockPos> placedSaplingsList = new CopyOnWriteArrayList<>();
    private final Timer plantTimer = new Timer();
    private final Timer bonemealTimer = new Timer();

    @Override
    public void onDisable()
    {
        super.onDisable();
        placedSaplingsList.clear();
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (plantSetting.getValue())
        {
            handlePlanting();
        }
        placedSaplingsList.removeIf((pos) ->
                MC.thePlayer.getDistance(pos.getX(), pos.getY(), pos.getZ()) >= spacingSetting.getValue());
        if (bonemealSetting.getValue())
        {
            handleBonemeal();
        }
    };

    private void handlePlanting()
    {
        if (!plantTimer.hasElapsed((long) (plantDelaySetting.getValue() * 1000.0)))
        {
            return;
        }
        final BlockPos placePos = getSaplingPlacePos();
        if (placePos == null)
        {
            return;
        }
        final int slot = getSapplingSlot();
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }
        plantTimer.resetTime();
        if (place(placePos.down(), EnumFacing.UP, slot))
        {
            placedSaplingsList.add(placePos);
        }
    }

    private void handleBonemeal()
    {
        if (!bonemealTimer.hasElapsed((long) (bonemealDelaySetting.getValue() * 1000.0)))
        {
            return;
        }
        final BlockPos saplingPos = getNearestTree(
                rangeSetting.getValue().intValue(), false, PlayerUtil.getOrigin());
        if (saplingPos == null)
        {
            return;
        }
        final int slot = InventoryUtil.getHotbarSlot((stack) ->
                stack.getItem() instanceof ItemDye && stack.getItemDamage() == 15);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }
        bonemealTimer.resetTime();

        Nebula.INVENTORY.spoof(slot);
        for (int i = 0; i < packetsSetting.getValue(); ++i)
        {
            place(saplingPos, ((IEnumFacing)(Object)PlayerUtil.getFacing()).nebula$getOpposite());
        }
        Nebula.INVENTORY.sync();
    }

    private BlockPos getNearestTree(final int range, final boolean includeWood, final BlockPos origin)
    {
        final TreeMap<Double, BlockPos> blockDistances = new TreeMap<>(); // haha, get it?
        final List<BlockPos> surroundingList = BlockUtil.RADIAL_BLOCK_MAP.get(range);
        for (final BlockPos offset : surroundingList)
        {
            final BlockPos pos = origin.add(offset);
            if (isPosTree(pos, includeWood))
            {
                blockDistances.put(MathUtil.getDistanceSq(pos, origin), pos);
            }
        }
        for (final BlockPos pos : placedSaplingsList)
        {
            if (isPosTree(pos, includeWood) && !blockDistances.containsValue(pos))
            {
                blockDistances.put(MathUtil.getDistanceSq(pos, origin), pos);
            }
        }
        if (blockDistances.isEmpty())
        {
            return null;
        }
        return blockDistances.firstEntry().getValue();
    }

    private boolean isPosTree(final BlockPos pos, final boolean includeWood)
    {
        final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
        return block instanceof BlockSapling || (includeWood && (block instanceof BlockWood || block instanceof BlockLeaves));
    }

    private BlockPos getSaplingPlacePos()
    {
        final int r = rangeSetting.getValue().intValue();
        final List<BlockPos> surroundingList = BlockUtil.RADIAL_BLOCK_MAP.get(r);
        final BlockPos origin = PlayerUtil.getOrigin();
        for (final BlockPos offset : surroundingList)
        {
            if (offset.getY() != 0)
            {
                continue;
            }
            final BlockPos pos = origin.add(offset);
            if (!BlockUtil.isReplaceable(pos))
            {
                continue;
            }
            final Block placeOnBlock = ((IWorld)MC.theWorld).nebula$getBlock(pos.down());
            if (placeOnBlock == Blocks.grass)
            {
                // make sure we don't place too close!
                final BlockPos nearestSaplingPos = getNearestTree(spacingSetting.getValue() + 1, true, pos);
                if (nearestSaplingPos != null && MathUtil.getDistance(nearestSaplingPos, pos) <= spacingSetting.getValue())
                {
                    continue;
                }
                return pos;
            }
        }
        return null;
    }

    private int getSapplingSlot()
    {
        return InventoryUtil.getHotbarSlot((stack) ->
        {
            if (!(stack.getItem() instanceof ItemBlock))
            {
                return false;
            }
            final Block block = ((ItemBlock) stack.getItem()).field_150939_a;
            if (!(block instanceof BlockSapling))
            {
                return false;
            }
            final int subType = stack.getItemDamage();
            switch (subType)
            {
                case 0:
                {
                    return oakSaplingSetting.getValue();
                }
                case 1:
                {
                    return spruceSaplingSetting.getValue();
                }
                case 2:
                {
                    return birchSaplingSetting.getValue();
                }
                case 3:
                {
                    return jungleSaplingSetting.getValue();
                }
                case 4:
                {
                    return acaciaSaplingSetting.getValue();
                }
                case 5:
                {
                    return darkOakSaplingSetting.getValue();
                }
                default:
                {
                    return false;
                }
            }
        });
    }
}
