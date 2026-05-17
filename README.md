# Sophisticated Tab

A small Forge 1.20.1 compatibility addon that adds a **LegendaryTabs** screen-navigation tab for opening **Sophisticated Backpacks** with one click.

## What it does

When both [LegendaryTabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs) and [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) are installed, this addon attaches **one tab per backpack** the player is currently carrying to the top of the inventory and backpack screens. Each tab renders the real `ItemStack` icon for that backpack on a clean LT-styled button chrome — dyed, renamed, and upgraded backpacks look distinct — and clicking it opens that specific backpack via Sophisticated Backpacks' own `BackpackOpenMessage(slot, identifier, handlerName)` packet. A "return to inventory" tab sits at the start of the row on backpack screens.

The tab corresponding to the backpack you're currently viewing stays highlighted (hover/active visual state) so you can see at a glance which one is open. The match uses each backpack's NBT-stored UUID (`IStorageWrapper.getContentsUuid()`), so the highlight survives slot moves and identical-tier backpacks.

Backpacks are discovered through Sophisticated Backpacks' `PlayerInventoryProvider`, so Curios slots, Cosmetic Armor, main inventory, offhand, and chest armor are all honored automatically. A pool of eight tabs is pre-registered at client setup; tabs without a backpack collapse out of the row (LegendaryTabs' filtered `enabledTabs` list). Players carrying more than eight backpacks can still open the rest via Sophisticated Backpacks' default `B` keybind — or hide unimportant ones via the settings screen to surface the rest.

### Configurable tab order, hiding, and settings (v0.5.0)

- **Drag-and-drop reorder.** Left-click-hold any backpack tab and drag horizontally to reorder. A faded ghost icon follows the cursor and a vertical drop indicator marks the target slot; release commits the change.
- **Right-click context menu.** Right-click a tab for `Open Backpack` / `Move Left` / `Move Right` / `Hide This Backpack`.
- **Settings tab (gear).** The rightmost tab on the row opens a settings screen listing every carried backpack with a visibility toggle, plus every backpack UUID still referenced by preferences but not currently carried. The screen has `Reset Order`, `Reset Hidden`, and `Clean Unused` footer buttons.
- **Settings keybind.** `Options > Controls > Sophisticated Tab > Open Tab Settings` (default unbound) opens the settings screen from gameplay without needing the inventory.
- **Persistent.** Order and hide state save to `config/sophisticatedtab/tab_preferences.json`, scoped per world (singleplayer, including LAN-hosted) or per server (multiplayer). UUID is the identity key, so renaming, dyeing, or moving a backpack between slots all preserve the user's choices.

### Stability and per-(world × player) scope hardening (v0.6.0)

- **Visual ghost-item fix.** Tab opens (and the right-click `Open Backpack` action) now route through a `BackpackOpenCoordinator` that closes the previous container cleanly before opening the backpack. v0.5.x sent `BackpackOpenMessage` directly while `InventoryScreen` was active; vanilla's `ServerPlayer.openMenu` short-circuited its own close call in that case, leaving crafting input and the carried stack stranded server-side. The new flow fires `LocalPlayer.closeContainer()` (server returns the crafting input + cursor), defers one client tick, and re-resolves the target backpack by UUID before sending the open message. Back-to-inventory tab gets the same close-first treatment, eliminating a latent desync where the server kept the backpack menu open while the client rendered `InventoryScreen`.
- **Cross-world preferences leak fix.** Preference profile keys now scope by **world save folder + player UUID** (was: display name only). Two worlds named the same don't share preferences, and different Mojang accounts on the same machine don't inherit each other's hide/order state. `tab_preferences.json` is silently migrated from schema v1 to v2 on first launch — old entries are preserved under a `legacy/` prefix but never read, so nothing is deleted and nothing leaks.

The mod does **not** add a vanilla creative inventory category. Sophisticated Backpacks already registers one; duplicating it would be noise.

## Requirements

- Minecraft 1.20.1
- Forge 47.x (built against 47.4.20)
- Java 17
- LegendaryTabs (mandatory, client-side)
- Sophisticated Backpacks + Sophisticated Core (mandatory, client-side)

## Building

