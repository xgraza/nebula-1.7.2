package ez.nebula.client.api.player.pathfinding.processor;

import ez.nebula.client.api.player.pathfinding.node.NodeContext;
import ez.nebula.client.api.player.pathfinding.node.NodeCosts;
import ez.nebula.client.api.player.pathfinding.Pathfinder;
import ez.nebula.client.impl.module.movement.JesusModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.src.BlockPos;

public interface IProcessor extends NodeCosts
{
    Minecraft MC = Minecraft.getMinecraft();

    float calculateCost(final NodeContext ctx, final float g);

    default boolean condition(final NodeContext ctx)
    {
        return true;
    }

    default float getGCostForBlock(final Block block, final Block blockUnder, final Block blockAbove, final BlockPos pos, float g)
    {
        // check to see if it could be a trap, we don't want to accidentally set off redstone
        if (block == Blocks.tripwire || block == Blocks.tripwire_hook || block instanceof BlockBasePressurePlate)
        {
            g += GENERAL_DANGER_COST;
        }
        if (block == Blocks.carpet
                && (blockUnder == Blocks.standing_sign
                || blockUnder == Blocks.wall_sign
                || blockUnder == Blocks.tripwire))
        {
            g += GENERAL_DANGER_COST;
        }

        // potentially deadly
        if ((block == Blocks.fire || block == Blocks.lava || block == Blocks.flowing_lava)
                && !MC.thePlayer.isPotionActive(Potion.fireResistance))
        {
            g += DANGER_COST;
        }

        if (blockAbove == Blocks.lava || blockAbove == Blocks.flowing_lava)
        {
            g += DANGER_COST;
        }

        if (block == Blocks.water || block == Blocks.flowing_water && blockAbove.getMaterial() != Material.air)
        {
            return IMPOSSIBLE;
        }

        if ((blockUnder == Blocks.water || blockUnder == Blocks.flowing_water) && !JesusModule.INSTANCE.isToggled())
        {
            g += DANGER_COST;
        }

        for (final int[] offsets : Pathfinder.BASIC_SURROUNDING_OFFSETS)
        {
            final BlockPos neighbor = pos.add(offsets[0], offsets[1], offsets[2]);
            final Block neighboringBlock = MC.theWorld.getBlock(neighbor);

            if (neighboringBlock == Blocks.cactus)
            {
                g += NOT_IDEAL_COST;
            } else if (neighboringBlock == Blocks.lava || neighboringBlock == Blocks.flowing_lava)
            {
                g += DANGER_COST;
            } else if (neighboringBlock == Blocks.ladder || neighboringBlock == Blocks.vine)
            {
                g += NOT_IDEAL_COST;
            }
        }

        return g;
    }
}
