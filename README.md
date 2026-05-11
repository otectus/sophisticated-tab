# Sophisticated Tab

A small Forge 1.20.1 compatibility addon that adds a **LegendaryTabs** screen-navigation tab for opening **Sophisticated Backpacks** with one click.

## What it does

When both [LegendaryTabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs) and [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) are installed, this addon attaches a new tab to the top of the inventory screen. Clicking it opens the player's first accessible backpack via Sophisticated Backpacks' own `BackpackOpenMessage` packet. The tab becomes enabled only when a backpack is reachable, and its hitbox tracks the backpack screen's dynamic size (9-slot vs 12-slot layouts, scrollbar present, GUI scale changes).

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
      ├─ SophisticatedBackpacksTab.java
      ├─ SophisticatedBackpacksLocator.java
      └─ SophisticatedBackpacksSizing.java
```

Optional-class references are kept inside `client/compat/legendarytabs/` so the rest of the mod stays class-load-safe if a dependency is missing.

## License

MIT — see [LICENSE](LICENSE) if present.
