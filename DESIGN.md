# Ultimate Slayer — development blueprint

Prepared for Kieron / imth3keeza0 · 4 November 2026

Status: research and design only. No plugin implementation, repository creation, existing project changes or submission has been performed. This document consolidates the supplied Pasted text.txt handoff and source checks. Proposed architecture and acceptance criteria below are design decisions, not claims of tested behavior.

## 1. Decision and readiness

There is enough information to begin a modular foundation when coding is explicitly requested. There is not yet enough evidence to promise a complete, accurate DPS optimiser or automatic Tile Packs control. Neither needs to block the foundation.

The distinguishing feature is a private, account-specific owned-equipment comparison linked to a searchable Slayer reference. Players choose their preferred combat style and optional modules. Recommendations explain their assumptions, data age and limitations. They never operate the game.

Three release gates remain: verify bank event ordering in a live client; settle the calculation implementation and licensing; obtain a supported integration route before automatically changing Tile Packs. A complete monster/item/master dataset has not been harvested or audited in this research pass.

## 2. Binding requirements

- Working name Ultimate Slayer; author identity imth3keeza0.
- Every substantial feature has an individual switch and a category master switch. Disabled modules stop their listeners, jobs and overlays where practical.
- Reference-only use works without automatic task detection, bank access or network requests.
- Capture bank contents only once per legitimate bank opening, after contents are available. Never scan continuously, on every tick, after each withdrawal, or during combat. Refresh only at the next opening.
- Store snapshots locally, separated by character; show capture time and provide deletion.
- Never read, capture, log, store or transmit PINs, passwords, authentication codes, login input or credential-bearing widgets. No input hooks or security bypasses.
- Never send bank contents to third parties by default. No combat, equipment, prayer, movement, banking or task-selection automation.
- Support main accounts and Ironmen; do not assume buying an item is an available acquisition method.
- Compare melee, ranged and magic separately; distinguish preferred style from highest calculated performance.
- Prefer versioned bundled reference data; avoid gameplay scraping and unnecessary web requests.
- Preserve manual Tile Packs choices; do not build a competing marker collection.
- Readability, larger text, high contrast, optional overlays and reduced clutter are core interface requirements.

## 3. Feature catalogue and scope

All features below remain individually selectable, including those marked core. Core means a first-release priority, not mandatory activation.

| Module | Planned features | Scope and dependency |
|---|---|---|
| Reference core | Search, monster variants, requirements, weaknesses, immunities, required equipment, source links | Core; offline reference |
| Task context | Recognition, assigned variant/group, original and remaining quantity, location restriction, completion state | Core; optional detection |
| Owned equipment | Bank snapshot, inventory/equipment context, item variants, usable candidates, missing gear, saved preferences | Core priority; independent of task detection |
| Equipment comparison | Style selection, loadout constraints, best supported owned setup per style, preference override | Staged; only claim best within evaluated candidates |
| Combat calculations | Accuracy, max hit, speed, DPS, expected kill time, ammunition/spell cost, special effects | Later; validated supported mechanics only |
| Masters | Assignment tables, weights, eligibility, points, skips, blocks, unlocks, extensions, comparisons, Mortimer | Reference first; personalised probability later |
| Locations | Dungeons, task restrictions, access requirements, teleports, optional map points and navigation information | Reference first; no automated movement |
| Alternatives | Boss alternatives, superior variants, task-credit eligibility | Reference with explicit restrictions |
| Drops and economy | Tables, rates, uniques, GE value, supply estimates | Optional; provenance and freshness required |
| Observed progress | Task history, XP, loot, supply use, completion estimates, long-term progress | Later; observed period only |
| Tile Packs | Task/location/variant-to-pack mapping, preferred packs, optional activation and cleanup | Experimental integration gate |
| Interface | Sidebar order, simple/advanced detail, font size, contrast, presets, account preferences | Core framework, staged polish |

Task XP and loot statistics cannot reconstruct activity that occurred while tracking was disabled or on another client. Supply cost is an estimate unless consumption can be attributed reliably. A monster's weakest defence alone does not determine best owned gear.

## 4. Verified technical starting points

### RuneLite baseline

