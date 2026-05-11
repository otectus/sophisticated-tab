package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public final class SophisticatedBackpacksLocator {
    private SophisticatedBackpacksLocator() {}

    public static Optional<IBackpackWrapper> findFirstBackpack(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        AtomicReference<IBackpackWrapper> result = new AtomicReference<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (stack, handlerName, identifier, slot) -> {
            stack.getCapability(CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY)
                    .ifPresent(result::set);
            return result.get() != null;
        });
        return Optional.ofNullable(result.get());
    }

    public static boolean hasOpenableBackpack(Player player) {
        return findFirstBackpack(player).isPresent();
    }
}
