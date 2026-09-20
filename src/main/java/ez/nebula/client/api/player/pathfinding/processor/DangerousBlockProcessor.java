package ez.nebula.client.api.player.pathfinding.processor;

import ez.nebula.client.api.player.pathfinding.node.NodeContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.init.Blocks;
import net.minecraft.src.BlockPos;

public final class DangerousBlockProcessor implements IProcessor
{
    @Override
    public float calculateCost(final NodeContext ctx, float g)
    {
        final BlockPos pos = ctx.getPos();

        Block block = MC.theWorld.getBlock(pos);
        Block blockUnder = MC.theWorld.getBlock(pos.down());

        // if we are trying to pass through water, give it a not-ideal cost
        if (block == Blocks.water || block == Blocks.flowing_water)
        {
            g += NOT_IDEAL_COST;
        }

        // we do not want to accidentally trip redstone
        if (block == Blocks.tripwire
                || block == Blocks.tripwire_hook
                || block instanceof BlockBasePressurePlate
                || blockUnder == Blocks.tripwire
                || blockUnder == Blocks.tripwire_hook
                || blockUnder instanceof BlockBasePressurePlate)
        {
            g += DANGER_COST;
        }

        // by walking over a redstone ore block, we can accidentally activate it by accident
        if (blockUnder == Blocks.redstone_ore || blockUnder == Blocks.lit_redstone_ore)
        {
            g += DANGER_COST; // we do not want to accidentally activate it by going across it
        }

        return g;
    }
}
