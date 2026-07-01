package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraft.world.entity.player.Player;

// One place that takes "what does PlayerInventoryProvider see" and applies the
// user's hide/order preferences to produce the canonical list of tabs to draw.
// BackpackTab(i) and the settings UI both read from here so behavior is
// consistent.
public final class BackpackTabResolver {

    private BackpackTabResolver() {}

    public static List<BackpackDescriptor> visible(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        List<BackpackDescriptor> discovered = SophisticatedBackpacksLocator.findAllBackpacks(player);
        ProfileEntry prefs = BackpackTabPreferences.current();

        // Defensive de-dup by contents UUID: a backpack should yield at most one tab even if it
        // were enumerated twice. UUID-less backpacks (unopened / no contents UUID) are genuinely
        // distinct and cannot be keyed, so they are always kept.
        Set<UUID> seenUuids = new HashSet<>();
        List<DescriptorWithIndex> filtered = new ArrayList<>(discovered.size());
        for (int i = 0; i < discovered.size(); i++) {
            BackpackDescriptor d = discovered.get(i);
            Optional<UUID> id = d.uuid();
            if (id.isPresent()) {
                if (prefs.isHidden(id.get())) {
                    continue;
                }
                if (!seenUuids.add(id.get())) {
                    continue;
                }
            }
            filtered.add(new DescriptorWithIndex(d, i));
        }
        filtered.sort(prefs.comparator());

        List<BackpackDescriptor> out = new ArrayList<>(filtered.size());
        for (DescriptorWithIndex d : filtered) {
            out.add(d.descriptor());
        }
        return out;
    }

    public static List<BackpackDescriptor> allCarried(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        return SophisticatedBackpacksLocator.findAllBackpacks(player);
    }

    // Adapter that pairs a BackpackDescriptor with its discovery index so the
    // preference comparator (which needs both a UUID and a stable position) can
    // sort it without forcing BackpackDescriptor to know about preferences.
    private record DescriptorWithIndex(BackpackDescriptor descriptor, int discoveryIndex)
            implements BackpackTabPreferences.UuidWithPosition {
        @Override
        public Optional<UUID> uuid() {
            return descriptor.uuid();
        }
    }
}