The official example build currently targets Java 11 through options.release.set(11), uses UTF-8, Gradle and compileOnly RuneLite client dependencies. A newer JDK used to run Gradle does not change the Java release target. Select a compatible JDK/wrapper combination at implementation time, pin the tested RuneLite release for reproducibility, and separately check compatibility with the latest release. Do not reuse old project build assumptions without checking them. [S1]

Use RuneLite's normal Plugin, Config, event bus, ClientThread, toolbar, sidebar and overlay facilities. Read client-owned state on the client thread, create immutable value copies, perform expensive pure calculations off-thread and update Swing on the event dispatch thread. Do not carry live NPC, widget or client objects into background calculations.

### Task integration

The current core SlayerPluginService exposes getTask(), getTaskLocation(), getInitialAmount(), getRemainingAmount() and getTargets(). The plugin exports it through its public module. Prefer this documented source surface over private fields or copying the entire core plugin. Null/unavailable service behavior and optional dependency injection need a small integration check. [S2]

Current core Slayer source uses relevant varp/varbit changes and game database rows to update task context; it includes Mortimer modifier and streak handling. This invalidates older designs based solely on chat parsing or counting XP drops. If a core-service adapter is unsuitable, implement a narrowly scoped adapter using the permitted current client values. Do not infer a task from nearby NPCs. [S3]

Reference mode stays available when task detection is off. Avoid a hard plugin dependency that forces core Slayer features on just to browse reference data.

### Bank and item events

ItemContainerChanged is a general container-change notification, including withdrawals; it is not synonymous with bank opening. WidgetLoaded and WidgetClosed are available interface lifecycle events. These are building blocks rather than a verified complete-snapshot signal. Match bank interfaces and containers using the selected RuneLite release's constants. A bank inventory container can persist after interface closure, so container presence alone is insufficient. [S4]

ItemVariationMapping provides base-item mapping. Treat it as a grouping aid, not proof that variants have identical combat behavior. Preserve the raw item ID and combat state alongside grouping IDs. [S5]

## 5. Bank snapshot contract

Proposed state sequence: CLOSED → WAITING_FOR_CONTENTS → CAPTURED → CLOSED. Account/profile changes invalidate any pending capture.

1. A legitimate bank interface opening creates a fresh session token. PIN entry is outside this system; no PIN widget or input is inspected.
2. Wait for the real item container to become available for that opening. Coordinate interface and container events without assuming their ordering.
3. If necessary, schedule a bounded readiness check tied to that opening. Do not repeatedly enumerate items while waiting. Stop when the bank closes, the account changes or the bounded wait expires.
4. After confirming that this opening's content is ready, copy item IDs and quantities once on the client thread. Reject capture if combat is detected; do not wait for combat to finish and silently scan later in the same opening.
5. Mark the session CAPTURED before scheduling downstream work so repeated events cannot trigger another enumeration.
6. Parse, filter, normalise and persist the immutable copy off-thread. Use atomic replacement, schema versioning and bounded input sizes.
7. Ignore later bank-container changes in that opening. Closing re-arms capture for the next opening.

Empty-bank versus not-yet-loaded must be tested explicitly. Searching, changing tabs, resizing, deposit boxes, group storage and other bank-like interfaces must not create false openings. Initially support the ordinary personal bank only; identify other stores separately rather than pretending they were scanned.

Important consequence: the bank remains an opening-time snapshot after withdrawals or deposits. Label it “Bank captured at [time]; changes appear next opening.” Do not describe it as a live bank.

Inventory/equipment may be captured with the opening for a coherent ownership view. Later live inventory/equipment can show what is currently carried, but must not be blindly added to an old bank snapshot: a withdrawn item could otherwise be counted twice. Initial recommendations should use the coherent opening-time ownership set and separately label current carried gear. When quantities matter, report uncertainty until the next bank opening instead of promising supplies that may no longer exist.

If the plugin starts while the bank is already open, wait for the next opening. If the bank capture module is disabled, there must be zero bank enumeration. A manual “recalculate” button uses saved data and never overrides the refresh-only-on-opening rule.

### Snapshot fields

schemaVersion, opaqueCharacterKey, capturedAt, sessionGeneration, captureSource, referenceDataVersion, and item entries containing rawItemId, quantity and container provenance. Store only the equipment-related subset needed for comparison unless a separately enabled supply feature justifies more. Do not persist names, chat, widgets or raw client dumps.

