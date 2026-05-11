# Sophisticated Tab

A small Forge 1.20.1 compatibility addon that adds a **LegendaryTabs** screen-navigation tab for opening **Sophisticated Backpacks** with one click.

## What it does

When both [LegendaryTabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs) and [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) are installed, this addon attaches **one tab per backpack** the player is currently carrying to the top of the inventory and backpack screens. Each tab renders the real `ItemStack` icon for that backpack on a clean LT-styled button chrome — dyed, renamed, and upgraded backpacks look distinct — and clicking it opens that specific backpack via Sophisticated Backpacks' own `BackpackOpenMessage(slot, identifier, handlerName)` packet. A "return to inventory" tab sits at the start of the row on backpack screens.

The tab corresponding to the backpack you're currently viewing stays highlighted (hover/active visual state) so you can see at a glance which one is open. The match uses each backpack's NBT-stored UUID (`IStorageWrapper.getContentsUuid()`), so the highlight survives slot moves and identical-tier backpacks.

Backpacks are discovered through Sophisticated Backpacks' `PlayerInventoryProvider`, so Curios slots, Cosmetic Armor, main inventory, offhand, and chest armor are all honored automatically. A pool of eight tabs is pre-registered at client setup; tabs without a backpack collapse out of the row (LegendaryTabs' filtered `enabledTabs` list). Players carrying more than eight backpacks can still open the rest via Sophisticated Backpacks' default `B` keybind.

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
   ├─ ClientBootstrap.java             # FMLClientSetupEvent → load checks → enqueueWork(...)
   └─ compat/legendarytabs/
      ├─ LegendaryTabsCompat.java      # TabsMenu.register bridge (isolates LT class refs)
      ├─ BackpackDescriptor.java       # immutable (handlerName, identifier, slot, iconStack, tooltip, wrapper)
      ├─ BackpackTab.java              # indexed TabBase — one per pool slot, resolves descriptor at render time
      ├─ BackToInventoryTab.java       # tab attached to BackpackScreen that returns to InventoryScreen
      ├─ SophisticatedBackpacksLocator.java
      └─ SophisticatedBackpacksSizing.java

src/main/resources/assets/sophisticatedtab/
├─ lang/en_us.json
└─ textures/gui/backpack_tab.png       # 52×22 chrome atlas — normal at U=0, hover/active at U=26

.tools/                                # not packaged in the jar
├─ generate_tab_chrome.py              # regenerates backpack_tab.png if you tweak colors/dimensions
└─ ...
```

Optional-class references are kept inside `client/compat/legendarytabs/` so the rest of the mod stays class-load-safe if a dependency is missing.

## License

MIT — see [LICENSE](LICENSE) if present.
