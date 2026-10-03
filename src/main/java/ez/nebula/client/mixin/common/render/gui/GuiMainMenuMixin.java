package ez.nebula.client.mixin.common.render.gui;

import ez.nebula.client.impl.gui.account.AccountSelectorScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class GuiMainMenuMixin extends GuiScreen
{
    @Inject(method = "addSingleplayerMultiplayerButtons", at = @At("TAIL"))
    private void hook$addSingleplayerMultiplayerButtons(int p_73969_1_, int p_73969_2_, CallbackInfo info)
    {
        buttonList.add(new GuiButton(420, 4, 4, 65, 20, "alfheim.pw"));
        buttonList.add(new GuiButton(69, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2 + 24, "Account Manager"));
    }

    @Inject(method = "initGui", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 2, shift = At.Shift.AFTER))
    private void hook$initGui$shiftButtonsDown(CallbackInfo info)
    {
        for (int i = 0; i < 3; ++i)
        {
            ((GuiButton)buttonList.get(buttonList.size() - 1 - i)).yPosition += 20;
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void hook$actionPreformed(GuiButton button, CallbackInfo info)
    {
        if (button.id == 420)
        {
            final ServerAddress address = ServerAddress.func_78860_a("alfheim.pw");
            mc.displayGuiScreen(new GuiConnecting(this, mc, address.getIP(), address.getPort()));
        } else if (button.id == 69)
        {
            mc.displayGuiScreen(new AccountSelectorScreen());
        }
    }
}