Use a local plugin-owned file for sensitive item snapshots. RuneLite profile configuration offers useful account-scoped settings, but do not assume its persistence is local-only; verify sync behavior before storing bank data there. Keep preferences separate from item data. Resolve character identity through supported mechanisms, never login fields or display name alone.

## 6. Equipment and combat model

Maintain three identities: raw item ID (what was seen), display/group ID (cosmetic grouping), and combat identity (functional behavior). Noted items can represent ownership but cannot be equipped directly. Placeholders and zero-quantity entries do not count as owned usable items. Unknown state stays unknown.

Never merge charged with uncharged, imbued with unimbued, intact with broken, or distinct ammunition types merely because a base mapping exists. Charge counts, darts inside a blowpipe, internal spell runes and degradation state may not all be exposed by container IDs. Ask the player for an assumption when needed and label it.

Candidate validation must precede scoring: level/quest requirements; equipment slots; two-handed/offhand exclusivity; ammunition compatibility; spell availability; task-specific protective items; attack style; set bonuses; target mechanics; remaining usable state. Required safety equipment is a hard constraint unless the player explicitly explores an invalid setup, which must be labelled unusable.

Loadout optimisation is a combination problem. The best item in each slot independently may not make the best complete setup because of set effects, attack speed, accuracy thresholds and special weapons. Start with compatible curated loadout templates and bounded candidate search. Preserve alternatives where safe pruning cannot establish dominance.

Comparison input contains player levels, chosen boosts/prayers, attack style, spells/ammunition, task status, monster variant/phase, defence assumptions and equipment effects. Show these assumptions. Do not assume every unlocked prayer or potion is being used.

Output rows: best evaluated owned melee, ranged and magic setups; highest calculated DPS among supported results; player's preferred setup; confidence/unsupported mechanics; snapshot and reference version. A score-only prototype must say “equipment comparison,” not “highest DPS.” Never manufacture a numeric result for unsupported cases.

The Wiki calculator is an open-source TypeScript application licensed GPL-3.0, not an established Java library or a verified public calculation API. Reusing or porting its code needs a licensing decision and attribution review before implementation. Do not assume a port removes obligations. An independently implemented Java engine still needs verified formulas, effect coverage and regression comparisons. A user-initiated browser link is the simplest early alternative; disclose any gear/stats encoded into an external link and do not export the bank. [S6]

Alchemical Hydra should be a later regression scenario: compare all supported styles using the same player inputs and actual target conditions; do not force ranged solely because of a defensive weakness. Expected kill time must distinguish a sustained-damage approximation from a model that includes movement, phases and downtime.

## 7. Reference data plan

Do data extraction during maintenance/build preparation, not in the overlay renderer or per-player bank workflow. Bundle a reviewed version with the plugin. Runtime web access is optional and limited to clearly identified links or price data.

| Dataset | Required fields | Validation |
|---|---|---|
| Assignments | Stable task key, name, eligible NPC variants/bosses, restrictions | Task-credit checks; unknown IDs explicit |
| Monsters | NPC IDs, variant/phase, combat stats, styles, immunities, requirements, tags | Cross-check Wiki and client IDs; never merge distinct phases |
| Equipment | IDs, states, slots, bonuses, speed/style, effects, requirements | Combat identity mapping and compatibility checks |
| Masters | Assignments/weights, unlock predicates, block/skip/point/streak rules | Master-specific fixtures; replacements and Mortimer handled |
| Locations | Coordinates, entrances, access, task area, teleports | World-map and restriction checks |
| Drops | Source table, quantity/distribution, chance, conditions, shared tables | Probabilities and conditional rate checks |
| Provenance | Source URL/revision, retrieved/verified dates, license, data schema | Required on every imported record/group |

Candidate sources are Wiki structured templates/modules and its supported MediaWiki interfaces, current RuneLite client/game data and calculator datasets where licensing permits. The exact extraction endpoint, current request policy and complete schema still need verification; direct Wiki page access was restricted in this session. Do not invent a universal API or rate limit. Identify the application in requests, batch and cache, obey current policy and Retry-After, and use bounded backoff.

The Wiki currently identifies its text as CC BY-NC-SA 3.0 except where specified. That is separate from calculator GPL code and game assets; keep provenance and licensing separate rather than assigning one blanket license to everything. Review exact dataset/content permissions before bundling. [S7]

