package ez.nebula.client.impl.module.render;

import com.google.common.collect.Lists;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.EnumSetting;
import ez.nebula.client.api.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.List;

/**
 * @author xgraza
 * @see Block
 * @see net.minecraft.client.renderer.WorldRenderer
 * @since 03/07/25
 */
@ModuleManifest(name = "XRay",
        description = "Exposes ores or other blocks underground",
        category = ModuleCategory.RENDER)
public final class XRayModule extends Module
{
    @ModuleInstance
    public static XRayModule INSTANCE;

    public static final List<Block> XRAY_WHITELIST = Lists.newArrayList(
            Blocks.diamond_ore,
            Blocks.coal_ore,
            Blocks.iron_ore,
            Blocks.emerald_ore,
            Blocks.lapis_ore,
            Blocks.redstone_ore,
            Blocks.lit_redstone_ore,
            Blocks.gold_ore,
            Blocks.quartz_ore,

            Blocks.diamond_block,
            Blocks.coal_block,
            Blocks.iron_block,
            Blocks.emerald_block,
            Blocks.lapis_block,
            Blocks.redstone_block,
            Blocks.gold_block,
            Blocks.quartz_block,

            Blocks.beacon,
            Blocks.anvil,
            Blocks.ender_chest,
            Blocks.chest,
            Blocks.trapped_chest,
            Blocks.furnace,
            Blocks.lit_furnace,
            Blocks.enchanting_table,
            Blocks.dragon_egg,
            Blocks.monster_egg,
            Blocks.mob_spawner,
            Blocks.bed,
            Blocks.bookshelf,
            Blocks.cauldron,
            Blocks.brewing_stand,
            Blocks.sponge,

            Blocks.water,
            Blocks.flowing_water,
            Blocks.lava,
            Blocks.flowing_lava
    );

    private final EnumSetting<Mode> modeSetting = enumBuilder("Mode", Mode.BASIC)
            .setDescription("How to show hidden blocks")
            .onValueChanged((value) -> reloadRenders())
            .build();

    public final NumberSetting<Integer> transparencySetting = numberBuilder("Transparency", 120)
            .setMin(30)
            .setMax(255)
            .setScale(1)
            .setDescription("How transparent blocks should be")
            .setVisibility((value) -> modeSetting.getValue() == Mode.TRANSPARENT)
            .onValueChanged((value) -> reloadRenders())
            .build();

    @Override
    public void onEnable()
    {
        super.onEnable();
        reloadRenders();
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.theWorld != null)
        {
            MC.renderGlobal.loadRenderers();
        }
    }

    public void reloadRenders()
    {
        if (MC.theWorld != null && isToggled())
        {
            MC.renderGlobal.loadRenderers();;
        }
    }

    public boolean isWireframe()
    {
        return modeSetting.getValue() == Mode.WIREFRAME;
    }

    public boolean isTransparent()
    {
        return modeSetting.getValue() == Mode.TRANSPARENT;
    }

    private enum Mode
    {
        BASIC, WIREFRAME, TRANSPARENT
    }
}
