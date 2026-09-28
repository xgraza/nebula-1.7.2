package ez.nebula.client.impl.module.combat;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.IEventPriorities;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventPostUpdate;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.player.EventAttackSprint;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.impl.module.player.AutoEatModule;
import ez.nebula.client.impl.module.player.FreecamModule;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import ez.nebula.client.util.minecraft.player.EntityUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.ItemUtil;
import ez.nebula.client.util.render.animation.Animation;
import ez.nebula.client.util.render.animation.AnimationEasing;
import ez.nebula.client.util.text.FormattingUtil;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

import java.util.Comparator;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 03/02/25
 */
@ModuleManifest(name = "KillAura",
        description = "Automatically attacks entities around you",
        category = ModuleCategory.COMBAT)
@RotationPriority(ModuleRotationPriorities.KILL_AURA)
public final class KillAuraModule extends InteractionModule
{
    @ModuleInstance
    public static KillAuraModule INSTANCE;

    private final EnumSetting<Mode> modeSetting = enumBuilder("Mode", Mode.SINGLE)
            .setDescription("How kill aura should select its targets")
            .build();
    private final EnumSetting<Priority> prioritySetting = enumBuilder("Priority", Priority.DISTANCE)
            .setDescription("How kill aura should prioritize its targets")
            .build();
    public final NumberSetting<Float> rangeSetting = numberBuilder("Range", 4.2f)
            .setMin(1.0f)
            .setMax(6.0f)
            .setScale(0.1f)
            .setDescription("The range should the target be attacked from")
            .build();
    private final Setting<Boolean> wallsSetting = builder("Walls", true)
            .setDescription("If attacking the target through solid walls is allowed")
            .build();
    private final EnumSetting<Weapon> weaponSetting = enumBuilder("Weapon", Weapon.NONE)
            .setDescription("What kind of weapon is required to attack the target")
            .build();
    private final Setting<Boolean> prefer32KSetting = builder("Prefer 32k Sword", false)
            .setDescription("If weapon swaps should prioritize 32k swords")
            .setVisibility((value) -> weaponSetting.getValue() == Weapon.SWAP)
            .build();
    private final Setting<Boolean> autoBlockSetting = builder("Auto Block", true)
            .setDescription("If to automatically block and unblock your sword for you")
            .build();
    private final Setting<Boolean> tickSetting = builder("Tick", false)
            .setDescription("If attack delay should be removed")
            .build();
    private final Setting<Boolean> keepSprint = builder("Keep Sprint", false)
            .setDescription("If attacking the target should reset your sprint state")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate towards your target")
            .build();
    private final Setting<Boolean> attackPlayersSetting = builder("Attack Players", true)
            .setDescription("If to target players")
            .build();
    private final Setting<Boolean> attackHostileSetting = builder("Attack Hostile", true)
            .setDescription("If to target hostile mobs")
            .build();
    private final Setting<Boolean> attackPassiveSetting = builder("Attack Passive", true)
            .setDescription("If to attack passive mobs")
            .build();
    private final Setting<Boolean> attackTamedSetting = builder("Attack Tamed", false)
            .setDescription("If to attack tamed animals")
            .build();
    private final Setting<Boolean> renderSetting = builder("Render", false)
            .setDescription("If to render over the target")
            .build();

    private final Animation renderAnimation = new Animation(
            AnimationEasing.CUBIC_IN_OUT, 750.0);

    private final Timer timer = new Timer();
    private EntityLivingBase target;
    private boolean blocking;
    private float[] angles;

    @Override
    public void onDisable()
    {
        super.onDisable();

        if (blocking && autoBlockSetting.getValue())
        {
            blockSword(false);
        }
        blocking = false;
        AutoGGModule.INSTANCE.setLastTarget(null);
        target = null;
        angles = null;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (modeSetting.getValue() == Mode.SWITCH || !isValidEntity(target))
        {
            target = getNextTarget();
        }
        if (target instanceof EntityPlayer)
        {
            AutoGGModule.INSTANCE.setLastTarget((EntityPlayer) target);
        }
        if (target == null || AutoPotModule.INSTANCE.isActive() || AutoEatModule.INSTANCE.isActive())
        {
            if (target == null)
            {
                AutoGGModule.INSTANCE.setLastTarget(null);
            }
            if (blocking)
            {
                blockSword(false);
            }
            return;
        }
        if (!handleWeapon() || !canAttack())
        {
            return;
        }

        if (rotateSetting.getValue() && !rotate(angles))
        {
            return;
        }

        timer.resetTime();
        if (autoBlockSetting.getValue())
        {
            blockSword(false);
        }
        attackTarget();
    };

