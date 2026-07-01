# Sophisticated Tab

**One-click access to every Sophisticated Backpack you're carrying — now with drag-to-reorder, right-click-to-hide, and a per-world settings screen.** Sophisticated Tab is a small client-side compat addon that adds [Mod Tabs](https://www.curseforge.com/minecraft/mc-mods/mod-tabs) navigation tabs for [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks). Carry one backpack and get one tab. Carry several across your main inventory, offhand, and Curios back slot and get one tab each — every tab renders the actual backpack item so you can tell them apart at a glance, every tab opens its own backpack on click, and you decide which ones show up and in what order.

> **Minecraft 1.21.1 · NeoForge 21.1+ · Java 21**
> Free, open source, MIT licensed.

> On 1.21.1 the tabs mod is **Mod Tabs** (`modtabs`), the NeoForge successor to Legendary Tabs.

---

## What it actually does

The mod adds tabs to the existing Mod Tabs strip:

- **One tab per backpack** — a tab appears at the top of your player inventory for each backpack you're carrying. Each tab renders the real `ItemStack` icon for that backpack (dyed, renamed, and upgraded backpacks look distinct) and clicking it opens *that specific* backpack, not whichever happens to be first. Tabs are contributed on demand as your carried backpacks change — there's no fixed cap. Backpacks are discovered through Sophisticated Backpacks' own `PlayerInventoryProvider`, so Curios, Cosmetic Armor, main inventory, offhand, and chest armor are all honored automatically.
- **Active-tab highlight** — the tab matching the backpack you're currently viewing stays in its hover/dark state so you can tell which one is open. Match uses each backpack's stored UUID, so it's stable across slot moves, dye changes, renames, and identical-tier backpacks.
- **Return-to-inventory tab** — appears at the top of the Sophisticated Backpack screen. One click, back to your player inventory. No more closing-and-reopening.
- **Drag-to-reorder** — left-click and hold any backpack tab, then drag it horizontally to a new slot. A faded ghost of the backpack icon follows your cursor and a small white indicator marks the drop position. Release to commit. Order persists across restarts.
- **Right-click context menu** — right-click any backpack tab for a small popup with **Open Backpack**, **Move Left**, **Move Right**, and **Hide This Backpack**. The hide action vanishes the tab from the row immediately.
- **Settings tab (the gear)** — opens a settings screen listing every backpack you're carrying, each with a visibility toggle, plus any UUIDs the mod has previously seen that aren't in your inventory right now. Footer buttons: *Reset Order*, *Reset Hidden*, *Clean Unused*. There's also a key binding (**Options → Controls → Sophisticated Tab → Open Tab Settings**, default unbound) to open the screen from gameplay without going through the inventory first.
- **Per-(world × player) and per-(server × player) preferences** — tab order and hidden-backpack state are saved in `config/sophisticatedtab/tab_preferences.json`, scoped by **world save folder + your player UUID** for singleplayer (so two worlds named the same don't share preferences, and different accounts on the same machine stay separate) and by **server address + your player UUID** for multiplayer. Backpack identity inside a scope is the contents UUID stored in the backpack, so your choices survive renaming, dyeing, slot moves, even leaving the backpack in a chest and picking it up days later. Entries from earlier installs are automatically relocated under a `legacy/` prefix on first launch and never read again — preserved on disk but inert.
- **Clean container transitions.** Opening a backpack tab while items sit in the vanilla 2×2 crafting grid no longer paints a visual duplicate — tab opens route through a small client-side transition coordinator that closes the previous container cleanly before sending the open packet, so the server returns the crafting input and the carried cursor stack before the new backpack screen arrives. The back-to-inventory tab gets the same treatment, fixing a latent server-side desync.

Tab chrome is a custom 52×22 PNG shipped with this addon, drawn in a Minecraft-style beveled-button look so it blends naturally with the rest of the Mod Tabs strip. The real backpack item renders on top of the chrome via vanilla item rendering, so resource packs, custom item renderers, and tint handlers all work without any special handling. Resource-pack authors can restyle the tab chrome by overriding `sophisticatedtab:textures/gui/backpack_tab.png`. The gear settings tab reuses the same chrome with a vanilla `comparator` overlaid as its glyph; the return-to-inventory tab reuses it with a `crafting_table` overlay.

---

## Why bother

If you have both Mod Tabs and Sophisticated Backpacks installed, Mod Tabs ships a single generic "Sophisticated Backpacks" tab that opens *one* backpack. If you carry several — different tiers, different colors, one in Curios, one in your main inventory — that single tab can't tell you which is which. Sophisticated Tab gives you a tab for **each**, with the actual backpack item drawn on the button, so you can find and open the right one in a single click. (It disables Mod Tabs' built-in single backpack tab automatically so you don't get both.)

If you carry *a lot* of backpacks — mining, food, building, junk, admin-only spares — the tab strip can get noisy. The drag-to-reorder, right-click-to-hide, and per-world settings screen are there to keep the row reflecting *your* priorities, not whatever order Sophisticated Backpacks happens to iterate through. Hidden backpacks are never deleted: they stay in your inventory and remain openable via Sophisticated Backpacks' `B` keybind. Only the tab is suppressed.

---

## Screenshots

> _Placeholder — add screenshots before publishing:_
>
> - Inventory screen with several backpack tabs across the top, plus the gear settings tab.
> - Mid-drag — a tab being dragged, faded ghost icon following the cursor, drop indicator between two adjacent tabs.
> - Right-click context menu open on a backpack tab.
> - Settings screen showing carried backpacks with visible/hidden toggles.

---

## Requirements

- **Minecraft 1.21.1** with **NeoForge 21.1.x** (built against 21.1.84).
- **[Mod Tabs](https://www.curseforge.com/minecraft/mc-mods/mod-tabs)** (`modtabs`) — mandatory. Mod Tabs itself requires **[MidnightLib](https://www.curseforge.com/minecraft/mc-mods/midnightlib)**.
- **[Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks)** + **[Sophisticated Core](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core)** — mandatory.

Both upstream mods are declared as hard `type="required"` dependencies. The mod will refuse to load with a clear error message if either is missing, rather than crash on startup.

---

## Installation

1. Install **NeoForge** for **1.21.1**.
2. Drop the following jars into your `.minecraft/mods/` folder:
   - `modtabs-…jar` and `midnightlib-…jar`
   - `sophisticatedcore-…jar`
   - `sophisticatedbackpacks-…jar`
   - `sophisticatedtab-…jar` (this mod)
3. Launch the game. Open your inventory — the backpack tabs are at the top.

Works on single-player, LAN, and dedicated servers. The mod itself is **client-only**: the dedicated server accepts the jar but it has no server-side payload. The actual "open my backpack" packet is Sophisticated Backpacks'.

---

## Mod compatibility

- **[Curios](https://www.curseforge.com/minecraft/mc-mods/curios)** and **[Cosmetic Armor Reworked](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked)** — fully supported because backpack lookup uses Sophisticated Backpacks' own `PlayerInventoryProvider`, which already integrates with both.
- **Mod Tabs' own built-in Sophisticated Backpacks tab** — automatically disabled at startup by this addon (it opens only a single backpack, which our per-backpack tabs supersede). See caveats for the re-enable edge case.
- **Other Mod Tabs tabs** — coexist peacefully. Backpack tabs are contributed dynamically and render alongside Mod Tabs' static tabs; the return-to-inventory and gear tabs use low/high priorities so they sit at sensible ends of the row.
- **Right-click capture** — the right-click context menu intercepts `ScreenEvent.MouseButtonPressed.Pre` at high priority and cancels it only when the click lands on one of *our* backpack tabs. Other Mod Tabs tabs keep their default click behavior.
- **Other backpack mods** — Sophisticated Tab is scoped exclusively to Sophisticated Backpacks. It does not touch Backpacked, Traveler's Backpack, or Quark backpacks; Mod Tabs has separate built-in tabs for those.
- **Resource packs** — backpack item icons inherit from whatever model + texture Sophisticated Backpacks (or your pack) defines for each backpack, because the tab uses vanilla item rendering. The tab chrome itself is `sophisticatedtab:textures/gui/backpack_tab.png` and can be overridden by any resource pack.
- **Key binding category** — adds one entry under **Options → Controls**: *Sophisticated Tab → Open Tab Settings* (default unbound). Won't collide with anything out of the box.

---

## For modpack makers

- **No modpack-side config.** There is no config TOML to ship or tune. The mod auto-creates `config/sophisticatedtab/tab_preferences.json` on first launch to persist each player's own tab order and hidden-backpack state per world or server. If you delete the file (or if it ever ends up corrupt), the mod renames the bad file to `tab_preferences.json.broken.<timestamp>` and starts fresh — players won't lose access to their backpacks, only the local preference state.
- **No mixins, no access transformers, no reflection.** Pure public-API integration on top of Mod Tabs and Sophisticated Backpacks. The one piece of cross-mod field access (Mod Tabs' public `TabButton.tabBase`) is wrapped in a try/catch — if a future release ever flips that field private, drag-and-drop and the right-click menu degrade to a logged warning while the settings screen still works.
- **Small jar.** ~65 KB. A small set of focused classes, one lang file, one PNG. Trivial to audit, trivial to ship.
- **Same jar on client and server.** Server-side it loads cleanly and does nothing — no network code, no registration, no state. The `@Mod(dist = Dist.CLIENT)` entrypoint is only constructed on the client, so client-only classes never link on a dedicated server.
- **No new server packets.** Opening a backpack uses Sophisticated Backpacks' own `BackpackOpenPayload`. Tab order and hidden state never leave the client.

---

## Known caveats

- **Tab list only refreshes when the screen (re)opens.** If you pick up a new backpack while your inventory screen is already open, you won't see its tab until you close and reopen the inventory. (Mod Tabs rebuilds its tab list per screen init.) Drag-reorder updates immediately within the same open screen; hide and unhide apply on the next inventory open.
- **UUID-less backpacks.** Backpacks that have never been opened may briefly lack their stored contents UUID. Those still get a tab and a real icon, but they can't be persistently hidden or reordered until the UUID is assigned (the settings screen marks them with a small `*`). Open the backpack once and they'll join the configurable pool.
- **Mod Tabs' built-in Sophisticated Backpacks tab.** This addon disables it at startup for you. If you open **and save** Mod Tabs' own config screen, MidnightLib may re-bake its config and re-enable that built-in tab until your next game launch (at which point this addon disables it again). If you ever want the built-in tab back permanently, remove this addon.
- **Sophisticated Backpacks already provides a hotkey** (default `B`) that opens the first reachable backpack. This mod is the discoverable, mouse-driven counterpart for players who don't memorize hotkeys — plus it lets you choose *which* backpack you open, in *which* order, with *which* ones hidden.

---

## License & links

- MIT licensed.
- [Source on GitHub](https://github.com/otectus/sophisticated-tab)
- [Issue tracker](https://github.com/otectus/sophisticated-tab/issues)
