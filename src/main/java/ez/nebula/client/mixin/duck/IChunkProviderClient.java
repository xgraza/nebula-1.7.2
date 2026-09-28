package ez.nebula.client.mixin.duck;

import net.minecraft.world.chunk.Chunk;

import java.util.List;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IChunkProviderClient
{
    List<Chunk> nebula$getChunkListing();
}
