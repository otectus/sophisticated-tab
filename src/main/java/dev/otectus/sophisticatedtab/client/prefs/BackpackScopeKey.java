package dev.otectus.sophisticatedtab.client.prefs;

import java.util.UUID;

// Identifies which preference profile to load for the current play context.
// Singleplayer scopes by save-folder name (NOT display name — those collide
// across recreated worlds with the same name). Multiplayer scopes by the
// server's connection address. UNKNOWN is a sentinel: when the player isn't
// ready or no server is connected, callers should treat the profile as
// ephemeral and not persist anything against it.
//
// The string form (`toStorageKey()`) is what lives in tab_preferences.json:
//   v2:sp/<level-folder>/<player-uuid>
//   v2:mp/<host:port>/<player-uuid>
//   v2:unknown
public record BackpackScopeKey(Kind kind, String identity, UUID playerUuid) {

    public enum Kind { SINGLEPLAYER, MULTIPLAYER, UNKNOWN }

    public static final BackpackScopeKey UNKNOWN = new BackpackScopeKey(Kind.UNKNOWN, "", null);

    public boolean isPersistent() {
        return kind != Kind.UNKNOWN && playerUuid != null;
    }

    public String toStorageKey() {
        return switch (kind) {
            case SINGLEPLAYER -> "v2:sp/" + sanitize(identity) + "/" + playerUuid;
            case MULTIPLAYER  -> "v2:mp/" + sanitize(identity) + "/" + playerUuid;
            case UNKNOWN      -> "v2:unknown";
        };
    }

    // Strips characters that would be invalid in a filesystem path (the JSON
    // key isn't a path itself, but we want it to remain greppable / readable
    // when the file is opened in an editor).
    static String sanitize(String raw) {
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
