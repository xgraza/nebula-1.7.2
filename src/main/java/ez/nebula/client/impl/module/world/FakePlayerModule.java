package ez.nebula.client.impl.module.world;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IEntityLivingBase;
import ez.nebula.client.util.math.MathUtil;
import ez.nebula.client.util.minecraft.world.DamageUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author xgraza
 * @since 06/06/25
 */
@ModuleManifest(name = "FakePlayer",
        description = "Spawns a fake player to test things on",
        category = ModuleCategory.WORLD)
public final class FakePlayerModule extends Module
{
    @ModuleInstance
    public static FakePlayerModule INSTANCE;

    private static final List<String> FAKE_USERNAMES = Lists.newArrayList(
            "Aestheticall", "epearl", "hometea", "iWoodz", "EstrogenInjector");
    private static final int FAKE_ENTITY_ID = -1337420;

    private final Setting<Boolean> takeDamageSetting = builder("Take Damage", false)
            .setDescription("If to allow the fake player to take damage like a real player")
            .build();
    private final Setting<Boolean> gapChugSetting = builder("Gap Chug", false)
            .setDescription("If to continuously eat golden apples to regenerate health")
            .setVisibility((value) -> takeDamageSetting.getValue())
            .build();
    private final Setting<Boolean> moveSetting = builder("Move", false)
            .setDescription("If to allow the fake player to move")
            .build();
    private final Setting<Boolean> recordSetting = builder("Start Recording", false)
            .setDescription("If to start recording movement")
            .setVisibility((value) -> moveSetting.getValue())
            .build();

    private final Queue<Movement> fakePlayerMovement = new ConcurrentLinkedQueue<>();
    private Movement lastRecordedMovement;
    private boolean shownRecordScreen;

    private EntityOtherPlayerMP fakePlayerEntity;

    @Override
    public void onEnable()
    {
        super.onEnable();
        if (MC.thePlayer == null || MC.theWorld == null)
        {
            setToggled(false);
        }
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.theWorld != null)
        {
            despawnFP();
        }

