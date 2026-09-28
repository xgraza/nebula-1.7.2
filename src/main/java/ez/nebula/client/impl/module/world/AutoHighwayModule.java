package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.api.setting.block.BlockSetting;
import ez.nebula.client.impl.module.combat.AutoBedModule;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.impl.module.player.AutoEatModule;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IBlock;
import ez.nebula.client.mixin.duck.IBlockSign;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.init.Blocks;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author xgraza
 * @since 06/26/26
 */
@ModuleManifest(name = "AutoHighway",
        description = "Automatically builds a walkable highway",
        category = ModuleCategory.WORLD)
public final class AutoHighwayModule extends InteractionModule
{
    private final BlockSetting blockSetting = blockBuilder("Block")
            .setBlock(Blocks.obsidian)
            .setDescription("The kind of block to make a highway with")
            .build();
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.1)
            .setDescription("The range to break and place at")
            .build();
    private final Setting<Boolean> excavateSetting = builder("Excavate", true)
            .setDescription("If to mine out space")
            .build();
    private final NumberSetting<Integer> excavateHeightSetting = numberBuilder("Excavate Height", 3)
            .setMin(2)
            .setMax(5)
            .setScale(1)
            .setDescription("The height from the player origin to mine")
            .build();
    private final Setting<Boolean> onlyBlockSetting = builder("Mine non selected", false)
            .setDescription("If when excavating to mine blocks on highway path positions that are not the selected block type")
            .build();
    private final NumberSetting<Integer> blocksPerTickSetting = numberBuilder("Blocks per Tick", 3)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many blocks to place/break per tick")
            .build();
    private final NumberSetting<Integer> sizeSetting = numberBuilder("Size", 1)
            .setMin(1)
            .setMax(3)
            .setScale(1)
            .setDescription("The width in blocks on each side for the highway")
            .build();
    private final NumberSetting<Integer> lengthSetting = numberBuilder("Length", 4)
            .setMin(1)
            .setMax(6)
            .setScale(1)
            .setDescription("The length of highway to build ahead of you")
            .build();
    private final Setting<Boolean> autoWalkSetting = builder("Auto Walk", false)
            .setDescription("If to automatically walk when building a highway")
            .build();
    private final Setting<Boolean> preserveSignsSetting = builder("Preserve Signs", false)
            .setDescription("If to attempt to not break signs")
            .build();

    private List<BlockPos> highwayPositionList;
    private BlockInfo breakInfo;
    private boolean walk;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
            if (walk)
            {
                MC.thePlayer.movementInput.moveForward = 0.0f;
            }
        }
        if (breakInfo != null && MC.theWorld != null)
        {
            MC.playerController.resetBlockRemoving();
        }
        breakInfo = null;
        walk = false;
        if (highwayPositionList != null)
        {
            highwayPositionList.clear();
        }
        Nebula.INTERACTIONS.restore();
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (breakInfo == null)
        {
            return;
        }
        MC.mcProfiler.startSection("autoHighway");
        final AxisAlignedBB aabb = IAxisAlignedBB.create(breakInfo.getPos());
        Render3D.filledAABB(aabb, QuadMask.mask(breakInfo.getFacing()), 0x80FF0000);
        Render3D.outlinedAABB(aabb, 1.5f, QuadMask.mask(breakInfo.getFacing()), 0xFFFF0000);
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdateInput.Post> updateInputEventListener = event ->
    {
        if (autoWalkSetting.getValue()
                // imagine dying because autohighway wouldnt let you run lol
                && !KillAuraModule.INSTANCE.isAttacking() && !AutoBedModule.INSTANCE.isActive()
                && event.getInput().equals(MC.thePlayer.movementInput))
        {
            event.getInput().moveForward = walk ? 1.0f : 0.0f;
        }
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (AutoEatModule.INSTANCE.isActive() || KillAuraModule.INSTANCE.isAttacking() || AutoBedModule.INSTANCE.isActive())
        {
            walk = false;
            return;
        }

        // let the thing do its magic, we'll thank it later...
        if (AutoLavaHoleFillModule.INSTANCE.isActive())
        {
            return;
        }

        highwayPositionList = getHighwayPositions();
        if (highwayPositionList.isEmpty())
        {
            walk = false;
            return;
        }

        final List<BlockInfo> excavatePositionList = getExcavatePositions(highwayPositionList);
        // break blocks first, can't place blocks where theres already blocks, duh!
        if (!excavatePositionList.isEmpty() && excavateSetting.getValue())
        {
            walk = false;
            excavate(excavatePositionList);
            return;
        } else
        {
            if (breakInfo != null)
            {
                MC.playerController.resetBlockRemoving();
                breakInfo = null;
            }
            // nothing to break, give control back
            Nebula.INTERACTIONS.restore();
            walk = false;

            Nebula.INVENTORY.sync();
        }

        final int slot = InventoryUtil.getHotbarSlot(blockSetting::isBlock);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            walk = false;
            return;
        }

        if (placeMultiPos(blocksPerTickSetting.getValue(), slot, false, highwayPositionList) > 0)
        {
            walk = false;
            Nebula.INVENTORY.sync();
        } else
        {
            walk = true;
        }
    };

    private void excavate(final List<BlockInfo> excavatePosList)
    {
        if (breakInfo != null)
        {
            final BlockPos pos = breakInfo.getPos();
            if (!(MC.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)
                    > rangeSetting.getValue())
                    && !BlockUtil.isReplaceable(pos)
                    && !breakBlock(breakInfo, true))
            {
                return;
            }
            MC.playerController.resetBlockRemoving();
            breakInfo = null;
        }
        breakInfo = breakMultiInfo(blocksPerTickSetting.getValue(), true, excavatePosList);
    }

    private List<BlockInfo> getExcavatePositions(final List<BlockPos> highwayPosList)
    {
        final int maxY = MathHelper.floor_double(MC.thePlayer.boundingBox.minY) + excavateHeightSetting.getValue();
        final List<BlockInfo> posList = new LinkedList<>();
        for (final BlockPos highwayPos : highwayPosList)
        {
            Block block = ((IWorld)MC.theWorld).nebula$getBlock(highwayPos);
            if (blockSetting.getBlock() != block
                    && ((IBlock)block).nebula$getBlockHardness() != -1.0f
                    && !BlockUtil.isReplaceable(highwayPos)
                    && onlyBlockSetting.getValue())
            {
                final BlockInfo info = getBreakInfo(highwayPos, block);
                if (info != null)
                {
                    posList.add(info);
                }
            }

            for (int y = 1; y <= excavateHeightSetting.getValue() + 1; ++y)
            {
                if (highwayPos.getY() + y >= maxY)
                {
                    continue;
                }
                final BlockPos pos = highwayPos.offset(EnumFacing.UP, y);
                block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
                if (!block.getMaterial().isReplaceable() && ((IBlock)block).nebula$getBlockHardness() != -1.0f)
                {
                    final BlockInfo info = getBreakInfo(pos, block);
                    if (info != null)
                    {
                        posList.add(info);
                    }
                }
            }
        }
        if (!posList.isEmpty())
        {
            posList.sort(Comparator.comparingDouble((info) ->
                    MC.thePlayer.getDistance(
                            info.getPos().getX() + 0.5, info.getPos().getY() + 1.0, info.getPos().getZ() + 0.5)));
        }
        return posList;
    }

    private BlockInfo getBreakInfo(final BlockPos pos, final Block block)
    {
        if (MC.thePlayer.getDistance(
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > rangeSetting.getValue())
        {
            return null;
        }
        if (preserveSignsSetting.getValue())
        {
            if (block instanceof BlockSign)
            {
                return null;
            }
            for (final EnumFacing face : EnumFacing.values())
            {
                // not much a sign can do with a downwards face..
                if (face == EnumFacing.DOWN)
                {
                    continue;
                }
                final BlockPos neighbor = pos.offset(face);
                final Block offsetBlock = ((IWorld)MC.theWorld).nebula$getBlock(neighbor);
                if (offsetBlock instanceof BlockSign)
                {
                    // if the sign is a standing sign, and we have an up face
                    if (((IBlockSign) offsetBlock).nebula$isStanding() && face == EnumFacing.UP)
                    {
                        return null;
                    }
                    final int meta = MC.theWorld.getBlockMetadata(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                    if (meta >= 2 && meta <= 5 && face == EnumFacing.values()[meta])
                    {
                        return null;
                    }
                }
            }
        }
        final EnumFacing face = AngleUtil.getVisibleFace(pos, 6.0);
        if (face == null)
        {
            return null;
        }
        return new BlockInfo(pos, face);
    }

    private List<BlockPos> getHighwayPositions()
    {
        final List<BlockPos> highwayPositionList = new LinkedList<>();
        final EnumFacing facing = PlayerUtil.getFacing();
        final BlockPos[] adjacent = BlockUtil.getAdjacent(facing);
        if (adjacent == null)
        {
            return highwayPositionList;
        }

        for (int b = 0; b < lengthSetting.getValue() + 1; ++b)
        {
            final BlockPos origin = PlayerUtil.getOrigin().offset(facing, b).down();
            highwayPositionList.add(origin);
            for (final BlockPos adjacentPos : adjacent)
            {
                for (int i = 0; i < sizeSetting.getValue(); ++i)
                {
                    highwayPositionList.add(origin.add(adjacentPos.getX() * (i + 1),
                            0, adjacentPos.getZ() * (i + 1)));
                }

                // this will now add our "rails"
                highwayPositionList.add(origin.add(adjacentPos.getX() * (sizeSetting.getValue() + 1),
                        1, adjacentPos.getZ() * (sizeSetting.getValue() + 1)));
            }
        }

        return highwayPositionList;
    }
}
