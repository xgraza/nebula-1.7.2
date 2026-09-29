package ez.nebula.client.mixin.impl.render.tile;

import ez.nebula.client.impl.module.render.NoRenderModule;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TileEntitySignRenderer.class)
public class TileEntitySignRendererMixin
{
    @Unique private static final String[] EMPTY_ARR = new String[0];

    @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySign;DDDF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/tileentity/TileEntitySign;signText:[Ljava/lang/String;", opcode = Opcodes.GETFIELD))
    private String[] redirect$(TileEntitySign instance)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.signTextSetting.getValue())
        {
            return EMPTY_ARR;
        }
        return instance.signText;
    }
}
