package dev.otectus.sophisticatedtab.client.gui;

import java.util.Optional;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackDescriptor;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackOpenCoordinator;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Tiny floating popup anchored under the right-clicked backpack tab. Renders
// the parent inventory/backpack screen as a dimmed background and presents
// four actions: Open, Move Left, Move Right, Hide. Click outside the menu
// dismisses it. Move/Hide are greyed out for UUID-less backpacks.
public final class BackpackTabContextMenu extends Screen {

    private static final int ENTRY_HEIGHT = 14;
    private static final int ENTRY_PADDING_X = 6;
    private static final int MENU_PADDING = 3;
    private static final int MIN_MENU_WIDTH = 96;
    private static final int BG_COLOR = 0xF0202020;
    private static final int BORDER_COLOR = 0xFFAAAAAA;

    // Forward pose Z so the dim + menu BG sit above parent inventory items
    // (which render at Z ~100-150 via ItemRenderer). Same rationale as in
    // BackpackSettingsScreen — without this, parent items pass the depth test
    // and remain visible through the panel.
    private static final float OVERLAY_Z = 400.0f;

    private final Screen parent;
    private final BackpackDescriptor target;
    private final int anchorX;
    private final int anchorY;

    private int menuX;
    private int menuY;
    private int menuWidth;
    private int menuHeight;

    public BackpackTabContextMenu(Screen parent, BackpackDescriptor target, int anchorX, int anchorY) {
        super(Component.translatable("gui.sophisticatedtab.context.title"));
        this.parent = parent;
        this.target = target;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    @Override
    protected void init() {
        Optional<UUID> id = target.uuid();
        ProfileEntry prefs = BackpackTabPreferences.current();
        boolean hasUuid = id.isPresent();
        boolean canMoveLeft = hasUuid && canMove(prefs, id.get(), -1);
        boolean canMoveRight = hasUuid && canMove(prefs, id.get(), +1);

        Entry[] entries = new Entry[] {
                new Entry(Component.translatable("gui.sophisticatedtab.context.open"), true, () -> openBackpack()),
                new Entry(Component.translatable("gui.sophisticatedtab.context.moveLeft"), canMoveLeft,
                        () -> id.ifPresent(u -> { prefs.move(u, -1); back(); })),
                new Entry(Component.translatable("gui.sophisticatedtab.context.moveRight"), canMoveRight,
                        () -> id.ifPresent(u -> { prefs.move(u, +1); back(); })),
                new Entry(Component.translatable("gui.sophisticatedtab.context.hide"), hasUuid,
                        () -> id.ifPresent(u -> { prefs.hide(u); back(); })),
        };

        int contentWidth = MIN_MENU_WIDTH;
        for (Entry e : entries) {
            int w = font.width(e.label) + ENTRY_PADDING_X * 2;
            if (w > contentWidth) contentWidth = w;
        }
        menuWidth = contentWidth + MENU_PADDING * 2;
        menuHeight = entries.length * ENTRY_HEIGHT + MENU_PADDING * 2;

        menuX = clampX(anchorX);
        menuY = clampY(anchorY);

        int y = menuY + MENU_PADDING;
        for (Entry e : entries) {
            Button b = Button.builder(e.label, btn -> e.action.run())
                    .pos(menuX + MENU_PADDING, y)
                    .size(contentWidth, ENTRY_HEIGHT)
                    .build();
            b.active = e.enabled;
            addRenderableWidget(b);
            y += ENTRY_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (parent != null) {
            parent.render(graphics, -1, -1, partialTick);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, OVERLAY_Z);
        graphics.fillGradient(0, 0, this.width, this.height, 0x40000000, 0x60000000);

        // Menu background + border.
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, BG_COLOR);
        graphics.renderOutline(menuX, menuY, menuWidth, menuHeight, BORDER_COLOR);

        RenderSystem.enableBlend();
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If the click lands inside the menu, defer to the button widgets via super.
        if (mouseX >= menuX && mouseX < menuX + menuWidth
                && mouseY >= menuY && mouseY < menuY + menuHeight) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // Click-outside dismisses without action.
        back();
        return true;
    }

    private void back() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void openBackpack() {
        // Coordinator sends a server close packet first if any container state
        // would otherwise be left stranded (crafting input, carried stack, or
        // a non-default containerMenu), then sends the open. The eventual
        // BackpackScreen from the server response replaces this menu.
        BackpackOpenCoordinator.openBackpackFromTab(target);
    }

    private static boolean canMove(ProfileEntry prefs, UUID id, int delta) {
        int idx = prefs.orderedBackpacks().indexOf(id);
        if (idx < 0) {
            // Not yet pinned; moving is allowed (it will be inserted at the end then shifted).
            return true;
        }
        int target = idx + delta;
        return target >= 0 && target < prefs.orderedBackpacks().size();
    }

    private int clampX(int desiredX) {
        if (desiredX + menuWidth > this.width) {
            return Math.max(0, this.width - menuWidth);
        }
        return Math.max(0, desiredX);
    }

    private int clampY(int desiredY) {
        if (desiredY + menuHeight > this.height) {
            return Math.max(0, this.height - menuHeight);
        }
        return Math.max(0, desiredY);
    }

    private record Entry(Component label, boolean enabled, Runnable action) {}
}
