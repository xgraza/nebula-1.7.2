package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.api.setting.block.BlockSetting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * @author xgraza
 * @since 6/28/26
 */
@ModuleManifest(name = "AutoLavaHoleFill",
        description = "Automatically fills the 1x1 pockets of lava in the nether around you",
        category = ModuleCategory.WORLD)
@RotationPriority(ModuleRotationPriorities.AUTO_LAVA_FILL)
public final class AutoLavaHoleFillModule extends InteractionModule
{
    @ModuleInstance
    public static AutoLavaHoleFillModule INSTANCE;

    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.1)
            .setDescription("The range to place at")
            .build();
    private final Setting<Boolean> partialSetting = builder("Partial", false)
            .setDescription("If to also fill already exposed holes with only one side exposed")
            .build();
    private final Setting<Boolean> anyBlockSetting = builder("Any Block", true)
            .setDescription("If to use any solid block to fill the lava hole")
            .build();
    private final BlockSetting blockSetting = blockBuilder("Block")
            .setBlock(Blocks.netherrack)
            .setDescription("The kind of block to fill the lava hole with")
            .setVisibility((value) -> !anyBlockSetting.getValue())
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate to the place the block will be placed")
            .build();

    private BlockInfo info;
    private float[] angles;

    @Override
    public void onDisable()
    {
        super.onDisable();
        info = null;
        angles = null;
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
    }

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (info != null && rotateSetting.getValue())
        {
            angles = AngleUtil.anglesToBlock(info.getPos(), info.getFacing(), event.getPartialTicks());
        }
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        final BlockPos pos = findLavaHole();
        if (pos == null)
        {
            info = null;
            angles = null;
            return;
        }
        final int slot = getSlot();
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            info = null;
            angles = null;
            return;
        }

        info = BlockUtil.getPlacement(pos);
        if (info == null)
        {
            return;
        }

        if (rotateSetting.getValue() && !rotate(angles))
        {
            return;
        }

        place(info, slot);
        info = null;
        angles = null;
    };

    private BlockPos findLavaHole()
    {
        final BlockPos origin = PlayerUtil.getOrigin();
        for (final BlockPos offset : BlockUtil.RADIAL_BLOCK_MAP.get(rangeSetting.getValue().intValue()))
        {
            if (Math.abs(offset.getY()) > 2)
            {
                continue;
            }

            final BlockPos pos = origin.add(offset);
            if (MC.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSetting.getValue())
            {
                continue;
            }
            final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
            if (block == null || block.getMaterial() != Material.lava)
            {
                continue;
            }
            int faces = 0;
            // all faces must be covering the lava
            for (final EnumFacing facing : EnumFacing.values())
            {
                final BlockPos pos1 = pos.offset(facing);
                if (!BlockUtil.isReplaceable(pos1))
                {
                    ++faces;
                }
            }
            if (partialSetting.getValue() && faces >= 5)
            {
                return pos;
            }
            if (faces != 6)
            {
                continue;
            }
            return pos;
        }
        return null;
    }

    private int getSlot()
    {
        for (int i = 0; i < 9; ++i)
        {
            final ItemStack itemStack = MC.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null || !(itemStack.getItem() instanceof ItemBlock))
            {
                continue;
            }
            final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
            if (!itemBlock.field_150939_a.getMaterial().isSolid())
            {
                continue;
            }
            if (anyBlockSetting.getValue())
            {
                return i;
            }
            if (blockSetting.isBlock(itemStack))
            {
                return i;
            }
        }
        return InventoryUtil.INVALID_SLOT;
    }

    @Override
    public boolean isActive()
    {
        return super.isActive() && info != null;
    }
}
