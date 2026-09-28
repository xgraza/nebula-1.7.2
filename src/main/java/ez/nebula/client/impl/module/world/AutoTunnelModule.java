/*
 * Copyright (c) xgraza 2025
 */

package ez.nebula.client.impl.module.world;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.input.EventUpdateInput;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.combat.AutoBedModule;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.impl.module.player.AutoEatModule;
import ez.nebula.client.mixin.duck.IBlock;
import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.item.ItemBlock;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author xgraza
 * @since 04/18/25
 */
@ModuleManifest(name = "AutoTunnel",
        description = "Automatically digs a tunnel in front of you",
        category = ModuleCategory.WORLD)
public final class AutoTunnelModule extends InteractionModule
{
    private final NumberSetting<Integer> lengthSetting = numberBuilder("Length", 4)
            .setMin(1)
            .setMax(6)
            .setScale(1)
            .setDescription("The length of the tunnel to dig ahead of you")
            .build();
    private final NumberSetting<Integer> blocksPerTickSetting = numberBuilder("Blocks per Tick", 3)
            .setMin(1)
            .setMax(20)
            .setScale(1)
            .setDescription("How many blocks to break per tick")
            .build();
    private final Setting<Boolean> autoWalkSetting = builder("Auto Walk", false)
            .setDescription("If to automatically walk when tunneling")
            .build();
    private final Setting<Boolean> backplaceSetting = builder("Backplace", false)
            .setDescription("If to replace the blocks behind you")
            .build();

    private final Set<BlockPos> replaceQueue = new ConcurrentSet<>();
    private BlockPos breakPos;
    private boolean walk;

    @Override
    public void onDisable()
    {
        super.onDisable();
        replaceQueue.clear();
        breakPos = null;
        walk = false;
    }

    @Subscribe
    private final EventListener<EventUpdateInput.Post> postEventListener = event ->
    {
        if (autoWalkSetting.getValue() && walk && event.getInput().equals(MC.thePlayer.movementInput))
        {
            event.getInput().sneak = false;
            event.getInput().moveForward = 1;
        }
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (KillAuraModule.INSTANCE.isAttacking() || AutoBedModule.INSTANCE.isActive() || AutoEatModule.INSTANCE.isActive())
        {
            walk = false;
            return;
        }

        walk = true;

        if (breakPos != null)
        {
            if (breakBlock(breakPos, true))
            {
                if (backplaceSetting.getValue())
                {
                    replaceQueue.add(breakPos);
                }
                walk = true;
                breakPos = null;
            }
            walk = false;
            return;
        }

        if (!replaceQueue.isEmpty() && backplaceSetting.getValue())
        {
            walk = true;
            int blocks = 0;
            for (final BlockPos pos : replaceQueue)
            {
                if (!isBlockBehindPlayer(pos))
                {
                    continue;
                }
                final int slot = InventoryUtil.getHotbarSlot(
                        (stack) -> stack.getItem() instanceof ItemBlock
                                && ((ItemBlock) stack.getItem()).field_150939_a.getMaterial().isSolid());
                if (slot == InventoryUtil.INVALID_SLOT)
                {
                    break;
                }
                final BlockInfo info = BlockUtil.getPlacement(pos);
                if (info == null)
                {
                    continue;
                }
                if (place(info, slot))
                {
                    replaceQueue.remove(pos);
                    ++blocks;
                }
                if (blocks >= blocksPerTickSetting.getValue())
                {
                    return;
                }
            }
            if (blocks > 0)
            {
                return;
            }
        }
        final List<BlockPos> tunnelBlockList = getTunnelBlockList();
        int blocks = 0;
        for (final BlockPos pos : tunnelBlockList)
        {
            if (breakBlock(pos, true))
            {
                walk = true;
                ++blocks;
                if (backplaceSetting.getValue())
                {
                    replaceQueue.add(pos);
                }
                if (blocksPerTickSetting.getValue() <= blocks)
                {
                    break;
                }
            } else
            {
                walk = false;
                // me must continue to break on the next tick
                breakPos = pos;
                return;
            }
        }
    };

    private List<BlockPos> getTunnelBlockList()
    {
        final List<BlockPos> blockPosList = new LinkedList<>();
        final BlockPos origin = PlayerUtil.getOrigin();
        final EnumFacing facing = PlayerUtil.getFacing();
        for (int i = 1; i < lengthSetting.getValue() + 1; ++i)
        {
            BlockPos pos = origin.offset(facing, i);
            if (BlockUtil.isNotAir(pos) && ((IBlock)((IWorld)MC.theWorld).nebula$getBlock(pos)).nebula$getBlockHardness() != -1.0f)
            {
                blockPosList.add(pos);
            }
            pos = pos.up();
            if (BlockUtil.isNotAir(pos) && ((IBlock)((IWorld)MC.theWorld).nebula$getBlock(pos)).nebula$getBlockHardness() != -1.0f)
            {
                blockPosList.add(pos);
            }
        }
        return blockPosList;
    }

    private boolean isBlockBehindPlayer(final BlockPos pos)
    {
        final BlockPos vec = ((IEnumFacing)(Object)PlayerUtil.getFacing()).nebula$getFaceOffset();
        int delta = 0;
        int axis = 0;
        if (vec.getX() != 0)
        {
            delta = MathHelper.floor_double(MC.thePlayer.posX) - pos.getX();
            axis = vec.getX();
        }
        if (vec.getZ() != 0)
        {
            delta = MathHelper.floor_double(MC.thePlayer.posZ) - pos.getZ();
            axis = vec.getZ();
        }
        return axis > 0 ? delta > 0 : delta < 0;
    }
}
