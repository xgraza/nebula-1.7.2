package ez.nebula.client.mixin.common.render.gui;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventBus;
import ez.nebula.client.api.listener.event.render.EventRender2D;
import ez.nebula.client.impl.module.render.BetterF3Module;
import ez.nebula.client.mixin.duck.IBlock;
import ez.nebula.client.mixin.duck.IMinecraft;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.render.gui.Render2D;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.Sys;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glPopMatrix;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class GuiInGameForgeMixin extends GuiIngame
{
    @Shadow private ScaledResolution res;

    public GuiInGameForgeMixin(Minecraft par1Minecraft)
    {
        super(par1Minecraft);
    }

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z", shift = At.Shift.BEFORE, remap = false), remap = false)
    private void hook$renderGameOverlay$getRes(float partialTicks, boolean hasScreen, int mouseX, int mouseY, CallbackInfo ci)
    {
        Render2D.RESOLUTION = res;
    }

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", shift = At.Shift.AFTER, ordinal = 1, remap = false), remap = false)
    private void hook$renderGameOverlay$render2DEvent(float partialTicks, boolean hasScreen, int mouseX, int mouseY, CallbackInfo ci)
    {
        mc.mcProfiler.startSection("nebulaRender2D");
        EventBus.dispatch(new EventRender2D(res, partialTicks));
        mc.mcProfiler.endSection();
    }

    @Inject(method = "renderHUDText", at = @At("HEAD"), cancellable = true, remap = false)
    private void hook$renderHUDText$BetterF3Hook(int width, int height, CallbackInfo info)
    {
        if (BetterF3Module.INSTANCE.isToggled() && mc.gameSettings.showDebugInfo)
        {
            mc.mcProfiler.startSection("nebula_betterf3");
            nebula$renderCustomDebug(width);
            mc.mcProfiler.endSection();
            info.cancel();
        }
    }

    @Redirect(method = "renderHotbar", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I", opcode = Opcodes.GETFIELD))
    private int redirect$renderHotbar$currentItem(InventoryPlayer instance)
    {
        return Nebula.INVENTORY.slot();
    }

    @Unique
    private void nebula$renderCustomDebug(int width)
    {
        glPushMatrix();

        final FontRenderer font = mc.fontRenderer;
        final int color = 16777215;

        int y = 2;

        font.drawStringWithShadow("Minecraft 1.7.2", 2, y, color);
        font.drawStringWithShadow("Nebula " + Nebula.VERSION, 2, y += 10, color);
        font.drawStringWithShadow("LWJGL " + Sys.getVersion(), 2, y += 10, color);
        font.drawStringWithShadow("OpenGL " + glGetString(GL_VERSION), 2, y += 10, color);

        font.drawStringWithShadow("FPS: " + ((IMinecraft)mc).nebula$getDebugFPS(), 2, y += 18, color);
        font.drawStringWithShadow("TPS: " + Nebula.SERVER.averageTPS() + " [" + Nebula.SERVER.tps() + "]", 2, y += 10, color);
        font.drawStringWithShadow("Chunk Updates: " + WorldRenderer.chunksUpdated, 2, y += 10, color);

        font.drawStringWithShadow(String.format("X: %.5f", mc.thePlayer.posX), 2, y += 18, color);
        font.drawStringWithShadow(String.format("Y: %.5f", mc.thePlayer.boundingBox.minY), 2, y += 10, color);
        font.drawStringWithShadow(String.format("Pose: %.5f", mc.thePlayer.posY), 2, y += 10, color);
        font.drawStringWithShadow(String.format("Z: %.5f", mc.thePlayer.posZ), 2, y += 10, color);
        font.drawStringWithShadow(String.format("Ground: %s", mc.thePlayer.onGround), 2, y += 10, color);

        font.drawStringWithShadow(String.format("Direction: %s (yaw: %.3f, pitch: %.3f)", PlayerUtil.getFacing(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), 2, y += 10, color);

        // other side
        y = 2;
        String text = String.format("Java Version: %s", System.getProperty("java.version", "NULL"));
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y, color);
        text = String.format("Java Vendor: %s", System.getProperty("java.vendor", "NULL"));
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
        text = String.format("OS: %s", System.getProperty("os.name", "NULL"));
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 18, color);
        text = String.format("Arch: %s", System.getProperty("os.arch", "NULL"));
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);

        double totalMemory = Runtime.getRuntime().totalMemory() * 1E-6;
        double freeMemory = Runtime.getRuntime().freeMemory() * 1E-6;
        double maxMemory = Runtime.getRuntime().maxMemory() * 1E-6;

        text = String.format("Total: %.2fMB", maxMemory);
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 18, color);
        text = String.format("Used: %.2fMB", freeMemory);
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
        text = String.format("Allocated: %.2fMB", totalMemory);
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
        text = String.format("Available Processors: %s", Runtime.getRuntime().availableProcessors());
        font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);

        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            y += 18;
            text = String.format("Hit Pos: %s, %s, %s", mc.objectMouseOver.blockX, mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ);
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y, color);
            text = String.format("Side: %s (%s)", EnumFacing.values()[mc.objectMouseOver.sideHit], mc.objectMouseOver.sideHit);
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);

            final Block block = mc.theWorld.getBlock(mc.objectMouseOver.blockX, mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ);
            text = String.format("Block: %s (%s)", block.getUnlocalizedName(), block.getClass().getSimpleName());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Replaceable: %s", block.getMaterial().isReplaceable());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Liquid: %s", block.getMaterial().isLiquid());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Can Burn: %s", block.getMaterial().getCanBurn());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Solid: %s", block.getMaterial().isSolid());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Opaque: %s", block.getMaterial().isOpaque());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Collidable: %s", block.isCollidable());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Normal: %s", block.isNormalCube());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Hardness: %s", ((IBlock)block).nebula$getBlockHardness());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Slipperiness: %s", block.slipperiness);
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Metadata: %s", mc.theWorld.getBlockMetadata(mc.objectMouseOver.blockX, mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ));
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
        }

        if (mc.pointedEntity != null)
        {
            Entity entity = mc.pointedEntity;

            y += 18;
            text = String.format("UUID: %s", entity.getUniqueID());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y, color);
            text = String.format("Command Sender Name: %s", entity.getCommandSenderName());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("Entity ID: %s", entity.getEntityId());
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("X: %.2f", entity.posX);
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("X: %.2f", entity.posY);
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
            text = String.format("X: %.2f", entity.posZ);
            font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);

            final DataWatcher watcher = entity.getDataWatcher();
            final List<DataWatcher.WatchableObject> watched = watcher.getAllWatched();
            if (!watched.isEmpty())
            {
                y += 18;
                text = (EnumChatFormatting.GRAY + EnumChatFormatting.UNDERLINE.toString() + "Watcher Data:");
                font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y, color);
                for (final DataWatcher.WatchableObject obj : watched)
                {
                    text = String.format("%s(%s) %s%s", EnumChatFormatting.GRAY, obj.getDataValueId(), EnumChatFormatting.BLUE, obj.getObject());
                    font.drawStringWithShadow(text, width - font.getStringWidth(text) - 2, y += 10, color);
                }
            }
        }

        glPopMatrix();
    }
}
