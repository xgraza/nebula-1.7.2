package ez.nebula.client.mixin.impl.render;

import ez.nebula.client.impl.module.player.AntiLagModule;
import ez.nebula.client.impl.module.render.GlintModule;
import ez.nebula.client.impl.module.render.ItemPhysicsModule;
import ez.nebula.client.impl.module.render.ItemTweaksModule;
import ez.nebula.client.mixin.duck.IRenderItem;
import ez.nebula.client.util.minecraft.player.ItemUtil;
import ez.nebula.client.util.optifine.OptifineHelper;
import ez.nebula.client.util.render.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(RenderItem.class)
public abstract class RenderItemMixin implements IRenderItem
{
    @Shadow protected abstract void renderGlint(int par1, int par2, int par3, int par4, int par5);

    @Shadow @Final private static ResourceLocation RES_ITEM_GLINT;

    @Shadow public abstract byte getMiniBlockCount(ItemStack stack, byte original);

    @Redirect(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;getMiniBlockCount(Lnet/minecraft/item/ItemStack;B)B"))
    private byte redirect$doRender$AntiLagGroupItems(RenderItem instance, ItemStack stack, byte original)
    {
        if (AntiLagModule.INSTANCE.isToggled() && AntiLagModule.INSTANCE.groupItemsSetting.getValue())
        {
            return 1;
        }
        return getMiniBlockCount(stack, original);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", remap = false, ordinal = 0))
    private void redirect$doRender$ItemPhysicsTranslate(float x, float y, float z, EntityItem p_77014_1_, double p_77014_2_, double p_77014_4_, double p_77014_6_, float p_77014_8_, float p_77014_9_)
    {
        final ItemStack itemStack = p_77014_1_.getEntityItem();
        if (itemStack != null && ItemPhysicsModule.INSTANCE != null && ItemPhysicsModule.INSTANCE.isToggled() && !RenderItem.renderInFrame)
        {
            float offset = 0.0f;
            if (!(itemStack.getItem() instanceof ItemBlock))
            {
                offset = -(p_77014_1_.height / 2.0f);
            }
            GL11.glTranslatef(x, y + offset, z);
        } else
        {
            GL11.glTranslatef(x, y, z);
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", remap = false))
    private void hook$doRender$ItemPhysicsLogic(EntityItem par1EntityItem, double x, double y, double z, float par2, float p_77014_4_, CallbackInfo ci)
    {
        if (ItemPhysicsModule.INSTANCE.isToggled() && !RenderItem.renderInFrame)
        {
            if (par1EntityItem.onGround)
            {
                par1EntityItem.rotationPitch = 90;
            } else
            {
                par1EntityItem.rotationPitch += 1.5f;
                par1EntityItem.rotationYaw += 1.5f;
            }
            GL11.glRotatef(par1EntityItem.rotationPitch, 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(par1EntityItem.rotationYaw, 0.0f, 0.0f, 1.0f);
        }
    }

    @Redirect(method = "renderDroppedItem(Lnet/minecraft/entity/item/EntityItem;Lnet/minecraft/util/IIcon;IFFFFI)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", remap = false, ordinal = 1), remap = false)
    private void redirect$renderDroppedItem$ItemPhysicsHook(float angle, float x, float y, float z)
    {
        // remove that hover & spin animation
        if (ItemPhysicsModule.INSTANCE == null || !ItemPhysicsModule.INSTANCE.isToggled())
        {
            GL11.glRotatef(angle, x, y, z);
        }
    }

    @Redirect(method = "renderGlint", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OpenGlHelper;glBlendFunc(IIII)V"))
    private void redirect$renderGlint$fixBlendFuncs(int par1, int par2, int par3, int par4)
    {
        if (OptifineHelper.isFastRender())
        {
            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
        } else
        {
            OpenGlHelper.glBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ZERO);
        }
    }

    @Redirect(method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;stackSize:I", ordinal = 0, opcode = Opcodes.GETFIELD))
    private int redirect$renderItemOverlayIntoGUI$ItemTweaksInfinitesRenderHook(ItemStack instance)
    {
        if (ItemTweaksModule.INSTANCE.showInfinites() && instance.stackSize != 1)
        {
            return Integer.MAX_VALUE; // MAX_VALUE > 1 = true, will continue if block
        }
        return instance.stackSize;
    }

    @Redirect(method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I"))
    private int redirect$renderItemOverlayIntoGUI$ItemTweaksRenderSize(FontRenderer instance, String p_78261_1_, int p_78261_2_, int p_78261_3_, int p_78261_4_, FontRenderer p_94148_1_, TextureManager p_94148_2_, ItemStack p_94148_3_, int p_94148_4_, int p_94148_5_, String p_94148_6_)
    {
        if (ItemTweaksModule.INSTANCE.showInfinites() && ItemUtil.isInfinite(p_94148_3_))
        {
            p_78261_1_ = EnumChatFormatting.RED + String.valueOf(p_94148_3_.stackSize);
        }
        return instance.drawStringWithShadow(p_78261_1_, p_78261_2_, p_78261_3_, p_78261_4_);
    }

    @Redirect(method = "renderItemIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderEffect(Lnet/minecraft/client/renderer/texture/TextureManager;II)V"), remap = false)
    private void redirect$renderItemIntoGUI$renderEffect(RenderItem instance, TextureManager manager, int x, int y)
    {
        // intentionally no-op
    }

    @Inject(method = "renderItemAndEffectIntoGUI", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IIZ)V", shift = At.Shift.AFTER), remap = false)
    private void hook$renderItemAndEffectIntoGUI$afterRender(FontRenderer fontRenderer, TextureManager p_82406_1_, ItemStack par1FontRenderer, int p_82406_2_, int par2TextureManager, CallbackInfo ci)
    {
        if (par1FontRenderer.isItemEnchanted())
        {
            renderEffect(p_82406_1_, p_82406_2_, par2TextureManager);
        }
    }

    @Override
    public void nebula$renderGlint(int p_77018_1_, int p_77018_2_, int p_77018_3_, int p_77018_4_, int p_77018_5_)
    {
        renderGlint(p_77018_1_, p_77018_2_, p_77018_3_, p_77018_4_, p_77018_5_);
    }

    /**
     * @author xgraza
     * @reason 1.7.2 code fucking sucks
     */
    @Overwrite(remap = false)
    public void renderEffect(TextureManager manager, int x, int y)
    {
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        manager.bindTexture(RES_ITEM_GLINT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        if (GlintModule.INSTANCE.isToggled())
        {
            RenderUtil.setGLColorOpaque(GlintModule.INSTANCE.colorSetting.getValue().getRGB());
        } else
        {
            GL11.glColor4f(0.5F, 0.25F, 0.8F, 1.0F);
        }
        nebula$renderGlint(x * 431278612 + y * 32178161, x - 2, y - 2, 20, 20);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }
}
