package dev.otectus.sophisticatedtab.client.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackDescriptor;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackTab;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackTabResolver;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.LegendaryTabsCompat;
import dev.otectus.sophisticatedtab.client.gui.BackpackTabContextMenu;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sfiomn.legendarytabs.api.tabs_menu.TabBase;
import sfiomn.legendarytabs.client.screens.TabButton;

// Forge-bus listener that routes mouse activity over our backpack tabs.
//
// Right-click on a backpack tab opens BackpackTabContextMenu.
// Left-click is captured as either:
//   - a "click" (release without significant move) -> opens the backpack
//   - a "drag" (release after horizontal motion past DRAG_THRESHOLD) -> reorders
//     the UUID via BackpackTabPreferences#reorder.
//
// State is global because Forge events are static-handler-friendly and the user
// can only have one active gesture at a time on the client.
public final class TabInteractionHandler {

    private static final Logger LOG = LoggerFactory.getLogger("sophisticatedtab/input");

    private static final int MOUSE_BUTTON_LEFT = 0;
    private static final int MOUSE_BUTTON_RIGHT = 1;

    // Pixel movement past which a left-press becomes a drag instead of a click.
    private static final double DRAG_THRESHOLD = 4.0;
    // Window during which a near-stationary left-release still counts as a click.
    private static final long CLICK_TIME_MS = 350L;
    private static final int DROP_INDICATOR_COLOR = 0xFFFFFFFF;
    private static final int DROP_INDICATOR_WIDTH = 2;

    private enum State { IDLE, ARMED, DRAGGING }

    private static State state = State.IDLE;
    private static Screen activeScreen;
    private static int armedIndex = -1;
    private static double pressX;
    private static double pressY;
    private static long pressTimeMs;
    private static List<UUID> visibleSnapshot = List.of();
    private static List<TabSlot> slotSnapshot = List.of();

    private TabInteractionHandler() {}

    // ===== events ============================================================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMousePressedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        int button = event.getButton();
        double mx = event.getMouseX();
        double my = event.getMouseY();
        Screen screen = event.getScreen();

