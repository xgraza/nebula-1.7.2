package ez.nebula.client.mixin.impl.render.gui;

import ez.nebula.client.mixin.duck.IGuiContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = GuiContainer.class)
public abstract class GuiContainerMixin implements IGuiContainer
{
    @Shadow public Container inventorySlots;

    @Shadow protected abstract void handleMouseClick(Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_);

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
