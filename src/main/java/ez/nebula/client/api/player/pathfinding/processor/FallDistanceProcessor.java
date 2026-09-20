package ez.nebula.client.api.player.pathfinding.processor;

import ez.nebula.client.api.player.pathfinding.node.NodeContext;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.src.BlockPos;

/**
 * @author xgraza
 * @since 9/18/26
 * This will process the cost of falling if this node decides to walk over this position
 */
public final class FallDistanceProcessor implements IProcessor
{
    @Override
    public float calculateCost(final NodeContext ctx, final float g)
    {
        final BlockPos pos = ctx.getPos();
        int fallDistance = 0;
        for (int y = 1; y < 256; ++y)
        {
            final BlockPos fallPos = pos.add(0, -y, 0);
            final Block fallBlock = MC.theWorld.getBlock(fallPos);
            if (fallBlock.getMaterial().blocksMovement() || fallBlock.getMaterial() == Material.water)
            {
                break;
            }
            ++fallDistance;
        }

        int maxFallDistance = (int) ((MC.thePlayer.getHealth() + MC.thePlayer.getAbsorptionAmount()) / 2.0f);
        if (fallDistance >= maxFallDistance + 0.5)
        {
            return IMPOSSIBLE;
        }
        return g + (fallDistance * 2.0f);
    }
}
