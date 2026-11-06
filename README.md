# Ultimate Slayer — 0.7.0

<img src="docs/images/ultimate-slayer-logo.png" alt="OSRS GEEZAS Ultimate Slayer" width="360">

Created by **Th3keeza0** for **OSRS geezers**. Information and planning only; no gameplay automation.

## Install and run

Extract the flat ZIP directly into C:\Users\User\IdeaProjects\UltimateSlayer, replacing matching files. build.gradle, gradlew.bat and src belong directly in that folder. Use JDK 17, Gradle 9.6.0; source target Java 11.

    .\gradlew.bat build
    .\gradlew.bat run

The development launcher retains safe mode. This is a development plugin, not a Plugin Hub release. Jagex account help: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## New in this release

- A real normal-attack combat model for a declared subset of ordinary monsters/weapons: hit chance, max hit, attack interval and DPS estimates. It uses exact attack types and distinguishes NPC Magic defence from physical defence.
- Complete owned-loadout comparisons: multiple weapons/stances, compatible ammunition, spells, protected equipment slots, two-handed constraints and supported full-set seeds. It can find a faster weapon or complete Void set that individual bonus leaders miss.
- Supported modifiers: matching-task Slayer helmet/black mask, non-stacking Salve bonuses against undead, ordinary/elite Void, crystal armour with crystal bow/Bowfa, supported elemental weaknesses and chaos gauntlets. Trident/Sanguinesti built-in attacks require an explicit usable-charge assumption.
- Compare all supported styles or just the selected style. Choose observed level boosts, an assumed prayer, on-task status and charged/degrading usability. These are planning assumptions; they do not activate anything in game. They reset on account changes.
- Run the search on a background worker, then choose **Show in draft** to display a result. Inputs, account transitions and module changes invalidate stale results. Current worn gear is evaluated separately, never added to an old ownership snapshot.
- The clan logo now has the Slayer skull/sword behind the existing clan artwork instead of the stopwatch. A matching AFK Timers-style welcome explains the basics, privacy and optional clan invitation. Reopen it with **Info** or **Show welcome**.

## Use it

1. Open your personal bank once. Close/reopen for a new ownership snapshot; withdrawals, tabs and searches do not recapture.
2. In the sidebar choose the actual **monster variant**. Task previews stay separate from your real assignment and assigned-area restrictions.
3. Choose a combat style. For standard magic, select a supported elemental spell in **SPELL PLAN**.
4. Check the combat assumptions and press **Find best modeled setup**. Select **Show in draft**, then expand **Why this setup / limits** for the applied effects, rolls and exclusions.
5. CHECK items still need requirement/state confirmation. Never treat a planning result as proof you can access the area or complete the whole task safely.

The original bonus shortlist remains available whenever a numerical model is unavailable. Search is bounded: six ordinary candidates per armour slot plus effect pieces, all observed ammunition candidates, 24 starting loadouts, three improvement passes and at most 40,000 evaluations. **Best found modeled setup** is not a proof of global optimality.

## Scope and privacy

The offline catalog retains 2,921 monster/variant records, 5,096 equipment records, 152 task/alias groups, 2,135 advisory item skill entries and 61 spell names/books. Catalog coverage is broader than combat-model coverage. Supported rune planning covers twenty standard elemental spells.

The combat model deliberately withholds numerical recommendations for unmodeled target mechanics and weapons/passives such as Fang, Scythe, Twisted bow, Shadow, blowpipe and enchanted-bolt procs. Normal attacks exclude special attacks, poison/burn, AoE, regeneration, overkill and downtime. See COMBAT_MODEL.md and COVERAGE.md for boundaries. Full master/Mortimer probabilities, drop/profit tables and Tile Packs remain future work.

No runtime networking, bank uploads, telemetry, credentials, PIN/login-widget access or game input. Snapshots remain local and character-specific. The bank is enumerated once per legitimate opening; comparisons use immutable capture-time ownership. Current carried/worn views remain separate. Clear suppresses current-opening recapture. The shared bounded IO queue preserves deletion ordering across controller changes; process-crash deletion durability is not guaranteed.

## Clan and validation

Want to join OSRS geezers? Add **Th3keeza0** in game or [join our Discord](https://discord.gg/UAAe6QEGD). The plugin opens this only when you click the button.

Build, six invariant check suites and synthetic sidebar/comparison/welcome previews pass against cached RuneLite 1.13.6. No in-game acceptance or FPS claim is made. See VALIDATION.md. Data licences and pinned sources are retained in THIRD_PARTY.md, SOURCES.md, third-party/ and data/manifest.json. The calculator is an original Java implementation of published equations, not a port of the GPL Wiki calculator code.
