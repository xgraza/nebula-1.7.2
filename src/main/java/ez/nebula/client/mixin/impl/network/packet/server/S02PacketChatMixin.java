package ez.nebula.client.mixin.impl.network.packet.server;

import ez.nebula.client.mixin.duck.IS02PacketChat;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = S02PacketChat.class)
public class S02PacketChatMixin implements IS02PacketChat
{
    @Shadow private IChatComponent field_148919_a;

    @Override
    public void nebula$setMessage(IChatComponent c)
    {
        field_148919_a = c;
    }
}
