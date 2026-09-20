package ez.nebula.client.api.player.pathfinding.node;

import net.minecraft.src.BlockPos;

public final class NodeContext
{
    private final Node node;
    private final BlockPos pos, prevPos;

    public NodeContext(Node node, BlockPos pos, BlockPos prevPos)
    {
        this.node = node;
        this.pos = pos;
        this.prevPos = prevPos;
    }

    public Node getNode()
    {
        return node;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public BlockPos getPrevPos()
    {
        return prevPos;
    }
}
