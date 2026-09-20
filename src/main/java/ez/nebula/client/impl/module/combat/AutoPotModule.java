package ez.nebula.client.impl.module.combat;

import com.google.common.collect.Lists;
import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.impl.module.player.AutoEatModule;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.MoveUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xgraza
 * @since 9/10/26
 */
@ModuleManifest(name = "AutoPot",
        description = "Automatically throws potions",
        category = ModuleCategory.COMBAT)
@RotationPriority(ModuleRotationPriorities.AUTO_POT)
public final class AutoPotModule extends InteractionModule
{
    @ModuleInstance
    public static AutoPotModule INSTANCE;

    private static final int MAX_DURATION_TICKS_NEVER_RUNOUT = 200; // 10 seconds
    private static final List<Integer> ALLOWED_POTION_EFFECTS = Lists.newArrayList(
            Potion.heal.getId(),
            Potion.moveSpeed.getId(),
            Potion.damageBoost.getId(),
            Potion.digSpeed.getId(),
            Potion.resistance.getId(),
            Potion.fireResistance.getId(),
            Potion.waterBreathing.getId(),
            Potion.healthBoost.getId(),
            Potion.saturation.getId(),
            Potion.regeneration.getId());

    private final NumberSetting<Integer> delaySetting = numberBuilder("Delay", 400)
            .setMin(0)
            .setMax(1000)
            .setScale(1)
            .setDescription("The minimum delay in MS to wait before the next throw pot")
            .build();
    private final NumberSetting<Float> healthSetting = numberBuilder("Health", 6.0f)
            .setMin(1.0f)
            .setMax(19.5f)
            .setScale(0.5f)
            .setDescription("At what health should instant healing potions be thrown")
            .build();
    private final Setting<Boolean> ignoreIllegalSetting = builder("Ignore Illegals", true)
            .setDescription("If to ignore illegal throw potions (i.e. over max level, over max duration)")
            .build();
    private final Setting<Boolean> neverRunOutSetting = builder("Never Run Out", false)
            .setDescription("If to throw a potion before the effect runs out")
            .build();
    private final Setting<Boolean> prioritizeAttackingSetting = builder("Prioritize Combat", false)
            .setDescription("If to prioritize attacking (i.e. KillAura, AutoBed) over throwing potions")
            .build();

    private final Timer timer = new Timer();
    private final List<Integer> expectedPotionEffects = new ArrayList<>();
    private int thrownPot = InventoryUtil.INVALID_SLOT;
    private boolean thrown;

    @Override
    public void onDisable()
    {
        super.onDisable();
        expectedPotionEffects.clear();
        thrownPot = InventoryUtil.INVALID_SLOT;
        thrown = false;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (thrownPot != InventoryUtil.INVALID_SLOT)
        {
            if (thrown && timer.hasElapsed((long) (delaySetting.getValue() + Nebula.SERVER.scaledLatency())))
            {
                thrown = false;
                thrownPot = InventoryUtil.INVALID_SLOT;
                expectedPotionEffects.clear();
            }
            return;
        }

        // we cannot override AutoBed, as we need to spoof rotations to properly place the bed in the direction we want
        if (AutoBedModule.INSTANCE.isActive() || MC.thePlayer.ridingEntity != null)
        {
            return;
        }

        // if we are not trying to save our life, we can allow auto eat to override us
        if (AutoEatModule.INSTANCE.isActive() && !isLowHealth())
        {
            return;
        }

        // do not try to pot above water or if we're falling
        if (PlayerUtil.isAboveWater() || MC.thePlayer.fallDistance > 3.0f)
        {
            return;
        }

        if ((prioritizeAttackingSetting.getValue() && (KillAuraModule.INSTANCE.isAttacking() || AutoBedModule.INSTANCE.isActive()))
                // allow us to finish the action we're doing
                || MC.thePlayer.getItemInUse() != null)
        {
            thrownPot = InventoryUtil.INVALID_SLOT;
            return;
        }

        thrownPot = getPotionSlot();
        if (thrownPot == InventoryUtil.INVALID_SLOT)
        {
            thrown = false;
            return;
        }

        if (!thrown)
        {
            thrown = true;
            timer.resetTime();
            queue(calculatePotAngles(), (rotation) -> use(thrownPot));
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (MC.thePlayer == null || MC.theWorld == null)
        {
            return;
        }
        if (event.getPacket() instanceof S1DPacketEntityEffect && thrown && !expectedPotionEffects.isEmpty())
        {
            final S1DPacketEntityEffect packet = event.getPacket();
            if (packet.getEntityId() == MC.thePlayer.getEntityId() && expectedPotionEffects.contains((int) packet.getPotionId()))
            {
                thrown = false;
                thrownPot = InventoryUtil.INVALID_SLOT;
            }
        }
    };

    private int getPotionSlot()
    {
        int slot = InventoryUtil.INVALID_SLOT;
        List<PotionEffect> effectList = null;
        float scale = 0.0f;

        for (int i = 0; i < InventoryUtil.HOTBAR_SLOTS; ++i)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemPotion) || !ItemPotion.isSplash(stack.getItemDamage()))
            {
                continue;
            }
            final ItemPotion itemPotion = (ItemPotion) stack.getItem();
            final List<PotionEffect> potionEffects = itemPotion.getEffects(stack);

