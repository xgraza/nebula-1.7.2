package ez.nebula.client.impl.module.world;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IGuiContainer;
import ez.nebula.client.util.math.MathUtil;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * @author xgraza
 * @since 05/26/26
 */
@ModuleManifest(name = "ChestStealer",
        description = "Automatically steals items from open chests",
        category = ModuleCategory.WORLD)
public final class ChestStealerModule extends Module
{
    @ModuleInstance
    public static ChestStealerModule INSTANCE;

    private static final String ENDER_CHEST_TRANSLATION_KEY = "container.enderchest";

    private final NumberSetting<Integer> delaySetting = numberBuilder("Delay", 100)
            .setMin(0)
            .setMax(1500)
            .setScale(1)
            .setDescription("How long in milliseconds to wait before clicking another item")
            .build();
    public final Setting<Boolean> automaticSetting = builder("Automatic", true)
            .setDescription("If to automatically steal from a container")
            .build();
    private final Setting<Boolean> randomOrderSetting = builder("Random Order", false)
            .setDescription("If to grab items in a random order")
            .build();
    private final Setting<Boolean> enderChestSetting = builder("Ender Chest", false)
            .setDescription("If to allow stealing/storing in an Ender Chest")
            .build();

    public final Timer timer = new Timer();

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (!automaticSetting.getValue()
                || !hasSpaceInLocalInventory()
                || !(MC.thePlayer.openContainer instanceof ContainerChest))
        {
            return;
        }
        moveItemsFromInventory(false);
    };

    public boolean moveItemsFromInventory(boolean store)
    {
        final Container container = MC.thePlayer.openContainer;
        if (!canStealFromOpenContainer(container))
        {
            return false;
        }
        final IInventory inventory = getInventory(container);
        if (!timer.hasElapsed((long)delaySetting.getValue()))
        {
            return true;
        }

        int slot = getNextStealSlot(store ? MC.thePlayer.inventory : inventory);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return false;
        }
        timer.resetTime();
        if (store)
        {
            if (slot >= 9)
            {
                slot -= 9;
            } else
            {
                slot += 27;
            }
            slot += getSize(inventory);
        }

        // use this instead of PlayerControllerMP#windowClick for compat with inf items
        ((IGuiContainer) MC.currentScreen).nebula$handleMouseClick(null, slot, 0, 1);
        return true;
    }

    public int getSize(final IInventory inventory)
    {
        return inventory instanceof InventoryPlayer ? 9 + 27 : inventory.getSizeInventory();
    }

    public IInventory getInventory(final Container container)
    {
        if (!(container instanceof ContainerChest || container instanceof ContainerPlayer))
        {
            return null;
        }
        if (container instanceof ContainerPlayer)
        {
            return MC.thePlayer.inventory;
        }
        return ((ContainerChest) container).getLowerChestInventory();
    }

    public boolean hasSpaceInLocalInventory()
    {
        return hasSpaceInInventory(9 + 27, MC.thePlayer.inventory);
    }

    public boolean hasSpaceInInventory(final int maxSlots, final IInventory inventory)
    {
        for (int i = 0; i < maxSlots; ++i)
        {
            final ItemStack itemStack = inventory.getStackInSlot(i);
            if (itemStack != null)
            {
                return true;
            }
        }
        return false;
    }

    public int getNextStealSlot(final IInventory inventory)
    {
        final int size = getSize(inventory) - 1;
        int slot = InventoryUtil.INVALID_SLOT;
        while (true)
        {
            slot = randomOrderSetting.getValue()
                    ? MathUtil.random(0, size)
                    : slot + 1;
            if (slot > size)
            {
                return InventoryUtil.INVALID_SLOT;
            }
            final ItemStack itemStack = inventory.getStackInSlot(slot);
            if (itemStack == null)
            {
                continue;
            }
            return slot;
        }
    }

    public boolean canStealFromOpenContainer(final Container container)
    {
        if (container instanceof ContainerPlayer)
        {
            return true;
        }
        if (!(container instanceof ContainerChest))
        {
            return false;
        }
        final IInventory inventory = ((ContainerChest) container).getLowerChestInventory();
        if (!enderChestSetting.getValue()
                && inventory.getInventoryName().equals(ENDER_CHEST_TRANSLATION_KEY))
        {
            return false;
        }
        return true;
    }
}
