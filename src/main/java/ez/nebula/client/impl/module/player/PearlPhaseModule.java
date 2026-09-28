package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.Event;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.player.EventPushFromBlocks;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockTorch;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

/**
 * @author xgraza
 * @since 03/24/25
 */
@ModuleManifest(name = "PearlPhase",
        description = "Phases into a block using pearls",
        category = ModuleCategory.PLAYER)
@RotationPriority(ModuleRotationPriorities.PEARL_PHASE)
public final class PearlPhaseModule extends InteractionModule
{
    private static final float PITCH = 79.452f;

    private final Setting<Boolean> extraCheckSetting = builder("No Fall", true)
            .setDescription("If to check under the block you're phasing into the ensure there is a block to stand on")
            .build();

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (!MC.thePlayer.isCollidedHorizontally || PlayerUtil.isPhased())
        {
            return;
        }
        final int slot = InventoryUtil.getHotbarItem(ItemEnderPearl.class);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            notifyError("You need an ender pearl in your hotbar to phase.", 5000L);
            toggle();
            return;
        }

        if (rotateAndWait(calcBlockTargetAngles()))
        {
            use(slot);
            toggle();
        }
    };

    @Subscribe
    private final EventListener<EventPushFromBlocks> pushFromBlocksEventListener = Event::cancel;

    private float[] calcBlockTargetAngles()
    {
        final float[] angles = { 0.0f, PITCH };

        final BlockPos pos = PlayerUtil.getOrigin();
        final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
        if (block instanceof BlockSign)
        {
            angles[1] = 84.922f;
        }

        for (final EnumFacing facing : BlockUtil.HORIZONTALS)
        {
            final BlockPos neighbor = pos.offset(facing);
            if (BlockUtil.isReplaceable(neighbor))
            {
                continue;
            }

            if (extraCheckSetting.getValue() && BlockUtil.isReplaceable(neighbor.down()))
            {
                continue;
            }

            angles[0] = MathHelper.wrapAngleTo180_float(BlockUtil.getHorizontalFacing(facing) * -90.0f);
            if (block instanceof BlockTorch)
            {
                if (angles[0] > 0.0f)
                {
                    angles[0] -= 29;
                } else
                {
                    angles[0] += 29;
                }
            }
            return angles;
        }
        return null;
    }
}
