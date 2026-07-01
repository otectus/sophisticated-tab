package dev.otectus.sophisticatedtab.client.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackDescriptor;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackOpenCoordinator;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackTab;
import dev.otectus.sophisticatedtab.client.gui.BackpackTabContextMenu;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vodmordia.modtabs.api.tabs_menu.TabBase;
import vodmordia.modtabs.api.tabs_menu.TabsMenu;
import vodmordia.modtabs.client.screens.TabButton;

// Game-bus listener that routes mouse activity over our backpack tabs.
//
// Right-click on a backpack tab opens BackpackTabContextMenu.
// Left-click is captured as either:
//   - a "click" (release without significant move) -> opens the backpack
//   - a "drag" (release after horizontal motion past DRAG_THRESHOLD) -> reorders
//     the UUID via BackpackTabPreferences#reorder.
//
// Mod Tabs creates a fresh BackpackTab per visible backpack each screen-init, so we read
// the live on-screen TabButtons (whose tabBase is a BackpackTab) rather than a static
// pool. After a reorder we ask Mod Tabs to rebuild the bar so the new order shows.
//
// State is global because the events are static-handler-friendly and the user can only
// have one active gesture at a time on the client.
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
    private static UUID armedUuid;
    private static BackpackDescriptor armedDescriptor;
    private static int armedSlotIndex = -1;
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
                openArmedBackpack();
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
        BackpackDescriptor desc = hit.get().tab().descriptor();
        event.setCanceled(true);
        resetState();
        Minecraft.getInstance().setScreen(
                new BackpackTabContextMenu(screen, desc, hit.get().anchorX(), hit.get().anchorY()));
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
        if (Minecraft.getInstance().player == null) {
            return;
        }

        BackpackDescriptor desc = hit.get().tab().descriptor();
        UUID uuid = desc.uuid().orElse(null);

        // Snapshot the on-screen backpack tabs (sorted left-to-right) at press time. The
        // reorder math relies on the ordering not shifting underneath us during the gesture.
        List<TabSlot> slots = snapshotSlots(screen);
        List<UUID> uuids = new ArrayList<>(slots.size());
        for (TabSlot s : slots) {
            uuids.add(s.uuid());
        }

        // Disallow dragging tabs whose backpack has no UUID — there is nothing stable to
        // persist. Pure left-click still opens it via the normal path.
        if (uuid == null) {
            // Still arm so a plain click opens it; just mark it undraggable.
            state = State.ARMED;
            activeScreen = screen;
            armedUuid = null;
            armedDescriptor = desc;
            armedSlotIndex = -1;
            pressX = mx;
            pressY = my;
            pressTimeMs = System.currentTimeMillis();
            visibleSnapshot = uuids;
            slotSnapshot = slots;
            event.setCanceled(true);
            return;
        }

        state = State.ARMED;
        activeScreen = screen;
        armedUuid = uuid;
        armedDescriptor = desc;
        armedSlotIndex = uuids.indexOf(uuid);
        pressX = mx;
        pressY = my;
        pressTimeMs = System.currentTimeMillis();
        visibleSnapshot = uuids;
        slotSnapshot = slots;

        event.setCanceled(true);
    }

    private static void openArmedBackpack() {
        if (armedDescriptor != null) {
            BackpackOpenCoordinator.openBackpackFromTab(armedDescriptor);
        }
    }

    private static void commitDrop(double dropX) {
        if (armedUuid == null) {
            return;
        }
        int target = dropIndexFor(dropX);
        if (target == armedSlotIndex) {
            return;
        }

        int clamped = Math.max(0, Math.min(visibleSnapshot.size() - 1, target));
        ProfileEntry prefs = BackpackTabPreferences.current();
        prefs.reorder(armedUuid, clamped, visibleSnapshot);
        LOG.debug("Reordered {} to visible index {}", armedUuid, clamped);
        // We stay on the same screen, so the tab bar will not rebuild on its own —
        // ask Mod Tabs to reinitialize it so the new order is reflected immediately.
        TabsMenu.reinitCurrentScreen();
    }

    // ===== rendering =========================================================

    private static void renderDragOverlay(GuiGraphics gui, double mx, double my) {
        if (armedDescriptor == null) {
            return;
        }

        // Ghost icon — semi-transparent copy of the dragged backpack at the cursor.
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 200);
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f);
        gui.renderItem(armedDescriptor.iconStack(), (int) mx - 8, (int) my - 8);
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

    public record TabHit(BackpackTab tab, TabButton button, int anchorX, int anchorY) {}

    public record TabSlot(UUID uuid, int x, int y, int width, int height) {}

    public static Optional<TabHit> hitTest(Screen screen, double mouseX, double mouseY) {
        if (screen == null) {
            return Optional.empty();
        }
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof TabButton button)) {
                continue;
            }
            TabBase underlying = readTabBase(button);
            if (!(underlying instanceof BackpackTab backpackTab)) {
                continue;
            }
            if (within(button, mouseX, mouseY)) {
                return Optional.of(new TabHit(backpackTab, button,
                        button.getX(), button.getY() + button.getHeight()));
            }
        }
        return Optional.empty();
    }

    static List<TabSlot> snapshotSlots(Screen screen) {
        if (screen == null) return List.of();
        List<TabSlot> slots = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof TabButton button)) continue;
            TabBase underlying = readTabBase(button);
            if (!(underlying instanceof BackpackTab backpackTab)) continue;
            slots.add(new TabSlot(backpackTab.descriptor().uuid().orElse(null),
                    button.getX(), button.getY(), button.getWidth(), button.getHeight()));
        }
        slots.sort((a, b) -> Integer.compare(a.x(), b.x()));
        return slots;
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

    // Try the documented public field first; if a future Mod Tabs release ever flips
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
        armedUuid = null;
        armedDescriptor = null;
        armedSlotIndex = -1;
        visibleSnapshot = List.of();
        slotSnapshot = List.of();
    }
}
