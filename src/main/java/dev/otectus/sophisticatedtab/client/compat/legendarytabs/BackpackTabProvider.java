package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import vodmordia.modtabs.api.tabs_menu.DynamicTabProvider;
import vodmordia.modtabs.api.tabs_menu.TabBase;

// Emits one BackpackTab per carried-and-visible backpack. Mod Tabs calls contribute()
// during the tab-bar rebuild of every registered tabbed screen; we limit ourselves to
// the player inventory and Sophisticated Backpacks' own screen (the two surfaces the
// old LegendaryTabs port attached to), and honor the user's hide/order preferences via
// BackpackTabResolver. Because the bar is rebuilt on every screen init, drag-reorder /
// hide changes show up after a TabsMenu.reinitCurrentScreen() (or the next screen open).
public final class BackpackTabProvider implements DynamicTabProvider {

    @Override
    public void contribute(Player player, Screen screen, List<TabBase> out) {
        if (player == null) {
            return;
        }
        if (!(screen instanceof InventoryScreen) && !(screen instanceof BackpackScreen)) {
            return;
        }
        for (BackpackDescriptor descriptor : BackpackTabResolver.visible(player)) {
            out.add(new BackpackTab(descriptor));
        }
    }
}
