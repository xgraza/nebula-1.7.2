package ez.nebula.client.mixin.impl.settings;

import ez.nebula.client.mixin.duck.IKeyBinding;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = KeyBinding.class)
public class KeyBindingMixin implements IKeyBinding
{
    @Shadow private boolean pressed;

    @Override
    public void nebula$setPressed(boolean bl)
    {
        pressed = bl;
    }
}
