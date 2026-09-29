package ez.nebula.client.mixin.impl.render.gui;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.player.EventContainerAction;
import ez.nebula.client.mixin.duck.IGuiContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = GuiContainer.class)
public abstract class GuiContainerMixin implements IGuiContainer
{
    @Shadow public Container inventorySlots;

    @Shadow protected abstract void handleMouseClick(Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_);

    @Redirect(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirect$(PlayerControllerMP instance, int id, int slot, int mb, int action, EntityPlayer player, Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_)
    {
        final EventContainerAction event = new EventContainerAction(id, slot, mb, action, p_146984_1_);
        if (EventBus.dispatch(event))
        {
            return null;
        }
        return instance.windowClick(event.getWindowId(), event.getSlotIndex(), event.getMouseButton(), event.getAction(), player);
    }

    @Override
    public void nebula$handleMouseClick(Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_)
    {
        handleMouseClick(p_146984_1_, p_146984_2_, p_146984_3_, p_146984_4_);
    }

    @Override
    public Container nebula$getContainer()
    {
        return inventorySlots;
    }
}