        if (button == MOUSE_BUTTON_RIGHT) {
            handleRightClick(event, screen, mx, my);
            return;
        }
        if (button != MOUSE_BUTTON_LEFT) {
            return;
        }
        handleLeftPress(event, screen, mx, my);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != MOUSE_BUTTON_LEFT || state == State.IDLE) {
            return;
        }
        if (event.getScreen() != activeScreen) {
            resetState();
            return;
        }

        double mx = event.getMouseX();
        boolean cancel = false;

        if (state == State.ARMED) {
            long elapsed = System.currentTimeMillis() - pressTimeMs;
            double dist = Math.abs(mx - pressX);
            if (dist < DRAG_THRESHOLD && elapsed < CLICK_TIME_MS) {
                openBackpackAt(armedIndex);
                cancel = true;
            }
        } else if (state == State.DRAGGING) {
            commitDrop(mx);
            cancel = true;
        }
        resetState();
        if (cancel) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (state == State.IDLE) {
            return;
        }
        if (event.getScreen() != activeScreen) {
            resetState();
            return;
        }
        double mx = event.getMouseX();
        if (state == State.ARMED && Math.abs(mx - pressX) > DRAG_THRESHOLD) {
            state = State.DRAGGING;
        }
        if (state == State.DRAGGING) {
            renderDragOverlay(event.getGuiGraphics(), mx, event.getMouseY());
        }
    }

    // Stale-state cleanup: any screen change while a gesture is in flight
    // forfeits the gesture rather than leaking state into the next screen.
    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (state != State.IDLE && event.getScreen() == activeScreen) {
            resetState();
        }
    }

    // ===== handlers ==========================================================

    private static void handleRightClick(ScreenEvent.MouseButtonPressed.Pre event,
                                         Screen screen, double mx, double my) {
        Optional<TabHit> hit = hitTest(screen, mx, my);
        if (hit.isEmpty()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Optional<BackpackDescriptor> desc = descriptorFor(player, hit.get().backpackTab().index());
        if (desc.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        resetState();
        Minecraft.getInstance().setScreen(
                new BackpackTabContextMenu(screen, desc.get(), hit.get().anchorX(), hit.get().anchorY()));
    }

    private static void handleLeftPress(ScreenEvent.MouseButtonPressed.Pre event,
                                        Screen screen, double mx, double my) {
        if (state != State.IDLE) {
            resetState();
        }
        Optional<TabHit> hit = hitTest(screen, mx, my);
        if (hit.isEmpty()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Snapshot visible UUIDs at press time. The reorder math relies on the
        // ordering not shifting underneath us during the gesture.
        List<BackpackDescriptor> visible = BackpackTabResolver.visible(player);
        int idx = hit.get().backpackTab().index();
        if (idx >= visible.size()) {
            return;
        }
        List<UUID> uuids = new ArrayList<>(visible.size());
        for (BackpackDescriptor d : visible) {
            uuids.add(d.uuid().orElse(null));
        }
        // Disallow dragging tabs whose backpack has no UUID — there is nothing
        // stable to persist. Pure left-click still opens it via the normal LT path.
        if (uuids.get(idx) == null) {
            return;
        }

        state = State.ARMED;
        activeScreen = screen;
        armedIndex = idx;
        pressX = mx;
        pressY = my;
        pressTimeMs = System.currentTimeMillis();
        visibleSnapshot = uuids;
        slotSnapshot = snapshotSlots(screen);

        event.setCanceled(true);
    }

    private static void openBackpackAt(int idx) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        List<BackpackTab> tabs = LegendaryTabsCompat.backpackTabs();
        if (idx < 0 || idx >= tabs.size()) return;
        tabs.get(idx).openTargetScreen(player);
    }

    private static void commitDrop(double dropX) {
        UUID moving = visibleSnapshot.get(armedIndex);
        if (moving == null) return;
        int target = dropIndexFor(dropX);
        if (target == armedIndex) return;

        // Translate slot-index drop to a visible-list index. The slot list is in
        // the same order as visibleSnapshot up to slotSnapshot.size().
        int clamped = Math.max(0, Math.min(visibleSnapshot.size() - 1, target));
        ProfileEntry prefs = BackpackTabPreferences.current();
        prefs.reorder(moving, clamped, visibleSnapshot);
        LOG.debug("Reordered {} to visible index {}", moving, clamped);
    }

    // ===== rendering =========================================================

    private static void renderDragOverlay(GuiGraphics gui, double mx, double my) {
        if (armedIndex < 0 || armedIndex >= visibleSnapshot.size()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        List<BackpackDescriptor> visible = BackpackTabResolver.visible(player);
        if (armedIndex >= visible.size()) return;

        BackpackDescriptor desc = visible.get(armedIndex);

        // Ghost icon — semi-transparent copy of the dragged backpack at the cursor.
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 200);
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f);
        gui.renderItem(desc.iconStack(), (int) mx - 8, (int) my - 8);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gui.pose().popPose();

        // Drop indicator at the target boundary.
        int target = dropIndexFor(mx);
        if (!slotSnapshot.isEmpty()) {
            int boundaryX;
            int top;
            int bottom;
            if (target <= 0) {
                TabSlot first = slotSnapshot.get(0);
                boundaryX = first.x();
                top = first.y();
                bottom = first.y() + first.height();
            } else if (target >= slotSnapshot.size()) {
                TabSlot last = slotSnapshot.get(slotSnapshot.size() - 1);
                boundaryX = last.x() + last.width();
                top = last.y();
                bottom = last.y() + last.height();
            } else {
                TabSlot left = slotSnapshot.get(target - 1);
                TabSlot right = slotSnapshot.get(target);
                boundaryX = (left.x() + left.width() + right.x()) / 2 - DROP_INDICATOR_WIDTH / 2;
                top = Math.min(left.y(), right.y());
                bottom = Math.max(left.y() + left.height(), right.y() + right.height());
            }
            gui.fill(boundaryX, top, boundaryX + DROP_INDICATOR_WIDTH, bottom, DROP_INDICATOR_COLOR);
        }

    }

    // ===== hit-test ==========================================================

    public record TabHit(BackpackTab backpackTab, TabButton button, int anchorX, int anchorY) {}

    public record TabSlot(int index, int x, int y, int width, int height) {}

    public static Optional<TabHit> hitTest(Screen screen, double mouseX, double mouseY) {
        if (screen == null) {
            return Optional.empty();
        }
        List<BackpackTab> ours = LegendaryTabsCompat.backpackTabs();
        if (ours.isEmpty()) {
            return Optional.empty();
        }
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof TabButton button)) {
                continue;
            }
            TabBase underlying = readTabBase(button);
            if (underlying == null) {
                continue;
            }
            BackpackTab match = findMatchingBackpackTab(ours, underlying);
            if (match == null) {
                continue;
            }
            if (within(button, mouseX, mouseY)) {
                return Optional.of(new TabHit(match, button,
                        button.getX(), button.getY() + button.getHeight()));
            }
        }
        return Optional.empty();
    }

    static List<TabSlot> snapshotSlots(Screen screen) {
        if (screen == null) return List.of();
        List<BackpackTab> ours = LegendaryTabsCompat.backpackTabs();
        if (ours.isEmpty()) return List.of();
        List<TabSlot> slots = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof TabButton button)) continue;
            TabBase underlying = readTabBase(button);
            if (underlying == null) continue;
            BackpackTab match = findMatchingBackpackTab(ours, underlying);
            if (match == null) continue;
            slots.add(new TabSlot(match.index(), button.getX(), button.getY(),
                    button.getWidth(), button.getHeight()));
        }
        slots.sort((a, b) -> Integer.compare(a.x(), b.x()));
        return slots;
    }

    static Optional<BackpackDescriptor> descriptorFor(Player player, int index) {
        List<BackpackDescriptor> visible = BackpackTabResolver.visible(player);
        if (index < 0 || index >= visible.size()) {
            return Optional.empty();
        }
        return Optional.of(visible.get(index));
    }

    private static int dropIndexFor(double mouseX) {
        if (slotSnapshot.isEmpty()) return 0;
        for (int i = 0; i < slotSnapshot.size(); i++) {
            TabSlot s = slotSnapshot.get(i);
            int midpoint = s.x() + s.width() / 2;
            if (mouseX < midpoint) {
                return i;
            }
        }
        return slotSnapshot.size();
    }

    private static boolean within(TabButton b, double mouseX, double mouseY) {
        int x = b.getX();
        int y = b.getY();
        return mouseX >= x && mouseX < x + b.getWidth()
                && mouseY >= y && mouseY < y + b.getHeight();
    }

    private static BackpackTab findMatchingBackpackTab(List<BackpackTab> ours, TabBase target) {
        for (BackpackTab t : ours) {
            if (t == target) {
                return t;
            }
        }
        return null;
    }

    // Try the documented public field first; if a future LT release ever flips
    // it to private with a getter, we degrade quietly rather than crash.
    private static TabBase readTabBase(TabButton button) {
        try {
            return button.tabBase;
        } catch (Throwable t) {
            LOG.warn("Could not read TabButton.tabBase ({}); disabling tab interaction.", t.toString());
            return null;
        }
    }

    private static void resetState() {
        state = State.IDLE;
        activeScreen = null;
        armedIndex = -1;
        visibleSnapshot = List.of();
        slotSnapshot = List.of();
    }
}
