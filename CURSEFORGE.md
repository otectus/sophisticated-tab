# Sophisticated Tab

**One-click access to every Sophisticated Backpack you're carrying.** Sophisticated Tab is a tiny client-side compat addon that adds [Legendary Tabs](https://www.curseforge.com/minecraft/mc-mods/legendary-tabs) navigation tabs for [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks). Carry one backpack and get one tab. Carry three different backpacks across your main inventory, offhand, and Curios back slot and get three tabs — each rendering the actual backpack item, so you can tell them apart at a glance, and each opens its own backpack on click.

> **Minecraft 1.20.1 · Forge 47+ · Java 17**
> Free, open source, MIT licensed.

---

## What it actually does

The mod adds tabs to the existing Legendary Tabs strip:

- **One tab per backpack** — up to eight tabs appear at the top of your player inventory, one for each backpack you're carrying. Each tab renders the real `ItemStack` icon for that backpack (dyed, renamed, and upgraded backpacks look distinct) and clicking it opens *that specific* backpack, not whichever happens to be first. Tabs collapse out of the row when no backpack is in that pool slot. Backpacks are discovered through Sophisticated Backpacks' own `PlayerInventoryProvider`, so Curios, Cosmetic Armor, main inventory, offhand, and chest armor are all honored automatically.
- **Active-tab highlight** — the tab matching the backpack you're currently viewing stays in its hover/dark state so you can tell which one is open. Match uses each backpack's NBT-stored UUID, so it's stable across slot moves, dye changes, renames, and identical-tier backpacks.
- **Return-to-inventory tab** — appears at the top of the Sophisticated Backpack screen. One click, back to your player inventory. No more closing-and-reopening.

Tab chrome is a custom 52×22 PNG shipped with this addon, drawn in a Minecraft-style beveled-button look so it blends naturally with the rest of the Legendary Tabs strip. The real backpack item renders on top of the chrome via vanilla item rendering, so resource packs, custom item renderers, and tint handlers all work without any special handling. Resource-pack authors can restyle the tab chrome by overriding `sophisticatedtab:textures/gui/backpack_tab.png`.

---

## Why bother

If you have both Legendary Tabs and Sophisticated Backpacks installed without this addon, **the backpack does not get a tab**. Legendary Tabs has built-in support for Backpacked and Traveler's Backpack but not Sophisticated Backpacks. Sophisticated Tab fills exactly that gap.

And if you carry several backpacks — different tiers, different colors, one in Curios, one in your main inventory — a single generic tab can't tell you which is which. Sophisticated Tab gives you a tab for each, with the actual backpack item drawn on the button, so you can find and open the right one in a single click.

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
- **Other Legendary Tabs tabs** — coexists peacefully. Backpack tabs use priorities `20`–`27` (one per pool slot), matching the convention of other backpack-style tabs (Backpacked, Traveler's Backpack). The return-to-inventory tab uses priority `10`.
- **Other backpack mods** — Sophisticated Tab is scoped exclusively to Sophisticated Backpacks. It does not touch Backpacked, Traveler's Backpack, or Quark backpacks; Legendary Tabs has separate built-in tabs for those.
- **Resource packs** — backpack item icons inherit from whatever model + texture Sophisticated Backpacks (or your pack) defines for each backpack, because the tab uses vanilla item rendering. The tab chrome itself is `sophisticatedtab:textures/gui/backpack_tab.png` and can be overridden by any resource pack. The return-to-inventory tab still reuses Legendary Tabs' atlas, so it follows LT's resource pack overrides.

---

## For modpack makers

- **Zero config.** The mod has no config file. If the two upstream mods are loaded, the tabs are registered.
- **No mixins, no access transformers, no reflection.** Pure public-API Forge integration. Survives upstream patch updates that don't break the public hooks.
- **Tiny jar.** Under 20 KB. A handful of small classes, one lang file. Trivial to audit, trivial to ship.
- **Same jar on client and server.** Server-side it loads cleanly and does nothing — no network code, no registration, no state.

---

## Known caveats

- **Eight-tab cap.** The mod pre-registers a pool of eight backpack tab slots. If you carry more than eight backpacks at once, the 9th and onward won't get a tab — they're still openable via Sophisticated Backpacks' default `B` keybind. Eight covers a Curios back slot plus six main-inventory backpacks comfortably; if you genuinely need more, open an issue.
- **Tab list only refreshes when the screen opens.** If you pick up a new backpack while your inventory screen is already open, you won't see its tab until you close and reopen the inventory. (Legendary Tabs caches its enabled-tab list per screen init.)
- **Sophisticated Backpacks already provides a hotkey** (default `B`) that opens the first reachable backpack. This mod is the discoverable, mouse-driven counterpart for players who don't memorize hotkeys — plus it lets you choose *which* backpack you open.

---

## License & links

- MIT licensed.
- [Source on GitHub](https://github.com/otectus/sophisticated-tab)
- [Issue tracker](https://github.com/otectus/sophisticated-tab/issues)
