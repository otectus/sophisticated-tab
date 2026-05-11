# Changelog

All notable changes to **Sophisticated Tab** are documented here. Format roughly follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows [Semantic Versioning](https://semver.org/) prefixed with the target Minecraft version.

## [Unreleased]

### Planned

- **Custom tab icon.** Currently the backpack tab reuses the shared backpack sprite from Legendary Tabs' atlas (same sprite as Backpacked and Traveler's Backpack). A unique Sophisticated-style icon shipped from this addon's own `assets/sophisticatedtab/textures/gui/` would visually differentiate the tab when multiple backpack mods are installed.
- **Optional config** to disable the return-to-inventory tab for users who prefer pressing `Esc` then re-opening the inventory.
- **NeoForge 1.21.1 port** if upstream Legendary Tabs and Sophisticated Backpacks both publish 1.21.1 builds.

## [1.20.1-0.1.0] - 2026-05-10

Initial release. A client-only compat addon bridging [Legendary Tabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs) and [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) on Forge 1.20.1.

### Added — Legendary Tabs integration

- **Backpack tab on the player inventory** ([SophisticatedBackpacksTab.java](src/main/java/dev/otectus/sophisticatedtab/client/compat/legendarytabs/SophisticatedBackpacksTab.java)) — extends `sfiomn.legendarytabs.api.tabs_menu.TabBase`. Attaches to both `net.minecraft.client.gui.screens.inventory.InventoryScreen` and `net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen` via `TabsMenu.addTabToScreen(...)`. Priority `20`, matching the Legendary Tabs convention used by `BackpackedTab` and `TravelersBackpackTab`. Click forwards to `SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage())` — the same server-side workflow Sophisticated Backpacks uses for its own keybind, so no custom networking is registered. The tab disables itself when the player has no reachable backpack.

- **Return-to-inventory tab on the backpack screen** ([BackToInventoryTab.java](src/main/java/dev/otectus/sophisticatedtab/client/compat/legendarytabs/BackToInventoryTab.java)) — separate `TabBase` subclass attached only to `BackpackScreen.class`. Necessary because Legendary Tabs' built-in `InventoryTab.initTabOnScreens()` has a hard-coded screen allowlist that includes Backpacked and Traveler's Backpack but **not** Sophisticated Backpacks. Priority `10` so it renders to the left of the backpack tab. Opens a fresh `InventoryScreen(player)` directly via `Minecraft.getInstance().setScreen(...)` (no network round-trip needed for a vanilla screen).

- **Backpack discovery** ([SophisticatedBackpacksLocator.java](src/main/java/dev/otectus/sophisticatedtab/client/compat/legendarytabs/SophisticatedBackpacksLocator.java)) — wraps Sophisticated Backpacks' own `PlayerInventoryProvider.get().runOnBackpacks(player, callback)` and exposes `findFirstBackpack(Player)` and `hasOpenableBackpack(Player)`. Reusing the upstream provider means the tab honors every inventory handler Sophisticated Backpacks itself supports (main, offhand, Curios, Cosmetic Armor) without our addon caring which mods are installed.

- **Dynamic backpack-screen sizing** ([SophisticatedBackpacksSizing.java](src/main/java/dev/otectus/sophisticatedtab/client/compat/legendarytabs/SophisticatedBackpacksSizing.java)) — mirrors the width/height math from `net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase`. Constants `HEIGHT_WITHOUT_STORAGE_SLOTS=114`, `SLOT_SIZE=18`, `BASE_WIDTH_PADDING=14`, `SCROLLBAR_WIDTH_DELTA=6`. Selects 9-vs-12-column layout from `(totalSlots + columnsTaken * totalRows) <= 81`, accounts for upgrade columns, clamps row count by the viewport via `Minecraft.getInstance().getWindow().getGuiScaledHeight()`. Falls back to vanilla `176×166` when no backpack is reachable so the tab stays aligned on a plain `InventoryScreen`.

### Added — Mod scaffolding

- **`@Mod` entrypoint** gated to `Dist.CLIENT` ([SophisticatedTab.java](src/main/java/dev/otectus/sophisticatedtab/SophisticatedTab.java)). Dedicated servers accept the jar but the client setup listener is never registered, so no client-only classes load.
- **Client bootstrap** with belt-and-braces `ModList.get().isLoaded(...)` checks for both upstream mods plus `FMLClientSetupEvent.enqueueWork(...)` to dodge parallel mod loading hazards.
- **Compat-class isolation** — every reference to `sfiomn.legendarytabs.*` and `net.p3pp3rf1y.*` lives under `client/compat/legendarytabs/`. The entrypoint and bootstrap classes never touch optional symbols, so a missing dependency cannot trigger a `NoClassDefFoundError` at startup.
- **Resources**: `META-INF/mods.toml` declares both upstream mods as `mandatory=true` `side="CLIENT"` dependencies with `ordering="AFTER"`. `pack.mcmeta` ships `pack_format: 15`. `en_us.json` carries two tooltip keys (`tooltip.sophisticatedtab.tab.sophisticatedbackpacks` and `tooltip.sophisticatedtab.tab.inventory`). No textures shipped — both tabs blit from Legendary Tabs' own `tab_menu_buttons.png` atlas.

### Added — Build

- **Composite-build-first dependency strategy** ([settings.gradle](settings.gradle)) — `includeBuild('../LegendaryTabs')` and `includeBuild('../SophisticatedBackpacks')` fire automatically when sibling clones exist. When they don't, the build falls back to **CurseMaven** coordinates pinned in [gradle.properties](gradle.properties) (LegendaryTabs `1172469:6079304`, Sophisticated Backpacks `422301:7414216`, Sophisticated Core `618298:7455151`). Both paths produce deobf jars; no manual setup required either way.
- **ForgeGradle `[6.0.24,6.2)`** targeting Minecraft `1.20.1` + Forge `47.4.20`, official mappings.
- **Sophisticated Core on the compile classpath** — required because `IBackpackWrapper extends IStorageWrapper`, `SBPPacketHandler extends PacketHandler`, and `BackpackScreen extends StorageScreenBase`, all of which are defined in Core. Without Core on `compileOnly`, the compiler can't follow the inheritance chain.
- **Reobfuscation** runs as part of `gradlew build`. Final jar is ~13 KB.

### Design notes for future contributors

- **`addTabToScreen`'s last argument is `priority`, not a Y offset.** The design document we worked from misnamed it. Legendary Tabs computes tab position from each `AbstractContainerScreen`'s `getGuiLeft()` / `getGuiTop()` per frame in `ClientForgeEvents.preRenderScreen` — there's no Y offset to set. Priority `10` shows first, `20` shows after, etc.
- **The hover stride in `tab_menu_buttons.png` is `+54` pixels, not `+26`.** The atlas pairs each tab's normal and hover variants with 54-pixel column spacing. The backpack sprite lives at `(27, 46)`, the inventory sprite at `(0, 0)`. Drop these into a new `TabBase` subclass and reuse them verbatim; matches the conventions in `BackpackedTab`, `TravelersBackpackTab`, and `InventoryTab` upstream.
- **The width/height suppliers run every frame** via Legendary Tabs' `ScreenEvent.Render.Pre` listener. They're cheap by design — `SophisticatedBackpacksSizing` reads the cached `IBackpackWrapper` directly without any allocation per frame.
