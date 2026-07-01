package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import dev.otectus.sophisticatedtab.client.gui.BackpackSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vodmordia.modtabs.api.tabs_menu.TabBase;

// Mod Tabs row tab that opens the Sophisticated Tab settings screen. Reuses the backpack
// tab chrome and overlays a comparator ItemStack as the "settings" glyph — keeps the
// iconography in line with the rest of the row (real items, not flat icons). It is added to
// the inventory + backpack screens by LegendaryTabsCompat.register() via addTabToScreen.
public final class SettingsTab extends TabBase {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sophisticatedtab", "textures/gui/backpack_tab.png");
    private static final int CHROME_TEXTURE_W = 52;
    private static final int CHROME_TEXTURE_H = 22;
    private static final int HOVER_DX = 26;
    private static final int ICON_INSET_X = 5;
    private static final int ICON_INSET_Y = 3;

    private static final ItemStack GEAR_ICON = new ItemStack(Items.COMPARATOR);

    @Override
    public boolean isEnabled(Player player) {
        return true;
    }

    @Override
    public void openTargetScreen(Player player) {
        Screen current = Minecraft.getInstance().screen;
        Minecraft.getInstance().setScreen(new BackpackSettingsScreen(current));
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
        gui.renderItem(GEAR_ICON, x + ICON_INSET_X, y + ICON_INSET_Y);
    }

    @Override
    public boolean isCurrentlyUsed(Screen currentScreen) {
        return currentScreen instanceof BackpackSettingsScreen;
    }

    @Override
    public Component getTooltip() {
        return Component.translatable("tooltip.sophisticatedtab.tab.settings");
    }
}