            float score = 0.0f;
            for (final PotionEffect effect : potionEffects)
            {
                final int id = effect.getPotionID();
                if (!ALLOWED_POTION_EFFECTS.contains(id)
                        // Alfheim has a patch where if the potion has a greater amplifier than normally allowed, it will not apply :(
                        || (ignoreIllegalSetting.getValue() && isPotionIllegal(effect)))
                {
                    score = -1.0f;
                    break;
                }

                if (id == Potion.heal.getId())
                {
                    // if we are desperately low, ignore finding another effect and focus on healing
                    if (isLowHealth())
                    {
                        return i;
                    } else
                    {
                        score = -1.0f;
                        break;
                    }
                }

                final int amplifier = isPotionApplied(id)
                        ? MC.thePlayer.getActivePotionEffect(id).getAmplifier()
                        : -1;
                if (amplifier < effect.getAmplifier())
                {
                    score += effect.getAmplifier() + 1;
                }
            }

            if (score <= 0.0f)
            {
                continue;
            }

            if (slot == InventoryUtil.INVALID_SLOT || score > scale)
            {
                effectList = potionEffects;
                slot = i;
                scale = score;
            }
        }

        if (slot != -1)
        {
            expectedPotionEffects.clear();
            for (final PotionEffect effect : effectList)
            {
                expectedPotionEffects.add(effect.getPotionID());
            }
        }

        return slot;
    }

    public boolean isLowHealth()
    {
        return MC.thePlayer.getHealth() <= healthSetting.getValue();
    }

    private boolean isPotionApplied(final int id)
    {
        if (!MC.thePlayer.isPotionActive(id))
        {
            return false;
        }
        if (neverRunOutSetting.getValue())
        {
            final PotionEffect effect = MC.thePlayer.getActivePotionEffect(id);
            return effect != null && effect.getDuration() >= MAX_DURATION_TICKS_NEVER_RUNOUT;
        }
        return true;
    }

    private boolean isPotionIllegal(final PotionEffect effect)
    {
        if (effect.getAmplifier() < 0)
        {
            return true;
        }
        final Potion potion = Potion.potionTypes[effect.getPotionID()];
        if (potion == Potion.fireResistance
                || potion == Potion.weakness
                || potion == Potion.moveSlowdown
                || potion == Potion.waterBreathing
                || potion == Potion.nightVision)
        {
            return effect.getAmplifier() != 0;
        }
        return effect.getAmplifier() > 1;
    }

    private float[] calculatePotAngles()
    {
        final float[] angles = new float[2];
        angles[0] = MC.thePlayer.rotationYaw;
        angles[1] = 90.0f;

        final double speed = Nebula.MOVEMENT.getMotionSpeed();
        if (speed > 1.0E-4)
        {
            angles[0] = (float) Math.toDegrees(Math.atan2(-MC.thePlayer.motionX, MC.thePlayer.motionZ));
            angles[1] -= (float) (speed * 250);
        } else
        {
            angles[1] = -90.0f;
        }

        if (Math.abs(MC.thePlayer.motionY) > 1.0E-4)
        {
            angles[1] += (float) (MC.thePlayer.motionY * 20.0);
        }

        angles[1] = MathHelper.clamp_float(angles[1], -90.0f, 90.0f);
        return angles;
    }

    @Override
    public boolean isActive()
    {
        return super.isActive() && (thrown || thrownPot != InventoryUtil.INVALID_SLOT);
    }
}