        fakePlayerMovement.clear();
        lastRecordedMovement = null;
        shownRecordScreen = false;
        recordSetting.setValue(false);
    }

    @Override
    public String getMetadata()
    {
        if (recordSetting.getValue())
        {
            return "Recording";
        }
        if (!fakePlayerMovement.isEmpty() && moveSetting.getValue())
        {
            return "Moving (" + fakePlayerMovement.size() + ")";
        }
        return super.getMetadata();
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (MC.thePlayer.ticksExisted < 20)
        {
            return;
        }

        if (recordSetting.getValue())
        {
            if (!shownRecordScreen)
            {
                if (MC.currentScreen != null)
                {
                    MC.displayGuiScreen(null);
                }

                lastRecordedMovement = null;
                shownRecordScreen = true;
                fakePlayerMovement.clear();

                notifyInfo("Begin moving now, sneak to end the recording", 7500L);
            }

            if (MC.gameSettings.keyBindSneak.getIsKeyPressed())
            {
                notifyInfo("Finished recording! Recorded " + fakePlayerMovement.size() + " movements", 7500L);
                lastRecordedMovement = null;
                recordSetting.setValue(false);
                return;
            }

            final Movement movement = new Movement(MC.thePlayer.posX, MC.thePlayer.boundingBox.minY, MC.thePlayer.posZ,
                    MC.thePlayer.rotationYaw, MC.thePlayer.rotationPitch);
            if (Objects.equals(movement, lastRecordedMovement))
            {
                return;
            }
            lastRecordedMovement = movement;
            fakePlayerMovement.add(movement);

            return;
        }

        if (isFakePlayerInvalidated())
        {
            fakePlayerMovement.clear();
            if (fakePlayerEntity != null)
            {
                MC.theWorld.removeEntityFromWorld(FAKE_ENTITY_ID);
                MC.theWorld.removePlayerEntityDangerously(fakePlayerEntity);
            }
            if (MC.thePlayer.ticksExisted < 20)
            {
                return;
            }
            fakePlayerEntity = createFakePlayer();
            MC.theWorld.addEntityToWorld(FAKE_ENTITY_ID, fakePlayerEntity);
        }

        if (moveSetting.getValue() && fakePlayerEntity != null && !fakePlayerMovement.isEmpty())
        {
            final Movement movement = fakePlayerMovement.poll();
            if (movement == null)
            {
                // should never be null, as we add this one to the end of the stack but...
                return;
            }

            fakePlayerEntity.setPositionAndRotation(movement.x, movement.y, movement.z, movement.yaw, movement.pitch);
            fakePlayerEntity.rotationYawHead = movement.yaw;
            ((IEntityLivingBase)fakePlayerEntity).nebula$setRenderPitch(movement.pitch);

            fakePlayerMovement.add(movement);
        }
    };

    @Subscribe(priority = IEventPriorities.HIGHEST)
    private final EventListener<EventPacket.Outbound> outboundEventListener = event ->
    {
        if (event.getPacket() instanceof C02PacketUseEntity)
        {
            final C02PacketUseEntity packet = event.getPacket();
            final Entity entity = packet.func_149564_a(MC.theWorld);
            if (entity != null && entity.getEntityId() == FAKE_ENTITY_ID)
            {
                event.cancel();
            }
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (fakePlayerEntity == null || !takeDamageSetting.getValue())
        {
            return;
        }
        if (event.getPacket() instanceof S27PacketExplosion)
        {
            final S27PacketExplosion packet = event.getPacket();
            double x = packet.func_149148_f();
            double y = packet.func_149143_g();
            double z = packet.func_149145_h();
            final double distance = fakePlayerEntity.getDistance(x, y, z);
            if (distance / packet.func_149146_i() < 1.0f)
            {
                float damage = DamageUtil.getExplosionDamage(fakePlayerEntity, x, y, z, packet.func_149146_i(), packet.func_149146_i() * 2);
                fakePlayerEntity.attackEntityFrom(DamageSource.setExplosionSource(new Explosion(MC.theWorld, MC.thePlayer, x, y, z, packet.func_149146_i())), damage);
            }
        }
    };

    private void despawnFP()
    {
        if (fakePlayerEntity == null)
        {
            return;
        }
        MC.theWorld.removeEntityFromWorld(FAKE_ENTITY_ID);
        MC.theWorld.removePlayerEntityDangerously(fakePlayerEntity);
        fakePlayerEntity = null;
    }

    public void critFake()
    {
        if (isToggled() && fakePlayerEntity != null)
        {
            MC.thePlayer.onCriticalHit(fakePlayerEntity);
        }
    }

    private EntityOtherPlayerMP createFakePlayer()
    {
        final EntityPlayerSP local = MC.thePlayer;
        final GameProfile gameProfile = new GameProfile(
                "", FAKE_USERNAMES.get(MathUtil.random(0, FAKE_USERNAMES.size() - 1)));
        return new EntityOtherPlayerMP(MC.theWorld, gameProfile)
        {
            {
                setEntityId(FAKE_ENTITY_ID);
                dimension = local.dimension;
                setLocationAndAngles(
                        local.posX, local.boundingBox.minY, local.posZ,
                        local.rotationYaw, local.rotationPitch);
                inventory.copyInventory(local.inventory);
            }

            @Override
            public void onLivingUpdate()
            {
                super.onLivingUpdate();

                if (getHealth() == 0.0f)
                {
                    setHealth(20.0f);
                }

                if (gapChugSetting.getValue() && local.ticksExisted % 120 == 0)
                {
                    inventory.mainInventory[0] = new ItemStack(Items.golden_apple);

                    setHealth(20.0f);
                    setAbsorptionAmount(4.0f);
                    addPotionEffect(new PotionEffect(Potion.field_76444_x.id, 2400, 0));
                }
            }

            @Override
            public boolean attackEntityFrom(final DamageSource src, float damage)
            {
                if (!takeDamageSetting.getValue())
                {
                    return false;
                }

                entityAge = 0;

                if (getHealth() <= 0.0f)
                {
                    return false;
                }

                if (src.isDifficultyScaled())
                {
                    switch (worldObj.difficultySetting)
                    {
                        case PEACEFUL:
                        {
                            damage = 0.0f;
                            break;
                        }
                        case EASY:
                        {
                            damage = damage / 2.0f + 1.0f;
                            break;
                        }
                        case HARD:
                        {
                            damage = damage * 3.0f / 2.0f;
                            break;
                        }
                    }
                }

                if (damage <= 0.0f)
                {
                    return false;
                }

                limbSwingAmount = 1.5f;

                boolean dealtFullDamage = true;
                if (hurtResistantTime > maxHurtResistantTime / 2.0f)
                {
                    if (damage <= lastDamage)
                    {
                        return false;
                    }

                    damageEntity(src, damage - lastDamage);
                    lastDamage = damage;
                    dealtFullDamage = false;
                } else
                {
                    lastDamage = damage;
                    prevHealth = getHealth();
                    hurtResistantTime = maxHurtResistantTime;
                    damageEntity(src, damage);
                    hurtTime = maxHurtTime = 10;
                }

                isDead = false;
                if (getHealth() <= 0.0f)
                {
                    // EventBus.dispatch(new EventPlayerDeath(this));
                    if (dealtFullDamage)
                    {
                        playSound(getDeathSound(), getSoundVolume(), getSoundPitch());
                    }
                    setHealth(20.0f);
                } else
                {
                    if (dealtFullDamage)
                    {
                        playSound(getHurtSound(), getSoundVolume(), getSoundPitch());
                    }
                }

                return true;
            }
        };
    }

    private boolean isFakePlayerInvalidated()
    {
        return fakePlayerEntity == null || fakePlayerEntity.isDead || fakePlayerEntity.dimension != MC.thePlayer.dimension;
    }

    public int getFakePlayerEntityID()
    {
        return fakePlayerEntity == null ? -1 : fakePlayerEntity.getEntityId();
    }

    private static final class Movement
    {
        private final double x, y, z;
        private final float yaw, pitch;

        public Movement(double x, double y, double z, float yaw, float pitch)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public int hashCode()
        {
            return (int) ((y + z * 31) * 31 + x + (yaw + pitch * 31));
        }
    }
}
