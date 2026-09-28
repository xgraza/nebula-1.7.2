package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventAttackBlock;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IBlock;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.ItemUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author xgraza
 * @since 04/02/25
 */
@ModuleManifest(name = "PacketMine",
        description = "Mines blocks faster and automatically with packets",
        category = ModuleCategory.WORLD)
public final class PacketMineModule extends Module
{
    @ModuleInstance
    public static PacketMineModule INSTANCE;

    private final NumberSetting<Double> percentSetting = numberBuilder("Percent", 0.95)
            .setMin(0.01)
            .setMax(1.0)
            .setScale(0.01)
            .setDescription("How far long to try to break a block before trying to break it")
            .build();
    private final Setting<Boolean> instantSetting = builder("Instant", false)
            .setDescription("If to instantly set a block to air if it can break quickly")
            .build();
    private final Setting<Boolean> ignoreGroundSetting = builder("Ignore Ground", false)
            .setDescription("If to ignore ground checks in the vanilla game")
            .build();
    private final Setting<Boolean> rebreakSetting = builder("Rebreak", false)
            .setDescription("If to save the previous block break and rebreak it after it reappears")
            .build();
    private final Setting<Boolean> renderSetting = builder("Render", true)
            .setDescription("If to render the break animation")
            .build();

    private final Queue<MinePosition> minePositionQueue = new ConcurrentLinkedQueue<>();
    private MinePosition currentPosition;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (currentPosition != null && currentPosition.sentBreak && MC.thePlayer != null)
        {
            abortBreakingBlock(currentPosition);
        }
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
        minePositionQueue.clear();
        currentPosition = null;
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (currentPosition == null || !renderSetting.getValue())
        {
            return;
        }

        final int x = currentPosition.x;
        final int y = currentPosition.y;
        final int z = currentPosition.z;
        if (MC.theWorld.isAirBlock(x, y, z))
        {
            return;
        }

        MC.mcProfiler.startSection("packetMine");

        AxisAlignedBB bb = MC.theWorld.getBlock(x, y, z).getSelectedBoundingBoxFromPool(MC.theWorld, x, y, z);
        if (bb == null)
        {
            bb = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        }
        bb = IAxisAlignedBB.create(((IAxisAlignedBB)bb).nebula$getCenter(), 0.0).copy();

        final double factor = MathHelper.clamp_double(currentPosition.progress, 0.0, 1.0);
        bb = bb.expand(factor * 0.5, factor * 0.5, factor * 0.5);

        final int color = factor >= percentSetting.getValue() ? 0x8000FF00 : 0x80FF0000;
        Render3D.filledAABB(bb, QuadMask.ALL_FACES, color);
        Render3D.outlinedAABB(bb, 1.5f, QuadMask.ALL_FACES, color);

        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        minePositionQueue.removeIf(
                (position) -> MC.thePlayer.getDistanceSq(
                        position.x, position.y, position.z) > getReachDistanceSq());
        if (currentPosition == null)
        {
            currentPosition = minePositionQueue.poll();
            if (currentPosition == null)
            {
                return;
            }
        }

        final boolean distanceCheck = MC.thePlayer.getDistanceSq(currentPosition.x + 0.5,
                currentPosition.y + 1,
                currentPosition.z + 0.5) > getReachDistanceSq();
        if (BlockUtil.isReplaceable(currentPosition.x, currentPosition.y, currentPosition.z) || distanceCheck)
        {
            if (currentPosition.sentBreak)
            {
                abortBreakingBlock(currentPosition);
            }

            if (!distanceCheck && rebreakSetting.getValue() && minePositionQueue.isEmpty())
            {
                currentPosition.sentBreak = false;
                currentPosition.sentStop = false;
                currentPosition.progress = 0.0f;
            } else
            {
                currentPosition = null;
            }

            return;
        }

