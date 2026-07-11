package dev.otectus.sophisticatedtab.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Tiny floating popup anchored under a right-clicked tab. Renders the parent
// inventory/backpack screen as a dimmed background and presents a vertical list
// of Entry actions; clicking outside the menu dismisses it. Subclasses supply
// the concrete entries via {@link #entries()}.
public abstract class AbstractTabContextMenu extends Screen {

    protected static final int ENTRY_HEIGHT = 14;
    protected static final int ENTRY_PADDING_X = 6;
    protected static final int MENU_PADDING = 3;
    protected static final int MIN_MENU_WIDTH = 96;
    protected static final int BG_COLOR = 0xF0202020;
    protected static final int BORDER_COLOR = 0xFFAAAAAA;

    // Forward pose Z so the dim + menu BG sit above parent inventory items
    // (which render at Z ~100-150 via ItemRenderer). Same rationale as in
    // BackpackSettingsScreen — without this, parent items pass the depth test
    // and remain visible through the panel.
    protected static final float OVERLAY_Z = 400.0f;

    protected final Screen parent;
    private final int anchorX;
    private final int anchorY;

    private int menuX;
    private int menuY;
    private int menuWidth;
    private int menuHeight;

    protected AbstractTabContextMenu(Component title, Screen parent, int anchorX, int anchorY) {
        super(title);
        this.parent = parent;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    // The actions this menu offers, top to bottom.
    protected abstract Entry[] entries();

    @Override
    protected void init() {
        Entry[] entries = entries();

        int contentWidth = MIN_MENU_WIDTH;
        for (Entry e : entries) {
            int w = font.width(e.label()) + ENTRY_PADDING_X * 2;
            if (w > contentWidth) contentWidth = w;
        }
        menuWidth = contentWidth + MENU_PADDING * 2;
        menuHeight = entries.length * ENTRY_HEIGHT + MENU_PADDING * 2;

        menuX = clampX(anchorX);
        menuY = clampY(anchorY);

        int y = menuY + MENU_PADDING;
        for (Entry e : entries) {
            Button b = Button.builder(e.label(), btn -> e.action().run())
                    .pos(menuX + MENU_PADDING, y)
                    .size(contentWidth, ENTRY_HEIGHT)
                    .build();
            b.active = e.enabled();
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

    protected void back() {
        Minecraft.getInstance().setScreen(parent);
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

    protected record Entry(Component label, boolean enabled, Runnable action) {}
}
