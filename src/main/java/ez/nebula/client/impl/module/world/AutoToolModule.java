package ez.nebula.client.impl.module.world;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventAttackBlock;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.mixin.duck.IBlock;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * @author xgraza
 * @since 03/26/25
 */
@ModuleManifest(name = "AutoTool",
        description = "Automatically switches to the best tool available",
        category = ModuleCategory.WORLD)
public final class AutoToolModule extends Module
{
    @Subscribe
    private final EventListener<EventAttackBlock> attackBlockEventListener = event ->
    {
        if (MC.playerController.isInCreativeMode())
        {
            return;
        }
        final Block block = MC.theWorld.getBlock(event.getX(), event.getY(), event.getZ());
        if (block == Blocks.air || ((IBlock)block).nebula$getBlockHardness() == -1.0f)
        {
            return;
        }
        final int slot = InventoryUtil.getBestToolSlotFor(block);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }
        MC.thePlayer.inventory.currentItem = slot;
    };
}