    @Subscribe
    private final EventListener<EventPostUpdate> moveUpdateEventListener = event ->
    {
        if (autoBlockSetting.getValue() && target != null)
        {
            blockSword(true);
        }
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (target == null)
        {
            return;
        }

        if (rotateSetting.getValue())
        {
            angles = AngleUtil.entityAngles(target, target.getEyeHeight() - 0.2f, event.getPartialTicks());
        }

        if (!renderSetting.getValue())
        {
            return;
        }

        MC.mcProfiler.startSection("killAura");

        glPushMatrix();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 0, 1);
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(2.5f);

        if (renderAnimation.getFactor() >= 1.0 || renderAnimation.getFactor() <= 0.0)
        {
            renderAnimation.setState(!renderAnimation.getState());
        }

        final float hurtTime = target.hurtTime / (float) target.maxHurtTime;
        if (hurtTime > 0.0f)
        {
            glColor4f(0.3f + hurtTime, 0.0f, 0.0f, 1.0f);
        } else
        {
            glColor4f(1.0f - (target.hurtResistantTime / (float) target.maxHurtResistantTime),
                    1.0f, 1.0f, 1.0f);
        }
        //glTranslated(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);

        final double x = target.prevPosX + (target.posX - target.prevPosX) * event.getPartialTicks();
        final double y = target.prevPosY + (target.posY - target.prevPosY) * event.getPartialTicks();
        final double z = target.prevPosZ + (target.posZ - target.prevPosZ) * event.getPartialTicks();

        glBegin(GL_LINE_LOOP);
        {
            final double radius = target.width + 0.2;
            for (double angle = 0.0; angle <= 360.0; angle += 1.0)
            {
                final double rad = Math.toRadians(angle);
                glVertex3d((x + (Math.sin(rad) * radius)) - RenderManager.renderPosX,
                        (y + target.height - (target.height * renderAnimation.getEasedFactor())) - RenderManager.renderPosY,
                        (z - (Math.cos(rad) * radius)) - RenderManager.renderPosZ);
            }
        }
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();

