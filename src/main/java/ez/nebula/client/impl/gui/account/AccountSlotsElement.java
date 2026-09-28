package ez.nebula.client.impl.gui.account;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.account.Account;
import ez.nebula.client.mixin.duck.IMinecraft;
import ez.nebula.client.util.render.HeadDownloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Session;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author xgraza
 * @since 04/03/25
 */
final class AccountSlotsElement extends GuiSlot
{
    private final GuiScreen parent;
    private int selected;

    public AccountSlotsElement(final Minecraft mc, final GuiScreen parent)
    {
        super(mc, parent.width, parent.height, 32, parent.height - 60, 27);
        this.parent = parent;
        this.selected = 0;
    }

    @Override
    protected int getSize()
    {
        return Nebula.ACCOUNTS.getAll().size();
    }

    @Override
    protected void elementClicked(int var1, boolean var2, int var3, int var4)
    {
        if (var2)
        {
            final List<Account> accountList = Nebula.ACCOUNTS.getAll();
            if (var1 > accountList.size() - 1)
            {
                return;
            }
            final Account account = accountList.get(var1);
            if (account != null)
            {
                ((IMinecraft)Minecraft.getMinecraft()).nebula$setSession(new Session(account.getUsername(), "", ""));
            }
        } else
        {
            selected = var1;
        }
    }

    @Override
    protected boolean isSelected(final int var1)
    {
        return selected == var1;
    }

    @Override
    protected void drawBackground()
    {
        parent.drawDefaultBackground();
    }

    @Override
    protected void drawSlot(int var1, int var2, int var3, int var4, Tessellator var5, int var6, int var7)
    {
        final List<Account> accountList = Nebula.ACCOUNTS.getAll();
        if (var1 > accountList.size() - 1 || var1 < 0)
        {
            return;
        }

        final Account account = accountList.get(var1);

        final int textureSize = 27 - 4;
        final DynamicTexture texture = HeadDownloader.getOrDownloadTexture(
                account.getUsername(), textureSize);
        if (texture != null)
        {
            glPushMatrix();
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            glBindTexture(GL_TEXTURE_2D, texture.getGlTextureId());
            glBegin(GL_QUADS);
            {
                glTexCoord2d(0, 0);
                glVertex2d(var2 + 1, var3 + 0.5);

                glTexCoord2d(0, 1);
                glVertex2d(var2 + 1, var3 + textureSize);

                glTexCoord2d(1, 1);
                glVertex2d(var2 + textureSize + 1, var3 + textureSize);

                glTexCoord2d(1, 0);
                glVertex2d(var2 + textureSize + 1, var3 + 0.5);
            }
            glEnd();
            glPopMatrix();
        }
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(account.getUsername(),
                var2 + (texture != null ? textureSize + 5 : 2),
                var3 + (27 / 2) - (Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2), -5592406);
    }

    public int getSelected()
    {
        return selected;
    }
}
