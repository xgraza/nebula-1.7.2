package ez.nebula.client.mixin.common.util;

import ez.nebula.client.mixin.duck.IRegistryNamespaced;
import net.minecraft.util.RegistryNamespaced;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * @author xgraza
 * @since 9/25/26
 */
@Mixin(value = RegistryNamespaced.class)
public class RegistryNamespacedMixin implements IRegistryNamespaced
{
    @Shadow @Final protected Map field_148758_b;

    @Override
    public Map nebula$getMap()
    {
        return field_148758_b;
    }
}
