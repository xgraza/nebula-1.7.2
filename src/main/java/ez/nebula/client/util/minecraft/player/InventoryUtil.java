package ez.nebula.client.util.minecraft.player;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

/**
 * @author xgraza
 * @since 03/01/25
 */
public final class InventoryUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static final int PLAYER_INVENTORY_WINDOW_ID = 0;

    public static final int INVALID_SLOT = -1;
    public static final int PLAYER_INVENTORY_SIZE = 36;
    public static final int PLAYER_INVENTORY_ARMOR_SIZE = 4;
    public static final int HOTBAR_SLOTS = 9;

    /**
     * When in the player inventory, if you want to access the hotbar slots, the index for each hotbar slot is id + 36
     * We must convert this slot into that for window clicks
     * @param slot the slot in the inventory
     * @return the correct slot
     */
    public static int toPacketSlot(final int slot)
    {
        return slot < HOTBAR_SLOTS ? slot + PLAYER_INVENTORY_SIZE : slot;
    }

    public static void windowClick(final int slot, final ClickType clickType)
    {
        windowClick(PLAYER_INVENTORY_WINDOW_ID, slot, clickType);
    }

    public static void windowClick(final int windowID, final int slot, final ClickType clickType)
    {
        MC.playerController.windowClick(windowID, slot, clickType.mouseButton, clickType.action, MC.thePlayer);
    }

    public static int getBestToolSlotFor(final Block attackedBlock)
    {
        float bestScore = 1.0f;
        int slot = -1;
        for (int i = 0; i < 9; ++i)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(i);
            if (stack == null)
            {
                continue;
            }
            final float score = ItemUtil.getToolScore(stack, attackedBlock);
            if (score > bestScore)
            {
                bestScore = score;
                slot = i;
            }
        }
        return slot;
    }

    @SafeVarargs
    public static int getHotbarItem(final Class<? extends Item>... items)
    {
        if (items.length == 0)
        {
            return INVALID_SLOT;
        }
        for (int slot = 0; slot < HOTBAR_SLOTS; ++slot)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null)
            {
                continue;
            }
            for (final Class<? extends Item> type : items)
            {
                if (type.isAssignableFrom(stack.getItem().getClass()))
                {
                    return slot;
                }
            }
        }
        return INVALID_SLOT;
    }

    public static int getHotbarItem(final Item... items)
    {
        if (items.length == 0)
        {
            return INVALID_SLOT;
        }
        for (int slot = 0; slot < HOTBAR_SLOTS; ++slot)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null)
            {
                continue;
            }
            for (final Item item : items)
            {
                if (stack.getItem() == item)
                {
                    return slot;
                }
            }
        }
        return INVALID_SLOT;
    }

    public static int getHotbarBlock(final Block... blocks)
    {
        if (blocks.length == 0)
        {
            return INVALID_SLOT;
        }
        for (int slot = 0; slot < HOTBAR_SLOTS; ++slot)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || !(stack.getItem() instanceof ItemBlock))
            {
                continue;
            }
            final Block itemBlock = ((ItemBlock) stack.getItem()).field_150939_a;
            for (final Block block : blocks)
            {
                if (block == itemBlock)
                {
                    return slot;
                }
            }
        }
        return INVALID_SLOT;
    }

    public static int getHotbarSlot(final Predicate<ItemStack> filter)
    {
        return getSlot(0, HOTBAR_SLOTS, filter);
    }

    public static int getSlot(final int start,
                              final int end,
                              final Predicate<ItemStack> filter)
    {
        for (int slot = start; slot < end; ++slot)
        {
            final ItemStack stack = MC.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && filter.test(stack))
            {
                return slot;
            }
        }
        return INVALID_SLOT;
    }

    public static boolean hasEnough(final ItemStack stack, final int count)
    {
        if (stack == null || stack.stackSize == 0)
        {
            return false;
        }
        if (MC.thePlayer.capabilities.isCreativeMode || ItemUtil.isInfinite(stack))
        {
            return true;
        }
        return count <= stack.stackSize;
    }

    public static boolean isValidHotbarSlot(final int slot)
    {
        return slot > 8 || slot < 0;
    }

    public enum ClickType
    {
        /**
         * Simple one finger click, picks up an item in an inventory
         */
        PICKUP(0, 0),
        /**
         * If you do not have an item picked up in the inventory, this will split the stack you're over
         * If you have an item picked up, it will put one of that item into whatever slot
         */
        RIGHT_CLICK(1, 0),
        /**
         * If when clicking an item to "quick move" it - equivalent to Left Shift + Click
         */
        SHIFT_CLICK(0, 1),
        /**
         * If to drop one item of the stack - equivalent to pressing Q
         */
        DROP_ONE(0, 4),
        /**
         * If to drop the entire stack you're - equivalent to pressing Command/Ctrl + Q
         */
        DROP_ALL(1, 4),
        /**
         * If in creative and you middle click (or three-finger click) it will duplicate that item into a stack of 64 (or max)
         */
        DUPLICATE(2, 0);

        private final int mouseButton, action;

        ClickType(int mouseButton, int action)
        {
            this.mouseButton = mouseButton;
            this.action = action;
        }
    }
}
