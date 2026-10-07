package ez.nebula.client.mixin.common.forge;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.FMLCommonHandler;
import ez.nebula.client.Nebula;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = FMLCommonHandler.class, remap = false)
public class FMLCommonHandlerMixin
{
    @Inject(method = "computeBranding", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;add(Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList$Builder;", ordinal = 5, shift = At.Shift.BEFORE, remap = false), remap = false, locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hook$computeBranding$addNebula(CallbackInfo ci, ImmutableList.Builder<String> brd, String forgeBranding, int tModCount, int aModCount)
    {
        brd.add("Nebula " + Nebula.VERSION);
    }
}
