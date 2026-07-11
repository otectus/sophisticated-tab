package dev.otectus.sophisticatedtab.client.gui;

import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Context menu anchored under a right-clicked Settings tab. Offers a single
// action: hide the Settings tab (recoverable via the toggle keybind). Returning
// to the parent screen re-runs its init(), so the tab drops from the row at once.
public final class SettingsTabContextMenu extends AbstractTabContextMenu {

    public SettingsTabContextMenu(Screen parent, int anchorX, int anchorY) {
        super(Component.translatable("gui.sophisticatedtab.context.title"), parent, anchorX, anchorY);
    }

    @Override
    protected Entry[] entries() {
        return new Entry[] {
                new Entry(Component.translatable("gui.sophisticatedtab.context.hideSettings"), true,
                        () -> { BackpackTabPreferences.setSettingsTabHidden(true); back(); }),
        };
    }
}
