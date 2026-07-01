# Sophisticated Tab

A small NeoForge 1.21.1 compatibility addon that adds a **Mod Tabs** screen-navigation tab for opening **Sophisticated Backpacks** with one click.

> **Ported from the original Forge 1.20.1 release.** On 1.21.1 the upstream tabs mod is **[Mod Tabs](https://www.curseforge.com/minecraft/mc-mods/mod-tabs)** (mod id `modtabs`) — the NeoForge successor to Legendary Tabs, maintained by vodmordia under MIT. See the [NeoForge 1.21.1 port](#neoforge-1211-port-1211-060) section below for what changed.

## What it does

When both [Mod Tabs](https://www.curseforge.com/minecraft/mc-mods/mod-tabs) and [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) are installed, this addon attaches **one tab per backpack** the player is currently carrying to the top of the inventory and backpack screens. Each tab renders the real `ItemStack` icon for that backpack on a clean button chrome — dyed, renamed, and upgraded backpacks look distinct — and clicking it opens that specific backpack via Sophisticated Backpacks' own `BackpackOpenPayload(slot, identifier, handlerName)` packet. A "return to inventory" tab sits at the start of the row on backpack screens.

The tab corresponding to the backpack you're currently viewing stays highlighted (hover/active visual state) so you can see at a glance which one is open. The match uses each backpack's stored UUID (`IStorageWrapper.getContentsUuid()`), so the highlight survives slot moves and identical-tier backpacks.

Backpacks are discovered through Sophisticated Backpacks' `PlayerInventoryProvider`, so Curios slots, Cosmetic Armor, main inventory, offhand, and chest armor are all honored automatically. Tabs are contributed on demand via a Mod Tabs **`DynamicTabProvider`** — one tab per carried backpack, with no fixed cap. Backpacks you don't want on the row can be hidden from the settings screen or the right-click menu; they remain openable via Sophisticated Backpacks' default `B` keybind.

Because the mod supplies its own per-backpack tabs, it **automatically disables Mod Tabs' built-in single "Sophisticated Backpacks" tab** on startup so the row isn't cluttered with a redundant entry (see [caveats](#known-caveats)).

### Configurable tab order, hiding, and settings

- **Drag-and-drop reorder.** Left-click-hold any backpack tab and drag horizontally to reorder. A faded ghost icon follows the cursor and a vertical drop indicator marks the target slot; release commits the change (the tab bar is rebuilt immediately via `TabsMenu.reinitCurrentScreen()`).
- **Right-click context menu.** Right-click a tab for `Open Backpack` / `Move Left` / `Move Right` / `Hide This Backpack`.
- **Settings tab (gear).** A gear tab opens a settings screen listing every carried backpack with a visibility toggle, plus every backpack UUID still referenced by preferences but not currently carried. The screen has `Reset Order`, `Reset Hidden`, and `Clean Unused` footer buttons.
- **Settings keybind.** `Options > Controls > Sophisticated Tab > Open Tab Settings` (default unbound) opens the settings screen from gameplay without needing the inventory.
- **Persistent.** Order and hide state save to `config/sophisticatedtab/tab_preferences.json`, scoped per world (singleplayer, including LAN-hosted) or per server (multiplayer). UUID is the identity key, so renaming, dyeing, or moving a backpack between slots all preserve the user's choices.

### Stability and per-(world × player) scope hardening

- **Visual ghost-item fix.** Tab opens (and the right-click `Open Backpack` action) route through a `BackpackOpenCoordinator` that closes the previous container cleanly before opening the backpack, so crafting input and the carried stack are returned by the server before the new backpack screen arrives. The back-to-inventory tab gets the same close-first treatment, eliminating a latent desync where the server kept the backpack menu open while the client rendered `InventoryScreen`.
- **Cross-world preferences leak fix.** Preference profile keys scope by **world save folder + player UUID** (not display name). Two worlds named the same don't share preferences, and different accounts on the same machine don't inherit each other's hide/order state. `tab_preferences.json` migrates from schema v1 to v2 on first launch — old entries are preserved under a `legacy/` prefix but never read, so nothing is deleted and nothing leaks.

The mod does **not** add a vanilla creative inventory category. Sophisticated Backpacks already registers one; duplicating it would be noise.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x (built against 21.1.84)
- Java 21
- [Mod Tabs](https://www.curseforge.com/minecraft/mc-mods/mod-tabs) (`modtabs`) — mandatory, client-side. Mod Tabs itself requires [MidnightLib](https://www.curseforge.com/minecraft/mc-mods/midnightlib).
- [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) + [Sophisticated Core](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core) — mandatory, client-side.

## Building

The build resolves the upstream mods from **CurseMaven** using the coordinates pinned in `gradle.properties` (`*_project` / `*_file` per mod). To bump a dependency, edit those properties.

```
./gradlew build
```

The build system is [ModDevGradle](https://github.com/neoforged/ModDevGradle) with Java 21 (auto-provisioned via the Gradle toolchain) and Parchment mappings.

> **JDK note:** Gradle 8.10 must itself run on a JDK in the **17..21** range. If your system default JDK is newer, run with `JAVA_HOME` pointing at a JDK 21 (e.g. `JAVA_HOME=/path/to/jdk-21 ./gradlew build`) or set `org.gradle.java.home` in your **user-level** `~/.gradle/gradle.properties` (not committed). The mod's own compile/run toolchain is Java 21 regardless.

## Running

```
./gradlew runClient        # launch the dev client with Mod Tabs + Sophisticated Backpacks/Core + MidnightLib loaded
./gradlew runServer        # dedicated-server smoke test (client-only mod loads and does nothing)
```

## Architecture

The addon is **client-only glue** (`@Mod(dist = Dist.CLIENT)`). Server installations accept the jar but it has no server-side payload — the opening packet is Sophisticated Backpacks' own `BackpackOpenPayload`, sent through NeoForge's `PacketDistributor`.

```
src/main/java/dev/otectus/sophisticatedtab/
├─ SophisticatedTab.java               # @Mod(dist = Dist.CLIENT) entrypoint; injected mod event bus
└─ client/
   ├─ ClientBootstrap.java             # FMLClientSetupEvent → load checks → register game-bus handlers
   ├─ compat/legendarytabs/            # (internal package name kept; targets Mod Tabs / vodmordia.modtabs.*)
   │  ├─ LegendaryTabsCompat.java      # registers static tabs + the DynamicTabProvider; disables Mod Tabs' built-in SB tab
   │  ├─ BackpackTabProvider.java      # DynamicTabProvider — contributes one BackpackTab per visible backpack
   │  ├─ BackpackDescriptor.java       # immutable record; iconStack is a defensive copy; wrapper via BackpackWrapper.fromStack
   │  ├─ BackpackOpenCoordinator.java  # routes tab opens / back-to-inventory through close-then-open; sends BackpackOpenPayload
   │  ├─ BackpackTab.java              # TabBase carrying its own BackpackDescriptor (dynamic, no pool)
   │  ├─ BackpackTabResolver.java      # discovery → filter hidden → de-dup by UUID → sort by preference (per call)
   │  ├─ BackToInventoryTab.java       # tab attached to BackpackScreen that returns to InventoryScreen
   │  ├─ SettingsTab.java              # gear tab opening BackpackSettingsScreen
   │  ├─ SophisticatedBackpacksLocator.java
   │  └─ SophisticatedBackpacksSizing.java
   ├─ prefs/
   │  ├─ BackpackTabPreferences.java   # in-memory model: ordered/hidden UUIDs per profile (schema v2)
   │  ├─ PreferencesStorage.java       # atomic JSON load/save under config/sophisticatedtab/; v1→v2 migration
   │  ├─ ProfileResolver.java          # v2:sp/<folder>/<uuid> | v2:mp/<host:port>/<uuid> | v2:unknown (ephemeral)
   │  └─ BackpackScopeKey.java         # structured scope key (Kind + identity + player UUID)
   ├─ input/
   │  ├─ TabInteractionHandler.java    # ScreenEvent.* listener — right-click menu, click vs drag, drop indicator
   │  └─ KeyBindings.java              # RegisterKeyMappingsEvent + ClientTickEvent.Post polling
   └─ gui/
      ├─ BackpackTabContextMenu.java   # right-click popup (Open / Move L|R / Hide)
      └─ BackpackSettingsScreen.java   # full settings GUI (visibility toggles + reset buttons)

src/main/resources/assets/sophisticatedtab/
├─ lang/en_us.json
└─ textures/gui/backpack_tab.png       # 52×22 chrome atlas — normal at U=0, hover/active at U=26
                                       #   reused by SettingsTab (comparator overlay) and BackToInventoryTab (crafting-table overlay)

.tools/                                # not packaged in the jar
└─ generate_tab_chrome.py              # regenerates backpack_tab.png if you tweak colors/dimensions
```

Optional-class references are kept inside `client/compat/legendarytabs/`, `client/input/`, and `client/gui/` so the rest of the mod stays class-load-safe if a dependency is missing. The entrypoint is annotated `@Mod(dist = Dist.CLIENT)`, so it is only constructed on the physical client and dedicated servers never link client-only classes.

## NeoForge 1.21.1 port (1.21.1-0.6.0)

Ported from Forge 1.20.1 (`1.20.1-0.6.0`). Behavior is preserved; the integration surface changed:

- **Tabs mod:** Legendary Tabs → **Mod Tabs** (mod id `legendarytabs` → `modtabs`, package `sfiomn.legendarytabs.*` → `vodmordia.modtabs.*`). The per-backpack tabs are now produced by a `DynamicTabProvider` instead of a pre-registered pool of eight — **the eight-tab cap is gone**. The mod also disables Mod Tabs' own built-in single Sophisticated Backpacks tab to avoid a duplicate.
- **Sophisticated Backpacks API:** capability lookup `stack.getCapability(...)` → `BackpackWrapper.fromStack(stack)`; the open packet `BackpackOpenMessage` / `SBPPacketHandler` → the `CustomPacketPayload` record `BackpackOpenPayload`, sent via `net.neoforged.neoforge.network.PacketDistributor.sendToServer(...)`.
- **Loader plumbing:** `net.minecraftforge.*` → `net.neoforged.*`; `@Mod` constructor takes the injected `IEventBus`; `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`; `TickEvent.ClientTickEvent` (phase END) → `ClientTickEvent.Post`; `new ResourceLocation(...)` → `ResourceLocation.fromNamespaceAndPath(...)`; `Screen#mouseScrolled` gained a second scroll axis; `META-INF/mods.toml` → `META-INF/neoforge.mods.toml`; `pack_format` 15 → 34.

## License

MIT — see [LICENSE](LICENSE).
