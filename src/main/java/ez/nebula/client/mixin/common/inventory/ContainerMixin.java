package ez.nebula.client.mixin.common.inventory;

import ez.nebula.client.mixin.duck.IContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = Container.class)
public class ContainerMixin implements IContainer
{
    @Shadow private short transactionID;

    @Override
    public void nebula$setTransactionId(short s)
    {
        transactionID = s;
    }

    @Override
    public short nebula$getTransactionId()
    {
        return transactionID;
    }
}
