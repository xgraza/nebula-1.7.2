package net.minecraft.src;

import net.minecraft.util.EnumFacing;

public class BlockPos
{
    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }

    public int getZ()
    {
        return this.z;
    }

    public BlockPos up()
    {
        return new BlockPos(x, y + 1, z);
    }

    public BlockPos down()
    {
        return new BlockPos(x, y - 1, z);
    }

    public BlockPos offset(final EnumFacing facing)
    {
        return offset(facing, 1);
    }

    public BlockPos offset(final EnumFacing facing, int n)
    {
        if (n == 0)
        {
            return this;
        }
        return new BlockPos(x + (facing.getFrontOffsetX() * n),
                y + (facing.getFrontOffsetY() * n),
                z + (facing.getFrontOffsetZ() * n));
    }

    public BlockPos add(final int x, final int y, final int z)
    {
        return new BlockPos(this.x + x, this.y + y, this.z + z);
    }

    public BlockPos add(BlockPos pos)
    {
        return new BlockPos(this.x + pos.x, this.y + pos.y, this.z + pos.z);
    }

    public BlockPos sub(BlockPos pos)
    {
        return new BlockPos(this.x - pos.x, this.y - pos.y, this.z - pos.z);
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        } else if (!(obj instanceof BlockPos))
        {
            return false;
        } else
        {
            BlockPos blockPos = (BlockPos) obj;
            return this.getX() == blockPos.getX() && (this.getY() == blockPos.getY() && this.getZ() == blockPos.getZ());
        }
    }

    public int hashCode()
    {
        return (this.getY() + this.getZ() * 31) * 31 + this.getX();
    }

    public String toString()
    {
        return this.x + ", " + this.y + ", " + this.z;
    }
}
