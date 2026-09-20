package ez.nebula.client.api.manager.module.type;

import ez.nebula.client.Nebula;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author xgraza
 * @since 9/4/26
 */
public abstract class InteractionModule extends RotationModule
{
    public InteractionModule()
    {
        this(-1);
    }

    public InteractionModule(final int rotationPriority)
    {
        super(rotationPriority);
    }

    /**
     * Uses an item in your hand (sends a C08PacketPlayerBlockPlacement)
     */
    protected void use()
    {
        final ItemStack stack = Nebula.INVENTORY.stack();
        if (stack == null)
        {
            ChatUtil.sendNebula("null");
            PacketUtil.send(new C08PacketPlayerBlockPlacement(null));
            MC.entityRenderer.itemRenderer.resetEquippedProgress();
            return;
        }
        if (MC.playerController.sendUseItem(MC.thePlayer, MC.theWorld, stack))
        {
            MC.entityRenderer.itemRenderer.resetEquippedProgress();
        }
    }

    /**
     * Uses an item in the slot provided
     * @param slot the slot 0-8
     */
    protected void use(final int slot)
    {
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.spoof(slot);
        }
        use();
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.sync();
        }
    }

    /**
     * Interacts with an entity with an item
     * @param entity the entity to interact with
     * @param slot the slot for the item
     */
    protected void interact(final Entity entity, final int slot)
    {
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.spoof(slot);
        }
        interact(entity);
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.sync();
        }
    }

    /**
     * Interacts with an entity (equivalent to right-clicking an entity)
     * @param entity the entity to interact with
     */
    protected void interact(final Entity entity)
    {
        PacketUtil.send(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT));
        MC.thePlayer.interactWith(entity);
    }

    protected void attack(final Entity entity, final boolean packet)
    {
        swing();
        if (packet)
        {
            PacketUtil.send(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));
        } else
        {
            MC.playerController.attackEntity(MC.thePlayer, entity);
        }
    }

    /**
     * Clicks a block once
     * @param pos the position
     * @param face the hit face
     */
    protected void click(final BlockPos pos, final EnumFacing face)
    {
        swing();
        MC.playerController.clickBlock(pos.getX(), pos.getY(), pos.getZ(), face.order_a);
    }

    protected boolean breakBlock(final BlockPos pos, final boolean autoSwap)
    {
        final EnumFacing face = AngleUtil.getVisibleFace(pos, 6.0);
        if (face == null)
        {
            return false;
        }
        return breakBlock(pos, face, autoSwap);
    }

    protected boolean breakBlock(final BlockInfo info, final boolean autoSwap)
    {
        return breakBlock(info.getPos(), info.getFacing(), autoSwap);
    }

    /**
     * Breaks a block
     * @param pos the position
     * @param face the hit face
     * @param autoSwap if to automatically swap to the best available tool slot
     * @return if the block was successfully broken
     * @apiNote if this returns false and it swapped, it will not swap back to the original slot
     */
    protected boolean breakBlock(final BlockPos pos, final EnumFacing face, final boolean autoSwap)
    {
        int slot = -1;
        if (autoSwap)
        {
            slot = InventoryUtil.getBestToolSlotFor(MC.theWorld.getBlock(pos));
        }

        if (slot != -1)
        {
            Nebula.INVENTORY.spoof(slot);
        }
        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = true;
        final boolean result = Nebula.INTERACTIONS.breakBlock(pos, face);
        if (slot != -1 && result)
        {
            Nebula.INVENTORY.sync();
        }
        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
        return result;
    }

    protected BlockInfo breakMultiInfo(final int maxBlocks, final boolean autoSwap, final List<BlockInfo> infoList)
    {
        if (infoList.isEmpty())
        {
            PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
            return null;
        }
        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = true;
        int broken = 0;
        for (final BlockInfo info : infoList)
        {
            if (broken >= maxBlocks)
            {
                PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
                return null;
            }
            int slot = -1;
            if (autoSwap)
            {
                slot = InventoryUtil.getBestToolSlotFor(MC.theWorld.getBlock(info.getPos()));
                if (slot != -1)
                {
                    Nebula.INVENTORY.spoof(slot);
                }
            }

            if (Nebula.INTERACTIONS.breakBlock(info.getPos(), info.getFacing()))
            {
                ++broken;
                if (slot != -1)
                {
                    Nebula.INVENTORY.sync();
                }
            } else
            {
                PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
                return info;
            }
        }
        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
        return null;
    }

    protected BlockInfo breakMultiPos(final int maxBlocks, final boolean autoSwap, final List<BlockPos> positions)
    {
        if (positions.isEmpty())
        {
            PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
            return null;
        }
        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = true;
        int broken = 0;
        for (final BlockPos pos : positions)
        {
            if (broken >= maxBlocks)
            {
                PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
                return null;
            }
            final EnumFacing face = AngleUtil.getVisibleFace(pos, 6.0);
            if (face == null)
            {
                continue;
            }

            int slot = -1;
            if (autoSwap)
            {
                slot = InventoryUtil.getBestToolSlotFor(MC.theWorld.getBlock(pos));
                if (slot != -1)
                {
                    Nebula.INVENTORY.spoof(slot);
                }
            }

            if (Nebula.INTERACTIONS.breakBlock(pos, face))
            {
                ++broken;
                if (slot != -1)
                {
                    Nebula.INVENTORY.sync();
                }
            } else
            {
                PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
                return new BlockInfo(pos, face);
            }
        }
        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
        return null;
    }

    /**
     * Swings your hand
     */
    protected void swing()
    {
        Nebula.INTERACTIONS.swingItem();
    }

    /**
     * Places at a position
     * @param pos the position
     * @return if the placement was successful
     */
    protected boolean place(final BlockPos pos)
    {
        final BlockInfo info = BlockUtil.getPlacement(pos);
        return info != null && place(info);
    }

    /**
     * Places at a position with a pre-determined {@link BlockInfo} with the current held item
     * @param info the info containing the {@link BlockPos} and {@link EnumFacing}
     * @return if the placement was successful
     */
    protected boolean place(final BlockInfo info)
    {
        return place(info.getPos(), info.getFacing());
    }

    /**
     * Places at a position with a pre-determined {@link BlockInfo} with the item in the slot
     * @param info the info containing the {@link BlockPos} and {@link EnumFacing}
     * @param slot the slot 0-8
     * @return if the placement was successful
     */
    protected boolean place(final BlockInfo info, final int slot)
    {
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.spoof(slot);
        }
        final boolean result = place(info);
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.sync();
        }
        return result;
    }

    /**
     * Places at a position with a face to place on
     * @param pos the position
     * @param face the hit face
     * @return if the placement was successful
     */
    protected boolean place(final BlockPos pos, final EnumFacing face)
    {
        return place(pos, face, true);
    }

    /**
     * Places at a position with a face to place on with the slot
     * @param pos the position
     * @param face the hit face
     * @param slot the slot 0-8
     * @return if the placement was successful
     */
    protected boolean place(final BlockPos pos, final EnumFacing face, final int slot)
    {
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.spoof(slot);
        }
        final boolean result = place(pos, face, true);
        if (slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.sync();
        }
        return result;
    }

    /**
     * Places at a position with a raytrace result
     * @param result the result
     * @return if the placement was successful
     */
    protected boolean place(final MovingObjectPosition result)
    {
        if (result == null || result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
        {
            return false;
        }
        return place(new BlockPos(result.blockX, result.blockY, result.blockZ), EnumFacing.faceList[result.sideHit], true);
    }

    /**
     * Attempts to place a block at a position and hit face, with the option to sneak if needed
     * @param pos the position
     * @param face the hit face
     * @param sneak if to if needed sneak when placing on an interactable block
     * @return if the placement was successful
     */
    protected boolean place(final BlockPos pos, final EnumFacing face, final boolean sneak)
    {
        return Nebula.INTERACTIONS.rightClickBlock(pos, face, sneak);
    }

    /**
     * Places multiple blocks in a single tick without rotations
     * @param maxBlocks the maximum amount of successful block placements
     * @param slot the slot to switch to when placing (or -1)
     * @param countFailures if to count a failed block place attempt towards the place counter
     * @param positions a {@link Collection} of {@link BlockPos} to place
     * @return the amount of block placements done
     */
    protected int placeMultiPos(final int maxBlocks, final int slot, final boolean countFailures, final Collection<BlockPos> positions)
    {
        if (positions.isEmpty())
        {
            return 0;
        }
        int placed = 0;
        for (final BlockPos pos : positions)
        {
            final BlockInfo info = BlockUtil.getPlacement(pos);
            if (info == null)
            {
                continue;
            }

            if (placed >= maxBlocks)
            {
                if (slot != InventoryUtil.INVALID_SLOT)
                {
                    Nebula.INVENTORY.sync();
                }
                break;
            }

            if (place(info, slot))
            {
                ++placed;
            } else
            {
                if (countFailures)
                {
                    ++placed;
                }
            }
        }
        return placed;

//        final List<BlockInfo> infoList = new ArrayList<>();
//        for (final BlockPos pos : positions)
//        {
//            if (!BlockUtil.isReplaceable(pos))
//            {
//                continue;
//            }
//            final BlockInfo info = BlockUtil.getPlacement(pos);
//            if (info != null)
//            {
//                infoList.add(info);
//            }
//        }
//        return infoList.isEmpty() ? 0 : placeMultiInfo(maxBlocks, slot, countFailures, infoList);
    }

    /**
     * Places multiple blocks in a single tick without rotations
     * @param maxBlocks the maximum amount of successful block placements
     * @param slot the slot to switch to when placing (or -1)
     * @param countFailures if to count a failed block place attempt towards the place counter
     * @param placements a {@link Collection} of {@link BlockInfo} to place
     * @return the amount of block placements done
     */
    protected int placeMultiInfo(final int maxBlocks, final int slot, final boolean countFailures, final Collection<BlockInfo> placements)
    {
        if (placements.isEmpty())
        {
            return 0;
        }
        int placed = 0;
        for (final BlockInfo info : placements)
        {
            if (placed >= maxBlocks)
            {
                if (slot != InventoryUtil.INVALID_SLOT)
                {
                    Nebula.INVENTORY.sync();
                }
                break;
            }
            if (info == null)
            {
                continue;
            }
            if (place(info, slot))
            {
                ++placed;
            } else
            {
                if (countFailures)
                {
                    ++placed;
                }
            }
        }
        return placed;
    }
}