This project uses a **composite build** as the primary upstream dependency strategy. Clone the upstream mods as siblings of this directory:

```
GitHub/
├─ sophisticated-tab/         <- this repo
├─ LegendaryTabs/             <- git clone -b 1.20.1 https://github.com/sfiomn/LegendaryTabs.git
└─ SophisticatedBackpacks/    <- git clone -b 1.20.x https://github.com/P3pp3rF1y/SophisticatedBackpacks.git
```

Then run from `sophisticated-tab/`:

```
gradlew build
```

### Without sibling clones

If you do not want to clone the upstream repos, the build automatically falls back to **CurseMaven** coordinates pinned in `gradle.properties`. The build runs unchanged — it just downloads the deobf jars from CurseForge instead of consuming them from local source.

To change which upstream versions the fallback uses, edit `*_curse_project` and `*_curse_file` properties in `gradle.properties`.

## Running

```
gradlew runClient        # launch the dev client with all mods loaded
gradlew runServer        # dedicated-server smoke test (no client-only code runs)
```

## Architecture

The addon is **client-only glue**. Server installations accept the jar but it has no server-side payload — the opening packet is Sophisticated Backpacks'.

```
src/main/java/dev/otectus/sophisticatedtab/
├─ SophisticatedTab.java               # @Mod entrypoint
└─ client/
   ├─ ClientBootstrap.java             # FMLClientSetupEvent → load checks → register handlers
   ├─ compat/legendarytabs/
   │  ├─ LegendaryTabsCompat.java      # TabsMenu.register bridge (isolates LT class refs)
   │  ├─ BackpackDescriptor.java       # immutable record; iconStack is a defensive copy of the live inventory stack
   │  ├─ BackpackOpenCoordinator.java  # routes tab opens / back-to-inventory through close-then-defer-then-open
   │  ├─ BackpackTab.java              # indexed TabBase — one per pool slot, resolves descriptor at render time
   │  ├─ BackpackTabResolver.java      # discovery → filter hidden → sort by preference (per call)
   │  ├─ BackToInventoryTab.java       # tab attached to BackpackScreen that returns to InventoryScreen
   │  ├─ SettingsTab.java              # gear tab opening BackpackSettingsScreen (priority 100)
   │  ├─ SophisticatedBackpacksLocator.java
   │  └─ SophisticatedBackpacksSizing.java
   ├─ prefs/
   │  ├─ BackpackTabPreferences.java   # in-memory model: ordered/hidden UUIDs per profile (schema v2)
   │  ├─ PreferencesStorage.java       # atomic JSON load/save under config/sophisticatedtab/; v1→v2 migration
   │  ├─ ProfileResolver.java          # v2:sp/<folder>/<uuid> | v2:mp/<host:port>/<uuid> | v2:unknown (ephemeral)
   │  └─ BackpackScopeKey.java         # structured scope key (Kind + identity + player UUID)
   ├─ input/
   │  ├─ TabInteractionHandler.java    # ScreenEvent.* listener — right-click menu, click vs drag, drop indicator
   │  └─ KeyBindings.java              # RegisterKeyMappingsEvent + ClientTickEvent polling
   └─ gui/
      ├─ BackpackTabContextMenu.java   # right-click popup (Open / Move L|R / Hide)
      └─ BackpackSettingsScreen.java   # full settings GUI (visibility toggles + reset buttons)

src/main/resources/assets/sophisticatedtab/
├─ lang/en_us.json
└─ textures/gui/backpack_tab.png       # 52×22 chrome atlas — normal at U=0, hover/active at U=26
                                       #   reused by SettingsTab with a Items.COMPARATOR overlay

.tools/                                # not packaged in the jar
├─ generate_tab_chrome.py              # regenerates backpack_tab.png if you tweak colors/dimensions
└─ ...
```

Optional-class references are kept inside `client/compat/legendarytabs/`, `client/input/`, and `client/gui/` so the rest of the mod stays class-load-safe if a dependency is missing. The `@Mod` entrypoint gates all client setup behind `FMLEnvironment.dist == Dist.CLIENT`, so dedicated servers never link client-only classes.

## License

MIT — see [LICENSE](LICENSE) if present.
