# Trip checklist and flat update package - 0.10.0

The **TRIP CHECKLIST** sits below the Wiki strategy selector. It covers task-specific protection, prayer restoration, example healing foods, required task tools, outbound travel and a return teleport. Each category has a persisted **HIGH / NORMAL / LOW** priority, adjustable with **Set supply priorities** or plugin settings. **Unpacked first** brings uncarried items forward; lowering a priority does not remove required-item rules.

Counts for items carried or worn now come from the normal inventory/equipment events. Bank quantities remain labelled **bank at capture** and are never combined with live quantities into an alleged current owned total: withdrawing can make the captured bank count stale. The bank is still read once per legitimate opening. Counts check availability, not enough supplies for a whole trip. Potion doses count every supported 1-4-dose bottle. Noted versions, potion storage, rune pouches, bags and unlisted food are not silently treated as available.

## Protection and tools

- Araxxor/araxytes, Vorkath and Zulrah receive a venom reminder. Extended anti-venom+, anti-venom+ and anti-venom are separate labelled alternatives; ordinary antipoison never satisfies that row. Protection durations differ.
- Ordinary dragon targets receive dragonfire supplies. Extended super/super antifire are the preferred standalone potion families. Ordinary/extended antifire is an alternative only when an appropriate shield has been observed; equip the shield and review the combination. Protect from Magic alone does not cover metal-dragon breath. Special boss breath requires the relevant strategy checks.
- Wyverns retain their icy-breath shield rules; ordinary antifire is not substituted for them. Existing masks, icons, boots, tools and finishing-item checks are reused. Salt/cooler goals use a one-use baseline for real remaining kills; preview tasks do not borrow the active task's count.
- Existing curated poison-task reminders remain distinct from venom protection. Prayer potions and super restores are alternatives; food is a clearly labelled list of tracked examples.

The selected Wiki strategy's **Inventory** section is also parsed and cached. Observed non-gear item names can match additional recommended supplies, including dose variants of combat potions. The full public reference list is available under **Show Wiki inventory list**. Links are reference recommendations, not a proof that every item is mandatory, accessible or sufficiently charged. Cached 0.9 tables are refreshed online to obtain this new section; Araxxor also has a bundled baseline.

## Outbound and return travel

**Prefer POH travel** defaults on. Enable **POH fairy ring confirmed** only if your house has that usable route; furniture is not scanned. Supported ring destinations include ALQ for the spider cave, CKS for the Slayer Tower, AJR for the Fremennik dungeon, CIR for Karuulm, BKP for the Smoke Devil Dungeon and DIP for the Abyssal Nexus. Direct alternatives include spider-cave scrolls/filled ectophial, charged Slayer rings, Rada's blessing 3/4 and charged dueling rings where relevant. Unlocks, daily uses and staffless fairy-ring rewards require confirmation. A dramen/lunar staff reminder appears for the fairy route, not when an observed direct teleport covers the approach.

Return choices include house tablets, supported loose-rune house casts and a charged dueling ring. If the outbound route uses a house teleport, one house tablet/cast is not counted for both journeys: reserve a second use or an independent return ring. House spells count only at 40+ Magic on the standard spellbook, with loose law/air/earth or dust runes; staff substitutions and rune pouches are unverified. Teleport restrictions and boss/area access still apply. Unknown locations receive a manual route check rather than an invented journey.

## Installing this update

Use the **UltimateSlayer-update** ZIP from the successful build's artifacts. Its top level contains `build.gradle`, `gradlew.bat`, `gradle/` and `src/` directly. It does not contain an extra `ultimate-slayer-main/` or version-number folder.

Close the development RuneLite window. Extract **into your existing UltimateSlayer project folder**, accept **Replace files**, then run `gradlew.bat run` there. Confirm the sidebar says **0.10.1**. The archive contains tracked source only; it does not include local caches, bank snapshots, passwords, IDE state or build outputs. Plugin configuration and bank snapshots are stored separately from source. No local project folder is deleted by this update.

## Validation and sources

`verifySupplies` covers task hazards, potion doses, bank/live provenance, shield combinations, wyvern distinctions, shared outbound/return resources, spellbook checks, user priority sorting, live Wiki inventory matching and a rendered sidebar. Existing Wiki gear/bank/combat checks remain in `check`; the CI live probe also checks Araxxor inventory links. CI prepares and verifies a flat source artifact from the exact checked commit.

Public factual sources reviewed 10 November 2026:

- https://oldschool.runescape.wiki/w/Araxxor/Strategies
- https://oldschool.runescape.wiki/w/Slayer_task/Araxytes
- https://oldschool.runescape.wiki/w/Extended_super_antifire
- https://oldschool.runescape.wiki/w/Extended_antifire
- https://oldschool.runescape.wiki/w/Metal_dragons/Strategies
- https://oldschool.runescape.wiki/w/Morytania_Spider_Cave
- https://oldschool.runescape.wiki/w/Spider_cave_teleport
- https://oldschool.runescape.wiki/w/Teleport_to_House
- https://oldschool.runescape.wiki/w/Teleport_to_house_(tablet)

No article prose is bundled. Only public page titles are requested from the Wiki; inventory and bank data remain local.

