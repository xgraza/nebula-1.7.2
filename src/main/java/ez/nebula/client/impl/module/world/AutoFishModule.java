package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.ItemUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S29PacketSoundEffect;

/**
 * @author xgraza
 * @since 06/24/25
 */
@ModuleManifest(name = "AutoFish",
        description = "Automatically uses a rod to catch items from a body of water",
        category = ModuleCategory.WORLD)
public final class AutoFishModule extends Module
{
    private static final String RANDOM_SPLASH = "random.splash";

    private final Setting<Boolean> autoCastSetting = builder("Auto Cast", true)
            .setDescription("If to automatically cast your fishing rod")
            .build();
    private final Setting<Boolean> autoSwapSetting = builder("Auto Swap", true)
            .setDescription("If to automatically swap to the best fishing rod")
            .build();

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (autoSwapSetting.getValue())
        {
            final int slot = getFishingHookItem();
            if (slot != InventoryUtil.INVALID_SLOT)
            {
                MC.thePlayer.inventory.currentItem = slot;
            }
        }

        if (isNotHoldingRod())
        {
            return;
        }

        if (autoCastSetting.getValue()
                && MC.thePlayer.fishEntity == null
                && MC.thePlayer.ticksExisted % 10 == 0)
        {
            cast();
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (MC.thePlayer == null || MC.thePlayer.fishEntity == null || isNotHoldingRod())
        {
            return;
        }
        if (event.getPacket() instanceof S29PacketSoundEffect)
        {
            final S29PacketSoundEffect packet = event.getPacket();
            if (!packet.func_149212_c().equals(RANDOM_SPLASH))
            {
                return;
            }
            final EntityFishHook entity = MC.thePlayer.fishEntity;
            if (entity.getDistance(packet.func_149207_d(), packet.func_149211_e(), packet.func_149210_f()) <= 1.0)
            {
                cast();
            }
        }
    };

    private void cast()
    {
        MC.playerController.sendUseItem(MC.thePlayer, MC.theWorld, MC.thePlayer.getHeldItem());
        Nebula.INTERACTIONS.swingItem();
    }

    private boolean isNotHoldingRod()
    {
        return MC.thePlayer.getHeldItem() == null
                || !(MC.thePlayer.getHeldItem().getItem() instanceof ItemFishingRod);
    }

    private int getFishingHookItem()
    {
        int slot = InventoryUtil.INVALID_SLOT;
        float score = 1.0f;

        for (int i = 0; i < 9; ++i)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemFishingRod))
            {
                continue;
            }
            float itemScore = 0.0f;

            itemScore += ItemUtil.getEnchantLevelNoLimit(Enchantment.unbreaking, stack) * 1.5f;
            itemScore += ItemUtil.getEnchantLevelNoLimit(Enchantment.field_151370_z, stack) * 5.5f;
            itemScore += ItemUtil.getEnchantLevelNoLimit(Enchantment.field_151369_A, stack) * 2.2f;

            if (itemScore > score || slot == -1)
            {
                slot = i;
                score = itemScore;
            }
        }

        return slot;
    }
}
