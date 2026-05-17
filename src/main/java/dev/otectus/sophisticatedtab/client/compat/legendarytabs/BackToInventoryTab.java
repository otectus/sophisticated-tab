package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

// LegendaryTabs' built-in InventoryTab only attaches to a hard-coded screen
// list that does not include SophisticatedBackpacks' BackpackScreen. We add a
// dedicated return-to-inventory tab on top of the SB screen so the user can
// navigate back without closing the GUI.
public final class BackToInventoryTab extends TabBase {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("legendarytabs", "textures/gui/tab_menu_buttons.png");

    // Inventory sprite position in tab_menu_buttons.png — same coordinates
    // LegendaryTabs uses for its own InventoryTab.
    private static final int ICON_U = 0;
    private static final int ICON_V = 0;
    private static final int HOVER_DX = 54;

    // Priority < SophisticatedBackpacksTab's 20 so this tab appears first.
    private static final int TAB_PRIORITY = 10;

    @Override
    public void openTargetScreen(Player player) {
        // Closing the BackpackContainer via the vanilla path first tells the
        // server the menu is gone (otherwise it stays "open" server-side while
        // the client renders InventoryScreen → guaranteed desync on next click).
        // The deferred setScreen runs on the next client tick.
        BackpackOpenCoordinator.openInventoryFromBackpack(player);
    }

    @Override
    public boolean isEnabled(Player player) {
        return true;
    }

    @Override
    public void initTabOnScreens() {
        TabsMenu.addTabToScreen(this, BackpackScreen.class,
                SophisticatedBackpacksSizing::getWidth,
                SophisticatedBackpacksSizing::getHeight,
                TAB_PRIORITY);
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, boolean hover) {
        int u = ICON_U + (hover ? HOVER_DX : 0);
        gui.blit(TEXTURE, x, y, u, ICON_V, TAB_WIDTH, TAB_HEIGHT);
    }

    @Override
    public boolean isCurrentlyUsed(Screen currentScreen) {
        return currentScreen instanceof InventoryScreen;
    }

    @Override
    public Component getTooltip() {
        return Component.translatable("tooltip.sophisticatedtab.tab.inventory");
    }
}
