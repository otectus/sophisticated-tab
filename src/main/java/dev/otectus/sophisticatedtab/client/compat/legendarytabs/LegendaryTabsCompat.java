package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import vodmordia.modtabs.api.tabs_menu.TabsMenu;
import vodmordia.modtabs.config.Config;

// Registers our tabs with Mod Tabs at client setup.
//
// IMPORTANT: the static tabs are added ONLY via TabsMenu.addTabToScreen — they are NOT
// passed to TabsMenu.register(). Mod Tabs' built-in InventoryTab / SophisticatedBackpacksTab
// mark the inventory / backpack screens as "all-tabs" screens, and
// TabsMenu.finalizePendingRegistrations() then re-adds every globally-registered tab to those
// screens at a different priority bucket. Since ScreenInfo.addTab only de-dups within a single
// priority bucket, registering a tab both globally (register) and per-screen (addTabToScreen)
// makes it appear twice. Adding only via addTabToScreen keeps each static tab a single,
// correctly-scoped instance (Settings on inventory+backpack; Back only on the backpack screen).
//
// The per-backpack tabs are produced on demand by a DynamicTabProvider, so there is no fixed
// pool of indexed instances.
public final class LegendaryTabsCompat {

    private static final int VANILLA_INVENTORY_WIDTH = 176;
    private static final int VANILLA_INVENTORY_HEIGHT = 166;

    private static boolean registered;

    private LegendaryTabsCompat() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Suppress Mod Tabs' built-in single "Sophisticated Backpacks" tab — it is redundant
        // with our per-backpack tabs. This is its public config flag (no reflection); re-applied
        // every launch. Note: if the user opens AND saves Mod Tabs' config screen, MidnightLib may
        // re-bake and re-enable it until the next launch.
        try {
            Config.Baked.sophisticatedBackpacksTabEnabled = false;
        } catch (Throwable ignored) {
            // Mod Tabs absent or its config layout changed — harmless to skip.
        }

        SettingsTab settings = new SettingsTab();
        BackToInventoryTab back = new BackToInventoryTab();

        // Settings tab: on the player inventory and on backpack screens.
        TabsMenu.addTabToScreen(settings, InventoryScreen.class,
                p -> VANILLA_INVENTORY_WIDTH,
                p -> VANILLA_INVENTORY_HEIGHT,
                100);
        TabsMenu.addTabToScreen(settings, BackpackScreen.class,
                SophisticatedBackpacksSizing::getWidth,
                SophisticatedBackpacksSizing::getHeight,
                100);

        // Back-to-inventory tab: only on backpack screens.
        TabsMenu.addTabToScreen(back, BackpackScreen.class,
                SophisticatedBackpacksSizing::getWidth,
                SophisticatedBackpacksSizing::getHeight,
                10);

        // One tab per carried-and-visible backpack, contributed on demand.
        TabsMenu.registerDynamicProvider(new BackpackTabProvider());
    }
}
