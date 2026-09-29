package ez.nebula.client.mixin.impl.world.block;

import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.world.EventModifyBoundBox;
import ez.nebula.client.impl.module.render.XRayModule;
import ez.nebula.client.mixin.duck.IBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin implements IBlock
{
    @Shadow protected float blockHardness;

    @Shadow public abstract AxisAlignedBB getCollisionBoundingBoxFromPool(World p_149668_1_, int p_149668_2_, int p_149668_3_, int p_149668_4_);

    @Redirect(method = "addCollisionBoxesToList", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getCollisionBoundingBoxFromPool(Lnet/minecraft/world/World;III)Lnet/minecraft/util/AxisAlignedBB;"))
    private AxisAlignedBB redirect$addCollisionBoxesToList$getCollisionBoundingBoxFromPool(Block instance, World world, int x, int y, int z, World p_149743_1_, int p_149743_2_, int p_149743_3_, int p_149743_4_, AxisAlignedBB p_149743_5_, List p_149743_6_, Entity p_149743_7_)
    {
        final AxisAlignedBB bb = getCollisionBoundingBoxFromPool(world, x, y, z);
        final EventModifyBoundBox event = new EventModifyBoundBox(x, y, z, p_149743_7_, p_149743_1_, bb);
        EventBus.dispatch(event);
        return event.getAabb();
    }

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void hook$shouldSideBeRendered(IBlockAccess iBlockAccess, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> info)
    {
        if (XRayModule.INSTANCE.isToggled())
        {
            if (XRayModule.INSTANCE.isTransparent() || XRayModule.INSTANCE.isWireframe())
            {
                if (XRayModule.XRAY_WHITELIST.contains((Block) (Object) this))
                {
                    info.setReturnValue(true);
                }
            } else
            {
                info.setReturnValue(XRayModule.XRAY_WHITELIST.contains((Block) (Object) this));
            }
        }
    }

    @Inject(method = "getRenderBlockPass", at = @At("RETURN"), cancellable = true)
    private void hook$getRenderPass(CallbackInfoReturnable<Integer> info)
    {
        if (XRayModule.INSTANCE.isToggled() && XRayModule.INSTANCE.isTransparent())
        {
            info.setReturnValue(XRayModule.XRAY_WHITELIST.contains((Block) (Object) this) ? 0 : 1);
        }
    }

    @Inject(method = "getLightValue()I", at = @At("RETURN"), cancellable = true)
    private void hook$getLightValue(CallbackInfoReturnable<Integer> info)
    {
        if (XRayModule.INSTANCE.isToggled() && XRayModule.XRAY_WHITELIST.contains((Block) (Object) this))
        {
            info.setReturnValue(10000);
        }
    }

    @Override
    public float nebula$getBlockHardness()
    {
        return blockHardness;
    }
}
