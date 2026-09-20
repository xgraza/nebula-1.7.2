package ez.nebula.client.api.player.pathfinding.node;

import ez.nebula.client.api.player.pathfinding.tasks.Task;
import net.minecraft.src.BlockPos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class Node
{
    private final BlockPos pos;
    private final Node parent;

    public float g, h;

    public final List<BlockPos> diagnoalList = new ArrayList<>();
    public final List<Task> taskList = new LinkedList<>();

    public Node(BlockPos pos, Node parent)
    {
        this.pos = pos;
        this.parent = parent;
    }

    public float getCost()
    {
        return h + g;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public Node getParent()
    {
        return parent;
    }

    @Override
    public int hashCode()
    {
        return (int) (pos.hashCode() + (31 * g) + h);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Node))
        {
            return false;
        }
        return obj.hashCode() == hashCode();
    }
}
