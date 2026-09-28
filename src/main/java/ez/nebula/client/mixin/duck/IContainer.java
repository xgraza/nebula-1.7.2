package ez.nebula.client.mixin.duck;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IContainer
{
    void nebula$setTransactionId(short s);

    short nebula$getTransactionId();
}
