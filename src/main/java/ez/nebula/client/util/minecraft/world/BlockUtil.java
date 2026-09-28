package ez.nebula.client.util.minecraft.world;

import com.google.common.collect.Lists;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IEnumFacing;
import ez.nebula.client.mixin.duck.IWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;

import java.util.*;

/**
 * @author xgraza
 * @since 03/24/25
 */
public final class BlockUtil
{
    private static final Minecraft MC = Minecraft.getMinecraft();
    public static final EnumFacing[] HORIZONTALS = {
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.EAST,
            EnumFacing.WEST };
    public static final List<Block> INTERACTABLE_BLOCK_LIST = Lists.newArrayList(
            Blocks.chest,
            Blocks.ender_chest,
            Blocks.trapped_chest,
            Blocks.trapdoor,
            Blocks.wooden_door,
            Blocks.stone_button,
            Blocks.wooden_button,
            Blocks.furnace,
            Blocks.lit_furnace,
            Blocks.crafting_table,
            Blocks.brewing_stand,
            Blocks.bed,
            Blocks.anvil,
            Blocks.cake,
            Blocks.daylight_detector,
            Blocks.noteblock,
            Blocks.jukebox,
            Blocks.dispenser,
            Blocks.dropper,
            Blocks.fence_gate,
            Blocks.dragon_egg,
            Blocks.beacon,
            Blocks.command_block,
            Blocks.hopper,
            Blocks.unpowered_comparator,
            Blocks.powered_comparator,
            Blocks.unpowered_repeater,
            Blocks.powered_repeater,
            Blocks.lever);
    public static final Map<Integer, List<BlockPos>> RADIAL_BLOCK_MAP = new HashMap<>();

    private static final Queue<BlockPos> SEARCH_QUEUE = new ArrayDeque<>(18);
    private static final Set<Integer> VISITED_SET = new HashSet<>();

    static
    {
        for (int i = 1; i <= 21; ++i)
        {
            final List<BlockPos> posList = new ArrayList<>();

            for (int y = -i; y <= i; ++y)
            {
                for (int x = -i; x <= i; ++x)
                {
                    for (int z = -i; z <= i; ++z)
                    {
                        posList.add(new BlockPos(x, y, z));
                    }
                }
            }

            RADIAL_BLOCK_MAP.put(i, posList);
        }
    }

    public static int getHorizontalFacing(final EnumFacing facing)
    {
        switch (facing)
        {
            case SOUTH:
            {
                return 0;
            }
            case EAST:
            {
                return 1;
            }
            case NORTH:
            {
                return 2;
            }
            case WEST:
            {
                return 3;
            }
        }
        return -1;
    }

    public static BlockPos[] getAdjacent(final EnumFacing facing)
    {
        switch (facing)
        {
            case SOUTH:
            case NORTH:
            {
                return new BlockPos[]{ new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0) };
            }
            case EAST:
            case WEST:
            {
                return new BlockPos[]{ new BlockPos(0, 0, 1), new BlockPos(0, 0, -1) };
            }
            default:
                return null;
        }
    }

    public static boolean blockHasSubType(final Block block)
    {
        final Item item = Item.getItemFromBlock(block);
        return item != null && item.getHasSubtypes();
    }

    public static BlockInfo getPlacement(final BlockPos pos)
    {
        for (final EnumFacing face : EnumFacing.values())
        {
            final BlockPos neighbor = pos.offset(face);
            final EnumFacing opposite = ((IEnumFacing)(Object)face).nebula$getOpposite();
            if (!isReplaceable(neighbor) && canPlace(neighbor, opposite))
            {
                return new BlockInfo(neighbor, opposite);
            }
        }

        for (final EnumFacing face : EnumFacing.values())
        {
            final BlockPos neighbor = pos.offset(face);
            for (final EnumFacing side : EnumFacing.values())
            {
                final BlockPos neighbor2 = neighbor.offset(side);
                final EnumFacing opposite = ((IEnumFacing)(Object)side).nebula$getOpposite();
                if (!isReplaceable(neighbor2) && canPlace(neighbor2, opposite))
                {
                    return new BlockInfo(neighbor2, opposite);
                }
            }
        }
        return null;
    }

    public static boolean canPlace(final BlockPos pos, final EnumFacing facing)
    {
        final Block block = ((IWorld)MC.theWorld).nebula$getBlock(pos);
        if (!block.getMaterial().isSolid())
        {
            if (block instanceof BlockReed)
            {
                if (facing != EnumFacing.UP)
                {
                    return false;
                }
            } else
            {
                if (block.getMaterial() != Material.carpet)
                {
                    return false;
                }
            }
        }

        if (!MC.theWorld.checkNoEntityCollision(IAxisAlignedBB.create(pos.offset(facing))))
        {
            return false;
        }

        final int worldHeight = MC.theWorld.getHeight();
        int posY = pos.getY();
        if (facing == EnumFacing.UP)
        {
            posY += 1;
        } else if (facing == EnumFacing.DOWN)
        {
            posY -= 1;
        }
        return posY > 0 && posY <= worldHeight;
    }

    public static boolean isNotAir(final BlockPos pos)
    {
        return !MC.theWorld.isAirBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isReplaceable(final BlockPos pos)
    {
        return isReplaceable(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isReplaceable(final int x, final int y, final int z)
    {
        return MC.theWorld.getBlock(x, y, z).getMaterial().isReplaceable();
    }

    public static boolean isFire(final BlockPos pos)
    {
        return isFire(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isFire(final int x, final int y, final int z)
    {
        return MC.theWorld.getBlock(x, y, z) instanceof BlockFire;
    }
}
