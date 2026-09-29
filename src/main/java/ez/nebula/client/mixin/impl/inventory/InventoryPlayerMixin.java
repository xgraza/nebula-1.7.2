package ez.nebula.client.mixin.impl.inventory;

import ez.nebula.client.Nebula;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(InventoryPlayer.class)
public final class InventoryPlayerMixin
{
    /**
     * @author xgraza
     * @reason shit mc code
     */
    @Overwrite
    public float func_146023_a(Block p_146023_1_)
    {
        float f = 1.0F;
        final ItemStack stack = Nebula.INVENTORY.stack();
        if (stack != null)
        {
            f *= stack.func_150997_a(p_146023_1_);
        }
        return f;
    }
}
