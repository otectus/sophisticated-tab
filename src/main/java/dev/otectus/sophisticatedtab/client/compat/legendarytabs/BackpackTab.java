package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.api.tabs_menu.TabsMenu;

// One indexed slot in the LegendaryTabs row. LegendaryTabs requires tabs to be
// registered statically at client setup, so we pre-register a pool of these
// (see LegendaryTabsCompat#register) and each instance resolves its own
// descriptor at render/click time. Tabs whose index >= current backpack count
// report isEnabled() == false and LT drops them from the layout.
public final class BackpackTab extends TabBase {

    // Our own 52x22 chrome — normal at U=0, hover/active at U=26 — drawn so a
    // real ItemStack can sit on top of a clean button frame instead of bleeding
    // through LegendaryTabs' atlas backpack silhouette.
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("sophisticatedtab", "textures/gui/backpack_tab.png");

    private static final int CHROME_U = 0;
    private static final int CHROME_V = 0;
    private static final int CHROME_TEXTURE_W = 52;
    private static final int CHROME_TEXTURE_H = 22;
    private static final int HOVER_DX = 26;

    // Inset to center a 16x16 item inside the 26x22 tab.
    private static final int ICON_INSET_X = 5;
    private static final int ICON_INSET_Y = 3;

    // Base priority. The back-to-inventory tab sits at 10 so any backpack tab
    // (priorities 20+) renders to its right.
    private static final int BASE_PRIORITY = 20;

    private static final int VANILLA_INVENTORY_WIDTH = 176;
    private static final int VANILLA_INVENTORY_HEIGHT = 166;

    private final int index;

    public BackpackTab(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    Optional<BackpackDescriptor> currentDescriptor(Player player) {
        List<BackpackDescriptor> visible = BackpackTabResolver.visible(player);
        return index < visible.size() ? Optional.of(visible.get(index)) : Optional.empty();
    }

    @Override
    public boolean isEnabled(Player player) {
        return currentDescriptor(player).isPresent();
    }

    @Override
    public void openTargetScreen(Player player) {
        currentDescriptor(player).ifPresent(BackpackOpenCoordinator::openBackpackFromTab);
    }

    @Override
    public void initTabOnScreens() {
        int priority = BASE_PRIORITY + index;
        TabsMenu.addTabToScreen(this, InventoryScreen.class,
                p -> VANILLA_INVENTORY_WIDTH,
                p -> VANILLA_INVENTORY_HEIGHT,
                priority);
        TabsMenu.addTabToScreen(this, BackpackScreen.class,
                SophisticatedBackpacksSizing::getWidth,
                SophisticatedBackpacksSizing::getHeight,
                priority);
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, boolean hover) {
        int u = CHROME_U + (hover ? HOVER_DX : 0);
        gui.blit(TEXTURE, x, y, u, CHROME_V, TAB_WIDTH, TAB_HEIGHT, CHROME_TEXTURE_W, CHROME_TEXTURE_H);

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        currentDescriptor(player).ifPresent(desc ->
                gui.renderItem(desc.iconStack(), x + ICON_INSET_X, y + ICON_INSET_Y));
    }

    // LegendaryTabs reads this to mark the tab as "active": when true the
    // TabButton stays in its hover/disabled visual and click-to-reopen no-ops.
    // We match by contents UUID — robust across slot moves and identical-tier
    // backpacks, since each Sophisticated Backpack carries its own UUID in NBT.
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
        return currentDescriptor(player)
                .flatMap(desc -> desc.wrapper().getContentsUuid())
                .map(myUuid -> myUuid.equals(openUuid.get()))
                .orElse(false);
    }

    @Override
    public Component getTooltip() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return Component.translatable("tooltip.sophisticatedtab.tab.backpack_empty");
        }
        return currentDescriptor(player)
                .map(BackpackDescriptor::tooltip)
                .orElseGet(() -> Component.translatable("tooltip.sophisticatedtab.tab.backpack_empty"));
    }
}
