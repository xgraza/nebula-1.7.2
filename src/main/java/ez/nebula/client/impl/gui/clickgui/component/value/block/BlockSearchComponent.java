package ez.nebula.client.impl.gui.clickgui.component.value.block;

import ez.nebula.client.api.setting.block.BlockValue;
import ez.nebula.client.mixin.duck.IRegistryNamespaced;
import ez.nebula.client.util.render.font.Fonts;
import ez.nebula.client.util.render.gui.Render2D;
import ez.nebula.client.util.render.gui.trait.GUIComponent;
import ez.nebula.client.util.render.gui.trait.IGUIInputListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.input.Keyboard.*;
import static org.lwjgl.opengl.GL11.*;

public class BlockSearchComponent extends GUIComponent implements IGUIInputListener
{
    private static final double PADDING = 1.0;
    private static final int MAX_LIST_SIZE = 5;
    private static final double BLOCK_DISPLAY_HEIGHT = 13.5;

    private static final int TEXTBOX_BACKGROUND_COLOR = new Color(33, 33, 33).getRGB();
    private static final int BLOCK_DISPLAY_SELECTED_COLOR = new Color(48, 48, 48).getRGB();
    private static final double TEXTBOX_HEIGHT = 15;

    private final List<BlockValue> searchResultList = new ArrayList<>();
    private final BlockSelectionCallback callback;

    private boolean typing;
    private String text = "";
    private int cursor, blinkTicks, index;
    private long lastClickTime;

