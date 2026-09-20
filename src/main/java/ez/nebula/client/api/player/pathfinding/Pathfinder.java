package ez.nebula.client.api.player.pathfinding;

import ez.nebula.client.api.player.pathfinding.node.Node;
import ez.nebula.client.api.player.pathfinding.node.NodeContext;
import ez.nebula.client.api.player.pathfinding.node.NodeCosts;
import ez.nebula.client.api.player.pathfinding.processor.BlockedPathProcessor;
import ez.nebula.client.api.player.pathfinding.processor.DangerousBlockProcessor;
import ez.nebula.client.api.player.pathfinding.processor.FallDistanceProcessor;
import ez.nebula.client.api.player.pathfinding.processor.IProcessor;
import ez.nebula.client.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.src.BlockPos;

import java.util.*;

/**
 * @author xgraza
 * @since 4/30/26
 */
public final class Pathfinder
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static final int[][] BLOCK_POSITION_OFFSETS = new int[26][];
    public static final int[][] BASIC_SURROUNDING_OFFSETS = {
            new int[]{ -1, 0, 0 },
            new int[]{ 1, 0, 0 },
            new int[]{ 0, 0, -1 },
            new int[]{ 0, 0, 1 }
    };

    static
    {
        int i = 0;
        for (int x = -1; x <= 1; ++x)
        {
            for (int y = -1; y <= 1; ++y)
            {
                for (int z = -1; z <= 1; ++z)
                {
                    if (x == 0 && y == 0 && z == 0)
                    {
                        continue;
                    }
                    BLOCK_POSITION_OFFSETS[i] = new int[]{ x, y, z };
                    ++i;
                }
            }
        }
    }

    private final List<IProcessor> nodeProcessorList = new LinkedList<>();

    public Pathfinder()
    {
        nodeProcessorList.add(new BlockedPathProcessor());
        nodeProcessorList.add(new DangerousBlockProcessor());
        nodeProcessorList.add(new FallDistanceProcessor());
    }

    public Node getGoalNode(final BlockPos origin, final BlockPos goal)
    {
        if (origin.equals(goal))
        {
            return null;
        }

        final PriorityQueue<Node> openNodeQueue = new PriorityQueue<>(
                Comparator.comparing(Node::getCost).thenComparing((node) -> node.h));
        final Map<BlockPos, Float> costMap = new HashMap<>();

        // create base node
        Node currentNode = new Node(origin, null);
        currentNode.h = getHeuristic(origin, goal);
        openNodeQueue.add(currentNode);

        Node closestNode = currentNode;

        while (!openNodeQueue.isEmpty())
        {
            currentNode = openNodeQueue.poll();
            if (currentNode == null)
            {
                break;
            }
            final BlockPos pos = currentNode.getPos();
            if (pos.equals(goal))
            {
                return currentNode;
            }

            if (currentNode.g > costMap.getOrDefault(pos, Float.MAX_VALUE))
            {
                continue;
            }

            if (currentNode.h < closestNode.h)
            {
                closestNode = currentNode;
            }

            for (final int[] offset : BLOCK_POSITION_OFFSETS)
            {
                final BlockPos neighborPos = pos.add(offset[0], offset[1], offset[2]);
                if (!MC.theWorld.blockExists(neighborPos.getX(), neighborPos.getY(), neighborPos.getZ()))
                {
                    continue;
                }

                final Node node = new Node(neighborPos, currentNode);
                float g = getGCost(node, neighborPos, pos);
                if (g >= NodeCosts.IMPOSSIBLE)
                {
                    continue;
                }

                float tentativeGCost = currentNode.g + g;
                if (tentativeGCost >= costMap.getOrDefault(neighborPos, Float.MAX_VALUE))
                {
                    continue;
                }

                node.h = getHeuristic(neighborPos, goal);
                node.g = tentativeGCost;

                costMap.put(neighborPos, tentativeGCost);
                openNodeQueue.add(node);
            }
        }

        if (!closestNode.getPos().equals(origin))
        {
            return closestNode;
        }

        return null;
    }

    private float getHeuristic(final BlockPos pos, final BlockPos goal)
    {
        return (float) MathUtil.getDistance(pos, goal);
    }

    private float getGCost(final Node node, final BlockPos pos, final BlockPos prevPos)
    {
        final NodeContext ctx = new NodeContext(node, pos, prevPos);
        float gCost = 1.0f;

        for (final IProcessor processor : nodeProcessorList)
        {
            if (processor.condition(ctx))
            {
                final float g = processor.calculateCost(ctx, gCost);
                if (g >= NodeCosts.IMPOSSIBLE)
                {
                    return NodeCosts.IMPOSSIBLE;
                }
                gCost = g;
            }
        }

        return gCost;
    }
}
