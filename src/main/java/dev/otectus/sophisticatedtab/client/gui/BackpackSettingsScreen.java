package dev.otectus.sophisticatedtab.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackDescriptor;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackTabResolver;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The escape hatch: lists every backpack the player is carrying plus every
// UUID still referenced by preferences (saved-but-not-carried), with toggles
// to flip visibility, buttons to bulk-reset state, and a manual prune option.
//
// Layout: a 240x200 panel centered over the parent screen, with the parent
// rendered dimmed behind. Manual mouse-wheel scroll for long lists.
public final class BackpackSettingsScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 204;
    private static final int PANEL_PADDING = 8;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_ICON_W = 16;
    private static final int VISIBLE_ROWS = 8;
    private static final int BG_COLOR = 0xF0202020;
    private static final int PANEL_BORDER = 0xFFAAAAAA;
    private static final int SECTION_HEADER_COLOR = 0xFFCCCCCC;
    private static final int ROW_HIGHLIGHT = 0x40FFFFFF;
    private static final int DIVIDER_COLOR = 0xFF505050;
    private static final ItemStack PLACEHOLDER_ICON = new ItemStack(Items.BARRIER);

    // Pose-Z baseline for our overlay draws. The parent screen renders items
    // via ItemRenderer at Z up to ~150 (slot quad + item model offsets); without
    // a forward translate, our panel BG (drawn via GuiGraphics.fill at Z=0) loses
    // the depth test on every pixel an item occupies. 400 also matches vanilla's
    // tooltip Z so our content never sits beneath a stale tooltip residue.
    private static final float OVERLAY_Z = 400.0f;

    private final Screen parent;

    private int panelX;
    private int panelY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int scrollOffset;

    private List<Row> rows = List.of();

    private Button resetOrderBtn;
    private Button resetHiddenBtn;
    private Button cleanUnusedBtn;
    private Button doneBtn;

    // "Confirm?" two-step state: which button is waiting and when it armed.
    private Button pendingConfirm;
    private long pendingConfirmAt;
    private static final long CONFIRM_WINDOW_MS = 3_000L;

    public BackpackSettingsScreen(Screen parent) {
        super(Component.translatable("gui.sophisticatedtab.settings.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;
        listX = panelX + PANEL_PADDING;
        listY = panelY + 24;
        listW = PANEL_WIDTH - PANEL_PADDING * 2;
        listH = VISIBLE_ROWS * ROW_HEIGHT;

        rebuildRows();

        int footerY = panelY + PANEL_HEIGHT - 24;
        int btnW = 56;
        int btnH = 18;
        int gap = 4;
        int x = panelX + PANEL_PADDING;

        resetOrderBtn = Button.builder(
                Component.translatable("gui.sophisticatedtab.settings.resetOrder"),
                b -> armOrCommitReset(b, () -> BackpackTabPreferences.current().resetOrder()))
                .pos(x, footerY).size(btnW, btnH).build();
        x += btnW + gap;
        resetHiddenBtn = Button.builder(
                Component.translatable("gui.sophisticatedtab.settings.resetHidden"),
                b -> armOrCommitReset(b, () -> {
                    BackpackTabPreferences.current().resetHidden();
                    rebuildRows();
                }))
                .pos(x, footerY).size(btnW, btnH).build();
        x += btnW + gap;
        cleanUnusedBtn = Button.builder(
                Component.translatable("gui.sophisticatedtab.settings.cleanUnused"),
                b -> {
                    BackpackTabPreferences.current().cleanUnused(currentlyCarriedUuids());
                    rebuildRows();
                })
                .pos(x, footerY).size(btnW, btnH).build();
        x += btnW + gap;
        doneBtn = Button.builder(
                Component.translatable("gui.sophisticatedtab.settings.done"),
                b -> back())
                .pos(panelX + PANEL_WIDTH - PANEL_PADDING - 40, footerY).size(40, btnH).build();

        addRenderableWidget(resetOrderBtn);
        addRenderableWidget(resetHiddenBtn);
        addRenderableWidget(cleanUnusedBtn);
        addRenderableWidget(doneBtn);
    }

    private void rebuildRows() {
        Player player = Minecraft.getInstance().player;
        List<Row> next = new ArrayList<>();
        Set<UUID> carriedUuids = new HashSet<>();

        if (player != null) {
            List<BackpackDescriptor> carried = BackpackTabResolver.allCarried(player);
            if (!carried.isEmpty()) {
                next.add(Row.header(Component.translatable("gui.sophisticatedtab.settings.section.carried")));
                for (BackpackDescriptor d : carried) {
                    Optional<UUID> id = d.uuid();
                    id.ifPresent(carriedUuids::add);
                    next.add(Row.carried(d, id.orElse(null)));
                }
            }
        }

        ProfileEntry prefs = BackpackTabPreferences.current();
        Set<UUID> savedNotCarried = new LinkedHashSet<>();
        for (UUID u : prefs.orderedBackpacks()) {
            if (!carriedUuids.contains(u)) savedNotCarried.add(u);
        }
        for (UUID u : prefs.hiddenBackpacks()) {
            if (!carriedUuids.contains(u)) savedNotCarried.add(u);
        }
        if (!savedNotCarried.isEmpty()) {
            next.add(Row.header(Component.translatable("gui.sophisticatedtab.settings.section.saved")));
            for (UUID u : savedNotCarried) {
                next.add(Row.unknown(u));
            }
        }

        if (next.isEmpty()) {
            next.add(Row.empty());
        }

        this.rows = next;
        clampScroll();
    }

    private Set<UUID> currentlyCarriedUuids() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return Set.of();
        Set<UUID> out = new HashSet<>();
        for (BackpackDescriptor d : BackpackTabResolver.allCarried(player)) {
            d.uuid().ifPresent(out::add);
        }
        return out;
    }

    private void armOrCommitReset(Button source, Runnable apply) {
        long now = System.currentTimeMillis();
        if (pendingConfirm == source && now - pendingConfirmAt < CONFIRM_WINDOW_MS) {
            apply.run();
            clearPendingConfirm(source);
            rebuildRows();
            return;
        }
        clearPendingConfirm(null);
        pendingConfirm = source;
        pendingConfirmAt = now;
        source.setMessage(Component.translatable("gui.sophisticatedtab.settings.confirm"));
    }

    private void clearPendingConfirm(Button keep) {
        if (pendingConfirm != null && pendingConfirm != keep) {
            pendingConfirm.setMessage(originalLabelFor(pendingConfirm));
        }
        if (keep == null) {
            pendingConfirm = null;
            pendingConfirmAt = 0;
        }
    }

    private Component originalLabelFor(Button b) {
        if (b == resetOrderBtn) return Component.translatable("gui.sophisticatedtab.settings.resetOrder");
        if (b == resetHiddenBtn) return Component.translatable("gui.sophisticatedtab.settings.resetHidden");
        if (b == cleanUnusedBtn) return Component.translatable("gui.sophisticatedtab.settings.cleanUnused");
        return b.getMessage();
    }

    // ===== rendering =========================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (parent != null) {
            parent.render(graphics, -1, -1, partialTick);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, OVERLAY_Z);
        graphics.fillGradient(0, 0, this.width, this.height, 0x40000000, 0x80000000);
        renderPanel(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderListContent(graphics, mouseX, mouseY);
        graphics.pose().popPose();
        expireConfirmIfStale();
    }

    private void renderPanel(GuiGraphics gui, int mouseX, int mouseY) {
        gui.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BG_COLOR);
        gui.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER);
        Component title = this.getTitle();
        int titleX = panelX + (PANEL_WIDTH - this.font.width(title)) / 2;
        gui.drawString(this.font, title, titleX, panelY + 8, 0xFFFFFFFF);
        gui.fill(listX, listY - 4, listX + listW, listY - 3, DIVIDER_COLOR);
        gui.fill(listX, listY + listH + 2, listX + listW, listY + listH + 3, DIVIDER_COLOR);
    }

    private void renderListContent(GuiGraphics gui, int mouseX, int mouseY) {
        int drawnRows = 0;
        int startIndex = scrollOffset;
        for (int i = startIndex; i < rows.size() && drawnRows < VISIBLE_ROWS; i++) {
            Row r = rows.get(i);
            int y = listY + drawnRows * ROW_HEIGHT;
            renderRow(gui, r, listX, y, listW, mouseX, mouseY);
            drawnRows++;
        }
    }

    private void renderRow(GuiGraphics gui, Row r, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_HEIGHT;
        if (hovered && r.kind != RowKind.HEADER && r.kind != RowKind.EMPTY) {
            gui.fill(x, y, x + w, y + ROW_HEIGHT, ROW_HIGHLIGHT);
        }
        switch (r.kind) {
            case HEADER -> gui.drawString(this.font, r.label, x, y + 5, SECTION_HEADER_COLOR);
            case EMPTY -> gui.drawString(this.font, r.label, x, y + 5, 0xFF888888);
            case CARRIED -> renderCarriedRow(gui, r, x, y, w);
            case UNKNOWN -> renderUnknownRow(gui, r, x, y, w);
        }
    }

    private void renderCarriedRow(GuiGraphics gui, Row r, int x, int y, int w) {
        BackpackDescriptor d = r.descriptor;
        ProfileEntry prefs = BackpackTabPreferences.current();
        UUID id = r.uuid;
        boolean hidden = id != null && prefs.isHidden(id);
        gui.renderItem(d.iconStack(), x + 2, y + 1);
        Component name = d.iconStack().getHoverName();
        int textColor = hidden ? 0xFF888888 : 0xFFFFFFFF;
        gui.drawString(this.font, name, x + 2 + ROW_ICON_W + 4, y + 5, textColor);

        Component toggleLabel = hidden
                ? Component.translatable("gui.sophisticatedtab.settings.toggleHidden")
                : Component.translatable("gui.sophisticatedtab.settings.toggleVisible");
        int tw = this.font.width(toggleLabel);
        int toggleX = x + w - tw - 4;
        int toggleColor = hidden ? 0xFFFF8080 : 0xFF80FF80;
        gui.drawString(this.font, toggleLabel, toggleX, y + 5, toggleColor);

        if (id == null) {
            gui.drawString(this.font, "*", x + w - tw - 12, y + 5, 0xFFFFCC00);
        }
    }

    private void renderUnknownRow(GuiGraphics gui, Row r, int x, int y, int w) {
        gui.renderItem(PLACEHOLDER_ICON, x + 2, y + 1);
        Component label = Component.translatable("gui.sophisticatedtab.settings.unknownBackpack");
        gui.drawString(this.font, label, x + 2 + ROW_ICON_W + 4, y + 5, 0xFF999999);
        String shortId = r.uuid != null ? r.uuid.toString().substring(0, 8) : "????????";
        int tw = this.font.width(shortId);
        gui.drawString(this.font, shortId, x + w - tw - 4, y + 5, 0xFF777777);
    }

    // ===== input =============================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int drawn = 0;
            for (int i = scrollOffset; i < rows.size() && drawn < VISIBLE_ROWS; i++) {
                Row r = rows.get(i);
                int y = listY + drawn * ROW_HEIGHT;
                if (mouseY >= y && mouseY < y + ROW_HEIGHT
                        && mouseX >= listX && mouseX < listX + listW) {
                    handleRowClick(r);
                    return true;
                }
                drawn++;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX < listX + listW
                && mouseY >= listY && mouseY < listY + listH) {
            scrollOffset -= (int) Math.signum(delta);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            back();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleRowClick(Row r) {
        if (r.kind == RowKind.CARRIED && r.uuid != null) {
            ProfileEntry prefs = BackpackTabPreferences.current();
            if (prefs.isHidden(r.uuid)) {
                prefs.unhide(r.uuid);
            } else {
                prefs.hide(r.uuid);
            }
            // Row state changes label; cheap to rebuild rather than mutate in place.
            rebuildRows();
        }
    }

    private void back() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void clampScroll() {
        int max = Math.max(0, rows.size() - VISIBLE_ROWS);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    private void expireConfirmIfStale() {
        if (pendingConfirm == null) return;
        if (System.currentTimeMillis() - pendingConfirmAt > CONFIRM_WINDOW_MS) {
            clearPendingConfirm(null);
        }
    }

    // ===== row model =========================================================

    private enum RowKind { HEADER, CARRIED, UNKNOWN, EMPTY }

    private static final class Row {
        final RowKind kind;
        final Component label;
        final BackpackDescriptor descriptor;
        final UUID uuid;

        private Row(RowKind kind, Component label, BackpackDescriptor descriptor, UUID uuid) {
            this.kind = kind;
            this.label = label;
            this.descriptor = descriptor;
            this.uuid = uuid;
        }

        static Row header(Component c) {
            return new Row(RowKind.HEADER, c, null, null);
        }

        static Row carried(BackpackDescriptor d, UUID id) {
            return new Row(RowKind.CARRIED, d.iconStack().getHoverName(), d, id);
        }

        static Row unknown(UUID id) {
            return new Row(RowKind.UNKNOWN,
                    Component.translatable("gui.sophisticatedtab.settings.unknownBackpack"), null, id);
        }

        static Row empty() {
            return new Row(RowKind.EMPTY,
                    Component.translatable("gui.sophisticatedtab.settings.empty"), null, null);
        }
    }

}
