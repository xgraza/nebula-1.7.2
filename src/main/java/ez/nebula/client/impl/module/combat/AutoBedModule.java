package ez.nebula.client.impl.module.combat;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.impl.module.player.AutoEatModule;
import ez.nebula.client.impl.module.player.FreecamModule;
import ez.nebula.client.impl.module.render.NameProtectModule;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.minecraft.world.DamageUtil;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBed;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Queue;

/**
 * @author xgraza
 * @since 06/06/25
 */
@ModuleManifest(name = "AutoBed",
        description = "Automatically places and breaks beds to damage another player",
        category = ModuleCategory.COMBAT)
@RotationPriority(ModuleRotationPriorities.AUTO_BED)
public final class AutoBedModule extends InteractionModule
{
    @ModuleInstance
    public static AutoBedModule INSTANCE;

    private static final double BED_EXPLOSION_SIZE = 5.0;
    private static final float BED_EXPLOSION_STRENGTH = 10.0f;

    private final NumberSetting<Float> rangeSetting = numberBuilder("Range", 4.2f)
            .setMin(1.0f)
            .setMax(6.0f)
            .setScale(0.1f)
            .setDescription("The range should the target be attacked from")
            .build();
    private final NumberSetting<Integer> yRangeSetting = numberBuilder("Y-Range", 1)
            .setMin(1)
            .setMax(5)
            .setScale(1)
            .setDescription("How many y-levels to explore for place positions")
            .build();
    private final Setting<Boolean> extinguishFireSetting = builder("Extinguish Fire", true)
            .setDescription("If to extinguish fire before placing a bed")
            .build();
    private final Setting<Boolean> packetPlaceSetting = builder("Packet Place", false)
            .setDescription("If to place on packet")
            .build();
    private final Setting<Boolean> packetBreakSetting = builder("Packet Break", false)
            .setDescription("If to break on packet")
            .build();

    private final NumberSetting<Float> minDamageSetting = numberBuilder("Min Damage", 6.0f)
            .setMin(1.0f)
            .setMax(19.5f)
            .setScale(0.1f)
            .setDescription("The absolute minimum damage a position needs to be considered")
            .build();
    private final Setting<Boolean> averageDamageSetting = builder("Average Damage", true)
            .setDescription("If to average the blast damage between the head and feet block of the bed")
            .build();

    private final Setting<Boolean> suicideSetting = builder("Suicide", false)
            .setDescription("If to ignore local player damage when finding a bed place position")
            .build();
    private final NumberSetting<Float> lethalHealthSetting = numberBuilder("Lethal Health", 12.0f)
            .setMin(2.0f)
            .setMax(19.5f)
            .setScale(0.1f)
            .setDescription("The maximum amount of damage allowed to the local player")
            .setVisibility((value) -> !suicideSetting.getValue())
            .build();
    private final NumberSetting<Float> lethalMultiplierSetting = numberBuilder("Lethal Multiplier", 1.2f)
            .setMin(1.0f)
            .setMax(2.0f)
            .setScale(0.1f)
            .setDescription("The multiplier to the local damage")
            .setVisibility((value) -> !suicideSetting.getValue())
            .build();
    private final NumberSetting<Float> protectHealthSetting = numberBuilder("Protect Health", 8.0f)
            .setMin(1.0f)
            .setMax(19.5f)
            .setScale(0.5f)
            .setDescription("The minimum amount of health you must have to continue")
            .build();
    private final NumberSetting<Float> swapPenaltySetting = numberBuilder("Swap Penalty", 2.0f)
            .setMin(0.5f)
            .setMax(12.0f)
            .setScale(0.1f)
            .setDescription("The minimum damage difference between the current position and a new position to be considered")
            .build();

    private final Setting<Boolean> renderSetting = builder("Render", true)
            .setDescription("If to render the bed placement position")
            .build();

    private BedBlockInfo blockInfo;
    private int bedSlot = InventoryUtil.INVALID_SLOT;
    private EntityPlayer target;