Updates: record source revisions/checksums; compare upstream changes; validate cross-references; review changed mechanics; release a new immutable dataset. Unknown or stale data displays a badge. A failed optional update keeps the last valid version. Never download executable plugin code as a data update.

For conventional single-task weighted assignment, probability is eligible task weight divided by total eligible weight. Apply blocks, quests, levels and unlocks first. Mortimer's choice of multiple tasks requires its own model: “offered at least once” differs from “selected,” and neither is the ordinary single-draw probability. Current source confirms special modifier handling; complete probability semantics and tables need their own validation. [S3, S8]

## 8. Tile Packs feasibility finding

Checked Plugin Hub manifest commit: b392d16ae7923cab0c5db7361c04f353e0cc21b4. At that commit, TilePackManager stores integer pack IDs and an enabled-list JSON value under group tilePacks / key packs. It loads the enabled list into memory at construction. PointManager uses that in-memory list. The plugin's configuration handler does not reload this list when the Tile Packs group changes; therefore writing the config value alone is insufficient to synchronise a running plugin. [S9–S12]

No supported cross-plugin activation service was established from the inspected classes. Do not use reflection, private-field access, automatic plugin restart, or direct config writes as a promised integration. This is not evidence that all future integration is impossible or prohibited.

Recommended first version: show matching pack names and location context and let the player enable them in Tile Packs. Automatic behavior stays disabled until a supported public service/event contract and review compatibility are established.

A future contract needs stable pack IDs/catalog versions, activation ownership, leases/reference counts, user overrides, and safe lifecycle cleanup. Only release packs acquired by this plugin. A manual takeover must preserve the pack even if its enabled state did not visibly change. Handle missing/disabled Tile Packs, alternate locations, boss variants, multiple matching packs, startup order, profile changes, crashes and unknown pack versions. Never restore a whole old enabled-list snapshot over the player's subsequent changes.

No request has been sent to the Tile Packs maintainer.

## 9. Proposed package and class responsibilities

Base package: com.imth3keeza0.ultimateslayer (proposal).

| Package | Proposed classes | Responsibility |
|---|---|---|
| core | UltimateSlayerPlugin, UltimateSlayerConfig, FeatureRegistry, FeatureLifecycle | Bootstrap, configuration and module activation |
| task | TaskContext, SlayerServiceAdapter, ClientTaskAdapter | Immutable task state and optional source adapters |
| reference | MonsterRepository, EquipmentRepository, MasterRepository, LocationRepository, DropRepository, Provenance | Read-only indexed data |
| bank | BankSessionTracker, BankSnapshotService, BankSnapshot, ItemIdentityResolver | Once-per-opening capture and item semantics |
| account | CharacterContext, LocalSnapshotStore, SnapshotMigration | Identity isolation, local atomic persistence and clearing |
| comparison | LoadoutConstraints, CandidateGenerator, LoadoutComparator, ComparisonResult | Valid loadouts and transparent ranking |
| combat | CombatInputs, CombatCalculator, EffectRegistry, CalculationSupport | Later validated mechanics and support reporting |
| integration | WikiLinks, PriceProvider, TilePacksAdapter | Optional external interfaces behind boundaries |
| progress | TaskHistoryStore, ObservedTaskMetrics | Locally observed history and estimates |
| ui | UltimateSlayerPanel, TaskView, MonsterView, EquipmentView, MasterView, DataStatusView | Sidebar with reusable display models |
| overlay | TaskInfoOverlay, LocationMapPoints | Optional low-work views of cached state |

No class should become a giant switch containing all monster rules. Modules declare dependencies. Enabling comparison may require reference data and a saved ownership snapshot, but must not quietly enable scanning or network access. A disabled module cancels outstanding jobs and discards late results using generation tokens.

## 10. Interface proposal

Sidebar entry opens Task, Search, Equipment, Masters and Settings views. Show only enabled sections. Task view starts with assignment, remaining count, location restriction and required items. Expand alternatives, locations, drops and advanced stats on demand.

Equipment view starts with target, preferred style and snapshot timestamp. Display one compact loadout card per selected style, explanations for excluded items and supported calculation results. Offer separate owned-only and missing-upgrade views. “Not observed in the last snapshot” is more accurate than “you do not own this.”

