package ez.nebula.client.api.world;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author xgraza
 * @since 6/4/26
 */
public final class BlockSearcher extends Thread
{
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
    private static final Minecraft MC = Minecraft.getMinecraft();

    private final Consumer<SearchedBlock> callback;
    private int searchRange;
    private boolean searching;

    public BlockSearcher(final String name, final Consumer<SearchedBlock> callback)
    {
        this.callback = callback;
        setName("(BlockSearcher Thread): " + name);
        setDaemon(true);
        start();
    }

    @Override
    public void run()
    {
        while (!isInterrupted())
        {
            if (!searching || searchRange <= 0)
            {
                continue;
            }
            final World world = MC.theWorld;
            final EntityPlayer player = MC.thePlayer;
            if (player == null || world == null)
            {
                continue;
            }

            final int baseChunkX = (int)Math.floor(player.posX) >> 4;
            final int baseChunkZ = (int)Math.floor(player.posZ) >> 4;
            for (int y = 0; y < 256; ++y)
            {
                if (!searching)
                {
                    break;
                }
                for (int x = -searchRange; x <= searchRange; ++x)
                {
                    for (int z = -searchRange; z <= searchRange; ++z)
                    {
                        final Chunk chunk = world.getChunkFromChunkCoords(baseChunkX + x, baseChunkZ + z);
                        if (chunk == null)
                        {
                            continue;
                        }

                        searchChunk(chunk, y);
                    }
                }
            }
        }
    }

    private void searchChunk(final Chunk chunk, final int y)
    {
        if (chunk == null || !searching)
        {
            return;
        }
        final ExtendedBlockStorage[] blockStorageArray = chunk.getBlockStorageArray();
        if (blockStorageArray.length > y >> 4)
        {
            final ExtendedBlockStorage blockStorage = blockStorageArray[y >> 4];
            if (blockStorage == null)
            {
                return;
            }

            final int chunkX = chunk.xPosition;
            final int chunkZ = chunk.zPosition;

            for (int x = 0; x <= 16; ++x)
            {
                for (int z = 0; z <= 16; ++z)
                {
                    if (!searching)
                    {
                        break;
                    }
                    final int posX = ((chunkX * 16) + x);
                    final int posZ = ((chunkZ * 16) + z);
                    final Block block = blockStorage.getBlockByExtId(posX & 15, y & 15, posZ & 15);
                    callback.accept(new SearchedBlock(posX, y, posZ, block));
                }
            }
        }
    }

    public void setSearchRange(int searchRange)
    {
        this.searchRange = searchRange;
    }

    public void setSearching(boolean searching)
    {
        this.searching = searching;
    }

    public boolean isSearching()
    {
        return searching;
    }

    public static final class SearchedBlock
    {
        private final int x, y, z;
        private final Block block;

        public SearchedBlock(int x, int y, int z, Block block)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }

        public int getX()
        {
            return x;
        }

        public int getY()
        {
            return y;
        }

        public int getZ()
        {
            return z;
        }

        public Block getBlock()
        {
            return block;
        }
    }
}
