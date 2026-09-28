package ez.nebula.client.mixin.impl.entity;

import ez.nebula.client.mixin.duck.IEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = Entity.class)
public class EntityMixin implements IEntity
{
    @Shadow protected boolean isInWeb;
    @Shadow protected boolean inPortal;
    @Shadow private int fire;

    @Override
    public boolean nebula$getIsInWeb()
    {
        return isInWeb;
    }

    @Override
    public boolean nebula$getInPoral()
    {
        return inPortal;
    }

    @Override
    public int nebula$getFire()
    {
        return fire;
    }
}
