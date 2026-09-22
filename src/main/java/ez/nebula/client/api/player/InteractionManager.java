package ez.nebula.client.api.player;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventSneak;
import ez.nebula.client.impl.module.player.NoSwingModule;
import ez.nebula.client.impl.module.world.PacketMineModule;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * @author xgraza
 * @since 04/29/25
 */
public final class InteractionManager
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    private boolean overrideSneak, sneaking;

    public void init()
    {
        EventBus.subscribe(this);
    }

    @Subscribe
    private final EventListener<EventSneak> sneakEventListener = event ->
    {
        if (!overrideSneak)
        {
            return;
        }
        MC.thePlayer.serverSneaking = sneaking;
        event.setState(sneaking);
    };

    public boolean rightClickBlock(final MovingObjectPosition raycast)
    {
        return rightClickBlock(new BlockPos(raycast.blockX, raycast.blockY, raycast.blockZ),
                EnumFacing.values()[raycast.sideHit]);
    }

    public boolean rightClickBlock(final BlockPos pos,
                                   final EnumFacing facing,
                                   final boolean sneak)
    {
        MC.rightClickDelayTimer = 4;

        final boolean sneakPacket = sneak
                && BlockUtil.INTERACTABLE_BLOCK_LIST.contains(MC.theWorld.getBlock(pos));
                //&& !MC.thePlayer.serverSneaking;
        overrideSneak = sneaking = sneakPacket;
        if (sneakPacket)
        {
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 1));
        }

        final boolean result = MC.playerController.onPlayerRightClick(MC.thePlayer,
                MC.theWorld,
                Nebula.INVENTORY.stack(),
                pos.getX(), pos.getY(), pos.getZ(), facing.order_a,
                createHitVec(pos, facing));
        if (result)
        {
            swingItem();
        }
        if (sneakPacket)
        {
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 2));
        }
        overrideSneak = sneaking = false;
        return result;
    }

    public boolean rightClickBlock(final BlockPos pos, final EnumFacing facing)
    {
        return rightClickBlock(pos, facing, false);
    }

    public boolean breakBlock(final int x, final int y, final int z, final int face)
    {
        final Block block = MC.theWorld.getBlock(x, y, z);
        if (block == null || block.getMaterial() == Material.air)
        {
            PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
            return true;
        }

        MC.playerController.blockHitDelay = 0;

        if (!MC.playerController.sameToolAndBlock(x, y, z))
        {
            PlayerControllerMP.ALLOW_BREAK_OVERRIDE = true;
            MC.playerController.resetBlockRemoving();
            MC.playerController.clickBlock(x, y, z, face);
            swingItem();

            // this is from inside PlayerControllerMP#clickBlock, however since clickBlock doesn't return a bool...
            if (block.getPlayerRelativeBlockHardness(MC.thePlayer, MC.theWorld, x, y, z) >= 1.0F)
            {
                PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
                return true;
            }
        }

        if (PacketMineModule.INSTANCE.isToggled())
        {
            PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
            return false;
        }

        PlayerControllerMP.ALLOW_BREAK_OVERRIDE = true;
        MC.playerController.onPlayerDamageBlock(x, y, z, face);
        if (MC.thePlayer.isCurrentToolAdventureModeExempt(x, y, z))
        {
            MC.effectRenderer.addBlockHitEffects(x, y, z, face);
            swingItem();
        }
        boolean brokeBlock = MC.playerController.curBlockDamageMP >= 1.0f;
        if (brokeBlock)
        {
            PlayerControllerMP.ALLOW_BREAK_OVERRIDE = false;
        }
        return brokeBlock;
    }

    public boolean breakBlock(final BlockPos pos, final EnumFacing facing)
    {
        return breakBlock(pos.getX(), pos.getY(), pos.getZ(), facing.order_a);
    }

    public void swingItem()
    {
        if (NoSwingModule.INSTANCE.isToggled())
        {
            MC.thePlayer.swingItemSilent();
        } else
        {
            MC.thePlayer.swingItem();
        }
    }

    private Vec3 createHitVec(final BlockPos pos, final EnumFacing facing)
    {
        final double faceX = facing.getFaceX() / 2.0;
        final double faceY = facing.getFaceY() / 2.0;
        final double faceZ = facing.getFaceZ() / 2.0;
        return Vec3.createVectorHelper(pos.getX() + faceX,
                pos.getY() + faceY,
                pos.getZ() + faceZ);
    }
}
