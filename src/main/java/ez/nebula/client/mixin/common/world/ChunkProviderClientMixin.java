package ez.nebula.client.mixin.common.world;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.world.EventUnloadChunk;
import ez.nebula.client.mixin.duck.IChunkProviderClient;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = ChunkProviderClient.class)
public class ChunkProviderClientMixin implements IChunkProviderClient
{
    @Shadow private List chunkListing;

    @Inject(method = "unloadChunk", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hook$unloadChunk$event(int i, int p_73234_1_, CallbackInfo info, Chunk chunk)
    {
        EventBus.dispatch(new EventUnloadChunk(chunk, (ChunkProviderClient) (Object) this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Chunk> nebula$getChunkListing()
    {
        return chunkListing;
    }
}
