package ez.nebula.client.mixin.duck;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IGuiContainer
{
    void nebula$handleMouseClick(Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_);

    Container nebula$getContainer();
}
