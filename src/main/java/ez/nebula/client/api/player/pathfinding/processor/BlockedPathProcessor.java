package ez.nebula.client.api.player.pathfinding.processor;

import ez.nebula.client.api.player.pathfinding.node.Node;
import ez.nebula.client.api.player.pathfinding.node.NodeContext;
import ez.nebula.client.api.player.pathfinding.tasks.MineTask;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.src.BlockPos;

/**
 * @author xgraza
 * @since 9/18/26
 * Checks if this path is unable to continue due to being blocked
 */
public final class BlockedPathProcessor implements IProcessor
{
    @Override 
    public float calculateCost(final NodeContext ctx, float g)
    {
        final BlockPos pos = ctx.getPos(), prevPos = ctx.getPrevPos();
        final Node node = ctx.getNode();

        final Block block = MC.theWorld.getBlock(pos);
        if (isBlockImpassible(block, pos))
        {
            if (block.blockHardness == -1 || block.blockHardness == 100.0f)
            {
                return IMPOSSIBLE;
            }
            // return IMPOSSIBLE;
            final MineTask task = new MineTask(node);
            task.addPosition(pos);
            node.taskList.add(task);
        }

        final Block blockAbove = MC.theWorld.getBlock(pos.up());
        final Block blockUnder = MC.theWorld.getBlock(pos.down());
        
        final int deltaX = pos.getX() - prevPos.getX();
        final int deltaY = pos.getY() - prevPos.getY();
        final int deltaZ = pos.getZ() - prevPos.getZ();
        final boolean diag = isOffsetDiagonal(deltaX, deltaZ);

        if (deltaY == 0 && !isBlockImpassible(blockUnder, pos.down()))
        {
            return IMPOSSIBLE;
        }

        // if we have moved up on the y-axis
        if (deltaY != 0)
        {
            if (deltaY > 1)
            {
                return IMPOSSIBLE;
            } else if (deltaY == 1)
            {
                if (isBlockImpassible(blockAbove, pos.up()) || !isBlockImpassible(blockUnder, pos.down()))
                {
                    return IMPOSSIBLE;
                }
            }

            if (deltaY < 0)
            {
                // can we even fall down?
                final BlockPos headPos = pos.up().up();
                if (isBlockImpassible(MC.theWorld.getBlock(headPos), headPos))
                {
                    return IMPOSSIBLE;
                }
            }
        }

        if (diag)
        {
            if (deltaY < 0)
            {
                g = getDiagonalGCost(node, pos.down(), deltaX, deltaZ, g);
                if (g >= IMPOSSIBLE)
                {
                    return IMPOSSIBLE;
                }
            }

            g = getDiagonalGCost(node, pos, deltaX, deltaZ, g);
            if (g >= IMPOSSIBLE)
            {
                return IMPOSSIBLE;
            }
            g = getDiagonalGCost(node, pos.up(), deltaX, deltaZ, g);
            if (g >= IMPOSSIBLE)
            {
                return IMPOSSIBLE;
            }
        }
        return g;
    }

    private boolean isOffsetDiagonal(int deltaX, int deltaZ)
    {
        return Math.abs(deltaX) == 1 && Math.abs(deltaZ) == 1;
    }

    private boolean isBlockImpassible(final Block block, final BlockPos pos)
    {
        final Block blockAbove = MC.theWorld.getBlock(pos.up());
        final Block blockUnder = MC.theWorld.getBlock(pos.down());

        // cannot pass through solid blocks
        if (block.getMaterial().blocksMovement() || blockAbove.getMaterial().blocksMovement())
        {
            return true;
        }

        // if a block does not block movement but we do not want to pass through it, return IMPOSSIBLE
        if (block == Blocks.web || block == Blocks.fire || block == Blocks.lava || block == Blocks.flowing_lava)
        {
            return true;
        }

        // let's specify blocks we don't want to walk over
        if (blockUnder == Blocks.web
                || blockUnder == Blocks.lava
                || blockUnder == Blocks.flowing_lava
                || blockUnder == Blocks.fire
                || blockUnder == Blocks.cactus)
        {
            return true;
        }

        // if we are under sand/gravel (a falling block)
        if (blockUnder == Blocks.sand || blockUnder == Blocks.gravel)
        {
            // check the block under the block at our feet -
            // we're checking to see if there are blocks under that could possibly create an update and let us fall
            final Block blockUnder2 = MC.theWorld.getBlock(pos.down().down());
            return blockUnder2 == Blocks.standing_sign
                    || blockUnder2 == Blocks.wall_sign
                    || blockUnder2 == Blocks.tripwire
                    || blockUnder2 == Blocks.carpet;
        }

        return false;
    }

    private float getDiagonalGCost(final Node node, final BlockPos pos, final int deltaX, final int deltaZ, float g)
    {
        final BlockPos adj1 = pos.add(-deltaX, 0, 0);
        final BlockPos adj2 = pos.add(0, 0, -deltaZ);

        if (node.diagnoalList.isEmpty())
        {
            node.diagnoalList.add(adj1);
            node.diagnoalList.add(adj2);
        }

        final Block block1 = MC.theWorld.getBlock(adj1);
        final Block block2 = MC.theWorld.getBlock(adj2);

        final boolean adjB1 = block1.getMaterial().blocksMovement();
        final boolean adjB2 = block2.getMaterial().blocksMovement();

        if (adjB1 || adjB2)
        {
            return IMPOSSIBLE;
        }

        g = getGCostForBlock(block1, MC.theWorld.getBlock(adj1.down()), MC.theWorld.getBlock(adj1.up()), adj1, g);
        g = getGCostForBlock(block2, MC.theWorld.getBlock(adj2.down()), MC.theWorld.getBlock(adj2.up()), adj2, g);

        return g;
    }
}
