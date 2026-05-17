package dev.otectus.sophisticatedtab.client.prefs;

import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

// Resolves the active preference scope from the current play context.
//
// Singleplayer (including "Open to LAN"-hosted): keyed by the save's FOLDER
// name, obtained via getWorldPath(LevelResource.ROOT). Folder is stable
// across rename and unique across recreate-with-same-display-name — display
// name was the original v1 scoping mechanism and is what caused stale UUIDs
// from prior worlds to leak into freshly-created worlds with the same name.
//
// Multiplayer / LAN-joined / Realm: keyed by ServerData.ip (which already
// encodes host:port). All three surface as mc.getCurrentServer() non-null
// and the connection address is the most stable identity vanilla exposes.
//
// Player UUID is appended in every persistent scope so a second Mojang
// account (or offline username change) on the same machine doesn't inherit
// the first account's hide/order state.
public final class ProfileResolver {

    public static final String UNKNOWN_PROFILE = BackpackScopeKey.UNKNOWN.toStorageKey();

    private ProfileResolver() {}

    public static BackpackScopeKey activeScope() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return BackpackScopeKey.UNKNOWN;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return BackpackScopeKey.UNKNOWN;
        }
        UUID playerUuid = player.getUUID();

        MinecraftServer integrated = mc.getSingleplayerServer();
        if (integrated != null) {
            String folder = levelFolderName(integrated);
            if (folder == null || folder.isEmpty()) {
                // Fall back to display name only when getWorldPath gave us nothing
                // useful — better than UNKNOWN, still scoped per-player so the
                // ambiguity is contained.
                folder = integrated.getWorldData().getLevelName();
            }
            return new BackpackScopeKey(BackpackScopeKey.Kind.SINGLEPLAYER, folder, playerUuid);
        }

        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isEmpty()) {
            return new BackpackScopeKey(BackpackScopeKey.Kind.MULTIPLAYER, server.ip, playerUuid);
        }

        return BackpackScopeKey.UNKNOWN;
    }

    public static String activeProfile() {
        return activeScope().toStorageKey();
    }

    private static String levelFolderName(MinecraftServer server) {
        try {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            Path name = root.getFileName();
            return name != null ? name.toString() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
