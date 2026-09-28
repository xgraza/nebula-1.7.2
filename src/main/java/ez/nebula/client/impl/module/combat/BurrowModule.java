package ez.nebula.client.impl.module.combat;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.RotationModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * @author xgraza
 * @since 03/25/25
 */
@ModuleManifest(name = "Burrow",
        description = "Places a blast resistant block at your feet to prevent explosion damage",
        category = ModuleCategory.COMBAT)
@RotationPriority(ModuleRotationPriorities.BURROW)
public final class BurrowModule extends RotationModule
{
    private static final Block[] VALID_BURROW_BLOCKS = { Blocks.obsidian, Blocks.anvil, Blocks.ender_chest };

    private final Setting<Boolean> instantSetting = builder("Instant", false)
            .setDescription("If to use packets to observe when the burrow block is replaced to instantly replace it")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate when placing the block")
            .build();

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        // SWAP IZZO
        // SEEYUH!
        if (!isBurrowed())
        {
            burrow();
            return;
        }
        if (!instantSetting.getValue())
        {
            toggle();
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (MC.thePlayer == null || MC.theWorld == null || !instantSetting.getValue())
        {
            return;
        }
        if (event.getPacket() instanceof S23PacketBlockChange)
        {
            final S23PacketBlockChange packet = event.getPacket();
            final BlockPos pos = PlayerUtil.getOrigin().add(0, 1, 0);
            if (packet.func_148879_d() == pos.getX()
                    && packet.func_148878_e() == pos.getY()
                    && packet.func_148877_f() == pos.getZ()
                    && packet.func_148880_c().getMaterial().isReplaceable())
            {
                burrow();
            }
        }
    };

    private void burrow()
    {
        final int slot = InventoryUtil.getHotbarBlock(VALID_BURROW_BLOCKS);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            notifyError("Obsidian/EnderChest/Anvil required in hotbar for burrow.", 7500L);
            toggle();
            return;
        }
        final BlockData blockData = getBlockData();
        if (blockData == null)
        {
            notifyError("No position found to place at!", 7500L);
            toggle();
            return;
        }

        final boolean sneak = isBlockUnderInteractable() && !MC.thePlayer.isSneaking();
        if (sneak)
        {
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 1));
        }

        Nebula.INVENTORY.spoof(slot);

        if (rotateSetting.getValue())
        {
            if (!canRotate())
            {
                return;
            }
            final float[] angles = AngleUtil.anglesToBlock(blockData.pos, blockData.facing, 1.0f);
            // because this is an immediate need, we will send a C06
            PacketUtil.send(new C03PacketPlayer.C06PacketPlayerPosLook(
                    MC.thePlayer.posX,
                    MC.thePlayer.boundingBox.minY,
                    MC.thePlayer.posY,
                    MC.thePlayer.posZ,
                    angles[0],
                    angles[1],
                    MC.thePlayer.onGround));
        }

        final Vec3 hitVec = Vec3.createVectorHelper(
                blockData.pos.getX() + 0.5,
                blockData.pos.getY() + 0.5,
                blockData.pos.getZ() + 0.5);
        PacketUtil.send(new C08PacketPlayerBlockPlacement(
                blockData.pos.getX(),
                blockData.pos.getY(),
                blockData.pos.getZ(),
                blockData.facing.ordinal(),
                MC.thePlayer.getHeldItem(),
                (float) (hitVec.xCoord - blockData.pos.getX()),
                (float) (hitVec.yCoord - blockData.pos.getY()),
                (float) (hitVec.zCoord - blockData.pos.getZ())
        ));
        Nebula.INTERACTIONS.swingItem();
        Nebula.INVENTORY.sync();

        if (sneak)
        {
            PacketUtil.send(new C0BPacketEntityAction(MC.thePlayer, 2));
        }
    }

    private boolean isBurrowed()
    {
        return !BlockUtil.isReplaceable(PlayerUtil.getOrigin());
    }

    private boolean isBlockUnderInteractable()
    {
        final int posX = MathHelper.floor_double(MC.thePlayer.posX);
        final int posY = (int) (Math.round(MC.thePlayer.boundingBox.minY) - 1);
        final int posZ = MathHelper.floor_double(MC.thePlayer.posZ);
        return BlockUtil.INTERACTABLE_BLOCK_LIST.contains(MC.theWorld.getBlock(posX, posY, posZ));
    }

    private BlockData getBlockData()
    {
        final BlockPos origin = PlayerUtil.getOrigin();
        for (final EnumFacing facing : EnumFacing.values())
        {
            final BlockPos n = origin.offset(facing);
            if (!BlockUtil.isReplaceable(n))
            {
                return new BlockData(n, ((IEnumFacing)(Object)facing).nebula$getOpposite());
            }
        }
        return null;
    }

    private static final class BlockData
    {
        private final BlockPos pos;
        private final EnumFacing facing;

        public BlockData(BlockPos pos, EnumFacing facing)
        {
            this.pos = pos;
            this.facing = facing;
        }
    }
}
