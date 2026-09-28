package ez.nebula.client.api.player;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.player.EventSneak;
import ez.nebula.client.impl.module.player.NoSwingModule;
import ez.nebula.client.impl.module.world.PacketMineModule;
import ez.nebula.client.mixin.duck.IEntityClientPlayerMP;
import ez.nebula.client.mixin.duck.IMinecraft;
import ez.nebula.client.mixin.duck.IPlayerControllerMP;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
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
    private static boolean ALLOW_BREAK_OVERRIDE = false;

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
        ((IEntityClientPlayerMP)MC.thePlayer).nebula$setWasSneaking(sneaking);
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
        ((IMinecraft) MC).nebula$setRightClickDelayTimer(4);

        final boolean sneakPacket = sneak
                && BlockUtil.INTERACTABLE_BLOCK_LIST.contains(((IWorld)MC.theWorld).nebula$getBlock(pos));
                //&& !MC.thePlayer.serverSneaking;
        overrideSneak = sneaking = sneakPacket;
        if (sneakPacket)
        {
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 1));
        }

        final boolean result = MC.playerController.onPlayerRightClick(MC.thePlayer,
                MC.theWorld,
                Nebula.INVENTORY.stack(),
                pos.getX(), pos.getY(), pos.getZ(), facing.ordinal(),
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
            restore();
            return true;
        }

        ((IPlayerControllerMP)MC.playerController).nebula$setBlockHitDelay(0);

        if (!((IPlayerControllerMP)MC.playerController).nebula$sameToolAndBlock(x, y, z))
        {
            override();
            MC.playerController.resetBlockRemoving();
            MC.playerController.clickBlock(x, y, z, face);
            swingItem();

            // this is from inside PlayerControllerMP#clickBlock, however since clickBlock doesn't return a bool...
            if (block.getPlayerRelativeBlockHardness(MC.thePlayer, MC.theWorld, x, y, z) >= 1.0F)
            {
                restore();
                return true;
            }
        }

        if (PacketMineModule.INSTANCE.isToggled())
        {
            restore();
            return false;
        }

        override();
        MC.playerController.onPlayerDamageBlock(x, y, z, face);
        if (MC.thePlayer.isCurrentToolAdventureModeExempt(x, y, z))
        {
            MC.effectRenderer.addBlockHitEffects(x, y, z, face);
            swingItem();
        }
        boolean brokeBlock = ((IPlayerControllerMP)MC.playerController).nebula$getCurBlockDamageMP() >= 1.0f;
        if (brokeBlock)
        {
            restore();
        }
        return brokeBlock;
    }

    public boolean breakBlock(final BlockPos pos, final EnumFacing facing)
    {
        return breakBlock(pos.getX(), pos.getY(), pos.getZ(), facing.ordinal());
    }

    public void swingItem()
    {
        if (NoSwingModule.INSTANCE.isToggled())
        {
            MC.thePlayer.swingItem();
            // MC.thePlayer.swingItemSilent();
        } else
        {
            MC.thePlayer.swingItem();
        }
    }

    public void override()
    {
        ALLOW_BREAK_OVERRIDE = true;
    }

    public void restore()
    {
        ALLOW_BREAK_OVERRIDE = false;
    }

    public boolean isOverrideBreak()
    {
        return ALLOW_BREAK_OVERRIDE;
    }

    private Vec3 createHitVec(final BlockPos pos, final EnumFacing facing)
    {
        final double faceX = facing.getFrontOffsetX() / 2.0;
        final double faceY = facing.getFrontOffsetY() / 2.0;
        final double faceZ = facing.getFrontOffsetZ() / 2.0;
        return Vec3.createVectorHelper(pos.getX() + faceX,
                pos.getY() + faceY,
                pos.getZ() + faceZ);
    }
}