        startBreak();
        if ((currentPosition.progress += getStrength(currentPosition)) >= percentSetting.getValue())
        {
            breakBlock();
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S23PacketBlockChange && currentPosition != null && rebreakSetting.getValue())
        {
            final S23PacketBlockChange packet = event.getPacket();
            if (packet.func_148879_d() == currentPosition.x && packet.func_148878_e() == currentPosition.y && packet.func_148877_f() == currentPosition.z)
            {
                final Block currentBlock = MC.theWorld.getBlock(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f());
                final Block newBlock = packet.func_148880_c();
                if (currentBlock.getMaterial().isReplaceable() && !newBlock.getMaterial().isReplaceable())
                {
                    startBreak();
                } else
                {
                    // queue must go through before rebreak happens
                    if (!minePositionQueue.isEmpty())
                    {
                        currentPosition = null;
                        return;
                    }
                    currentPosition.progress = 0.0f;
                    currentPosition.sentStop = false;
                    currentPosition.sentBreak = false;
                }
            }
        }
    };

    @Subscribe
    private final EventListener<EventAttackBlock> attackBlockEventListener = event ->
    {
        if (MC.playerController.isInCreativeMode())
        {
            return;
        }
        final Block block = MC.theWorld.getBlock(event.getX(), event.getY(), event.getZ());
        if (((IBlock)block).nebula$getBlockHardness() == -1.0f)
        {
            return;
        }

        event.cancel();
        // do not allow duplicates
        if (minePositionQueue.stream().anyMatch((pos) ->
                pos.x == event.getX() && pos.y == event.getY() && pos.z == event.getZ())
                || (currentPosition != null
                    && currentPosition.x == event.getX()
                    && currentPosition.y == event.getY()
                    && currentPosition.z == event.getZ()))
        {
            return;
        }

        minePositionQueue.add(new MinePosition(event.getX(), event.getY(), event.getZ(), event.getSide(), block));
    };

    private void startBreak()
    {
        if (currentPosition.sentBreak)
        {
            return;
        }
        currentPosition.progress = 0.0f;
        currentPosition.sentBreak = true;
        PacketUtil.send(new C07PacketPlayerDigging(
                0,
                currentPosition.x, currentPosition.y, currentPosition.z,
                currentPosition.side));
    }

    private void breakBlock()
    {
        if (currentPosition.sentStop)
        {
            return;
        }
        currentPosition.sentStop = true;
        Nebula.INVENTORY.spoof(getSlot(MC.theWorld.getBlock(currentPosition.x, currentPosition.y, currentPosition.z)));
        PacketUtil.send(new C07PacketPlayerDigging(
                2,
                currentPosition.x, currentPosition.y, currentPosition.z,
                currentPosition.side));
        PacketUtil.send(new C07PacketPlayerDigging(
                2,
                currentPosition.x, currentPosition.y, currentPosition.z,
                currentPosition.side));
        Nebula.INVENTORY.sync();
        if (instantSetting.getValue())
        {
            MC.theWorld.setBlockToAir(currentPosition.x, currentPosition.y, currentPosition.z);
        }
    }

    private int getSlot(final Block block)
    {
        int slot = InventoryUtil.getBestToolSlotFor(block);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            slot = MC.thePlayer.inventory.currentItem;
        }
        return slot;
    }

    private void abortBreakingBlock(final MinePosition minePosition)
    {
        PacketUtil.send(new C07PacketPlayerDigging(
                1, minePosition.x, minePosition.y, minePosition.z, minePosition.side));
    }

    private double getStrength(final MinePosition position)
    {
        final Block block = MC.theWorld.getBlock(position.x, position.y, position.z);
        if (((IBlock)block).nebula$getBlockHardness() < 0.0f)
        {
            return 0.0f;
        }
        final ItemStack itemStack = MC.thePlayer.inventory.getStackInSlot(getSlot(block));
        final double speed = getDestroySpeed(block, itemStack);
        final double factor = ((itemStack != null && itemStack.func_150998_b(block)) || block.getMaterial().isToolNotRequired())
                ? 30.0f
                : 100.0f;
        return speed / ((IBlock)block).nebula$getBlockHardness() / factor;
    }

    private double getDestroySpeed(final Block blockAt, final ItemStack itemStack)
    {
        float breakSpeed = 1.0f;
        if (itemStack != null)
        {
            breakSpeed *= itemStack.func_150997_a(blockAt);
        }

        if (breakSpeed > 1.0f)
        {
            final int efficiency = ItemUtil.getEnchantLevelNoLimit(Enchantment.efficiency, itemStack);
            if (efficiency > 0)
            {
                float mod = efficiency * efficiency + 1.0f;
                breakSpeed += (itemStack.func_150998_b(blockAt) ? mod : mod * 0.08f);
            }
        }

        if (MC.thePlayer.isPotionActive(Potion.digSpeed))
        {
            final int hasteMod = MC.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier();
            if (hasteMod > 0)
            {
                breakSpeed *= 1.0f + (hasteMod + 1.0f) * 0.2f;
            }
        }

        if (MC.thePlayer.isPotionActive(Potion.digSlowdown))
        {
            final int fatigueMod = MC.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier();
            if (fatigueMod > 0)
            {
                breakSpeed *= 1.0f - (fatigueMod + 1.0f) * 0.2f;
            }
        }

        if (MC.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(MC.thePlayer))
        {
            breakSpeed /= 5.0f;
        }

        if (!MC.thePlayer.onGround && !ignoreGroundSetting.getValue())
        {
            breakSpeed /= 5.0f;
        }

        return breakSpeed;
    }

    private float getReachDistanceSq()
    {
        return MC.playerController.getBlockReachDistance() * MC.playerController.getBlockReachDistance();
    }

    @Override
    public String getMetadata()
    {
        if (currentPosition == null || currentPosition.progress < 0.0f)
        {
            return super.getMetadata();
        }
        return String.format("%.1f", (currentPosition.progress * 100.0f)) + "%";
    }

    private static final class MinePosition
    {
        public final Block originalBlock;
        public final int x, y, z, side;
        public double progress;
        public boolean sentBreak, sentStop;

        public MinePosition(int x, int y, int z, int side, Block originalBlock)
        {
            this.originalBlock = originalBlock;
            this.x = x;
            this.y = y;
            this.z = z;
            this.side = side;
        }
    }
}
