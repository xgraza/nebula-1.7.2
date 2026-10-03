package ez.nebula.client.mixin.common.network.packet.client;

import ez.nebula.client.mixin.duck.IC0DPacketCloseWindow;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = C0DPacketCloseWindow.class)
public class C0DPacketCloseWindowMixin implements IC0DPacketCloseWindow
{
    @Shadow private int field_149556_a;

    @Override
    public int nebula$getWindowID()
    {
        return field_149556_a;
    }
}
