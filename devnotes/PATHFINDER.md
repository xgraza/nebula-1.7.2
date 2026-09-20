ignore this file, this is the plan for pathfinding

essentially, i want to "simulate" in a way events that could happen along an otherwise impossible path

in any function of a node where these simulated behaviors happen, i want to have the world reflect the simulated behavior

an example of this is to have a Map<BlockPos, BlockState> of some kind where say we simulate breaking a block, we can reference that map
in a function such as getBlock(BlockPos) where that new simulated BlockState is reflected.

say,

```java

// calculating nodes...

if (isBlockingPathSomehowFunction())

    final MineTask task = node.addMineTask();
    task.addPosition(pos1);
    task.addPosition(pos2);
    
    nodeWorldContext.simulateTask(task);

```

that `simulateTask` would update that map with the resulted blocks mined (if possible of course), and then the path could continue to calculate
as if the action is already effective in the world...

only issue is that i would need a specific node to be the overall parent of each other node and record that, as other node paths may not share
the same simulated tasks. 