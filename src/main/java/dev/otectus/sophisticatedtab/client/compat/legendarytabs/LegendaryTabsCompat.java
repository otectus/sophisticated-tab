package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

public final class LegendaryTabsCompat {
    // Pre-register this many indexed BackpackTab instances. LegendaryTabs
    // requires tabs to be declared at client setup, so we reserve a finite pool;
    // disabled tabs collapse out of the layout via TabsMenu.enabledTabs.
    // Eight covers a Curios back slot plus several main-inventory backpacks
    // comfortably; the 9th+ remain openable via SB's default keybind.
    private static final int BACKPACK_TAB_POOL = 8;

    private LegendaryTabsCompat() {}

    public static void register() {
        TabsMenu.register(new BackToInventoryTab());
        for (int i = 0; i < BACKPACK_TAB_POOL; i++) {
            TabsMenu.register(new BackpackTab(i));
        }
    }
}
