package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

// Immutable view of one backpack in the player's inventory: where it lives
// (handler + identifier + slot — the triple SophisticatedBackpacks' server
// handler needs to open this specific backpack), what to draw on the tab,
// and the wrapper for dynamic screen-sizing math.
public record BackpackDescriptor(
        String handlerName,
        String identifier,
        int slot,
        ItemStack iconStack,
        Component tooltip,
        IBackpackWrapper wrapper) {

    public static Optional<BackpackDescriptor> from(ItemStack stack, String handlerName, String identifier, int slot) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        IBackpackWrapper wrapper = stack.getCapability(CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY)
                .orElse(null);
        if (wrapper == null) {
            return Optional.empty();
        }
        return Optional.of(new BackpackDescriptor(handlerName, identifier, slot, stack, stack.getHoverName(), wrapper));
    }
}
