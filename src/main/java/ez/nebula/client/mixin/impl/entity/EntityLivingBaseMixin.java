package ez.nebula.client.mixin.impl.entity;

import ez.nebula.client.mixin.duck.IEntityLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = EntityLivingBase.class)
public class EntityLivingBaseMixin implements IEntityLivingBase
{
    @Unique private float renderPitch, prevRenderPitch;

    @Shadow private int jumpTicks;

    @Override
    public void nebula$setJumpTicks(int i)
    {
        jumpTicks = i;
    }

    @Override
    public float nebula$getRenderPitch()
    {
        return renderPitch;
    }

    @Override
    public void nebula$setRenderPitch(float f)
    {
        renderPitch = f;
    }

    @Override
    public float nebula$getPrevRenderPitch()
    {
        return prevRenderPitch;
    }

    @Override
    public void nebula$setPrevRenderPitch(float f)
    {
        prevRenderPitch = f;
    }
}
