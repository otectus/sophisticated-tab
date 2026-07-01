package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vodmordia.modtabs.api.tabs_menu.TabBase;

// A dedicated return-to-inventory tab on top of Sophisticated Backpacks' BackpackScreen
// so the user can navigate back without closing the GUI. Drawn with our own chrome plus
// a crafting-table glyph (the 1.20.1 build reused LegendaryTabs' button atlas, which no
// longer exists under the renamed "modtabs" assets).
public final class BackToInventoryTab extends TabBase {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sophisticatedtab", "textures/gui/backpack_tab.png");

    private static final int CHROME_TEXTURE_W = 52;
    private static final int CHROME_TEXTURE_H = 22;
    private static final int HOVER_DX = 26;
    private static final int ICON_INSET_X = 5;
    private static final int ICON_INSET_Y = 3;

    private static final ItemStack INVENTORY_ICON = new ItemStack(Items.CRAFTING_TABLE);

    @Override
    public void openTargetScreen(Player player) {
        // Closing the BackpackContainer first tells the server the menu is gone
        // (otherwise it stays "open" server-side while the client renders
        // InventoryScreen → guaranteed desync on next click). The coordinator
        // sends the close packet without invoking setScreen(null), then swaps
        // to InventoryScreen on the same tick.
        BackpackOpenCoordinator.openInventoryFromBackpack(player);
    }

    @Override
    public boolean isEnabled(Player player) {
        return true;
    }

    @Override
    public void initTabOnScreens() {
        // Registration is handled by LegendaryTabsCompat.register() via addTabToScreen,
        // not here — this tab is never passed to TabsMenu.register(), so this is never called.
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, boolean hover) {
        int u = hover ? HOVER_DX : 0;
        gui.blit(TEXTURE, x, y, u, 0, TAB_WIDTH, TAB_HEIGHT, CHROME_TEXTURE_W, CHROME_TEXTURE_H);
        gui.renderItem(INVENTORY_ICON, x + ICON_INSET_X, y + ICON_INSET_Y);
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
