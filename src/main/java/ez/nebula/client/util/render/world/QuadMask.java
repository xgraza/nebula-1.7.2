package ez.nebula.client.util.render.world;

import net.minecraft.util.EnumFacing;

public final class QuadMask
{
    public static final int UP = 0x02;
    public static final int DOWN = 0x01;
    public static final int NORTH = 0x04;
    public static final int SOUTH = 0x08;
    public static final int EAST = 0x20;
    public static final int WEST = 0x10;

    private static final int[] values = { UP, DOWN, NORTH, SOUTH, EAST, WEST };
    public static final int ALL_FACES;

    static
    {
        int allMask = 0;
        for (int value : values)
        {
            allMask |= value;
        }
        ALL_FACES = allMask;
    }

    /**
     * Calculates a bitmask with each {@link EnumFacing}
     * @param facings vararg of {@link EnumFacing} of faces to draw
     * @return the integer bitmask
     */
    public static int mask(final EnumFacing... facings)
    {
        int mask = 0;
        for (final EnumFacing facing : facings)
        {
            mask |= values[facing.ordinal()];
        }
        return mask;
    }

    /**
     * Calculates if a bitmask contains a value
     * @param renderMask the bitmask
     * @param face the value
     * @return if this bitmask has that value
     */
    public static boolean has(final int renderMask, final int face)
    {
        return (renderMask & face) != 0;
    }
}
