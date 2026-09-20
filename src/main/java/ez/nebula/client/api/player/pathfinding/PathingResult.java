package ez.nebula.client.api.player.pathfinding;

import ez.nebula.client.api.player.pathfinding.node.Node;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PathingResult
{
    private final Queue<Node> nodeQueue = new ConcurrentLinkedQueue<>();

    public PathingResult(final Node node)
    {
        reverseNodeTree(node);
    }

    public Node getNext(final boolean peek)
    {
        return peek ? nodeQueue.peek() : nodeQueue.poll();
    }

    private void reverseNodeTree(final Node node)
    {
        final List<Node> pathList = new LinkedList<>();

        Node n = node;
        while (n != null)
        {
            pathList.add(n);
            n = n.getParent();
        }

        Collections.reverse(pathList);
        nodeQueue.addAll(pathList);
    }

    public Queue<Node> getNodes()
    {
        return nodeQueue;
    }
}
