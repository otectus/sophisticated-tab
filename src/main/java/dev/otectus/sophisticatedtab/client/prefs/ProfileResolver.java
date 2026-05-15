package dev.otectus.sophisticatedtab.client.prefs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;

// Picks the active preference profile based on the current world/server.
// Singleplayer (including LAN-hosted) uses the integrated server's level name.
// Multiplayer uses the server's connection address. Both null cases fall back
// to "unknown" so callers never have to special-case empty keys.
public final class ProfileResolver {

    public static final String UNKNOWN_PROFILE = "unknown";

    private ProfileResolver() {}

    public static String activeProfile() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return UNKNOWN_PROFILE;
        }
        MinecraftServer integrated = mc.getSingleplayerServer();
        if (integrated != null) {
            String level = integrated.getWorldData().getLevelName();
            return "singleplayer:" + sanitize(level);
        }
        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isEmpty()) {
            return "server:" + sanitize(server.ip);
        }
        return UNKNOWN_PROFILE;
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "_";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 0x20 || "/\\:*?\"<>|".indexOf(c) >= 0) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
