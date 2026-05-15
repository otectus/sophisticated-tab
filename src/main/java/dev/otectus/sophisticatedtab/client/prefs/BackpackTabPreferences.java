package dev.otectus.sophisticatedtab.client.prefs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

// In-memory preference model. Holds one ProfileEntry per (world | server) key.
// Mutations mark the global dirty flag; PreferencesStorage owns the actual I/O.
// All access is single-threaded (client thread).
public final class BackpackTabPreferences {

    public static final int SCHEMA_VERSION = 1;

    private static final Map<String, ProfileEntry> PROFILES = new HashMap<>();
    private static final AtomicBoolean DIRTY = new AtomicBoolean(false);

    private BackpackTabPreferences() {}

    // ----- Profile registry ---------------------------------------------------

    public static ProfileEntry current() {
        return profile(ProfileResolver.activeProfile());
    }

    public static ProfileEntry profile(String key) {
        return PROFILES.computeIfAbsent(key, k -> new ProfileEntry());
    }

    public static Map<String, ProfileEntry> allProfiles() {
        return PROFILES;
    }

    public static void replaceAll(Map<String, ProfileEntry> loaded) {
        PROFILES.clear();
        PROFILES.putAll(loaded);
        DIRTY.set(false);
    }

    public static boolean takeDirty() {
        return DIRTY.getAndSet(false);
    }

    public static void markDirty() {
        DIRTY.set(true);
    }

    // ----- ProfileEntry -------------------------------------------------------

    public static final class ProfileEntry {
        private final List<UUID> orderedBackpacks = new ArrayList<>();
        private final Set<UUID> hiddenBackpacks = new LinkedHashSet<>();

        public List<UUID> orderedBackpacks() {
            return orderedBackpacks;
        }

        public Set<UUID> hiddenBackpacks() {
            return hiddenBackpacks;
        }

        public boolean isHidden(UUID id) {
            return id != null && hiddenBackpacks.contains(id);
        }

        public void hide(UUID id) {
            if (id == null) return;
            if (hiddenBackpacks.add(id)) {
                markDirty();
            }
        }

        public void unhide(UUID id) {
            if (id == null) return;
            if (hiddenBackpacks.remove(id)) {
                markDirty();
            }
        }

        // Shift a UUID's position in orderedBackpacks by `delta` slots, clamped.
        // If the UUID isn't present yet it's first appended at the end, then shifted.
        public void move(UUID id, int delta) {
            if (id == null || delta == 0) return;
            int current = orderedBackpacks.indexOf(id);
            if (current < 0) {
                orderedBackpacks.add(id);
                current = orderedBackpacks.size() - 1;
            }
            int target = Math.max(0, Math.min(orderedBackpacks.size() - 1, current + delta));
            if (target == current) return;
            orderedBackpacks.remove(current);
            orderedBackpacks.add(target, id);
            markDirty();
        }

        // Reorder applied after a drag: take a snapshot of the visible UUIDs the
        // user just rearranged, place `moving` at targetVisibleIndex within that
        // snapshot, then merge into orderedBackpacks. Off-screen UUIDs (saved but
        // not currently carried) retain relative order at the tail.
        public void reorder(UUID moving, int targetVisibleIndex, List<UUID> currentVisibleUuids) {
            if (moving == null || currentVisibleUuids == null) return;
            List<UUID> proposed = new ArrayList<>(currentVisibleUuids);
            proposed.remove(moving);
            int idx = Math.max(0, Math.min(proposed.size(), targetVisibleIndex));
            proposed.add(idx, moving);

            List<UUID> tail = new ArrayList<>();
            for (UUID u : orderedBackpacks) {
                if (!proposed.contains(u)) {
                    tail.add(u);
                }
            }
            orderedBackpacks.clear();
            orderedBackpacks.addAll(proposed);
            orderedBackpacks.addAll(tail);
            markDirty();
        }

        public void resetOrder() {
            if (orderedBackpacks.isEmpty()) return;
            orderedBackpacks.clear();
            markDirty();
        }

        public void resetHidden() {
            if (hiddenBackpacks.isEmpty()) return;
            hiddenBackpacks.clear();
            markDirty();
        }

        public void cleanUnused(Collection<UUID> currentlyCarried) {
            boolean changed = orderedBackpacks.retainAll(currentlyCarried);
            changed |= hiddenBackpacks.retainAll(currentlyCarried);
            if (changed) {
                markDirty();
            }
        }

        // Comparator over BackpackDescriptors keyed by Optional<UUID>:
        //   bucket 0 — UUID present and in orderedBackpacks (sort by its index)
        //   bucket 1 — UUID present but unknown (sort by discovery position)
        //   bucket 2 — UUID absent (sort by discovery position)
        // Ties broken by discovery position keep the comparator stable.
        public Comparator<UuidWithPosition> comparator() {
            return (a, b) -> {
                int aBucket = bucket(a);
                int bBucket = bucket(b);
                if (aBucket != bBucket) {
                    return Integer.compare(aBucket, bBucket);
                }
                if (aBucket == 0) {
                    int aIdx = orderedBackpacks.indexOf(a.uuid().orElseThrow());
                    int bIdx = orderedBackpacks.indexOf(b.uuid().orElseThrow());
                    if (aIdx != bIdx) {
                        return Integer.compare(aIdx, bIdx);
                    }
                }
                return Integer.compare(a.discoveryIndex(), b.discoveryIndex());
            };
        }

        private int bucket(UuidWithPosition u) {
            Optional<UUID> id = u.uuid();
            if (id.isEmpty()) return 2;
            return orderedBackpacks.contains(id.get()) ? 0 : 1;
        }
    }

    // Tiny adapter so the comparator can sort anything that has an optional UUID
    // and a stable discovery position without forcing a hard dependency on
    // BackpackDescriptor here (lets us unit-test in isolation).
    public interface UuidWithPosition {
        Optional<UUID> uuid();
        int discoveryIndex();
    }
}
