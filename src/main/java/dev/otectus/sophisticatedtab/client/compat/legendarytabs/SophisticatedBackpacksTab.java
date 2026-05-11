package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

public final class SophisticatedBackpacksTab extends TabBase {

    // LegendaryTabs ships a sprite atlas at this path. We blit its existing
    // backpack sprite rather than duplicate the texture in our own jar.
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("legendarytabs", "textures/gui/tab_menu_buttons.png");

    // Backpack sprite position inside tab_menu_buttons.png — same coordinates
    // used by LegendaryTabs' own BackpackedTab and TravelersBackpackTab. The
    // hover variant sits +54px to the right of the normal one.
    private static final int ICON_U = 27;
    private static final int ICON_V = 46;
    private static final int HOVER_DX = 54;

    // Tab ordering hint passed to TabsMenu.addTabToScreen — built-in backpack
    // tabs use 20 (inventory tab is 10, so this renders to the right of it).
    private static final int TAB_PRIORITY = 20;

    // Fallback dimensions used when registering on the vanilla InventoryScreen.
    private static final int INV_W = 176;
    private static final int INV_H = 166;

    @Override
    public void openTargetScreen(Player player) {
        SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage());
    }

    @Override
    public boolean isEnabled(Player player) {
        return SophisticatedBackpacksLocator.hasOpenableBackpack(player);
    }

    @Override
    public void initTabOnScreens() {
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> INV_W, p -> INV_H, TAB_PRIORITY);
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
        return currentScreen instanceof BackpackScreen;
    }

    @Override
    public Component getTooltip() {
        return Component.translatable("tooltip.sophisticatedtab.tab.sophisticatedbackpacks");
    }
}
