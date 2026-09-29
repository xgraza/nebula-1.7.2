package ez.nebula.client.mixin.impl.world;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.world.EventRemoveTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Chunk.class)
public class ChunkMixin
{
    @Inject(method = "removeTileEntity", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hook$removeTileEntity(int x, int y, int z, CallbackInfo info, ChunkPosition pos, TileEntity te)
    {
        EventBus.dispatch(new EventRemoveTileEntity(te, x, y, z));
    }
}
