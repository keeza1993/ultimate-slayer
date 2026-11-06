# Reference provenance — v0.5.1

Reviewed 5 November 2026. These are independently worded factual checks, not imported Wiki articles or calculator code. Review date means this package's review; inherited v0.4 facts have not all been re-fetched. Direct Wiki access can return old cached pages; indexed excerpts were used where needed. Links are maintained for review and correction, and do not make coverage exhaustive.

## Task and variant records

Base Slayer levels in ProgressionChecks use [Slayer monsters](https://oldschool.runescape.wiki/w/Slayer_monsters). Each named rule has the following primary reference; singular/plural aliases share a record.

| Record | Source |
|---|---|
| Crawling hands | https://oldschool.runescape.wiki/w/Crawling_Hand |
| Cave crawlers / poison note | https://oldschool.runescape.wiki/w/Cave_crawler |
| Banshees / ear protection | https://oldschool.runescape.wiki/w/Banshee |
| Rockslugs / salt | https://oldschool.runescape.wiki/w/Bag_of_salt |
| Desert lizards / ice coolers | https://oldschool.runescape.wiki/w/Desert_Lizard |
| Cockatrice / shield | https://oldschool.runescape.wiki/w/Cockatrice |
| Wall beasts / spiny helmet | https://oldschool.runescape.wiki/w/Spiny_helmet |
| Basilisks / shield | https://oldschool.runescape.wiki/w/Basilisk |
| Basilisk Knights / Exiles / 60 Slayer | https://oldschool.runescape.wiki/w/Basilisk_Knight |
| Bloodveld | https://oldschool.runescape.wiki/w/Bloodveld |
| Turoth / leaf weapons | https://oldschool.runescape.wiki/w/Turoth |
| Cave horrors / icon / area quest | https://oldschool.runescape.wiki/w/Cave_horror |
| Aberrant spectres / nose protection | https://oldschool.runescape.wiki/w/Aberrant_spectre |
| Dust devils / smoke protection | https://oldschool.runescape.wiki/w/Dust_devil |
| Kurask / leaf weapons | https://oldschool.runescape.wiki/w/Kurask |
| Skeletal wyverns / icy breath | https://oldschool.runescape.wiki/w/Skeletal_Wyvern/Strategies |
| Gargoyles / finishing tools | https://oldschool.runescape.wiki/w/Gargoyle |
| Nechryael | https://oldschool.runescape.wiki/w/Nechryael |
| Abyssal demons | https://oldschool.runescape.wiki/w/Abyssal_demon |
| Wyrms | https://oldschool.runescape.wiki/w/Wyrm |
| Drakes | https://oldschool.runescape.wiki/w/Drake |
| Hydras | https://oldschool.runescape.wiki/w/Hydra |
| Alchemical Hydra / task-only | https://oldschool.runescape.wiki/w/Alchemical_Hydra |
| Smoke devils / task-only | https://oldschool.runescape.wiki/w/Smoke_devil |
| Thermonuclear Smoke Devil | https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil |
| Cave krakens | https://oldschool.runescape.wiki/w/Slayer_task/Cave_krakens |
| Kraken / 87 Slayer / 50 Magic / task-only | https://oldschool.runescape.wiki/w/Kraken |
| Abyssal Sire / eligible assignment | https://oldschool.runescape.wiki/w/Abyssal_Sire |
| Fossil Island / ancient wyverns | https://oldschool.runescape.wiki/w/Fossil_Island_Wyverns/Strategies |

## Equipment eligibility

- [Slayer equipment](https://oldschool.runescape.wiki/w/Slayer_equipment), [Slayer helmet](https://oldschool.runescape.wiki/w/Slayer_helmet), [Spiny helmet](https://oldschool.runescape.wiki/w/Spiny_helmet), [Witchwood icon](https://oldschool.runescape.wiki/w/Witchwood_icon).
- [Boots of brimstone](https://oldschool.runescape.wiki/w/Boots_of_Brimstone), [boots of stone](https://oldschool.runescape.wiki/w/Boots_of_stone), [granite equipment](https://oldschool.runescape.wiki/w/Granite_equipment). All four brimstone requirements are checked. Granite requires both Strength and Defence.
- [Dragon scimitar](https://oldschool.runescape.wiki/w/Dragon_scimitar), [abyssal whip](https://oldschool.runescape.wiki/w/Abyssal_whip), [leaf-bladed spear](https://oldschool.runescape.wiki/w/Leaf-bladed_spear), [Attack weapons table](https://oldschool.runescape.wiki/w/Attack/Weapons_table). Leaf sword is 50 Attack; battleaxe is 65; all three need 55 Slayer.
- [Melee armour](https://oldschool.runescape.wiki/w/Armour/Melee_armour), [rune equipment](https://oldschool.runescape.wiki/w/Rune_equipment), [rune platebody](https://oldschool.runescape.wiki/w/Rune_platebody). Ordinary named items only; poisoned/trimmed/degrading and special exceptions are not generalized. Warhammers are deliberately outside the Attack-based pattern.
- [Elemental shield](https://oldschool.runescape.wiki/w/Elemental_shield), [mind shield](https://oldschool.runescape.wiki/w/Mind_shield), [mirror shield](https://oldschool.runescape.wiki/w/Mirror_shield), [V's shield](https://oldschool.runescape.wiki/w/V%27s_shield).

Internal charges/degradation are **not** inferred from name or bonuses. The family warning list is a conservative product policy, not an assertion that each item is empty. Unknown items remain visible with CHECK badges and are not claimed usable. This version does not read weapon charge variables.

## Location/travel records

Each TaskReferenceData.Location carries its Wiki path. Primary pages: Karuulm Slayer Dungeon, Slayer Tower, Fremennik Slayer Dungeon, Catacombs of Kourend, Smoke Devil Dungeon, Smoke Dungeon, Wyrmscraig Cavern, Jormungand's Prison, Mos Le'Harmless Cave, Asgarnian Ice Dungeon, Lumbridge Swamp Caves, Desert lizard, Kraken Cove, Abyssal Nexus and Wyvern Cave, under https://oldschool.runescape.wiki/w/.

Snapshot ring counts use exact RuneLite gameval ItemID constants for Slayer rings 1–8, eternal Slayer ring and dueling rings 1–8. [Slayer ring](https://oldschool.runescape.wiki/w/Slayer_ring), [ring of dueling](https://oldschool.runescape.wiki/w/Ring_of_dueling), [fairy rings](https://oldschool.runescape.wiki/w/Fairy_rings). Completion of Fairytale II establishes progression beyond initial ring access; a non-complete quest does not prove rings are unavailable. Staff requirements remain explicit and unverified.

## Supply arithmetic and APIs

Prayer/antipoison dose weights are the dose counts in their exact ItemID variants; sharks/lobsters are examples, not a food database. One salt/cooler per remaining kill is a labelled baseline for the applicable standard monster, without alternative finisher or wasted-use modelling. No DPS or prayer-duration estimate is fabricated.

RuneLite API usage is checked against installed API/client classes and a full build. Upstream reference: https://github.com/runelite/runelite ; game item IDs: net.runelite.api.gameval.ItemID ; quest states: Quest.getState(Client) ; levels: Client.getRealSkillLevel. Existing Slayer assignment table adapter and Filepath storage are retained from v0.4.


# v0.6.0 bundled reference sources

Data: https://github.com/weirdgloop/osrs-dps-calc/tree/498bd2ae666c83b0af3ac2265076ed237a27d1aa/cdn/json ; 5 November 2026 snapshot. Arrays are preserved and compacted. Counts and SHA-256 hashes are in src/main/resources/com/imth3keeza0/ultimateslayer/data/manifest.json. No images or calculator implementation source were imported.

Item skill-reference data: https://github.com/RSPSApp/osrsreboxed-db/blob/961011bda6daac734208b12bcd58c0e8660da730/data/items/items-skill-requirements.json . Its 2,135 entries are advisory and may be stale. Curated Twisted bow requirement: https://oldschool.runescape.wiki/w/Twisted_bow (85 Ranged).

Task/alias data: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/Task.java . 152 named groups retained as quoted factual data; original BSD notice in third-party/RuneLite-Task.txt.

Spell costs/levels: https://oldschool.runescape.wiki/w/Standard_spellbook and https://oldschool.runescape.wiki/w/Fire_runes . Standard-elemental costs are independently represented in SpellPlanning; no spell damage formula was copied.

Ammo references: https://oldschool.runescape.wiki/w/Rune_crossbow , https://oldschool.runescape.wiki/w/Magic_shortbow , https://oldschool.runescape.wiki/w/Adamant_crossbow , https://oldschool.runescape.wiki/w/Twisted_bow . Matching is a supported subset; unknown pairings stay unknown.

Wiki licensing footer: https://oldschool.runescape.wiki/w/Mind_shield identifies CC BY-NC-SA 3.0 and additional Weird Gloop terms. THIRD_PARTY.md and third-party/GPL-3.0.txt retain separate upstream notices. The old v0.5 statements above describe that version; the v0.6 section and COVERAGE.md supersede them for this release.


# v0.7.0 combat and branding additions

Published ordinary combat equations and modifier references are listed in COMBAT_MODEL.md. The Java calculator is original; no upstream TypeScript/GPL calculation implementation was copied. Reviewed 5 November 2026.

Weapon stance facts: https://github.com/RSPSApp/osrsreboxed-db/blob/961011bda6daac734208b12bcd58c0e8660da730/data/items/weapon-stances.json , compacted into data/stances.json with a recorded hash and the existing upstream licence notice.

Welcome-page style and exact clan invite were read from the user's AFK Timers WelcomeDialog.java. The logo was edited from the user's existing AFK stopwatch clan badge, using RuneLite's Slayer icon as the motif reference. No personal bank data or credentials were involved in the artwork or source upload. See docs/BRANDING.md.
