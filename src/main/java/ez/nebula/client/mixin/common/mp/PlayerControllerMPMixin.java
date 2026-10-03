package ez.nebula.client.mixin.common.mp;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.world.EventPlace;
import ez.nebula.client.mixin.duck.IPlayerControllerMP;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = PlayerControllerMP.class)
public abstract class PlayerControllerMPMixin implements IPlayerControllerMP
{
    @Shadow private int blockHitDelay;
    @Shadow private float curBlockDamageMP;
    @Shadow private WorldSettings.GameType currentGameType;

    @Shadow protected abstract boolean sameToolAndBlock(int par1, int par2, int par3);

    @Shadow private int currentPlayerItem;

    @Inject(method = "onPlayerRightClick", at = @At("HEAD"))
    private void hook$onPlayerRightClick$placeEvent(EntityPlayer player, World world, ItemStack stack, int x, int y, int z, int side, Vec3 vec, CallbackInfoReturnable<Boolean> info)
    {
        EventBus.dispatch(new EventPlace(x, y, z, side, stack, vec));
    }

    @Redirect(method = "onPlayerRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirect$onPlayerRightClick$getCurrentItem(InventoryPlayer instance, EntityPlayer p_78760_1_, World p_78760_2_, ItemStack p_78760_3_, int p_78760_4_, int p_78760_5_, int p_78760_6_, int p_78760_7_, Vec3 p_78760_8_)
    {
        return p_78760_3_;
    }

    @Redirect(method = "sendUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirect$sendUseItem$getCurrentItem(InventoryPlayer instance, EntityPlayer p_78769_1_, World p_78769_2_, ItemStack p_78769_3_)
    {
        return p_78769_3_;
    }

    @Redirect(method = "sendUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I", opcode = Opcodes.GETFIELD))
    private int redirect$sendUseItem$currentItem(InventoryPlayer instance)
    {
        return Nebula.INVENTORY.slot();
    }

    @Redirect(method = "clickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getHeldItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirect$(EntityClientPlayerMP instance)
    {
        return Nebula.INVENTORY.stack();
    }

    @Override
    public boolean nebula$sameToolAndBlock(int x, int y, int z)
    {
        return sameToolAndBlock(x, y, z);
    }

    @Override
    public WorldSettings.GameType nebula$getCurrentGameType()
    {
        return currentGameType;
    }

    @Override
    public float nebula$getCurBlockDamageMP()
    {
        return curBlockDamageMP;
    }

    @Override
    public void nebula$setBlockHitDelay(int delay)
    {
        blockHitDelay = delay;
    }

    @Override
    public void nebula$setCurrentPlayerItem(int i)
    {
        currentPlayerItem = i;
    }
}
