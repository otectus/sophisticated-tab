package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;

// Routes all backpack opens (from tabs, context menu) and back-to-inventory
// transitions through a single helper that sends the close packet to the
// server WITHOUT going through LocalPlayer.closeContainer().
//
// Why bypass LocalPlayer.closeContainer(): vanilla's path ends in
// clientSideCloseContainer() -> Minecraft.setScreen(null), which calls
// MouseHandler.grabMouse() -> GLFW setCursorPos to (width/2, height/2). That's
// a physical OS-level cursor reposition. v0.6.0 did this once per tab switch
// when cleanup was needed; on backpack->backpack switches the server's
// openMenu() -> closeContainer() path sends ClientboundContainerClosePacket
// back, which the client handles via clientSideCloseContainer() too -- same
// setScreen(null), same recenter. By closing locally before sending the open,
// the server sees containerMenu == inventoryMenu when openMenu() runs and
// skips its own closeContainer() call, so no ClientboundContainerClosePacket
// is sent back either.
//
// The server-side cleanup that the original v0.6.0 fix was after is still
// triggered: the ServerboundContainerClosePacket runs InventoryMenu.removed()
// -> clearContainer() -> placeItemBackInInventory(), returning craft-grid
// items and the carried stack to the player inventory.
public final class BackpackOpenCoordinator {

    private BackpackOpenCoordinator() {}

    public static void openBackpackFromTab(BackpackDescriptor desc) {
        if (desc == null) {
            return;
        }
        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp == null) {
            return;
        }
        closeQuietly(lp);
        sendOpen(desc.slot(), desc.identifier(), desc.handlerName());
    }

    public static void openInventoryFromBackpack(Player player) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        if (lp == null) {
            return;
        }
        closeQuietly(lp);
        mc.setScreen(new InventoryScreen(player));
    }

    // Mirrors LocalPlayer.closeContainer() minus the setScreen(null) inside
    // clientSideCloseContainer(). current.removed(lp) substitutes for the
    // Screen.removed() -> menu.removed(player) dispatch that setScreen(null)
    // would have triggered -- for InventoryMenu it clears craftSlots locally,
    // for BackpackContainer it runs SB's own client-side teardown.
    private static void closeQuietly(LocalPlayer lp) {
        AbstractContainerMenu current = lp.containerMenu;
        boolean nothingToClean =
                current == lp.inventoryMenu
                && current.getCarried().isEmpty()
                && !(current instanceof InventoryMenu inv && hasNonEmptyCraftingState(inv));
        if (nothingToClean) {
            return;
        }
        lp.connection.send(new ServerboundContainerClosePacket(current.containerId));
        current.removed(lp);
        lp.containerMenu = lp.inventoryMenu;
    }

    private static void sendOpen(int slot, String identifier, String handlerName) {
        // 1.21.1 SBP replaced its SimpleChannel BackpackOpenMessage with a vanilla
        // CustomPacketPayload (BackpackOpenPayload), sent through NeoForge's
        // PacketDistributor. SBP registers the payload type/codec; we just send it.
        PacketDistributor.sendToServer(new BackpackOpenPayload(slot, identifier, handlerName));
    }

    // Uses vanilla InventoryMenu constants: RESULT_SLOT (0) through
    // CRAFT_SLOT_END (5, exclusive) — the result slot plus the four 2x2 craft
    // inputs. Result slot is included because a pending recipe preview is part
    // of the stranded state.
    private static boolean hasNonEmptyCraftingState(InventoryMenu inv) {
        for (int i = InventoryMenu.RESULT_SLOT; i < InventoryMenu.CRAFT_SLOT_END; i++) {
            if (!inv.getSlot(i).getItem().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