    @Override
    public void onDisable()
    {
        super.onDisable();
        AutoGGModule.INSTANCE.setLastTarget(null);
        target = null;
        blockInfo = null;
        bedSlot = InventoryUtil.INVALID_SLOT;
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (blockInfo == null || target == null || !renderSetting.getValue())
        {
            return;
        }
        MC.mcProfiler.startSection("autoBed");
        final AxisAlignedBB bb = IAxisAlignedBB.create(blockInfo.getPos())
                .addCoord(blockInfo.getFacing().getFrontOffsetX(),
                        blockInfo.getFacing().getFrontOffsetY(),
                        blockInfo.getFacing().getFrontOffsetZ());
        bb.maxY = blockInfo.getPos().getY() + 0.5;
        Render3D.outlinedAABB(bb, 1.5f, QuadMask.ALL_FACES, 0xFFFF0000);
        Render3D.filledAABB(bb, QuadMask.ALL_FACES, 0x80FF0000);
        final Vec3 center = ((IAxisAlignedBB)bb).nebula$getCenter();
        Render3D.billboard(center.xCoord, center.yCoord, center.zCoord, 0.2, () ->
        {
            final String text = String.format("%.2f", blockInfo.getTargetDamage());
            MC.fontRenderer.drawStringWithShadow(text, -MC.fontRenderer.getStringWidth(text) / 2, 2, -1);
        });
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (isInOverworld())
        {
            if (blockInfo != null || target != null)
            {
                Nebula.INVENTORY.sync();
            }
            target = null;
            blockInfo = null;
            return;
        }
        if (!isValidEntity(target))
        {
            AutoGGModule.INSTANCE.setLastTarget(null);
            target = getPlayerInRange();
            return;
        }
        AutoGGModule.INSTANCE.setLastTarget(target);
        bedSlot = InventoryUtil.getHotbarItem(ItemBed.class);
        if (bedSlot == InventoryUtil.INVALID_SLOT)
        {
            if (blockInfo != null)
            {
                Nebula.INVENTORY.sync();
            }
            blockInfo = null;
            return;
        }
        if (MC.thePlayer.getHealth() <= protectHealthSetting.getValue())
        {
            if (blockInfo != null)
            {
                Nebula.INVENTORY.sync();
            }
            blockInfo = null;
            return;
        }
        calculatePlacePosition();
        if (blockInfo == null)
        {
            return;
        }

        if (AutoEatModule.INSTANCE.isActive() || AutoPotModule.INSTANCE.isActive())
        {
            return;
        }

        if (extinguishFireSetting.getValue())
        {
            final BlockPos pos1 = blockInfo.getPos();
            final BlockPos pos2 = blockInfo.getPos().offset(blockInfo.getFacing());

            if (BlockUtil.isFire(pos1))
            {
                click(pos1, EnumFacing.UP);
            }

            if (BlockUtil.isFire(pos2))
            {
                click(pos2, EnumFacing.UP);
            }
        }

        tryPlaceBreakBed();
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (MC.thePlayer == null || MC.theWorld == null)
        {
            return;
        }
        if (event.getPacket() instanceof S23PacketBlockChange && blockInfo != null)
        {
            final S23PacketBlockChange packet = event.getPacket();
            final BlockPos placePos = blockInfo.getPos();
            final BlockPos pos = new BlockPos(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f());
            if (!placePos.equals(pos))
            {
                return;
            }

            final float damage = calcDamage(MC.thePlayer, pos) * lethalMultiplierSetting.getValue();
            final float damage2 = calcDamage(MC.thePlayer, pos.offset(blockInfo.getFacing())) * lethalMultiplierSetting.getValue();
            if (!suicideSetting.getValue() && (damage + damage2) * 0.5f >= lethalHealthSetting.getValue())
            {
                return;
            }

            if (packet.func_148880_c() instanceof BlockBed)
            {
                if (!packetBreakSetting.getValue())
                {
                    return;
                }
                PacketUtil.send(new C08PacketPlayerBlockPlacement(
                        packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f(),
                        EnumFacing.UP.ordinal(),
                        null,
                        0.5f, 0.5f, 0.5f));
                swing();
            } else if (packet.func_148880_c() instanceof BlockAir)
            {
                if (!packetPlaceSetting.getValue())
                {
                    return;
                }
                MC.theWorld.setBlockToAir(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f());
                tryPlaceBreakBed();
            }
        }
    };

    private void tryPlaceBreakBed()
    {
        if (bedSlot == InventoryUtil.INVALID_SLOT || blockInfo == null)
        {
            return;
        }
        if (rotate(BlockUtil.getHorizontalFacing(blockInfo.getFacing()) * 90.0f, 0.0f))
        {
            Nebula.INVENTORY.spoof(bedSlot);
            if (place(blockInfo.getPos().down(), EnumFacing.UP))
            {
                Nebula.INVENTORY.sync();
                place(blockInfo.getPos(), EnumFacing.UP);
            }
        }
    }

