package net.minecraft.client.gui;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GuiTextField extends Gui
{
    private final FontRenderer fontRenderer;
    public final int posX;
    public final int posY;
    public final int width;
    public final int height;
    private String text = "";
    private int maxTextLength = 32;
    private int cursorCounter;
    private boolean field_146215_m = true;
    private boolean field_146212_n = true;
    private boolean focused;
    private boolean enabled = true;
    private int field_146225_q;
    private int cursorTextOffset;
    private int cursorPosition;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;
    private boolean field_146220_v = true;

    public GuiTextField(FontRenderer par1FontRenderer, int x, int y, int width, int height)
    {
        this.fontRenderer = par1FontRenderer;
        this.posX = x;
        this.posY = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Increments the cursor counter
     */
    public void updateCursorCounter()
    {
        ++this.cursorCounter;
    }

    /**
     * Sets the text of the textbox
     */
    public void setText(String text)
    {
        if (text.length() > this.maxTextLength)
        {
            this.text = text.substring(0, this.maxTextLength);
        } else
        {
            this.text = text;
        }

        this.func_146202_e();
    }

    /**
     * Returns the contents of the textbox
     */
    public String getText()
    {
        return this.text;
    }

    public String func_146207_c()
    {
        int var1 = Math.min(this.cursorTextOffset, this.cursorPosition);
        int var2 = Math.max(this.cursorTextOffset, this.cursorPosition);
        return this.text.substring(var1, var2);
    }

    public void func_146191_b(String input)
    {
        String var2 = "";
        String sanitized = ChatAllowedCharacters.filerAllowedCharacters(input);
        int var4 = Math.min(this.cursorTextOffset, this.cursorPosition);
        int var5 = Math.max(this.cursorTextOffset, this.cursorPosition);
        int var6 = this.maxTextLength - this.text.length() - (var4 - this.cursorPosition);
        boolean var7 = false;

        if (!this.text.isEmpty())
        {
            var2 = var2 + this.text.substring(0, var4);
        }

        int length;

        if (var6 < sanitized.length())
        {
            var2 = var2 + sanitized.substring(0, var6);
            length = var6;
        } else
        {
            var2 = var2 + sanitized;
            length = sanitized.length();
        }

        if (!this.text.isEmpty() && var5 < this.text.length())
        {
            var2 = var2 + this.text.substring(var5);
        }

        this.text = var2;
        // System.out.println(var2);
        this.func_146182_d(var4 - this.cursorPosition + length);
    }

    public void func_146177_a(int p_146177_1_)
    {
        if (!this.text.isEmpty())
        {
            if (this.cursorPosition != this.cursorTextOffset)
            {
                this.func_146191_b("");
            } else
            {
                this.func_146175_b(this.func_146187_c(p_146177_1_) - this.cursorTextOffset);
            }
        }
    }

    public void func_146175_b(int p_146175_1_)
    {
        if (!this.text.isEmpty())
        {
            if (this.cursorPosition != this.cursorTextOffset)
            {
                this.func_146191_b("");
            } else
            {
                boolean var2 = p_146175_1_ < 0;
                int var3 = var2 ? this.cursorTextOffset + p_146175_1_ : this.cursorTextOffset;
                int var4 = var2 ? this.cursorTextOffset : this.cursorTextOffset + p_146175_1_;
                String var5 = "";

                if (var3 >= 0)
                {
                    var5 = this.text.substring(0, var3);
                }

                if (var4 < this.text.length())
                {
                    var5 = var5 + this.text.substring(var4);
                }

                this.text = var5;

                if (var2)
                {
                    this.func_146182_d(p_146175_1_);
                }
            }
        }
    }

    public int func_146187_c(int p_146187_1_)
    {
        return this.func_146183_a(p_146187_1_, this.func_146198_h());
    }

    public int func_146183_a(int p_146183_1_, int p_146183_2_)
    {
        return this.func_146197_a(p_146183_1_, this.func_146198_h(), true);
    }

    public int func_146197_a(int p_146197_1_, int p_146197_2_, boolean p_146197_3_)
    {
        int var4 = p_146197_2_;
        boolean var5 = p_146197_1_ < 0;
        int var6 = Math.abs(p_146197_1_);

        for (int var7 = 0; var7 < var6; ++var7)
        {
            if (var5)
            {
                while (p_146197_3_ && var4 > 0 && this.text.charAt(var4 - 1) == 32)
                {
                    --var4;
                }

                while (var4 > 0 && this.text.charAt(var4 - 1) != 32)
                {
                    --var4;
                }
            } else
            {
                int var8 = this.text.length();
                var4 = this.text.indexOf(32, var4);

                if (var4 == -1)
                {
                    var4 = var8;
                } else
                {
                    while (p_146197_3_ && var4 < var8 && this.text.charAt(var4) == 32)
                    {
                        ++var4;
                    }
                }
            }
        }

        return var4;
    }

    public void func_146182_d(int p_146182_1_)
    {
        this.func_146190_e(this.cursorPosition + p_146182_1_);
    }

    public void func_146190_e(int offset)
    {
        this.cursorTextOffset = offset;
        int length = this.text.length();

        if (this.cursorTextOffset < 0)
        {
            this.cursorTextOffset = 0;
        }

        if (this.cursorTextOffset > length)
        {
            this.cursorTextOffset = length;
        }

        this.func_146199_i(this.cursorTextOffset);
    }

    public void func_146196_d()
    {
        this.func_146190_e(0);
    }

    public void func_146202_e()
    {
        this.func_146190_e(this.text.length());
    }

    /**
     * Call this method from your GuiScreen to process the keys into the textbox
     */
    public boolean textboxKeyTyped(char typedChar, int keyCode)
    {
        if (!this.focused)
        {
            return false;
        } else
        {
            switch (typedChar)
            {
                case 1:
                    this.func_146202_e();
                    this.func_146199_i(0);
                    return true;

                case 3:
                    GuiScreen.setClipboardString(this.func_146207_c());
                    return true;

                case 22:
                    if (this.enabled)
                    {
                        this.func_146191_b(GuiScreen.getClipboardString());
                    }

                    return true;

                case 24:
                    GuiScreen.setClipboardString(this.func_146207_c());

                    if (this.enabled)
                    {
                        this.func_146191_b("");
                    }

                    return true;

                default:
                    switch (keyCode)
                    {
                        case Keyboard.KEY_BACK:
                            if (GuiScreen.isCtrlKeyDown())
                            {
                                if (this.enabled)
                                {
                                    this.func_146177_a(-1);
                                }
                            } else if (this.enabled)
                            {
                                this.func_146175_b(-1);
                            }

                            return true;

                        case Keyboard.KEY_HOME:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                this.func_146199_i(0);
                            } else
                            {
                                this.func_146196_d();
                            }

                            return true;

                        case Keyboard.KEY_LEFT:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                if (GuiScreen.isCtrlKeyDown())
                                {
                                    this.func_146199_i(this.func_146183_a(-1, this.func_146186_n()));
                                } else
                                {
                                    this.func_146199_i(this.func_146186_n() - 1);
                                }
                            } else if (GuiScreen.isCtrlKeyDown())
                            {
                                this.func_146190_e(this.func_146187_c(-1));
                            } else
                            {
                                this.func_146182_d(-1);
                            }

                            return true;

                        case Keyboard.KEY_RIGHT:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                if (GuiScreen.isCtrlKeyDown())
                                {
                                    this.func_146199_i(this.func_146183_a(1, this.func_146186_n()));
                                } else
                                {
                                    this.func_146199_i(this.func_146186_n() + 1);
                                }
                            } else if (GuiScreen.isCtrlKeyDown())
                            {
                                this.func_146190_e(this.func_146187_c(1));
                            } else
                            {
                                this.func_146182_d(1);
                            }

                            return true;

                        case Keyboard.KEY_END:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                this.func_146199_i(this.text.length());
                            } else
                            {
                                this.func_146202_e();
                            }

                            return true;

                        case Keyboard.KEY_DELETE:
                            if (GuiScreen.isCtrlKeyDown())
                            {
                                if (this.enabled)
                                {
                                    this.func_146177_a(1);
                                }
                            } else if (this.enabled)
                            {
                                this.func_146175_b(1);
                            }

                            return true;

                        default:
                            if (ChatAllowedCharacters.isAllowedCharacter(typedChar))
                            {
                                if (this.enabled)
                                {
                                    this.func_146191_b(Character.toString(typedChar));
                                }

                                return true;
                            } else
                            {
                                return false;
                            }
                    }
            }
        }
    }

    /**
     * Args: x, y, buttonClicked
     */
    public void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        boolean isInBounds = mouseX >= this.posX && mouseX < this.posX + this.width && mouseY >= this.posY && mouseY < this.posY + this.height;

        if (this.field_146212_n)
        {
            this.setFocused(isInBounds);
        }

        if (this.focused && mouseButton == 0)
        {
            int deltaX = mouseX - this.posX;
            if (this.field_146215_m)
            {
                deltaX -= 4;
            }

            String var6 = this.fontRenderer.trimStringToWidth(this.text.substring(this.field_146225_q), this.func_146200_o());
            this.func_146190_e(this.fontRenderer.trimStringToWidth(var6, deltaX).length() + this.field_146225_q);
        }
    }

    /**
     * Draws the textbox
     */
    public void drawTextBox()
    {
        if (this.func_146176_q())
        {
            if (this.func_146181_i())
            {
                drawRect(this.posX - 1, this.posY - 1, this.posX + this.width + 1, this.posY + this.height + 1, -6250336);
                drawRect(this.posX, this.posY, this.posX + this.width, this.posY + this.height, -16777216);
            }

            int var1 = this.enabled ? this.enabledColor : this.disabledColor;
            int var2 = this.cursorTextOffset - this.field_146225_q;
            int var3 = this.cursorPosition - this.field_146225_q;
            String var4 = this.fontRenderer.trimStringToWidth(this.text.substring(this.field_146225_q), this.func_146200_o());
            boolean var5 = var2 >= 0 && var2 <= var4.length();
            boolean var6 = this.focused && this.cursorCounter / 6 % 2 == 0 && var5;
            int var7 = this.field_146215_m ? this.posX + 4 : this.posX;
            int var8 = this.field_146215_m ? this.posY + (this.height - 8) / 2 : this.posY;
            int var9 = var7;

            if (var3 > var4.length())
            {
                var3 = var4.length();
            }

            if (var4.length() > 0)
            {
                String var10 = var5 ? var4.substring(0, var2) : var4;
                var9 = this.fontRenderer.drawStringWithShadow(var10, var7, var8, var1);
            }

            boolean var13 = this.cursorTextOffset < this.text.length() || this.text.length() >= this.getMaxTextLength();
            int var11 = var9;

            if (!var5)
            {
                var11 = var2 > 0 ? var7 + this.width : var7;
            } else if (var13)
            {
                var11 = var9 - 1;
                --var9;
            }

            if (var4.length() > 0 && var5 && var2 < var4.length())
            {
                this.fontRenderer.drawStringWithShadow(var4.substring(var2), var9, var8, var1);
            }

            if (var6)
            {
                if (var13)
                {
                    Gui.drawRect(var11, var8 - 1, var11 + 1, var8 + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
                } else
                {
                    this.fontRenderer.drawStringWithShadow("_", var11, var8, var1);
                }
            }

            if (var3 != var2)
            {
                int var12 = var7 + this.fontRenderer.getStringWidth(var4.substring(0, var3));
                this.func_146188_c(var11, var8 - 1, var12 - 1, var8 + 1 + this.fontRenderer.FONT_HEIGHT);
            }
        }
    }

    private void func_146188_c(int p_146188_1_, int p_146188_2_, int p_146188_3_, int p_146188_4_)
    {
        int var5;

        if (p_146188_1_ < p_146188_3_)
        {
            var5 = p_146188_1_;
            p_146188_1_ = p_146188_3_;
            p_146188_3_ = var5;
        }

        if (p_146188_2_ < p_146188_4_)
        {
            var5 = p_146188_2_;
            p_146188_2_ = p_146188_4_;
            p_146188_4_ = var5;
        }

        if (p_146188_3_ > this.posX + this.width)
        {
            p_146188_3_ = this.posX + this.width;
        }

        if (p_146188_1_ > this.posX + this.width)
        {
            p_146188_1_ = this.posX + this.width;
        }

        Tessellator var6 = Tessellator.instance;
        GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glLogicOp(GL11.GL_OR_REVERSE);
        var6.startDrawingQuads();
        var6.addVertex(p_146188_1_, p_146188_4_, 0.0D);
        var6.addVertex(p_146188_3_, p_146188_4_, 0.0D);
        var6.addVertex(p_146188_3_, p_146188_2_, 0.0D);
        var6.addVertex(p_146188_1_, p_146188_2_, 0.0D);
        var6.draw();
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public void setMaxTextLength(int length)
    {
        this.maxTextLength = length;

        if (this.text.length() > length)
        {
            this.text = this.text.substring(0, length);
        }
    }

    public int getMaxTextLength()
    {
        return this.maxTextLength;
    }

    public int func_146198_h()
    {
        return this.cursorTextOffset;
    }

    public boolean func_146181_i()
    {
        return this.field_146215_m;
    }

    public void func_146185_a(boolean p_146185_1_)
    {
        this.field_146215_m = p_146185_1_;
    }

    public void setEnabledColor(int p_146193_1_)
    {
        this.enabledColor = p_146193_1_;
    }

    public void setDisabledColor(int p_146204_1_)
    {
        this.disabledColor = p_146204_1_;
    }

    /**
     * Sets focus to this gui element
     */
    public void setFocused(boolean focused)
    {
        if (focused && !this.focused)
        {
            this.cursorCounter = 0;
        }

        this.focused = focused;
    }

    /**
     * Getter for the focused field
     */
    public boolean isFocused()
    {
        return this.focused;
    }

    public void setEnabled(boolean p_146184_1_)
    {
        this.enabled = p_146184_1_;
    }

    public int func_146186_n()
    {
        return this.cursorPosition;
    }

    public int func_146200_o()
    {
        return this.func_146181_i() ? this.width - 8 : this.width;
    }

    public void func_146199_i(int p_146199_1_)
    {
        int var2 = this.text.length();

        if (p_146199_1_ > var2)
        {
            p_146199_1_ = var2;
        }

        if (p_146199_1_ < 0)
        {
            p_146199_1_ = 0;
        }

        this.cursorPosition = p_146199_1_;

        if (this.fontRenderer != null)
        {
            if (this.field_146225_q > var2)
            {
                this.field_146225_q = var2;
            }

            int var3 = this.func_146200_o();
            String var4 = this.fontRenderer.trimStringToWidth(this.text.substring(this.field_146225_q), var3);
            int var5 = var4.length() + this.field_146225_q;

            if (p_146199_1_ == this.field_146225_q)
            {
                this.field_146225_q -= this.fontRenderer.trimStringToWidth(this.text, var3, true).length();
            }

            if (p_146199_1_ > var5)
            {
                this.field_146225_q += p_146199_1_ - var5;
            } else if (p_146199_1_ <= this.field_146225_q)
            {
                this.field_146225_q -= this.field_146225_q - p_146199_1_;
            }

            if (this.field_146225_q < 0)
            {
                this.field_146225_q = 0;
            }

            if (this.field_146225_q > var2)
            {
                this.field_146225_q = var2;
            }
        }
    }

    public void func_146205_d(boolean p_146205_1_)
    {
        this.field_146212_n = p_146205_1_;
    }

    public boolean func_146176_q()
    {
        return this.field_146220_v;
    }

    public void func_146189_e(boolean p_146189_1_)
    {
        this.field_146220_v = p_146189_1_;
    }
}
