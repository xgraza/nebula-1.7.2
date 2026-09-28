package ez.nebula.client.mixin.duck;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IEntityLivingBase
{
    void nebula$setJumpTicks(int i);

    float nebula$getRenderPitch();

    void nebula$setRenderPitch(float f);

    float nebula$getPrevRenderPitch();

    void nebula$setPrevRenderPitch(float f);
}