    public BlockSearchComponent(final BlockSelectionCallback callback)
    {
        this.callback = callback;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        Render2D.roundedRectangle(x, y, width, TEXTBOX_HEIGHT, 3.0f, TEXTBOX_BACKGROUND_COLOR);

        if (!typing)
        {
            blinkTicks = 0;
            cursor = text.isEmpty() ? 0 : text.length();
        } else
        {
            index = 0;
            ++blinkTicks;
            if (blinkTicks > 200)
            {
                blinkTicks = 0;
            }
        }

        String renderedTextField = text.isEmpty() && !typing ? "Block name..." : text;
        double middle = Fonts.getMiddlePoint(TEXTBOX_HEIGHT, Fonts.POPPINS.getFontHeight());
        Fonts.POPPINS.drawStringShadow(renderedTextField, x + 2, y + middle, text.isEmpty() || !typing ? 0xFFAAAAAA : -1);

        if (typing && 100 < blinkTicks)
        {
            double cursorLength = 5.0;
            String value = cursor >= text.length() - 1 ? text : text.substring(0, cursor);
            double textWidth = Fonts.POPPINS.getStringWidth(value);
            double lineY = y + middle + Fonts.POPPINS.getFontHeight() - 1;
            Render2D.line(x + textWidth + 2, lineY, x + textWidth + 2 + cursorLength, lineY, 2.5f, 0xFFAAAAAA);
        }

        updateSearch();
        if (!searchResultList.isEmpty())
        {
            double posY = y + TEXTBOX_HEIGHT + PADDING;
            middle = Fonts.getMiddlePoint(BLOCK_DISPLAY_HEIGHT, Fonts.POPPINS.getFontHeight());
            int i = 0;
            for (final BlockValue blockValue : searchResultList)
            {
                ++i;
                final ItemStack itemStack = new ItemStack(blockValue.getBlock(), 1, blockValue.getSubType());
                if (itemStack.getItem() == null)
                {
                    continue;
                }

                if (i == index)
                {
                    Render2D.roundedRectangle(x, posY, width, BLOCK_DISPLAY_HEIGHT, 2.5f, BLOCK_DISPLAY_SELECTED_COLOR);
                }

                Fonts.POPPINS.drawStringShadow(itemStack.getDisplayName(), x + 2, posY + middle, -1);

                glPushMatrix();
                {
                    RenderHelper.enableGUIStandardItemLighting();
                    glTranslated(getX() + getWidth() - 14, posY + middle, 0);
                    glScaled(0.8, 0.8, 0.8);
                    Render2D.itemNoEffects(itemStack, 0, 0);
                    RenderHelper.disableStandardItemLighting();
                }
                glPopMatrix();

                posY += BLOCK_DISPLAY_HEIGHT + PADDING;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (isMouseIn(mouseX, mouseY, x, y, width, TEXTBOX_HEIGHT))
        {
            typing = !typing;
            return;
        }
        typing = false;

        double posY = y + TEXTBOX_HEIGHT + PADDING;
        int i = 0;
        for (final BlockValue blockValue : searchResultList)
        {
            ++i;
            if (isMouseIn(mouseX, mouseY, x, posY, width, BLOCK_DISPLAY_HEIGHT) && mouseButton == 0)
            {
                if (index == i && System.currentTimeMillis() - lastClickTime <= 500L)
                {
                    callback.selectBlock(blockValue);
                    index = 0;
                    break;
                }
                
                lastClickTime = System.currentTimeMillis();
                index = i;
                break;
            }
            posY += BLOCK_DISPLAY_HEIGHT + PADDING;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode)
    {
        if (!typing)
        {
            return;
        }
        switch (keyCode)
        {
            case KEY_RIGHT:
                moveCursor(1);
                break;
            case KEY_LEFT:
                moveCursor(-1);
                break;
            case KEY_DELETE:
            case KEY_BACK:
            {
                removeAtCursor(Keyboard.isKeyDown(KEY_LSHIFT));
                break;
            }
            case KEY_RETURN:
            {
                typing = false;
                break;
            }
            default:
            {
                if (ChatAllowedCharacters.isAllowedCharacter(typedChar))
                {
                    insertAtCursor(typedChar);
                }
                break;
            }
        }
    }

    private void insertAtCursor(char typedChar)
    {
        if (cursor == 0)
        {
            text = typedChar + text;
        } else if (cursor >= text.length() - 1)
        {
            // eos
            text = text + typedChar;
        } else
        {
            final String beforeCursor = text.substring(0, cursor);
            final String afterCursor = text.substring(cursor);
            text = beforeCursor + typedChar + afterCursor;
        }

        moveCursor(1);
    }

    private void removeAtCursor(boolean shift)
    {
        if (text.isEmpty() || cursor <= 0)
        {
            cursor = 0;
            return;
        }
        if (cursor >= text.length())
        {
            if (shift)
            {
                text = "";
                return;
            }
            text = text.substring(0, text.length() - 1);
            moveCursor(-1);
        } else
        {
            if (shift)
            {
                text = text.substring(0, cursor);
            } else
            {
                final String beforeCursor = text.substring(0, cursor - 1);
                final String afterCursor = text.substring(cursor);
                text = beforeCursor + afterCursor;
            }
        }
    }

    private void moveCursor(int offset)
    {
        if (text.isEmpty())
        {
            cursor = 0;
            return;
        }
        cursor = MathHelper.clamp_int(cursor + offset, 0, text.length());
    }

    private void updateSearch()
    {
        searchResultList.clear();
        if (text.trim().isEmpty())
        {
            return;
        }

        try
        {
            int blockId, subType = -1;

            if (text.contains(":"))
            {
                final String[] parts = text.split(":");
                if (parts.length == 0 || parts.length > 2)
                {
                    return;
                }
                blockId = Integer.parseInt(parts[0]);
                if (blockId > 256 || blockId <= 0)
                {
                    return;
                }
                if (parts.length == 2)
                {
                    subType = Integer.parseInt(parts[1]);
                } else
                {
                    subType = 0;
                }
            } else
            {
                blockId = Integer.parseInt(text);
            }

            final Block block = Block.getBlockById(blockId);
            if (block == null)
            {
                return;
            }
            // TODO: if subtype is -1, display all sub types of a block
            searchResultList.add(new BlockValue(block, subType == -1 ? 0 : subType));
            return;
        } catch (NumberFormatException ignored)
        {
        }

        String str = text.trim().toLowerCase();
        for (final Object o : ((IRegistryNamespaced)Block.blockRegistry).nebula$getMap().values())
        {
            if (!(o instanceof String))
            {
                continue;
            }
            final String s = (String) o;

            final String blockName = s.replace("_", "").toLowerCase();
            if (blockName.contains(str) || blockName.startsWith(str) || blockName.endsWith(str))
            {
                final Block block = Block.getBlockFromName(s);
                if (block == null || block instanceof BlockAir)
                {
                    continue;
                }

                if (Item.getItemFromBlock(block) == null)
                {
                    continue;
                }

                searchResultList.add(new BlockValue(block, 0));
            }
            if (searchResultList.size() >= MAX_LIST_SIZE)
            {
                return;
            }
        }
    }

    @Override
    public double getHeight()
    {
        double height = PADDING + TEXTBOX_HEIGHT + PADDING;
        if (!searchResultList.isEmpty())
        {
            height += searchResultList.size() * ((PADDING * 2) + BLOCK_DISPLAY_HEIGHT);
        }
        return height;
    }
}
