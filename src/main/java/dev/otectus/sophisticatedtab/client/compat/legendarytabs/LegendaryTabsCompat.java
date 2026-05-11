package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

public final class LegendaryTabsCompat {
    private LegendaryTabsCompat() {}

    public static void register() {
        TabsMenu.register(new BackToInventoryTab());
        TabsMenu.register(new SophisticatedBackpacksTab());
    }
}
