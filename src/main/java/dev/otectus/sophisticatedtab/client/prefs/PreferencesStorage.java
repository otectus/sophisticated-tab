package dev.otectus.sophisticatedtab.client.prefs;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.otectus.sophisticatedtab.client.prefs.BackpackTabPreferences.ProfileEntry;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Load/save of tab_preferences.json. Atomic-write via tmp+rename; corruption
// recovery via .broken.<ts> rename; capped IO retries. Single-threaded usage.
public final class PreferencesStorage {

    private static final Logger LOG = LoggerFactory.getLogger("sophisticatedtab/prefs");
    private static final String DIR_NAME = "sophisticatedtab";
    private static final String FILE_NAME = "tab_preferences.json";
    private static final int MAX_CONSECUTIVE_WRITE_FAILS = 5;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final AtomicInteger CONSECUTIVE_WRITE_FAILS = new AtomicInteger(0);

    private PreferencesStorage() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(DIR_NAME).resolve(FILE_NAME);
    }

    public static void loadAll() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            LOG.info("No tab_preferences.json present at {}; using defaults.", path);
            BackpackTabPreferences.replaceAll(new HashMap<>());
            return;
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            ParseResult result = parse(root);
            BackpackTabPreferences.replaceAll(result.profiles());
            if (result.migrated()) {
                // Force a rewrite at the new schema so the migrated keys (and
                // version bump) hit disk on the next tick.
                BackpackTabPreferences.markDirty();
                LOG.info("Migrated {} legacy profile(s) from {}; will rewrite at v{}.",
                        result.profiles().size(), path, BackpackTabPreferences.SCHEMA_VERSION);
            } else {
                LOG.info("Loaded {} profile(s) from {}.", result.profiles().size(), path);
            }
        } catch (JsonSyntaxException | IOException | IllegalStateException e) {
            LOG.warn("Failed to parse {}: {}. Renaming and starting fresh.", path, e.getMessage());
            quarantine(path);
            BackpackTabPreferences.replaceAll(new HashMap<>());
        }
    }

    public static void flushIfDirty() {
        if (!BackpackTabPreferences.takeDirty()) {
            return;
        }
        if (CONSECUTIVE_WRITE_FAILS.get() >= MAX_CONSECUTIVE_WRITE_FAILS) {
            return;
        }
        try {
            writeAtomic(BackpackTabPreferences.allProfiles());
            CONSECUTIVE_WRITE_FAILS.set(0);
        } catch (IOException e) {
            int n = CONSECUTIVE_WRITE_FAILS.incrementAndGet();
            if (n == MAX_CONSECUTIVE_WRITE_FAILS) {
                LOG.error("Reached {} consecutive write failures; suspending save attempts until next launch.", n);
            } else {
                LOG.warn("Failed to write tab_preferences.json (attempt {}): {}", n, e.toString());
            }
            // Re-mark dirty so a future tick may try again unless we've capped.
            if (n < MAX_CONSECUTIVE_WRITE_FAILS) {
                BackpackTabPreferences.markDirty();
            }
        }
    }

    // ----- parsing ------------------------------------------------------------

    // Result of parsing: the loaded profiles plus a flag indicating whether
    // any v1 keys were migrated to the legacy/ prefix (and thus the file
    // should be rewritten at the new schema version).
    record ParseResult(Map<String, ProfileEntry> profiles, boolean migrated) {}

    static ParseResult parse(JsonElement root) {
        Map<String, ProfileEntry> out = new HashMap<>();
        if (root == null || !root.isJsonObject()) {
            return new ParseResult(out, false);
        }
        JsonObject obj = root.getAsJsonObject();
        int version = obj.has("version") && obj.get("version").isJsonPrimitive()
                ? obj.get("version").getAsInt() : BackpackTabPreferences.SCHEMA_VERSION;
        if (version > BackpackTabPreferences.SCHEMA_VERSION) {
            LOG.warn("tab_preferences.json schema version {} is newer than this build (v{}); loading anyway.",
                    version, BackpackTabPreferences.SCHEMA_VERSION);
        }
        if (!obj.has("profiles") || !obj.get("profiles").isJsonObject()) {
            return new ParseResult(out, false);
        }
        // v1 keyed by display-name (e.g. "singleplayer:NewWorld"); those collide
        // across worlds with the same display name and leaked stale UUIDs into
        // freshly created worlds. Move them under "legacy/" so they're inert
        // from now on (preserved on disk for archeology, never read by current()).
        boolean migrating = version < BackpackTabPreferences.SCHEMA_VERSION;
        boolean migrated = false;
        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("profiles").entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            String key = e.getKey();
            if (BackpackScopeKey.UNKNOWN.toStorageKey().equals(key)) {
                // Belt-and-suspenders: an ephemeral scope key should never be on
                // disk, but if a malformed write or manual edit put one there,
                // drop it on load so it can't propagate.
                continue;
            }
            if (migrating && !key.startsWith("legacy/")) {
                key = "legacy/" + key;
                migrated = true;
            }
            ProfileEntry entry = new ProfileEntry();
            JsonObject p = e.getValue().getAsJsonObject();
            readUuids(p, "orderedBackpacks").forEach(entry.orderedBackpacks()::add);
            readUuids(p, "hiddenBackpacks").forEach(entry.hiddenBackpacks()::add);
            out.put(key, entry);
        }
        return new ParseResult(out, migrated);
    }

    private static List<UUID> readUuids(JsonObject parent, String key) {
        List<UUID> out = new java.util.ArrayList<>();
        if (!parent.has(key) || !parent.get(key).isJsonArray()) return out;
        JsonArray arr = parent.getAsJsonArray(key);
        for (JsonElement e : arr) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) continue;
            try {
                out.add(UUID.fromString(e.getAsString()));
            } catch (IllegalArgumentException ex) {
                LOG.debug("Skipping malformed UUID '{}' under '{}'.", e.getAsString(), key);
            }
        }
        return out;
    }

    // ----- writing ------------------------------------------------------------

    private static void writeAtomic(Map<String, ProfileEntry> profiles) throws IOException {
        Path path = file();
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(FILE_NAME + ".tmp");

        JsonObject root = new JsonObject();
        root.addProperty("version", BackpackTabPreferences.SCHEMA_VERSION);
        JsonObject profilesJson = new JsonObject();
        // Sort keys for stable diffs across saves.
        Map<String, ProfileEntry> sorted = new LinkedHashMap<>();
        profiles.keySet().stream().sorted().forEach(k -> sorted.put(k, profiles.get(k)));
        for (Map.Entry<String, ProfileEntry> e : sorted.entrySet()) {
            JsonObject p = new JsonObject();
            JsonArray ordered = new JsonArray();
            for (UUID u : e.getValue().orderedBackpacks()) ordered.add(u.toString());
            p.add("orderedBackpacks", ordered);
            JsonArray hidden = new JsonArray();
            for (UUID u : e.getValue().hiddenBackpacks()) hidden.add(u.toString());
            p.add("hiddenBackpacks", hidden);
            profilesJson.add(e.getKey(), p);
        }
        root.add("profiles", profilesJson);

        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
        try {
            Files.move(tmp, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Windows fallback when crossing filesystems / volumes.
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void quarantine(Path path) {
        String ts = Instant.now().toString().replace(':', '-');
        Path broken = path.resolveSibling(FILE_NAME + ".broken." + ts);
        try {
            Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING);
            LOG.warn("Preserved corrupt preferences at {} for inspection.", broken);
        } catch (IOException e) {
            LOG.error("Could not quarantine corrupt preferences {}: {}", path, e.toString());
        }
    }
}
