/*
 * Copyright (c) xgraza 2025
 */

package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.player.EventJump;
import ez.nebula.client.api.listener.event.player.EventSafeWalk;
import ez.nebula.client.api.listener.event.render.EventRender2D;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.BindSetting;
import ez.nebula.client.api.setting.ColorSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.impl.module.render.HUDModule;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.math.Timer;
import ez.nebula.client.util.minecraft.player.*;
import ez.nebula.client.util.minecraft.world.BlockInfo;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import ez.nebula.client.util.render.font.Fonts;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.src.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

/**
 * @author xgraza
 * @since 02/16/25
 */
@ModuleManifest(name = "Scaffold",
        description = "Automatically places blocks under you to give the appearance of flying",
        category = ModuleCategory.WORLD)
@RotationPriority(ModuleRotationPriorities.SCAFFOLD)
public final class ScaffoldModule extends InteractionModule
{
    @ModuleInstance
    public static ScaffoldModule INSTANCE;

    private final NumberSetting<Double> extend = numberBuilder("Extend", 0.0)
            .setMin(0.0)
            .setMax(6.0)
            .setScale(0.5)
            .setDescription("How far to extend forward")
            .build();
    private final Setting<Boolean> towerSetting = builder("Tower", true)
            .setDescription("If to allow quicker upwards movement")
            .build();
    private final Setting<Boolean> keeepYSetting = builder("Keep Y", false)
            .setDescription("If to keep your original y-level when scaffolding")
            .build();
    private final Setting<Boolean> safeWalkSetting = builder("SafeWalk", false)
            .setDescription("If to use safe walk")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate towards the block you're placing")
            .build();
    private final Setting<Boolean> renderSetting = builder("Render", false)
            .setDescription("If to render where the block is being placed")
            .build();
    private final BindSetting downwardsSetting = bindBuilder("Downwards")
            .setKeyCode(Keyboard.KEY_NONE)
            .setDescription("The key to press to toggle downwards scaffold")
            .build();
    private final Setting<Boolean> prioritizeHeldSetting = builder("Prioritize Held", true)
            .setDescription("If to prioritize the block item held in your hand")
            .build();

    private final Timer towerTimer = new Timer();
    private double basePosY;
    private BlockInfo blockData;
    private int towerTicks, slot;
    private float[] angles;

    @Override
    public void onEnable()
    {
        super.onEnable();
        downwardsSetting.getValue().setState(false);
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
        blockData = null;
        basePosY = -1.0;
        towerTicks = 0;
        slot = InventoryUtil.INVALID_SLOT;
        angles = null;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        slot = InventoryUtil.getHotbarSlot(this::isStackValid);
        if (prioritizeHeldSetting.getValue() && isStackValid(MC.thePlayer.getHeldItem()))
        {
            slot = MC.thePlayer.inventory.currentItem;
        }
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }

        blockData = getBlockData();
        if (blockData == null)
        {
            return;
        }

        if (rotateSetting.getValue() && !rotate(angles))
        {
            return;
        }
        place(blockData, slot);

