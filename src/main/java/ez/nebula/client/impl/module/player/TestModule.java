package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.DebugFeature;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.input.EventKey;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.DamageSource;
import org.lwjgl.input.Keyboard;

/**
 * See {@link EntityEnderCrystal#attackEntityFrom(DamageSource, float)}
 */
@DebugFeature
@ModuleManifest(name = "Test", category = ModuleCategory.PLAYER)
public final class TestModule extends Module
{
    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
//        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement)
//        {
//            final C08PacketPlayerBlockPlacement packet = event.getPacket();
//            if (isCrystal(packet.func_149574_g()) && packet.getSide() != 255)
//            {
//                final int x = packet.getPosX();
//                final int y = packet.getPosY();
//                final int z = packet.getPosZ();
//
//                // pre 1.13
//                if (!MC.theWorld.isAirBlock(x, y + 2, z))
//                {
//                    return;
//                }
//
//                final Block block = MC.theWorld.getBlock(x, y, z);
//                if (block != Blocks.obsidian && block != Blocks.bedrock)
//                {
//                    return;
//                }
//                MC.theWorld.spawnEntityInWorld(new EntityEnderCrystal(MC.theWorld, x + 0.5, y + 1, z + 0.5));
//            }
//        }
    };

    @Subscribe
    private final EventListener<EventKey> keyEventListener = event ->
    {
        if (MC.isSingleplayer() && event.getKeyCode() == Keyboard.KEY_F && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
        {
            ChatUtil.sendNebula("dropping");
            final ItemStack stack = new ItemStack(Items.spawn_egg, 1, 200);
            EntityItem var11 = MC.thePlayer.dropPlayerItemWithRandomChoice(stack, false);
            var11.delayBeforeCanPickup = 0;
            var11.func_145797_a(MC.thePlayer.getCommandSenderName());
        }
    };

    private boolean isCrystal(final ItemStack itemStack)
    {
        return itemStack != null && itemStack.getItem() instanceof ItemMonsterPlacer && itemStack.getItemDamage() == 200;
    }
}
