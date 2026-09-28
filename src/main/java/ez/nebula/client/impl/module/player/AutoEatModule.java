package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventBindStopUse;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.player.EventItemUseFinish;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.combat.AutoBedModule;
import ez.nebula.client.impl.module.combat.AutoPotModule;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

/**
 * @author xgraza
 * @since 06/27/26
 */
@ModuleManifest(name = "AutoEat",
        description = "Automatically eats food or golden apples when you need it",
        category = ModuleCategory.PLAYER)
public final class AutoEatModule extends Module
{
    @ModuleInstance
    public static AutoEatModule INSTANCE;

    private final Setting<Boolean> gappleSetting = builder("Golden Apple", true)
            .setDescription("Automatically eat golden apples when your absorption hearts deplete")
            .build();
    private final NumberSetting<Integer> hungerSetting = numberBuilder("Hunger", 18)
            .setMin(1)
            .setMax(19)
            .setScale(1)
            .setDescription("At what hunger level to eat food")
            .build();
    public final Setting<Boolean> prioritzeCombatSetting = builder("Prioritize Combat", true)
            .setDescription("If to not eat when using a combat module")
            .build();

    private int prevSlot = InventoryUtil.INVALID_SLOT;
    private boolean use, sentUse;

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (prevSlot != InventoryUtil.INVALID_SLOT && MC.thePlayer != null)
        {
            MC.thePlayer.inventory.currentItem = prevSlot;
        }
        prevSlot = InventoryUtil.INVALID_SLOT;
        use = false;
        sentUse = false;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (prioritzeCombatSetting.getValue() && (KillAuraModule.INSTANCE.isAttacking() || AutoBedModule.INSTANCE.isActive()))
        {
            use = sentUse = false;
            return;
        }

        // do not override AutoPot trying to throw healing pots
        if (AutoPotModule.INSTANCE.isActive() && AutoPotModule.INSTANCE.isLowHealth())
        {
            use = sentUse = false;
            return;
        }

        final int slot = getFoodSlot();
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            if (prevSlot != InventoryUtil.INVALID_SLOT)
            {
                MC.thePlayer.inventory.currentItem = prevSlot;
                prevSlot = InventoryUtil.INVALID_SLOT;
            }
            use = sentUse = false;
            return;
        }
        use = true;
        if (slot != MC.thePlayer.inventory.currentItem)
        {
            sentUse = false;
            if (prevSlot == InventoryUtil.INVALID_SLOT)
            {
                prevSlot = MC.thePlayer.inventory.currentItem;
            }
            MC.thePlayer.inventory.currentItem = slot;
            return;
        }
        // from Minecraft.java
        if (!sentUse || !MC.thePlayer.isUsingItem())
        {
            // ChatUtil.sendNebula("started to use item");
            MC.playerController.sendUseItem(MC.thePlayer, MC.theWorld, MC.thePlayer.getHeldItem());
            sentUse = true;
            MC.entityRenderer.itemRenderer.resetEquippedProgress();
        }
    };

    @Subscribe
    private final EventListener<EventBindStopUse> bindStopUseEventListener = event ->
    {
        if (use || sentUse)
        {
            event.cancel();
        }
    };

    @Subscribe
    private final EventListener<EventItemUseFinish> itemUseFinishEventListener = event ->
    {
        // ChatUtil.sendNebula("finished using");
        sentUse = false;
    };

    private int getFoodSlot()
    {
        if (!shouldEat())
        {
            return InventoryUtil.INVALID_SLOT;
        }
        ItemStack stack = null;
        int slot = InventoryUtil.INVALID_SLOT;
        for (int i = 0; i < 9; ++i)
        {
            final ItemStack itemStack = MC.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null || !(itemStack.getItem() instanceof ItemFood))
            {
                continue;
            }
            final ItemFood food = (ItemFood) itemStack.getItem();
            if (needsGapple())
            {
                if (food instanceof ItemAppleGold)
                {
                    if (stack == null || stack.getItemDamage() < itemStack.getItemDamage())
                    {
                        stack = itemStack;
                        slot = i;
                    }
                }
                continue;
            }
            if (stack == null || food.func_150905_g(itemStack) > ((ItemFood)stack.getItem()).func_150905_g(stack))
            {
                stack = itemStack;
                slot = i;
            }
        }
        return slot;
    }

    private boolean shouldEat()
    {
        return MC.thePlayer.getFoodStats().getFoodLevel() <= hungerSetting.getValue() || needsGapple();
    }

    private boolean needsGapple()
    {
        return gappleSetting.getValue() && MC.thePlayer.getAbsorptionAmount() < 4.0f;
    }

    @Override
    public boolean isActive()
    {
        return super.isActive() && use;
    }
}
