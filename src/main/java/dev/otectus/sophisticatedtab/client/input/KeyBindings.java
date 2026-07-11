package dev.otectus.sophisticatedtab.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.otectus.sophisticatedtab.client.gui.BackpackSettingsScreen;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Keybinds registered on the mod bus. Polled from ClientBootstrap's tick
// listener; default mappings are UNKNOWN so they never collide with another mod.
// Users assign keys in Options > Controls > Sophisticated Tab.
public final class KeyBindings {

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.sophisticatedtab.openSettings",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.sophisticatedtab");

    // Toggles the global "Settings tab hidden" flag. Since the Settings tab is
    // the primary way to reach the settings screen, this keybind is the only
    // way to bring the tab back once hidden — users should bind it.
    public static final KeyMapping TOGGLE_SETTINGS_TAB = new KeyMapping(
            "key.sophisticatedtab.toggleSettingsTab",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.sophisticatedtab");

    private KeyBindings() {}

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
        event.register(TOGGLE_SETTINGS_TAB);
    }

    // Called once per client tick from ClientBootstrap. consumeClick drains the
    // press buffer so a held key fires once per press, not once per tick.
    public static void pollKeys() {
        while (OPEN_SETTINGS.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new BackpackSettingsScreen(null));
            }
        }
        // Flip the flag regardless of screen state; the tab row rebuilds on the
        // next screen init (i.e. next inventory/backpack open).
        while (TOGGLE_SETTINGS_TAB.consumeClick()) {
            BackpackTabPreferences.setSettingsTabHidden(!BackpackTabPreferences.isSettingsTabHidden());
        }
    }
}
