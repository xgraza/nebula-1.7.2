package ez.nebula.client.mixin.duck;

import net.minecraft.util.IChatComponent;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IS02PacketChat
{
    void nebula$setMessage(IChatComponent c);
}
