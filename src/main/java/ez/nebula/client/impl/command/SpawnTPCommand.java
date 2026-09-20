package ez.nebula.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import ez.nebula.client.api.manager.command.Command;
import ez.nebula.client.api.manager.command.trait.CommandManifest;
import ez.nebula.client.api.manager.command.trait.CommandSource;
import ez.nebula.client.util.minecraft.network.PacketUtil;
import net.minecraft.network.play.client.C03PacketPlayer;

@CommandManifest(aliases = { "spawntp", "spawn", "stp" },
        description = "Uses an old Bukkit crash packet to send you back to the server spawn")
public final class SpawnTPCommand extends Command
{
    @Override
    public void createBuilder(LiteralArgumentBuilder<CommandSource> literal)
    {
        literal.executes((ctx) ->
        {
            PacketUtil.sendInstant(new C03PacketPlayer.C04PacketPlayerPosition(
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, false));
            return ctx.getSource().respond("Sent invalid packet");
        });
    }
}