        if (MC.gameSettings.keyBindJump.getIsKeyPressed() && towerSetting.getValue())
        {
            ++towerTicks;
            if (MC.thePlayer.isPotionActive(Potion.jump))
            {
                if (MC.thePlayer.onGround)
                {
                    MC.thePlayer.motionY = MoveUtil.getJumpHeight(0.42f);
                }
                MC.thePlayer.jump();
            } else
            {
                if (MC.thePlayer.onGround || MC.thePlayer.motionY == 0.16477328182606651)
                {
                    MC.thePlayer.motionY = MoveUtil.getJumpHeight(0.42f);
                }
                if (!MC.thePlayer.onGround && towerTimer.hasElapsed(800L, true))
                {
                    MC.thePlayer.motionY = -0.78f;
                    MC.thePlayer.motionX *= 0.33;
                    MC.thePlayer.motionZ *= 0.33;
                }
            }
            MC.thePlayer.motionX *= 0.99;
            MC.thePlayer.motionZ *= 0.99;
        } else
        {
            towerTimer.resetTime();
            towerTicks = 0;
        }
    };

    @Subscribe
    private final EventListener<EventJump> jumpEventListener = event ->
    {
        if (towerSetting.getValue() && MC.gameSettings.keyBindJump.getIsKeyPressed())
        {
            event.cancel();
        }
    };

    @Subscribe
    private final EventListener<EventSafeWalk> eventSafeWalkEventListener = event ->
    {
        if (MC.thePlayer.onGround && !MC.gameSettings.keyBindJump.getIsKeyPressed() && safeWalkSetting.getValue())
        {
            event.cancel();
        }
    };

    @Subscribe
    private final EventListener<EventRender2D> render2DEventListener = event ->
    {
        if (slot == InventoryUtil.INVALID_SLOT || !renderSetting.getValue())
        {
            return;
        }
        final ItemStack itemStack = MC.thePlayer.inventory.getStackInSlot(slot);
        if (itemStack == null || !(itemStack.getItem() instanceof ItemBlock))
        {
            return;
        }
        MC.mcProfiler.startSection("scaffold");

        int blocksLeft = 0;
        if (ItemUtil.isInfinite(itemStack))
        {
            blocksLeft = -1;
        } else
        {
            final int stackSize = itemStack.stackSize;
            if (stackSize != 0)
            {
                blocksLeft = stackSize;
            }
        }

        final String text = String.format("%sBlocks left:%s %s",
                EnumChatFormatting.GRAY,
                EnumChatFormatting.RESET,
                blocksLeft == -1 ?
                        EnumChatFormatting.RED + "Infinite"
                        : blocksLeft);
        final double totalWidth = 16 + 2 + Fonts.POPPINS.getStringWidth(text);
        final double posX = event.getResolution().getScaledWidth_double() / 2.0 - totalWidth / 2.0;
        final double posY = event.getResolution().getScaledHeight_double() / 2.0 + 100;

        Render2D.rectangle(posX - 2, posY - 2, totalWidth + 4, 16 + 4, 0x80000000);
        Render2D.itemNoEffects(itemStack, (int) posX, (int) posY);
        Fonts.POPPINS.drawStringShadow(text, posX + 16, posY + 2, -1);
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (blockData == null)
        {
            return;
        }

        if (rotateSetting.getValue())
        {
            angles = AngleUtil.anglesToBlock(blockData.getPos(), blockData.getFacing(), event.getPartialTicks());
        }

        if (!renderSetting.getValue())
        {
            return;
        }

        MC.mcProfiler.startSection("scaffold");

        final AxisAlignedBB aabb = IAxisAlignedBB.create(blockData.getPos());
        final ColorSetting cs = HUDModule.INSTANCE.primaryColorSetting;
        Render3D.filledAABB(aabb, QuadMask.mask(blockData.getFacing()), cs.getValueInt(120));
        Render3D.outlinedAABB(aabb, 1.5f, QuadMask.mask(blockData.getFacing()), cs.getValueInt());
        MC.mcProfiler.endSection();
    };

    // @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S08PacketPlayerPosLook)
        {
            ChatUtil.sendNebula("Ticks: " + towerTicks);
        }
    };

    private boolean isStackValid(final ItemStack stack)
    {
        return stack != null
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).field_150939_a.getMaterial().isSolid();
    }

    private BlockInfo getBlockData()
    {
        double minY = MC.thePlayer.boundingBox.minY;
        // if we're on ground and our remainder is not 0.0 (ex: 0.875 on ender chests)
        if (minY % 0.015625 == 0.0 && minY % 1.0 != 0.0)
        {
            minY += 1.0 - (minY % 1.0);
        }

        if (!keeepYSetting.getValue()
                || (towerSetting.getValue() && MC.gameSettings.keyBindJump.getIsKeyPressed())
                || basePosY == -1.0
                || downwardsSetting.getValue().isToggled())
        {
            basePosY = minY - 1.0;
        }

        if (basePosY > 256)
        {
            basePosY = 256;
        }

        if (downwardsSetting.getValue().isToggled() && !MC.gameSettings.keyBindJump.getIsKeyPressed())
        {
            basePosY -= 1;
        }

        BlockPos pos = PlayerUtil.getOrigin(basePosY);
        if (extend.getValue() > 0.0 && !MC.gameSettings.keyBindJump.getIsKeyPressed())
        {
            final float yaw = MC.thePlayer.rotationYaw * 0.017453292f;

            double distance = 0.0;
            while (distance <= extend.getValue())
            {
                distance += 0.5;
                final BlockPos extendedPos = pos.add(new BlockPos(
                        (int) (-Math.sin(yaw) * distance),
                        0, (int) (Math.cos(yaw) * distance)));
                if (BlockUtil.isReplaceable(extendedPos))
                {
                    pos = extendedPos;
                    break;
                }
            }
        }
        return BlockUtil.getPlacement(pos);
    }
}