        MC.mcProfiler.endSection();
    };

    @Subscribe(priority = IEventPriorities.HIGHEST)
    private final EventListener<EventAttackSprint> attackSprintEventListener = event ->
    {
        if (keepSprint.getValue() && event.getEntity().equals(target))
        {
            event.setSlowdown(false);
            event.cancel();
        }
    };

    private void attackTarget()
    {
        Entity attackEntity = target;
        if (attackEntity instanceof EntityDragon)
        {
            final EntityDragon dragon = (EntityDragon) attackEntity;

            double dist = -1;
            EntityDragonPart attackPart = null;
            for (final EntityDragonPart part : dragon.dragonPartArray)
            {
                final double d = MC.thePlayer.getDistanceToEntity(part);
                if (dist == -1 || d < dist)
                {
                    dist = d;
                    attackPart = part;
                }
            }

            if (attackPart == null)
            {
                attackPart = dragon.dragonPartHead;
            }
            attackEntity = attackPart;
        }

        attack(attackEntity, false);
    }

    private boolean canAttack()
    {
        if (tickSetting.getValue())
        {
            return true;
        }
        return timer.hasElapsed((long) (50L + (Math.random() * 150)))
                || target.hurtResistantTime <= 0;
    }

    private void blockSword(final boolean block)
    {
        final ItemStack itemStack = MC.thePlayer.getHeldItem();
        if (itemStack == null || !(itemStack.getItem() instanceof ItemSword))
        {
            blocking = false;
            return;
        }

        if (block)
        {
            blocking = true;
            MC.playerController.sendUseItem(MC.thePlayer, MC.theWorld, itemStack);
        } else
        {
            blocking = false;
            PacketUtil.send(new C07PacketPlayerDigging(
                    5, 0, 0, 0, 255));
        }
    }

    private boolean handleWeapon()
    {
        final ItemStack itemStack = MC.thePlayer.getHeldItem();
        if (blocking && (itemStack == null || !(itemStack.getItem() instanceof ItemSword)))
        {
            blocking = false;
        }

        switch (weaponSetting.getValue())
        {
            case SWAP:
            {
                float maxSwordScore = 0.0f;
                int slot = InventoryUtil.INVALID_SLOT;
                for (int i = 0; i < 9; ++i)
                {
                    final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(i);
                    if (stack == null || !(stack.getItem() instanceof ItemSword))
                    {
                        continue;
                    }
                    final float score = ItemUtil.getSwordScore(
                            stack, prefer32KSetting.getValue());
                    if (score > maxSwordScore)
                    {
                        maxSwordScore = score;
                        slot = i;
                    }
                }
                if (slot != InventoryUtil.INVALID_SLOT)
                {
                    MC.thePlayer.inventory.currentItem = slot;
                    return true;
                }
                return false;
            }
            case REQUIRE:
            {
                if (itemStack == null)
                {
                    return false;
                }
                return itemStack.getItem() instanceof ItemSword;
            }
            default:
                return true;
        }
    }

    @SuppressWarnings("unchecked")
    private EntityLivingBase getNextTarget()
    {
        return (EntityLivingBase) MC.theWorld.loadedEntityList
                .stream()
                .filter((entity) -> entity instanceof EntityLivingBase
                        && isValidEntity((EntityLivingBase) entity))
                .min(Comparator.comparingDouble((entity)
                        -> prioritySetting.getValue().test((EntityLivingBase) entity)))
                .orElse(null);
    }

    private boolean isValidEntity(final EntityLivingBase entity)
    {
        if (entity == null
                || entity.getHealth() <= 0.0f
                || entity.isDead
                || entity.equals(MC.thePlayer)
                || entity.getEntityId() == FreecamModule.CAMERA_ENTITY_ID
                || entity.equals(MC.thePlayer.ridingEntity))
        {
            return false;
        }
        final double distanceSq = MC.thePlayer.getDistanceSqToEntity(entity);
        if (distanceSq > rangeSetting.getValue() * rangeSetting.getValue())
        {
            return false;
        }
        if (entity instanceof EntityPlayer && Nebula.FRIENDS.has((EntityPlayer) entity) && !NoFriendsModule.INSTANCE.isToggled())
        {
            return false;
        }
        if (!wallsSetting.getValue() && !MC.thePlayer.canEntityBeSeen(entity))
        {
            return false;
        }
        if (!attackPlayersSetting.getValue() && entity instanceof EntityPlayer)
        {
            return false;
        }
        if (!attackHostileSetting.getValue() && EntityUtil.isEntityHostile(entity))
        {
            return false;
        }
        if (!attackPassiveSetting.getValue() && EntityUtil.isEntityPassive(entity))
        {
            return false;
        }
        if (!attackTamedSetting.getValue())
        {
            if (entity instanceof EntityTameable && ((EntityTameable) entity).isTamed())
            {
                return false;
            }
            if (entity instanceof EntityHorse && ((EntityHorse) entity).isTame())
            {
                return false;
            }
        }
        return true;
    }

    public boolean isBlocking()
    {
        return blocking;
    }

    public boolean isAttacking()
    {
        return target != null;
    }

    public EntityLivingBase getTarget()
    {
        return target;
    }

    @Override
    public String getMetadata()
    {
        return FormattingUtil.formatEnum(modeSetting.getValue());
    }

    private enum Mode
    {
        SINGLE, SWITCH
    }

    private enum Priority
    {
        DISTANCE((entity) -> MC.thePlayer.getDistanceSqToEntity(entity)),
        HEALTH((entity) -> (double) (entity.getHealth() + entity.getAbsorptionAmount())),
        ARMOR((entity) -> (double) entity.getTotalArmorValue());

        private final Function<EntityLivingBase, Double> sortFunction;

        Priority(final Function<EntityLivingBase, Double> sortFunction)
        {
            this.sortFunction = sortFunction;
        }

        public double test(final EntityLivingBase entity)
        {
            return sortFunction.apply(entity);
        }
    }

    private enum Weapon
    {
        NONE, SWAP, REQUIRE
    }
}
