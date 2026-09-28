package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.world.EventModifyBoundBox;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockFire;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author xgraza
 * @since 03/07/25
 */
@ModuleManifest(name = "AntiBlock",
        description = "Prevents you from walking into certain blocks accidentally",
        category = ModuleCategory.PLAYER)
public final class AntiBlockModule extends Module
{
    private static final AxisAlignedBB FULL_BLOCK_AABB = AxisAlignedBB.getBoundingBox(
            0, 0, 0, 1, 1, 1);

    private final Setting<Boolean> exemptSneaking = builder("Exempt Sneak", false)
            .setDescription("If to allow interaction with blacklisted blocks when sneaking")
            .build();
    private final Setting<Boolean> cactusSetting = builder("Cactus", false)
            .setDescription("If to prevent damage from a cactus")
            .build();
    private final Setting<Boolean> endPortalSetting = builder("End Portals", false)
            .setDescription("If to prevent entering an end portal")
            .build();
    private final Setting<Boolean> fireSetting = builder("Fire", false)
            .setDescription("If to prevent walking into fire")
            .build();

    @Subscribe
    private final EventListener<EventModifyBoundBox> modifyBoundBoxEventListener = event ->
    {
        if (MC.thePlayer == null || MC.theWorld == null)
        {
            return;
        }

        if (!MC.thePlayer.equals(event.getEntity()) || !MC.theWorld.equals(event.getWorld()))
        {
            return;
        }
        if (exemptSneaking.getValue() && MC.gameSettings.keyBindSneak.getIsKeyPressed())
        {
            return;
        }
        final Block block = MC.theWorld.getBlock(event.getX(), event.getY(), event.getZ());
        if (isWhitelisted(block))
        {
            event.setAabb(FULL_BLOCK_AABB.copy().offset(event.getX(), event.getY(), event.getZ()));
        }
    };

    public boolean isWhitelisted(final Block block)
    {
        return (block instanceof BlockCactus && cactusSetting.getValue())
                || (block instanceof BlockEndPortal && endPortalSetting.getValue())
                || (block instanceof BlockFire && fireSetting.getValue());
    }
}
