package ez.nebula.client.mixin.impl.world;

import ez.nebula.client.mixin.duck.IChunkProviderClient;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = ChunkProviderClient.class)
public class ChunkProviderClientMixin implements IChunkProviderClient
{
    @Shadow private List chunkListing;

    @Override
    @SuppressWarnings("unchecked")
    public List<Chunk> nebula$getChunkListing()
    {
        return chunkListing;
    }
}
