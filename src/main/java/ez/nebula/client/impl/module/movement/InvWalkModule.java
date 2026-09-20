package ez.nebula.client.impl.module.movement;

import com.google.common.collect.Lists;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.gui.hud.HUDEditorScreen;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

/**
 * @author xgraza
 * @since 03/01/25
 */
@SuppressWarnings("unchecked")
@ModuleManifest(name = "InvWalk",
        description = "Allows you to freely walk in GUIs",
        category = ModuleCategory.MOVEMENT)
public final class InvWalkModule extends Module
{
    // preset GUI screens that require keyboard input that may be annoying to use with inv walk
    private static final List<Class<? extends GuiScreen>> SCREEN_BLACKLIST = Lists.newArrayList(
            GuiChat.class,
            HUDEditorScreen.class,
            GuiContainerCreative.class,
            GuiCommandBlock.class,
            GuiEditSign.class,
            GuiScreenBook.class,
            GuiRepair.class);
    private static final int MOUSE_KEY_OFFSET = 100;

    private final Setting<Boolean> allowBlacklistedGUIsSetting = builder("Allow Blacklisted GUIs", false)
            .setDescription("If to allow movement in all GUIs regardless of their issues")
            .build();

    private final KeyBinding[] moveKeyBindings = new KeyBinding[6];

    @Override
    public void onEnable()
    {
        super.onEnable();
        moveKeyBindings[0] = MC.gameSettings.keyBindForward;
        moveKeyBindings[1] = MC.gameSettings.keyBindBack;
        moveKeyBindings[2] = MC.gameSettings.keyBindRight;
        moveKeyBindings[3] = MC.gameSettings.keyBindLeft;
        moveKeyBindings[4] = MC.gameSettings.keyBindJump;
        moveKeyBindings[5] = MC.gameSettings.keyBindSprint;
    }

    @Override
    public void onDisable()
    {
        super.onDisable();

        Arrays.fill(moveKeyBindings, null);
        if (MC.currentScreen != null)
        {
            MC.currentScreen.allowUserInput = false;
        }
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (MC.currentScreen == null || isScreenBlacklisted())
        {
            return;
        }
        MC.currentScreen.allowUserInput = true;
        for (final KeyBinding keyBinding : moveKeyBindings)
        {
            final int keyCode = keyBinding.getKeyCode();
            keyBinding.pressed = keyCode < -MOUSE_KEY_OFFSET
                    ? Mouse.isButtonDown(keyCode - MOUSE_KEY_OFFSET)
                    : Keyboard.isKeyDown(keyCode);
        }
    };

    private boolean isScreenBlacklisted()
    {
        return MC.currentScreen != null
                && SCREEN_BLACKLIST.contains(MC.currentScreen.getClass())
                && !allowBlacklistedGUIsSetting.getValue();
    }
}
