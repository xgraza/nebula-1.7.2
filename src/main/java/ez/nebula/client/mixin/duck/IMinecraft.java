package ez.nebula.client.mixin.duck;

import net.minecraft.util.Session;
import net.minecraft.util.Timer;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IMinecraft
{
    void nebula$setRightClickDelayTimer(final int delay);

    Timer nebula$getTimer();

    float nebula$getDebugFPS();

    void nebula$setSession(Session s);
}
