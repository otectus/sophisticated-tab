package dev.otectus.sophisticatedtab.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.otectus.sophisticatedtab.client.gui.BackpackSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// Single keybind registered on the mod bus. Polled from ClientBootstrap's tick
// listener; default mapping is UNKNOWN so it never collides with another mod.
// Users assign a key in Options > Controls > Sophisticated Tab.
public final class KeyBindings {

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.sophisticatedtab.openSettings",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.sophisticatedtab");

    private KeyBindings() {}

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    // Called once per client tick from ClientBootstrap. consumeClick drains the
    // press buffer so a held key fires once per press, not once per tick.
    public static void pollSettingsKey() {
        while (OPEN_SETTINGS.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new BackpackSettingsScreen(null));
            }
        }
    }
}
