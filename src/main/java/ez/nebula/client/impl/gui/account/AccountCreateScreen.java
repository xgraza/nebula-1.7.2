package ez.nebula.client.impl.gui.account;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.account.Account;
import ez.nebula.client.util.render.font.Fonts;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/**
 * @author xgraza
 * @since 04/03/25
 */
final class AccountCreateScreen extends GuiScreen
{
    private GuiTextField usernameTextField, passwordTextField;

    @Override
    public void initGui()
    {
        buttonList.clear();

        Keyboard.enableRepeatEvents(true);

        final int middleX = width / 2;
        final int middleY = height / 2;
        usernameTextField = new GuiTextField(mc.fontRenderer, middleX - 88, middleY - 130, 176, 20);
        passwordTextField = new GuiTextField(mc.fontRenderer, middleX - 88, middleY - 90, 176, 20);

        buttonList.add(new GuiButton(0, middleX - 100, middleY - 40, "Add"));
        buttonList.add(new GuiButton(1, middleX - 100, middleY - 18, "Go Back"));
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        usernameTextField.updateCursorCounter();
        passwordTextField.updateCursorCounter();
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
    {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        usernameTextField.drawTextBox();
        passwordTextField.drawTextBox();

        Fonts.POPPINS.drawStringShadow("Username",
                usernameTextField.xPosition,
                usernameTextField.yPosition - Fonts.POPPINS.getFontHeight() - 2,
                -1);

        Fonts.POPPINS.drawStringShadow("Password (optional)",
                passwordTextField.xPosition,
                passwordTextField.yPosition - Fonts.POPPINS.getFontHeight() - 2,
                -1);
    }

    @Override
    protected void actionPerformed(final GuiButton button)
    {
        if (button.id == 0)
        {
            if (usernameTextField.getText().isEmpty())
            {
                return;
            }
            final Account account = new Account(usernameTextField.getText());
            if (!passwordTextField.getText().isEmpty())
            {
                account.setPassword(passwordTextField.getText());
            }
            Nebula.ACCOUNTS.add(account);
        }
        mc.displayGuiScreen(new AccountSelectorScreen());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        usernameTextField.mouseClicked(mouseX, mouseY, mouseButton);
        passwordTextField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
    {
        super.keyTyped(typedChar, keyCode);
        usernameTextField.textboxKeyTyped(typedChar, keyCode);
        passwordTextField.textboxKeyTyped(typedChar, keyCode);
    }
}
