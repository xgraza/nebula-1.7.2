package ez.nebula.client.api.player.pathfinding.tasks;

import ez.nebula.client.api.player.pathfinding.node.Node;
import net.minecraft.client.Minecraft;

public abstract class Task
{
    protected static final Minecraft MC = Minecraft.getMinecraft();

    private final Node node;
    private boolean finished;

    public Task(Node node)
    {
        this.node = node;
    }

    public abstract boolean execute();

    protected void finish()
    {
        finished = true;
    }

    public Node getNode()
    {
        return node;
    }

    public boolean isFinished()
    {
        return finished;
    }
}
