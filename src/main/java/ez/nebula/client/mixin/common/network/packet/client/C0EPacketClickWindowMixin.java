package ez.nebula.client.mixin.common.network.packet.client;

import ez.nebula.client.mixin.duck.IC0EPacketClickWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/25/26
 */
@Mixin(value = C0EPacketClickWindow.class)
public class C0EPacketClickWindowMixin implements IC0EPacketClickWindow
{
    @Shadow private int field_149554_a;

    @Override
    public void nebula$setWindowID(int i)
    {
        field_149554_a = i;
    }
}
