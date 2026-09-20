package ez.nebula.client.api.player.pathfinding.tasks;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.player.pathfinding.node.Node;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MineTask extends Task
{
    private final Queue<BlockPos> positionList = new ConcurrentLinkedQueue<>();

    private BlockInfo info;

    public MineTask(final Node node)
    {
        super(node);
    }

    @Override
    public boolean execute()
    {
        if (info != null)
        {
            swapToBestSlot(info.getPos());
            if (!Nebula.INTERACTIONS.breakBlock(info.getPos(), info.getFacing()))
            {
                return false;
            }
            info = null;
            Nebula.INVENTORY.sync();
        }

        while (!positionList.isEmpty())
        {
            final BlockPos pos = positionList.poll();
            if (pos == null)
            {
                finish();
                return true;
            }
            final EnumFacing face = AngleUtil.getVisibleFace(pos, 6.0);
            if (face == null)
            {
                continue;
            }
            swapToBestSlot(pos);
            if (!Nebula.INTERACTIONS.breakBlock(pos, face))
            {
                info = new BlockInfo(pos, face);
                return false;
            }
            Nebula.INVENTORY.sync();
        }

        finish();
        return true;
    }

    private void swapToBestSlot(final BlockPos pos)
    {
        final int slot = InventoryUtil.getBestToolSlotFor(MC.theWorld.getBlock(pos));
        if (slot != -1)
        {
            Nebula.INVENTORY.spoof(slot);
        }
    }

    public void addPosition(final BlockPos pos)
    {
        positionList.add(pos);
    }
}
