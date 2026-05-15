package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.Collections;
import java.util.List;

import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

public final class LegendaryTabsCompat {
    // Pre-register this many indexed BackpackTab instances. LegendaryTabs
    // requires tabs to be declared at client setup, so we reserve a finite pool;
    // disabled tabs collapse out of the layout via TabsMenu.enabledTabs.
    // Eight covers a Curios back slot plus several main-inventory backpacks
    // comfortably; the 9th+ remain openable via SB's default keybind.
    public static final int BACKPACK_TAB_POOL = 8;

    private static List<BackpackTab> BACKPACK_TABS = Collections.emptyList();

    private LegendaryTabsCompat() {}

    public static void register() {
        TabsMenu.register(new BackToInventoryTab());

        java.util.ArrayList<BackpackTab> tabs = new java.util.ArrayList<>(BACKPACK_TAB_POOL);
        for (int i = 0; i < BACKPACK_TAB_POOL; i++) {
            BackpackTab tab = new BackpackTab(i);
            TabsMenu.register(tab);
            tabs.add(tab);
        }
        BACKPACK_TABS = Collections.unmodifiableList(tabs);

        TabsMenu.register(new SettingsTab());
    }

    // Backpack tabs in registration order (index 0..N-1). Empty until register()
    // runs at client setup. Used by TabInteractionHandler to identify which
    // TabButton on the active screen corresponds to which logical index.
    public static List<BackpackTab> backpackTabs() {
        return BACKPACK_TABS;
    }
}
