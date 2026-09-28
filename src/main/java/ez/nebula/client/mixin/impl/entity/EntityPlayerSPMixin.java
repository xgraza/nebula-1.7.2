package ez.nebula.client.mixin.impl.entity;

import ez.nebula.client.mixin.duck.IEntityPlayerSP;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = EntityPlayerSP.class)
public class EntityPlayerSPMixin implements IEntityPlayerSP
{
    @Shadow private int horseJumpPowerCounter;

    @Override
    public void nebula$setHorseJumpPowerCounter(int i)
    {
        horseJumpPowerCounter = i;
    }
}
