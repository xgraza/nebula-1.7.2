package ez.nebula.client.mixin.impl.render;

import ez.nebula.client.mixin.duck.IRenderItem;
import net.minecraft.client.renderer.entity.RenderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = RenderItem.class)
public abstract class RenderItemMixin implements IRenderItem
{
    @Shadow protected abstract void renderGlint(int par1, int par2, int par3, int par4, int par5);

    @Override
    public void nebula$renderGlint(int p_77018_1_, int p_77018_2_, int p_77018_3_, int p_77018_4_, int p_77018_5_)
    {
        renderGlint(p_77018_1_, p_77018_2_, p_77018_3_, p_77018_4_, p_77018_5_);
    }
}
