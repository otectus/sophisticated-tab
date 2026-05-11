# Sophisticated Tab

**One-click access to your Sophisticated Backpack from any inventory screen.** Sophisticated Tab is a tiny client-side compat addon that adds a [Legendary Tabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs) navigation tab for [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks). Open your inventory, click the backpack tab, the first accessible backpack pops open. Done.

> **Minecraft 1.20.1 · Forge 47+ · Java 17**
> Free, open source, MIT licensed.

---

## What it actually does

The mod adds **two tabs** to the existing Legendary Tabs strip:

- **Backpack tab** — appears at the top of your player inventory. Click it to open the first reachable Sophisticated Backpack (main inventory, offhand, Curios/Cosmetic Armor slots if those mods are installed — the same lookup Sophisticated Backpacks uses for its own keybind). Disabled when no backpack is carried.
- **Return-to-inventory tab** — appears at the top of the Sophisticated Backpack screen. One click, back to your player inventory. No more closing-and-reopening.

Both tabs reuse the sprites that ship inside Legendary Tabs, so they fit naturally next to its other tabs — no off-style icon ruining your modpack's HUD.

---

## Why bother

If you have both Legendary Tabs and Sophisticated Backpacks installed without this addon, **the backpack does not get a tab**. Legendary Tabs has built-in support for Backpacked and Traveler's Backpack but not Sophisticated Backpacks. Sophisticated Tab fills exactly that gap.

If you only have Sophisticated Backpacks installed, this mod adds nothing — Sophisticated Backpacks already has a creative-mode tab and a configurable keybind. The point of this addon is to plug into Legendary Tabs' navigation strip the same way other backpack mods already do.

---

## Screenshots

> _Placeholder — add screenshots before publishing:_
>
> - Inventory screen with the backpack tab visible at top.
> - Backpack screen with the return-to-inventory tab visible on the left.
> - The backpack tab in its disabled state when no backpack is carried.

---

## Requirements

- **Minecraft 1.20.1** with **Forge 47.x** (built against 47.4.20).
- **[Legendary Tabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs)** — mandatory.
- **[Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks)** + **[Sophisticated Core](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core)** — mandatory.

Both upstream mods are declared as hard `mandatory=true` dependencies. The mod will refuse to load with a clear error message if either is missing, rather than crash on startup.

---

## Installation

1. Install **Minecraft Forge** for **1.20.1**.
2. Drop the following jars into your `.minecraft/mods/` folder:
   - `legendarytabs-…jar`
   - `sophisticatedcore-…jar`
   - `sophisticatedbackpacks-…jar`
   - `sophisticatedtab-…jar` (this mod)
3. Launch the game. Open your inventory — the backpack tab is at the top.

Works on single-player, LAN, and dedicated servers. The mod itself is **client-only**: the dedicated server accepts the jar but it has no server-side payload. The actual "open my backpack" packet is Sophisticated Backpacks'.

---

## Mod compatibility

- **[Curios](https://www.curseforge.com/minecraft/mc-mods/curios)** and **[Cosmetic Armor Reworked](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked)** — fully supported because backpack lookup uses Sophisticated Backpacks' own `PlayerInventoryProvider`, which already integrates with both.
- **Other Legendary Tabs tabs** — coexists peacefully. Tab priority is `20`, matching the convention of other backpack-style tabs (Backpacked, Traveler's Backpack).
- **Other backpack mods** — Sophisticated Tab is scoped exclusively to Sophisticated Backpacks. It does not touch Backpacked, Traveler's Backpack, or Quark backpacks; Legendary Tabs has separate built-in tabs for those.
- **Resource packs** — the tab icons live inside Legendary Tabs' atlas, so any resource pack that retextures Legendary Tabs' button sheet automatically restyles this addon's tabs too.

---

## For modpack makers

- **Zero config.** The mod has no config file. If the two upstream mods are loaded, the tabs are registered.
- **No mixins, no access transformers, no reflection.** Pure public-API Forge integration. Survives upstream patch updates that don't break the public hooks.
- **Tiny jar.** ~13 KB. Seven small classes, one lang file. Trivial to audit, trivial to ship.
- **Same jar on client and server.** Server-side it loads cleanly and does nothing — no network code, no registration, no state.

---

## Known caveats

- **Shared backpack sprite.** The tab icon is reused from Legendary Tabs' own atlas (the same backpack sprite already used by the Backpacked and Traveler's Backpack tabs). If you have multiple backpack mods installed, their tabs will look identical to this one. A unique Sophisticated-style icon is on the roadmap — see CHANGELOG.
- **Sophisticated Backpacks already provides a hotkey** (default `B`) that does the same opening action. This mod is the discoverable, mouse-driven counterpart for players who don't memorize hotkeys.

---

## License & links

- MIT licensed.
- [Source on GitHub](https://github.com/otectus/sophisticated-tab)
- [Issue tracker](https://github.com/otectus/sophisticated-tab/issues)
