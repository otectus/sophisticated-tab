package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;

// Routes all backpack opens (from tabs, context menu) and back-to-inventory
// transitions through a single close-then-open helper.
//
// Why this is needed: vanilla ServerPlayer.openMenu short-circuits its
// closeContainer() call when containerMenu == inventoryMenu, which is exactly
// the case when transitioning from InventoryScreen to a BackpackScreen. The
// abandoned InventoryMenu retains its crafting input and carried stack
// server-side, which surfaces as a visual duplicate (and a real desync hazard)
// once the new BackpackContainer opens on top of that stranded state.
//
// Strategy: detect when cleanup is needed, fire LocalPlayer.closeContainer()
// (the vanilla close path, server-authoritative), and defer the actual open
// to the next client tick. Re-resolve the target backpack by contents-UUID
// at fire time so post-close inventory repacking doesn't open the wrong
// backpack.
public final class BackpackOpenCoordinator {

    private static final int MAX_DEFER_TICKS = 5;

    private static PendingOpen pendingOpen;
    private static PendingInventory pendingInventory;

    private BackpackOpenCoordinator() {}

    public static void openBackpackFromTab(BackpackDescriptor desc) {
        if (desc == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        if (lp == null) {
            return;
        }

        Screen screen = mc.screen;
        AbstractContainerMenu menu = lp.containerMenu;
        boolean cleanupNeeded =
                screen instanceof InventoryScreen
                && menu instanceof InventoryMenu inv
                && (hasNonEmptyCraftingState(inv) || !menu.getCarried().isEmpty());

        if (!cleanupNeeded) {
            sendOpen(desc.slot(), desc.identifier(), desc.handlerName());
            return;
        }

        pendingInventory = null;
        pendingOpen = new PendingOpen(
                desc.uuid().orElse(null),
                desc.handlerName(),
                desc.identifier(),
                desc.slot(),
                MAX_DEFER_TICKS);
        lp.closeContainer();
    }

    public static void openInventoryFromBackpack(Player player) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        if (lp == null) {
            return;
        }
        pendingOpen = null;
        pendingInventory = new PendingInventory(MAX_DEFER_TICKS);
        lp.closeContainer();
    }

    public static void tick() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            pendingOpen = null;
            pendingInventory = null;
            return;
        }
        if (pendingOpen != null) {
            firePendingOpen(player);
        }
        if (pendingInventory != null) {
            firePendingInventory(player);
        }
    }

    private static void firePendingOpen(Player player) {
        PendingOpen p = pendingOpen;
        pendingOpen = null;
        BackpackDescriptor resolved = resolve(player, p);
        if (resolved != null) {
            sendOpen(resolved.slot(), resolved.identifier(), resolved.handlerName());
            return;
        }
        int next = p.ticksLeft() - 1;
        if (next > 0) {
            pendingOpen = new PendingOpen(p.uuid(), p.handlerName(), p.identifier(), p.slot(), next);
        }
    }

    private static void firePendingInventory(Player player) {
        pendingInventory = null;
        Minecraft.getInstance().setScreen(new InventoryScreen(player));
    }

    private static BackpackDescriptor resolve(Player player, PendingOpen p) {
        List<BackpackDescriptor> all = SophisticatedBackpacksLocator.findAllBackpacks(player);
        if (p.uuid() != null) {
            for (BackpackDescriptor d : all) {
                if (d.uuid().filter(u -> u.equals(p.uuid())).isPresent()) {
                    return d;
                }
            }
        }
        for (BackpackDescriptor d : all) {
            if (d.handlerName().equals(p.handlerName())
                    && d.identifier().equals(p.identifier())
                    && d.slot() == p.slot()) {
                return d;
            }
        }
        return null;
    }

    private static void sendOpen(int slot, String identifier, String handlerName) {
        SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage(slot, identifier, handlerName));
    }

    // Uses vanilla InventoryMenu constants: RESULT_SLOT (0) through
    // CRAFT_SLOT_END (5, exclusive) — that's the result slot plus the four
    // 2x2 craft inputs. Result slot is included because a pending recipe
    // preview is part of the stranded state.
    private static boolean hasNonEmptyCraftingState(InventoryMenu inv) {
        for (int i = InventoryMenu.RESULT_SLOT; i < InventoryMenu.CRAFT_SLOT_END; i++) {
            if (!inv.getSlot(i).getItem().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private record PendingOpen(UUID uuid, String handlerName, String identifier, int slot, int ticksLeft) {}
    private record PendingInventory(int ticksLeft) {}
}
