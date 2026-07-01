package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import vodmordia.modtabs.api.tabs_menu.TabBase;

// One Mod Tabs tab bound to a single backpack. Mod Tabs' DynamicTabProvider model
// (see BackpackTabProvider) creates a fresh instance per visible backpack each time a
// tabbed screen initializes, so each tab simply carries its own immutable descriptor —
// no pooling / index bookkeeping like the old LegendaryTabs static-registration model.
public final class BackpackTab extends TabBase {

    // Our own 52x22 chrome — normal at U=0, hover/active at U=26 — drawn so a
    // real ItemStack can sit on top of a clean button frame.
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sophisticatedtab", "textures/gui/backpack_tab.png");

    private static final int CHROME_U = 0;
    private static final int CHROME_V = 0;
    private static final int CHROME_TEXTURE_W = 52;
    private static final int CHROME_TEXTURE_H = 22;
    private static final int HOVER_DX = 26;

    // Inset to center a 16x16 item inside the tab.
    private static final int ICON_INSET_X = 5;
    private static final int ICON_INSET_Y = 3;

    private final BackpackDescriptor descriptor;

    public BackpackTab(BackpackDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public BackpackDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public boolean isEnabled(Player player) {
        // The provider only emits tabs for backpacks that should be visible, so a
        // contributed BackpackTab is always enabled.
        return true;
    }

    @Override
    public void openTargetScreen(Player player) {
        BackpackOpenCoordinator.openBackpackFromTab(descriptor);
    }

    @Override
    public void initTabOnScreens() {
        // Dynamic tabs are attached to whichever screen contributed them; nothing to register.
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, boolean hover) {
        int u = CHROME_U + (hover ? HOVER_DX : 0);
        gui.blit(TEXTURE, x, y, u, CHROME_V, TAB_WIDTH, TAB_HEIGHT, CHROME_TEXTURE_W, CHROME_TEXTURE_H);
        gui.renderItem(descriptor.iconStack(), x + ICON_INSET_X, y + ICON_INSET_Y);
    }

    // Mod Tabs reads this to mark the tab "active": when true the TabButton stays in
    // its hover/disabled visual and click-to-reopen no-ops. We match by contents UUID —
    // robust across slot moves and identical-tier backpacks, since each Sophisticated
    // Backpack carries its own UUID.
    @Override
    public boolean isCurrentlyUsed(Screen currentScreen) {
        if (!(currentScreen instanceof BackpackScreen backpackScreen)) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        AbstractContainerMenu menu = backpackScreen.getMenu();
        if (!(menu instanceof BackpackContainer container)) {
            return false;
        }
        BackpackContext context = container.getBackpackContext();
        if (context == null) {
            return false;
        }
        IBackpackWrapper openWrapper = context.getBackpackWrapper(player);
        if (openWrapper == null) {
            return false;
        }
        Optional<UUID> openUuid = openWrapper.getContentsUuid();
        if (openUuid.isEmpty()) {
            return false;
        }
        return descriptor.uuid()
                .map(myUuid -> myUuid.equals(openUuid.get()))
                .orElse(false);
    }

    @Override
    public Component getTooltip() {
        return descriptor.tooltip();
    }
}
