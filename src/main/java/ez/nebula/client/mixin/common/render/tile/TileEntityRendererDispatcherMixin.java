package ez.nebula.client.mixin.common.render.tile;

import ez.nebula.client.impl.module.render.ChamsModule;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin
{
    @Unique private boolean nebula$renderChams;

    @Inject(method = "renderTileEntityAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDF)V", shift = At.Shift.BEFORE))
    private void hook$renderTileEntityAt$ChamsPre(TileEntity p_147549_1_, double p_147549_2_, double p_147549_4_, double p_147549_6_, float p_147549_8_, CallbackInfo ci)
    {
        if (ChamsModule.INSTANCE.isToggled() && ChamsModule.INSTANCE.isTileEntityValid(p_147549_1_))
        {
            ChamsModule.INSTANCE.preEntityRender();
            nebula$renderChams = true;
        }
    }

    @Inject(method = "renderTileEntityAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDF)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hook$renderTileEntityAt$ChamsPost(TileEntity p_147549_1_, double p_147549_2_, double p_147549_4_, double p_147549_6_, float p_147549_8_, CallbackInfo ci, TileEntitySpecialRenderer var9)
    {
        if (nebula$renderChams && ChamsModule.INSTANCE.postEntityRender())
        {
            var9.renderTileEntityAt(p_147549_1_, p_147549_2_, p_147549_4_, p_147549_6_, p_147549_8_);
        }
        nebula$renderChams = false;
    }
}