    private void calculatePlacePosition()
    {
        if (isInOverworld())
        {
            blockInfo = null;
            return;
        }
        final BlockPos origin = PlayerUtil.getOrigin(target);
        final Queue<BlockInfo> placements = getPlacements(origin);
        if (placements.isEmpty())
        {
            return;
        }

        BedBlockInfo info = null;
        while (!placements.isEmpty())
        {
            final BlockInfo placeInfo = placements.poll();
            if (placeInfo == null)
            {
                break;
            }

            final BlockPos bedOrigin = placeInfo.getPos();
            final BlockPos bedNeighbor = placeInfo.getPos().offset(placeInfo.getFacing());

            final float targetDmg1 = calcDamage(target, bedOrigin);
            final float targetDmg2 = calcDamage(target, bedNeighbor);
            final float damage = (targetDmg1 + targetDmg2) * 0.5f;
            if (damage < minDamageSetting.getValue())
            {
                continue;
            }

            if (blockInfo != null && blockInfo.getTargetDamage() < damage && damage - blockInfo.getTargetDamage() <= swapPenaltySetting.getValue())
            {
                continue;
            }

            // average both block positions with both local & target damage...
            float localDamage = 0.0f;
            if (!suicideSetting.getValue() && !MC.thePlayer.capabilities.isCreativeMode)
            {
                final float lethal = lethalHealthSetting.getValue();
                final float lethalMulti = lethalMultiplierSetting.getValue();
                final float localDmg1 = calcDamage(MC.thePlayer, bedOrigin) * lethalMulti;
                final float localDmg2 = calcDamage(MC.thePlayer, bedNeighbor) * lethalMulti;
                if (localDmg1 >= lethal || localDmg2 >= lethal)
                {
                    continue;
                }
                localDamage = averageDamageSetting.getValue() ? (localDmg1 + localDmg2) * 0.5f : localDmg1;
                if (localDamage >= lethal || localDamage >= damage)
                {
                    continue;
                }
            }
            if (info == null || info.getTargetDamage() < damage)
            {
                info = new BedBlockInfo(placeInfo.getPos(), placeInfo.getFacing());
                info.setTargetDamage(damage);
                info.setLocalDamage(localDamage);
            }
        }
        blockInfo = info;
    }

    private Queue<BlockInfo> getPlacements(final BlockPos pos)
    {
        final Queue<BlockInfo> placements = new ArrayDeque<>();
        for (final BlockPos offset : BlockUtil.RADIAL_BLOCK_MAP.get(rangeSetting.getValue().intValue()))
        {
            if (offset.getY() < -1 || offset.getY() > yRangeSetting.getValue())
            {
                continue;
            }
            final BlockPos neighbor = pos.add(offset);
            final EnumFacing face = getBedPlaceDirection(neighbor);
            if (face != null)
            {
                placements.add(new BlockInfo(neighbor, face));
            }
        }
        return placements;
    }

    private EnumFacing getBedPlaceDirection(final BlockPos pos)
    {
        if (BlockUtil.isNotAir(pos) || BlockUtil.isReplaceable(pos.down()))
        {
            return null;
        }
        for (final EnumFacing facing : EnumFacing.values())
        {
            final BlockPos n = pos.offset(facing);
            if (BlockUtil.isNotAir(n) || BlockUtil.isReplaceable(n.down()))
            {
                continue;
            }
            return facing;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private EntityPlayer getPlayerInRange()
    {
        return (EntityPlayer) MC.theWorld.loadedEntityList
                .stream()
                .filter((entity) -> entity instanceof EntityPlayer
                        && isValidEntity((EntityLivingBase) entity))
                .min(Comparator.comparingDouble((entity)
                        -> ((Entity)entity).getDistanceToEntity(MC.thePlayer)))
                .orElse(null);
    }

    private boolean isValidEntity(final EntityLivingBase entity)
    {
        if (!(entity instanceof EntityPlayer)
                || entity.getHealth() <= 0.0f
                || entity.isDead
                || entity.equals(MC.thePlayer)
                || entity.getEntityId() == FreecamModule.CAMERA_ENTITY_ID)
        {
            return false;
        }
        final double distanceSq = MC.thePlayer.getDistanceSqToEntity(entity);
        if (distanceSq > rangeSetting.getValue() * rangeSetting.getValue())
        {
            return false;
        }
        return NoFriendsModule.INSTANCE.isToggled() || !Nebula.FRIENDS.has((EntityPlayer) entity);
    }

    private float calcDamage(final EntityPlayer entity, final BlockPos pos)
    {
        return DamageUtil.getExplosionDamage(entity, pos, BED_EXPLOSION_SIZE, BED_EXPLOSION_STRENGTH);
    }

    private boolean isInOverworld()
    {
        return MC.thePlayer.dimension == 0;
    }

    public EntityPlayer getTarget()
    {
        return target;
    }

    @Override
    public boolean isActive()
    {
        return super.isActive() && target != null && blockInfo != null;
    }

    @Override
    public String getMetadata()
    {
        if (target != null)
        {
            return NameProtectModule.INSTANCE.protect(target.getCommandSenderName());
        }
        return super.getMetadata();
    }

    private static final class BedBlockInfo extends BlockInfo
    {
        private float targetDamage = 1.0f, localDamage;

        public BedBlockInfo(BlockPos pos, EnumFacing facing)
        {
            super(pos, facing);
        }

        public float getTargetDamage()
        {
            return targetDamage;
        }

        public void setTargetDamage(float targetDamage)
        {
            this.targetDamage = targetDamage;
        }

        public float getLocalDamage()
        {
            return localDamage;
        }

        public void setLocalDamage(float localDamage)
        {
            this.localDamage = localDamage;
        }
    }
}