Settings group master switches, individual toggles, preset selection, text size/contrast, overlay preferences, account-local data status and a clear saved equipment-data action. Simple mode hides advanced assumptions until expanded; advanced mode exposes them without changing the underlying facts.

Useful optional additions: required-equipment checklist; explain-why comparisons; favourites per monster/location; stale-data badges; manual target selection; no-network reference preset. None changes gameplay.

## 11. Performance and security validation

Performance targets are proposed budgets, not measured results: zero bank enumerations outside authorised capture; at most one enumeration per opening; zero calculations in overlay rendering; no disk/network operations on the client thread; bounded latest-request-only comparison work; bounded history and image caches. Measure client-thread latency and frame-time distribution on a modest PC before setting release thresholds.

Read only relevant container/interface events while enabled. Index by task/NPC/item ID. Lazy-load detail panels. Cache comparison results by account generation, ownership revision, target variant, assumptions and data version. Render cached display models. Cancel work on logout, account/profile changes and shutdown. Treat results from earlier generations as invalid.

| Threat/failure | Required safeguard | Acceptance evidence |
|---|---|---|
| Credential access | No login/PIN widgets, input listeners or general client dumps | Source/dependency review; synthetic credential canaries absent from output |
| Accidental bank upload | No snapshot network path; explicit external payload schemas | Record requests using synthetic data; snapshots never leave process |
| Cross-account leakage | Stable character namespace and generation checks | Switch accounts mid-capture/calculation; no old result displayed |
| Corrupt cache | Bounds, schema validation, atomic write, safe invalidation | Truncated/malformed/oversized fixtures cannot crash client |
| Incorrect gear | Preserve item states and full loadout constraints | Charged/unusable/placeholder/ammo/set fixtures |
| Lag | Once-only capture; bounded worker work; cached rendering | Counters and frame-time measurements with modules on/off |
| Manual marker loss | No automatic control until ownership contract works | Manual-before/during/after-task lifecycle scenarios |

Local item data is still private information. Keep diagnostics aggregate and off by default; no complete inventory dumps. Clearing removes persisted and in-memory snapshots and cancels pending writes so deleted data cannot reappear. There is no claim of absolute security without implementation review and runtime validation.

## 12. Existing work and compliance

Core Slayer already supplies task presentation, counts, notifications and NPC-related features. Ultimate Slayer should add the owned-loadout/reference layer rather than duplicate all core overlays. Slayer Helper provides monster search and reference information; Slayer Wiki offers task-to-Wiki access. These repositories are comparison sources, not verified evidence of current Hub installation status. [S3, S13, S14]

Plugin Hub review covers security and rule compliance but does not certify accuracy, performance or compatibility. Informational intent does not guarantee approval. Keep navigation informational, combat comparisons outside reactive combat advice, and all game actions under player control. Review current client rules before any new overlay or automation-adjacent integration. [S15]

Submission preparation eventually needs correctly renamed template classes/packages/config group, metadata, author identity, icon, LICENSE and attribution, README, validation record and a manifest pointing to the source repository and full commit hash. The Hub builds/reviews source; a locally running JAR alone is not a published plugin. [S1, S16]

## 13. Implementation roadmap and acceptance gates

| Phase | Deliverable | Completion condition |
|---|---|---|
| 0: preparation | This spec, source register, licensing choices and test plan | Requirements retained; unresolved research clearly recorded |
| 1: foundation | Modular lifecycle, accessible sidebar, small versioned reference set, manual target selection | Reference works offline; toggles remove work; no bank/network dependency |
| 2: task context | Optional core-service adapter with explicit unavailable state | Login, hop, cancellation, completion, restricted task and Mortimer cases verified |
| 3: ownership | Once-per-opening local snapshot, item-state handling, delete controls | Event ordering, empty bank, repeated events and account isolation all pass |
| 4: gear comparison | Owned usable candidates, style preferences, constrained templates | Known loadout cases match expectations; no false DPS/best-in-game claims |
| 5: combat engine | Supported formula subset, effect coverage and regression cases | Checked results against a pinned trusted calculator revision and formula cases |
| 6: breadth | Audited master/location/drop datasets, observed progress, optional prices | Provenance coverage, personalisation correctness and runtime profiling pass |
| 7: integration/release | Tile Packs only if contract exists; Hub packaging and review | Manual preferences preserved; all release gates and source/license checks pass |

