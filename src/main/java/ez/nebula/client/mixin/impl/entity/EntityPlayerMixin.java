package ez.nebula.client.mixin.impl.entity;

import ez.nebula.client.mixin.duck.IEntityPlayer;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = EntityPlayer.class)
public class EntityPlayerMixin implements IEntityPlayer
{
    @Shadow private int itemInUseCount;

    @Override
    public void nebula$setItemInUseCount(int i)
    {
        itemInUseCount = i;
    }
}
