package ez.nebula.client.impl.hud2;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.hud2.trait.HUDManifest;
import ez.nebula.client.api.manager.hud2.type.TextHUDElement;
import ez.nebula.client.util.minecraft.player.MoveUtil;
import net.minecraft.util.EnumChatFormatting;

/**
 * @author xgraza
 * @since 9/6/26
 */
@HUDManifest(value = "Speed", description = "Displays how many blocks per second you are traveling")
public final class SpeedHUDElement extends TextHUDElement
{
    @Override
    public String text()
    {
        final double moveDelta = Nebula.MOVEMENT.getMoveSpeed();
        double speed = (moveDelta / 1000) / (0.05 / 3600);
        speed *= MC.timer.timerSpeed;
        speed /= 3.6;
        return EnumChatFormatting.NEBULA_CLIENT_COLOR
                + "Speed (BPS): "
                + EnumChatFormatting.GRAY
                + String.format("%.1f", speed);
    }
}
