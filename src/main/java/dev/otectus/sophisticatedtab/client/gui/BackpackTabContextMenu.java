package dev.otectus.sophisticatedtab.client.gui;

import java.util.Optional;
import java.util.UUID;

import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackDescriptor;
import dev.otectus.sophisticatedtab.client.compat.legendarytabs.BackpackOpenCoordinator;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Context menu anchored under a right-clicked backpack tab. Presents four
// actions: Open, Move Left, Move Right, Hide. Move/Hide are greyed out for
// UUID-less backpacks. Layout/render/dismiss live in AbstractTabContextMenu.
public final class BackpackTabContextMenu extends AbstractTabContextMenu {

    private final BackpackDescriptor target;

    public BackpackTabContextMenu(Screen parent, BackpackDescriptor target, int anchorX, int anchorY) {
        super(Component.translatable("gui.sophisticatedtab.context.title"), parent, anchorX, anchorY);
        this.target = target;
    }

    @Override
    protected Entry[] entries() {
        Optional<UUID> id = target.uuid();
        ProfileEntry prefs = BackpackTabPreferences.current();
        boolean hasUuid = id.isPresent();
        boolean canMoveLeft = hasUuid && canMove(prefs, id.get(), -1);
        boolean canMoveRight = hasUuid && canMove(prefs, id.get(), +1);

        return new Entry[] {
                new Entry(Component.translatable("gui.sophisticatedtab.context.open"), true, () -> openBackpack()),
                new Entry(Component.translatable("gui.sophisticatedtab.context.moveLeft"), canMoveLeft,
                        () -> id.ifPresent(u -> { prefs.move(u, -1); back(); })),
                new Entry(Component.translatable("gui.sophisticatedtab.context.moveRight"), canMoveRight,
                        () -> id.ifPresent(u -> { prefs.move(u, +1); back(); })),
                new Entry(Component.translatable("gui.sophisticatedtab.context.hide"), hasUuid,
                        () -> id.ifPresent(u -> { prefs.hide(u); back(); })),
        };
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
}
