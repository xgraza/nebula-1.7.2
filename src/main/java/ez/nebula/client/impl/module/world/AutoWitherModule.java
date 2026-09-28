package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.world.EventPlace;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.LinkedList;
import java.util.List;

/**
 * @author xgraza
 * @since 6/25/26
 */
@ModuleManifest(name = "AutoWither",
        description = "Automatically finishes placing a wither spawn after the first base soul sand block is placed",
        category = ModuleCategory.WORLD)
public final class AutoWitherModule extends InteractionModule
{
    private static final int WITHER_HEAD_DAMAGE = 1;

    private BlockPos pos;

    @Override
    public void onEnable()
    {
        super.onEnable();
        pos = null;
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (pos == null)
        {
            return;
        }
        final int witherHeadSlot = getWitherSkullSlot();
        if (witherHeadSlot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }
        final List<BlockPos> soulSandPositions = getSoulSandPositions();
        if (soulSandPositions == null)
        {
            Nebula.INVENTORY.sync();
            pos = null;
            return; // null result = failure
        }

        if (!soulSandPositions.isEmpty() && placeMultiPos(Integer.MAX_VALUE, InventoryUtil.INVALID_SLOT, true, soulSandPositions) >= 1)
        {
            return;
        }

        for (final BlockPos soulSandPos : soulSandPositions)
        {
            if (!(((IWorld)MC.theWorld).nebula$getBlock(soulSandPos) instanceof BlockSoulSand))
            {
                Nebula.INVENTORY.sync();
                pos = null;
                return;
            }
        }

        final List<BlockPos> tPosList = getTTop(pos.up());
        if (tPosList == null || tPosList.isEmpty())
        {
            Nebula.INVENTORY.sync();
            pos = null;
            return;
        }

        if (placeMultiPos(Integer.MAX_VALUE, witherHeadSlot, false, tPosList) >= tPosList.size())
        {
            Nebula.INVENTORY.sync();
            pos = null;
        }
    };

    @Subscribe
    private final EventListener<EventPlace> placeEventListener = event ->
    {
        // WRONG!
        if (event.getSide() == -1 || event.getSide() == 255)
        {
            return;
        }

        if (pos != null)
        {
            // do not override if this module is the one placing the new soulsand blocks
            final double distance = MC.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            if (distance <= MC.playerController.getBlockReachDistance())
            {
                return;
            }
        }

        final ItemStack itemStack = event.getItemStack();
        if (itemStack != null
                && itemStack.getItem() instanceof ItemBlock
                && ((ItemBlock) itemStack.getItem()).field_150939_a instanceof BlockSoulSand)
        {
            // we need at least 5 blocks of soul sand (pre place) to make a wither
            if (!InventoryUtil.hasEnough(itemStack, 4))
            {
                return;
            }
            // get the correct pos via the place face
            pos = new BlockPos(event.getX(), event.getY(), event.getZ())
                    .add(((IEnumFacing)(Object)EnumFacing.values()[event.getSide()]).nebula$getFaceOffset());
            return;
        }
        pos = null;
    };

    private List<BlockPos> getSoulSandPositions()
    {
        if (pos == null)
        {
            return null;
        }
        final BlockPos[] adjacent = BlockUtil.getAdjacent(PlayerUtil.getFacing());
        if (adjacent == null)
        {
            return null;
        }
        final List<BlockPos> posList = new LinkedList<>();
        if (BlockUtil.isReplaceable(pos))
        {
            posList.add(pos);
        }
        BlockPos pos1 = pos.up(); // start of the T
        if (BlockUtil.isReplaceable(pos1))
        {
            // ensure space to place wither head
            if (BlockUtil.isNotAir(pos1.up()))
            {
                return null;
            }
            posList.add(pos1);
        }

        final BlockPos tOffsetPos1 = pos1.add(adjacent[0]);
        if (BlockUtil.isReplaceable(tOffsetPos1))
        {
            // ensure space to place wither head
            if (BlockUtil.isNotAir(tOffsetPos1.up()))
            {
                return null;
            }
            posList.add(tOffsetPos1);
        }

        final BlockPos tOffsetPos2 = pos1.add(adjacent[1]);
        if (BlockUtil.isReplaceable(tOffsetPos2))
        {
            // ensure space to place wither head
            if (BlockUtil.isNotAir(tOffsetPos2.up()))
            {
                return null;
            }
            posList.add(tOffsetPos2);
        }

        return posList;
    }

    private List<BlockPos> getTTop(final BlockPos origin)
    {
        final BlockPos[] adjacent = BlockUtil.getAdjacent(PlayerUtil.getFacing());
        if (adjacent == null)
        {
            return null;
        }
        final List<BlockPos> posList = new LinkedList<>();
        posList.add(origin.up());
        posList.add(origin.add(adjacent[0]).up());
        posList.add(origin.add(adjacent[1]).up());
        // if the position above the block ins't replaceable (cant place a wither head)
        posList.removeIf(BlockUtil::isNotAir);
        return posList;
    }

    private int getWitherSkullSlot()
    {
        for (int i = 0; i < 9; ++i)
        {
            final ItemStack itemStack = MC.thePlayer.inventory.getStackInSlot(i);
            if (itemStack != null
                    && itemStack.getItem() instanceof ItemSkull
                    && itemStack.getItemDamage() == WITHER_HEAD_DAMAGE
                    && InventoryUtil.hasEnough(itemStack, 3))
            {
                return i;
            }
        }
        return InventoryUtil.INVALID_SLOT;
    }
}
