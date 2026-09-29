package ez.nebula.client.impl.module.render;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.player.FreecamModule;
import ez.nebula.client.util.io.NetworkUtil;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import ez.nebula.client.util.minecraft.player.EntityUtil;
import ez.nebula.client.util.render.font.Fonts;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.render.world.Render3D;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;

/**
 * @author xgraza
 * @since 03/29/25
 */
@ModuleManifest(name = "Nametags",
        description = "Displays entity information above their head",
        category = ModuleCategory.RENDER)
public final class NametagsModule extends Module
{
    @ModuleInstance
    public static NametagsModule INSTANCE;

    private static final ItemStack FAKE_BONE_STACK = new ItemStack(Items.bone, 1);
    private static final ItemStack FAKE_I_HORSE_ARMOR_STACK = new ItemStack(Items.iron_horse_armor, 1);
    private static final ItemStack FAKE_G_HORSE_ARMOR_STACK = new ItemStack(Items.golden_horse_armor, 1);
    private static final ItemStack FAKE_D_HORSE_ARMOR_STACK = new ItemStack(Items.diamond_horse_armor, 1);
    private static final ResourceLocation NEBULA_ICON_LOCATION = new ResourceLocation(
            "nebula", "texture/icon/128x.png");
    private static final int ITEM_RENDER_SIZE = 16;

    private final Setting<Boolean> backgroundSetting = builder("Background", false)
            .setDescription("If to render a rectangular backplate to the entity name")
            .build();
    private final Setting<Boolean> customFontSetting = builder("Custom Font", true)
            .setDescription("If to use the client's custom font to render the entity name")
            .build();
    private final NumberSetting<Float> sizeSetting = numberBuilder("Size", 0.25f)
            .setMin(0.05f)
            .setMax(3.0f)
            .setScale(0.05f)
            .setDescription("The scale at which the nametags render at")
            .build();
    private final Setting<Boolean> playersSetting = builder("Players", true)
            .setDescription("If to render nametags over player entities heads")
            .build();
    private final Setting<Boolean> pingSetting = builder("Ping", true)
            .setDescription("If to display the player's latency in the nametag")
            .setVisibility((value) -> playersSetting.getValue())
            .build();
    private final Setting<Boolean> tamedMobsSetting = builder("Tamed", true)
            .setDescription("If to display the player who tamed an animal")
            .build();
    private final Setting<Boolean> droppedItemsSetting = builder("Dropped Items", false)
            .setDescription("If to display what dropped items are")
            .build();
    private final Setting<Boolean> namedMobsSetting = builder("Named Mobs", true)
            .setDescription("If to show a mobs custom name tag if it has one")
            .build();

