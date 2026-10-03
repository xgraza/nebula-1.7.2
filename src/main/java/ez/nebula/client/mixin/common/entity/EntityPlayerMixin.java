package ez.nebula.client.mixin.common.entity;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.player.EventJump;
import ez.nebula.client.mixin.duck.IEntityPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = EntityPlayer.class)
public class EntityPlayerMixin implements IEntityPlayer
{
    @Shadow private int itemInUseCount;

    @Redirect(method = "getBreakSpeed(Lnet/minecraft/block/Block;ZIIII)F", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hook$getBreakSpeed$getCurrentItem(InventoryPlayer instance)
    {
        return Nebula.INVENTORY.stack();
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void hook$jump(CallbackInfo info)
    {
        if ((EntityPlayer) (Object) this instanceof EntityPlayerSP && EventBus.dispatch(new EventJump()))
        {
            info.cancel();
        }
    }

    @Override
    public void nebula$setItemInUseCount(int i)
    {
        itemInUseCount = i;
    }
}
