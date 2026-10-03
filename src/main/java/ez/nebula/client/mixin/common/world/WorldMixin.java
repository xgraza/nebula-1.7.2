package ez.nebula.client.mixin.common.world;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.player.EventPushWater;
import ez.nebula.client.impl.module.render.NoRenderModule;
import ez.nebula.client.mixin.duck.IWorld;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = World.class)
public abstract class WorldMixin implements IWorld
{
    @Shadow public abstract Block getBlock(int x, int y, int z);

    @Inject(method = "getWeightedThunderStrength", at = @At("RETURN"), cancellable = true)
    private void hook$getWeightedThunderStrength$NoRenderHook(float v, CallbackInfoReturnable<Float> info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.weatherSetting.getValue())
        {
            info.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getRainStrength", at = @At("RETURN"), cancellable = true)
    private void hook$getRainStrength$NoRenderHook(float v, CallbackInfoReturnable<Float> info)
    {
        if (NoRenderModule.INSTANCE.isToggled() && NoRenderModule.INSTANCE.weatherSetting.getValue())
        {
            info.setReturnValue(0.0f);
        }
    }

    @Inject(method = "handleMaterialAcceleration", at = @At("HEAD"), cancellable = true)
    private void hook$handleMaterialAcceleration$eventPushWater(AxisAlignedBB bb, Material material, Entity entity, CallbackInfoReturnable<Boolean> info)
    {
        if (EventBus.dispatch(new EventPushWater(entity)))
        {
            info.setReturnValue(false);
        }
    }

    @Override
    public Block nebula$getBlock(BlockPos pos)
    {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }
}