    private final Frustrum frustrum = new Frustrum();

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        MC.mcProfiler.startSection("nametags");
        frustrum.setPosition(MC.renderViewEntity.posX, MC.renderViewEntity.posY, MC.renderViewEntity.posZ);
        for (final Object o : MC.theWorld.loadedEntityList)
        {
            if (!(o instanceof Entity))
            {
                continue;
            }
            final Entity entity = (Entity)o;
            if (entity.getEntityId() == FreecamModule.CAMERA_ENTITY_ID
                    || (entity == MC.renderViewEntity && MC.gameSettings.thirdPersonView == 0))
            {
                continue;
            }
            if ((!playersSetting.getValue() && entity instanceof EntityPlayer)
                    || (!tamedMobsSetting.getValue() && (entity instanceof EntityTameable || entity instanceof EntityHorse))
                    || (!droppedItemsSetting.getValue() && entity instanceof EntityItem)
                    || (!namedMobsSetting.getValue()
                        && entity instanceof EntityLiving
                        && ((EntityLiving) entity).hasCustomNameTag()))
            {
                continue;
            }

            // only four/five entities we want
            if (!(entity instanceof EntityPlayer
                    || entity instanceof EntityTameable
                    || entity instanceof EntityHorse
                    || entity instanceof EntityItem
                    || (namedMobsSetting.getValue()
                        && entity instanceof EntityLiving
                        && ((EntityLiving) entity).hasCustomNameTag())))
            {
                continue;
            }

            if (entity instanceof EntityTameable && ((EntityTameable) entity).getOwnerName() == null)
            {
                continue;
            }

            if (entity instanceof EntityHorse && ((EntityHorse) entity).getOwnerName() == null)
            {
                continue;
            }

            // don't render shit out of our view...
            if (!frustrum.isBoundingBoxInFrustum(entity.boundingBox))
            {
                continue;
            }

            final double x = entity.prevPosX + (entity.posX - entity.prevPosX) * event.getPartialTicks();
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * event.getPartialTicks();
            if (entity != MC.thePlayer && !(entity instanceof EntityItem))
            {
                y += entity.height - 0.2;
            }
            final double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * event.getPartialTicks();
            Render3D.billboard(x, y + 0.5, z, sizeSetting.getValue(), () ->
            {
                glEnable(GL_DEPTH_CLAMP);

                final String text = NameProtectModule.INSTANCE.protect(getDisplayInfo(entity)).trim();
                double textWidth = 0;
                int textHeight = 0;
                if (!text.isEmpty())
                {
                    if (customFontSetting.getValue())
                    {
                        textWidth = Fonts.POPPINS.getStringWidth(text) / 2.0;
                        textHeight = (int) Fonts.POPPINS.getFontHeight();
                    } else
                    {
                        textWidth = MC.fontRenderer.getStringWidth(text) / 2.0;
                        textHeight = MC.fontRenderer.FONT_HEIGHT;
                    }
                }

                if (backgroundSetting.getValue())
                {
                    Render2D.rectangle(-textWidth, -textHeight, (textWidth * 2), textHeight, 0x60000000);
                }

                if (customFontSetting.getValue())
                {
                    Fonts.POPPINS.drawStringShadow(text, -textWidth, -textHeight, -1);
                } else
                {
                    MC.fontRenderer.drawStringWithShadow(text, (int) -textWidth, -textHeight, -1);
                }

//                if (entity.equals(MC.thePlayer) || NWS.INSTANCE.isNebulaUser(entity.getCommandSenderName()))
//                {
//                    Render2D.texture(NEBULA_ICON_LOCATION, -(textWidth + textHeight + 1), -textHeight, textHeight, textHeight);
//                }

                glEnable(GL_DEPTH_TEST);

                int startY = -((textHeight * 2) + 8);

                if (entity instanceof EntityPlayer)
                {
                    final EntityPlayer player = (EntityPlayer) entity;
                    final ItemStack heldStack = player.getHeldItem();
                    int itemX = (-12 * player.inventory.armorInventory.length)
                            + (heldStack == null ? 8 : 0);

                    if (heldStack != null)
                    {
                        renderItemStack(heldStack, itemX, startY);
                        itemX += (ITEM_RENDER_SIZE + 4);
                    }

                    for (int i = 3; i >= 0; --i)
                    {
                        final ItemStack stack = player.inventory.armorInventory[i];
                        if (stack != null)
                        {
                            renderItemStack(stack, itemX, startY);
                            itemX += (ITEM_RENDER_SIZE + 4);
                        }
                    }
                } else if (entity instanceof EntityTameable && !text.isEmpty())
                {
                    renderItemStack(FAKE_BONE_STACK, (int) -(textWidth + ITEM_RENDER_SIZE + 4), -11);
                } else if (entity instanceof EntityHorse && !text.isEmpty())
                {
                    ItemStack stack = null;
                    int horseArmor = ((EntityHorse) entity).func_110241_cb();
                    switch (horseArmor)
                    {
                        case 1:
                        {
                            stack = FAKE_I_HORSE_ARMOR_STACK;
                            break;
                        }
                        case 2:
                        {
                            stack = FAKE_G_HORSE_ARMOR_STACK;
                            break;
                        }
                        case 3:
                        {
                            stack = FAKE_D_HORSE_ARMOR_STACK;
                            break;
                        }
                    }
                    if (stack != null)
                    {
                        renderItemStack(stack, (int) -(textWidth + ITEM_RENDER_SIZE + 4), -11);
                    }
                }
                glDisable(GL_DEPTH_CLAMP);
            });
        }
        MC.mcProfiler.endSection();
    };

    private void renderItemStack(final ItemStack stack, final int x, final int y)
    {
        Render2D.itemWithEffects(stack, x, y);

        final Map<Integer, Integer> enchantmentList = EnchantmentHelper.getEnchantments(stack);
        if (enchantmentList.isEmpty())
        {
            return;
        }

        glPushMatrix();

        glDisable(GL_DEPTH_TEST);
        glScaled(0.5, 0.5, 0.5);

        final double textHeight = customFontSetting.getValue()
                ? Fonts.POPPINS.getFontHeight()
                : MC.fontRenderer.FONT_HEIGHT;

        double textPosY = y;
        for (final int id : enchantmentList.keySet())
        {
            final Enchantment enchantment = Enchantment.enchantmentsList[id];
            if (enchantment == null)
            {
                continue;
            }

            final int level = enchantmentList.get(id);
            String text = enchantment.getTranslatedName(level).substring(0, 3) + " ";
            if (level >= Short.MAX_VALUE)
            {
                text += EnumChatFormatting.RED + "32k";
            } else
            {
                text += level;
            }

            textPosY -= ((textHeight + ITEM_RENDER_SIZE) * 0.5) - 1;
            if (customFontSetting.getValue())
            {
                Fonts.POPPINS.drawStringShadow(text, x * 2, textPosY, -1);
            } else
            {
                MC.fontRenderer.drawStringWithShadow(text, (int) (x * 2.0), (int) textPosY, -1);
            }
        }

        glScaled(2.0, 2.0, 0.0);
        glEnable(GL_DEPTH_TEST);

        glPopMatrix();
    }

    private String getDisplayInfo(final Entity entity)
    {
        if (entity instanceof EntityTameable)
        {
            String text = ((EntityTameable) entity).getOwnerName();
            if (namedMobsSetting.getValue())
            {
                String mobName = getCustomTag(entity);
                if (mobName != null && !mobName.isEmpty())
                {
                    text = EnumChatFormatting.ITALIC
                            + mobName
                            + EnumChatFormatting.RESET
                            + " (" + text + ")";
                }
            }
            return text;
        } else if (entity instanceof EntityHorse)
        {
            if (entity.equals(MC.thePlayer.ridingEntity))
            {
                return "";
            }
            String text = ((EntityHorse) entity).getOwnerName();
            if (namedMobsSetting.getValue())
            {
                String horseName = getCustomTag(entity);
                if (horseName != null && !horseName.isEmpty())
                {
                    text = EnumChatFormatting.ITALIC
                            + horseName
                            + EnumChatFormatting.RESET
                            + " (" + text + ")";
                }
            }
            return text;
        } else if (entity instanceof EntityItem)
        {
            final EntityItem entityItem = (EntityItem) entity;
            final ItemStack itemStack = entityItem.getEntityItem();
            return itemStack == null
                    ? "Null Item?"
                    : EnumChatFormatting.GRAY
                      + itemStack.getDisplayName()
                      + EnumChatFormatting.RED
                      + " x"
                      + itemStack.stackSize;
        } else if (entity instanceof EntityPlayer)
        {
            final EntityPlayer player = (EntityPlayer) entity;
            final StringBuilder builder = new StringBuilder();

            if (pingSetting.getValue())
            {
                builder.append(EnumChatFormatting.GRAY);
                builder.append(NetworkUtil.getLatency(player));
                builder.append("ms ");
                builder.append(EnumChatFormatting.RESET);
            }

            if (Nebula.FRIENDS.has(player) || player == MC.thePlayer)
            {
                builder.append(ChatUtil.NEBULA_CLIENT_COLOR);
            }
            if (player.isSneaking())
            {
                builder.append(EnumChatFormatting.GOLD);
            }
            builder.append(player.func_145748_c_().getFormattedText());
            builder.append(EnumChatFormatting.RESET);

            builder.append(" ");

            final float health = EntityUtil.getHealth(player);

            if (health >= 20.0f)
            {
                builder.append(EnumChatFormatting.GREEN);
            } else if (health >= 10.0f)
            {
                builder.append(EnumChatFormatting.YELLOW);
            } else if (health >= 8.0f)
            {
                builder.append(EnumChatFormatting.RED);
            } else
            {
                builder.append(EnumChatFormatting.DARK_RED);
            }

            builder.append(String.format("%.1f", health));

            return builder.toString();
        } else if (entity instanceof EntityLiving)
        {
            if (namedMobsSetting.getValue())
            {
                String text = getCustomTag(entity);
                if (text != null && !text.isEmpty())
                {
                    return EnumChatFormatting.ITALIC + text;
                }
            }
        }

        return "";
    }

    private String getCustomTag(final Entity entity)
    {
        if (entity instanceof EntityLiving)
        {
            final EntityLiving living = (EntityLiving) entity;
            return living.hasCustomNameTag() ? living.getCustomNameTag() : "";
        }
        return "";
    }
}
