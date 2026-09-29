package ez.nebula.client.mixin.impl.render;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.render.EventRenderWaterEffects;
import ez.nebula.client.impl.module.combat.KillAuraModule;
import ez.nebula.client.impl.module.render.NoRenderModule;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin
{
    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItemUseAction()Lnet/minecraft/item/EnumAction;", ordinal = 1))
    private EnumAction redirect$renderItemInFirstPerson$getItemUseAction$KillAuraHook(ItemStack instance)
    {
        if (KillAuraModule.INSTANCE.isBlocking())
        {
            return EnumAction.block;
        }
        return instance.getItemUseAction();
    }

    @Inject(method = "renderWarpedTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void hook$renderWarpedTextureOverlay$event(float v, CallbackInfo info)
    {
        if (EventBus.dispatch(new EventRenderWaterEffects()))
        {
            info.cancel();
        }
    }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void hook$renderFireInFirstPerson$NoRenderHook(float v, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.fireSetting.getValue())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderInsideOfBlock", at = @At("HEAD"), cancellable = true)
    private void hook$renderInsideOfBlock$NoRenderHook(float v, IIcon p_78446_1_, CallbackInfo info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.blockSetting.getValue())
        {
            info.cancel();
        }
    }
}
