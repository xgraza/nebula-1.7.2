package ez.nebula.client.util.minecraft.player;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xgraza
 * @since 03/26/25
 */
public final class ItemUtil
{
    private static final Map<Enchantment, Float> TOOL_ENCHANTMENTS = new HashMap<>();
    private static final Map<Enchantment, Float> SWORD_ENCHANTMENTS = new HashMap<>();

    static
    {
        // tools
        TOOL_ENCHANTMENTS.put(Enchantment.unbreaking, 1.5f);
        TOOL_ENCHANTMENTS.put(Enchantment.looting, 1.5f);
        TOOL_ENCHANTMENTS.put(Enchantment.fortune, 1.5f);
        TOOL_ENCHANTMENTS.put(Enchantment.efficiency, 1.2f);

        // sword
        SWORD_ENCHANTMENTS.put(Enchantment.sharpness, 1.8f);
        SWORD_ENCHANTMENTS.put(Enchantment.unbreaking, 1.5f);
        SWORD_ENCHANTMENTS.put(Enchantment.looting, 1.3f);
        SWORD_ENCHANTMENTS.put(Enchantment.baneOfArthropods, 1.05f);
        SWORD_ENCHANTMENTS.put(Enchantment.smite, 1.05f);
    }

    public static float getSwordScore(final ItemStack itemStack, final boolean _32k)
    {
        if (itemStack == null || !(itemStack.getItem() instanceof ItemSword))
        {
            return 0.0f;
        }
        return getEnchantScore(((ItemSword) itemStack.getItem()).func_150931_i() + 4.0f,
                SWORD_ENCHANTMENTS, itemStack, _32k);
    }

    public static float getToolScore(final ItemStack itemStack, final Block attackedBlock)
    {
        float damage = itemStack.func_150997_a(attackedBlock);
        if (damage <= 1.0f)
        {
            return 0.0f;
        }
        return getEnchantScore(damage, TOOL_ENCHANTMENTS, itemStack, true);
    }

    public static float getEnchantScore(float score,
                                        final Map<Enchantment, Float> map,
                                        final ItemStack itemStack,
                                        final boolean _32k)
    {
        for (final Enchantment enchantment : map.keySet())
        {
            score += (_32k
                    ? getEnchantLevelNoLimit(enchantment, itemStack)
                    : getEnchantLevel(enchantment, itemStack)) * map.get(enchantment);
        }
        return score;
    }

    public static int getEnchantLevelNoLimit(final Enchantment enchantment, final ItemStack itemStack)
    {
        return EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, itemStack);
    }

    public static int getEnchantLevel(final Enchantment enchantment, final ItemStack itemStack)
    {
        return Math.min(getEnchantLevelNoLimit(enchantment, itemStack),
                enchantment.getMaxLevel());
    }

    public static boolean isIllegal(final ItemStack itemStack)
    {
        return isInfinite(itemStack) || is32k(itemStack);
    }

    public static boolean isInfinite(final ItemStack itemStack)
    {
        return itemStack != null && itemStack.stackSize < 0;
    }

    @SuppressWarnings("unchecked")
    public static boolean is32k(final ItemStack itemStack)
    {
        return itemStack != null
                && EnchantmentHelper.getEnchantments(itemStack)
                .keySet()
                .stream()
                .anyMatch((x) -> (short)x >= Short.MAX_VALUE);
    }
}
