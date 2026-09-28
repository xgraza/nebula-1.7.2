package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.DebugFeature;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventPostUpdate;
import ez.nebula.client.api.listener.event.player.EventAttackBlock;
import ez.nebula.client.api.listener.event.player.EventMoveUpdate;
import ez.nebula.client.api.listener.event.world.EventModifySelectedBoundBox;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.init.Items;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

/**
 * @author xgraza
 * @since 05/24/26
 */
@DebugFeature
@ModuleManifest(name = "PortalBreaker",
        description = "Attempts to breaks placed end portal blocks (not frames) when you try to break them",
        category = ModuleCategory.WORLD)
public final class PortalBreakerModule extends Module
{
    private static final int PORTAL_BREAKER_ROTATION_PRIORITY = 80;

    private MovingObjectPosition result;
    private int slot = InventoryUtil.INVALID_SLOT;
    private BlockPos pos;
    private boolean tryAfterRotate;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null && slot != InventoryUtil.INVALID_SLOT)
        {
            Nebula.INVENTORY.sync();
        }
        slot = InventoryUtil.INVALID_SLOT;
        pos = null;
        tryAfterRotate = false;
    }

    @Subscribe
    private final EventListener<EventMoveUpdate> moveUpdateEventListener = event ->
    {
        if (slot == InventoryUtil.INVALID_SLOT || pos == null || tryAfterRotate)
        {
            return;
        }

        result = null;
        float[] angles = null;
        for (final EnumFacing facing : EnumFacing.values())
        {
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN)
            {
                continue;
            }
            final BlockPos neighborPos = pos.offset(facing);
            if (!BlockUtil.isReplaceable(neighborPos))
            {
                angles = AngleUtil.anglesToBlock(neighborPos, ((IEnumFacing)(Object)facing).nebula$getOpposite(), 1.0f);
                result = AngleUtil.raytrace(5, angles[0], angles[1]);
                if (result == null
                        || (result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                            || MC.theWorld.getBlock(result.blockX, result.blockY, result.blockZ) instanceof BlockEndPortal)
                            || result.sideHit < 2) // if sideHit is UP/DOWN
                {
                    result = null;
                }
                break;
            }
        }
        if (result == null || angles == null)
        {
            notifyError("Could not find stable supporting block to place water on", 7500L);
            pos = null;
            slot = InventoryUtil.INVALID_SLOT;
            return;
        }

        if (tryAfterRotate)
        {
            return;
        }
        tryAfterRotate = Nebula.ROTATIONS.spoof(
                angles[0], angles[1], PORTAL_BREAKER_ROTATION_PRIORITY);
    };

    @Subscribe
    private final EventListener<EventPostUpdate> postUpdateEventListener = event ->
    {
        if (!tryAfterRotate)
        {
            return;
        }

        Nebula.INVENTORY.spoof(slot);
        Nebula.INTERACTIONS.rightClickBlock(result); // click w/ water bucket
        Nebula.INTERACTIONS.rightClickBlock(result); // collect water
        Nebula.INVENTORY.sync();

        slot = InventoryUtil.INVALID_SLOT;
        pos = null;
        tryAfterRotate = false;
    };

    @Subscribe
    private final EventListener<EventModifySelectedBoundBox> modifySelectedBoundBoxEventListener = event ->
    {
        final Block block = event.getWorld().getBlock(event.getX(), event.getY(), event.getZ());
        if (!(block instanceof BlockEndPortal))
        {
            return;
        }
        event.setAabb(IAxisAlignedBB.create(new BlockPos(event.getX(), event.getY(), event.getZ())));
    };

    @Subscribe
    private final EventListener<EventAttackBlock> attackBlockEventListener = event ->
    {
        final BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
        if (!(((IWorld)MC.theWorld).nebula$getBlock(pos) instanceof BlockEndPortal))
        {
            return;
        }

        slot = InventoryUtil.getHotbarItem(Items.water_bucket);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            notifyInfo("You need a water bucket in your hotbar", 7500L);
            return;
        }
        this.pos = pos;
        event.cancel();
    };
}
