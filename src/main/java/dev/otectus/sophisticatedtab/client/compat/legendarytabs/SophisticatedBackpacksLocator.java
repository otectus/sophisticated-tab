package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public final class SophisticatedBackpacksLocator {
    private SophisticatedBackpacksLocator() {}

    // Walks every backpack-bearing slot Sophisticated Backpacks knows about:
    // Curios/Cosmetic Armor (prepended by SB's compat handlers), main inventory,
    // offhand, armor. Iteration order is stable across frames as long as stacks
    // don't move; the callback always returns false so PlayerInventoryProvider
    // visits every slot rather than stopping at the first match.
    public static List<BackpackDescriptor> findAllBackpacks(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        List<BackpackDescriptor> result = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (stack, handlerName, identifier, slot) -> {
            BackpackDescriptor.from(stack, handlerName, identifier, slot).ifPresent(result::add);
            return false;
        });
        return result;
    }

    public static Optional<BackpackDescriptor> findFirstBackpack(Player player) {
        List<BackpackDescriptor> all = findAllBackpacks(player);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    public static boolean hasOpenableBackpack(Player player) {
        return !findAllBackpacks(player).isEmpty();
    }
}
