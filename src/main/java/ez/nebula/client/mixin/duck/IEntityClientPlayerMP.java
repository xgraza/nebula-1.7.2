package ez.nebula.client.mixin.duck;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IEntityClientPlayerMP
{
    void nebula$setWasSneaking(boolean bl);

    void nebula$setWasSprinting(boolean bl);

    boolean nebula$getWasSprinting();

    int nebula$getGroundTicks();

    int nebula$getAirTicks();
}