The first coding request should build phases 1–2 as a small reviewable foundation, followed by the bank feature as a separately validated milestone. Do not defer modularity, account isolation or privacy until the end.

## 14. Outstanding research register

1. Live RuneLite bank event timing, complete-container readiness and interface constants for the pinned release.
2. Exact optional Slayer service injection/lifecycle and release compatibility.
3. Stable character identity and local persistence path; preference-sync separation.
4. Full Wiki extraction route, current usage policy, per-dataset licensing and complete coverage audit.
5. Calculator implementation/license choice; supported effect list; validation fixtures and optimizer bounds.
6. Full master tables and Mortimer choice/modifier probability semantics; visible versus manually supplied unlocks.
7. Supported Tile Packs collaboration interface and approval suitability.
8. Current relevant Plugin Hub extensions beyond the source examples inspected here; avoid claiming an exhaustive ecosystem audit.
9. In-game performance, accessibility and conflict testing. No live client validation has occurred in this task.

These are engineering follow-ups, not questions Kieron needs to answer before the foundation can start. Until settled, the interface should say unavailable/unknown rather than guess.

## 15. Source register

Accessed 4 November 2026. Default-branch links can change; pin full repository revisions when implementation begins. Blob hashes below identify exact inspected content where retained; they are not repository commit hashes.

- S1: [Official example build](https://github.com/runelite/example-plugin/blob/master/build.gradle) and [template packaging guidance](https://github.com/runelite/example-plugin/blob/master/AGENTS.md).
- S2: [SlayerPluginService](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPluginService.java), blob 814542a7fb4e5067bbdc84b5fe92d5f492814898; [implementation](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPluginServiceImpl.java).
- S3: [Current core Slayer source](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPlugin.java), blob cee0521bc43c39dad150c5e46b9b2a1ed3108994.
- S4: [ItemContainerChanged](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/events/ItemContainerChanged.html) and [event catalogue](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/events/package-summary.html).
- S5: [ItemVariationMapping](https://static.runelite.net/runelite-client/apidocs/net/runelite/client/game/ItemVariationMapping.html) and [ConfigManager](https://static.runelite.net/runelite-client/apidocs/net/runelite/client/config/ConfigManager.html).
- S6: [Wiki DPS calculator repository and GPL-3.0 license](https://github.com/weirdgloop/osrs-dps-calc). Formula coverage and integration API have not been audited completely.
- S7: [Wiki policies](https://oldschool.runescape.wiki/w/RuneScape:Policies). Retrieved indexed policy text; direct API access was restricted. Verify exact reuse terms before extraction.
- S8: [Mortimer reference](https://oldschool.runescape.wiki/w/Mortimer) and [Slayer Master reference](https://oldschool.runescape.wiki/w/Slayer_Master). Indexed current reference; complete tables not extracted.
- S9: [Tile Packs Hub manifest](https://github.com/runelite/plugin-hub/blob/master/plugins/tile-packs), published source commit b392d16ae7923cab0c5db7361c04f353e0cc21b4.
- S10: [TilePackManager at published commit](https://github.com/TrevorMDev/tile-packs/blob/b392d16ae7923cab0c5db7361c04f353e0cc21b4/src/main/java/com/tilepacks/TilePackManager.java).
- S11: [PointManager at published commit](https://github.com/TrevorMDev/tile-packs/blob/b392d16ae7923cab0c5db7361c04f353e0cc21b4/src/main/java/com/tilepacks/PointManager.java).
- S12: [TilePacksPlugin source](https://github.com/TrevorMDev/tile-packs/blob/master/src/main/java/com/tilepacks/TilePacksPlugin.java), inspected blob d99ec91c2d6f25366168207bb8f639d10cd8ca69. Lifecycle handler checked on default branch; recheck published and future versions before integration.
- S13: [Slayer Helper](https://github.com/adrocic/slayer-helper).
- S14: [Slayer Wiki](https://github.com/ffalor/slayer-wiki).
- S15: [Plugin Hub review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review).
- S16: [Plugin Hub submission README](https://github.com/runelite/plugin-hub/blob/master/README.md).

Original requirement source: Pasted text.txt, read in full (472 lines). This blueprint refines implementation choices without superseding its no-automation, modularity, privacy or bank-refresh constraints.
