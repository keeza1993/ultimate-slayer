# Ultimate Slayer — v0.7.0 handoff

Prepared 6 November 2026. Package UltimateSlayer-0.7.0-flat.zip. Development authorised. User confirmed v0.5.1 gear visibility; v0.6.0 supplied the offline catalog. This batch adds supported combat/loadout recommendations, the adapted clan logo, the AFK Timers-style information page and an upload under keeza1993. Do not treat DESIGN.md's original planning-only wording as current.

## Install, run and source

Extract flat ZIP contents directly into C:\Users\User\IdeaProjects\UltimateSlayer. JDK 17 / Gradle 9.6.0, Java target 11. Run gradlew.bat build, then gradlew.bat run. Safe/developer-mode launch retained. Work was done in the chat's isolated work/UltimateSlayer source; the user's intended installation was not overwritten. No Plugin Hub submission.

## New architecture

CombatCalculator is original Java implementing published ordinary-PvM equations for the explicit supported target/weapon subset. It reports attack/defence rolls, accuracy, max hit, ticks and normal-attack DPS. It supports exact stances, observed boosts, optional prayer/on-task/charge assumptions, non-stacking Salve/Slayer effects, full normal/elite Void, crystal pieces, twenty elemental spells and three powered-staff families. COMBAT_MODEL.md documents excluded effects and special targets. Do not broaden capability claims merely because a monster/item exists in the catalog.

LoadoutOptimizer evaluates complete owned loadouts with multiple weapon/stance/ordinary/Void/crystal starts, then whole-setup swaps. It preserves task/location protection, two-handed exclusivity, ammo compatibility and snapshot-only ownership. Limits: six ordinary armour candidates plus effect pieces per slot, all observed ammo, 24 starts, three passes, 40,000 evaluations. It is a bounded best-found search, not a global optimum proof. Standard magic requires loose runes for one cast; this is not trip endurance.

UltimateSlayerPanel runs the search on a cancellable background worker. Inputs/account/snapshot/target/style changes invalidate old results; applying a result changes only the sidebar. Worn gear is separately copied from live equipped IDs, never merged into saved ownership. Prayer-point decay and XP-only changes do not continually cancel owned results. Relevant observed attack/strength/ranged/magic boosts update damage inputs.

WelcomeDialog follows AFK Timers' dark/cyan information-page style. It appears once, reopens via Info/settings and offers the exact existing optional clan invite https://discord.gg/UAAe6QEGD plus in-game contact Th3keeza0. Browsers open only on click. Branding replaces the timer with the Slayer skull/sword and preserves OSRS GEEZAS. Full transparent logo is in docs/images and packaged branding/logo.png; it is also used for the toolbar icon. Artwork generated with the built-in image tool from the existing AFK badge and Slayer skill reference; prompt/provenance recorded in docs/BRANDING.md.

## Contracts retained

Information only: no combat, movement, prayer, banking, equipment or task-selection automation. No runtime HTTP, bank upload, telemetry, credentials, PIN/login-widget access or input hooks. The personal bank is enumerated once after each legitimate interface opening; withdraw/search/tab events do not recapture. Immutable coherent bank/inventory/equipment snapshots are local and character-specific with hashed profile names. Current carried/worn items stay separate. Clear suppresses that opening. Shared bounded LocalSnapshotIo retains cross-controller deletion ordering; process crashes can interrupt pending deletion.

Hidden required-item display does not disable draft constraints. Karuulm elite reward exemption is session-only explicit claim confirmation, not inferred from diary completion or owning an item. Unknown items/levels/quests/charges remain uncertain; unmodeled numerical cases fall back to the visible bonus shortlist. Preview/current assignment and assigned-area restrictions remain separate. Metadata remains in batches of 48 saved IDs; client reads and Swing writes retain their thread boundaries.

## Validation and upload identity

Full build and six invariant suites plus comparison/sidebar/welcome renders pass against cached RuneLite client/API 1.13.6. CombatChecks proves ordinary hit chance against exhaustive small dice and checks fixed rolls/max hits/speeds, zero-to-one damage, set completeness, Salve non-stacking, crystal, powered attacks, boosts, required slots, ownership and joint-search choices. ComparisonPreview verifies background completion, apply-to-draft, stale account cancellation, style reset, optional clan page, transparent alpha and 225px bounds. No in-game validation or FPS claim.

GitHub identity was independently verified as keeza1993 (ID 301443897), matching AFK Timers ownership and the installed Git account. Commit author/committer must use that identity, not a bot or alternate account. The browser creation attempt stopped because its URL could not be verified; direct authenticated GitHub operations are used instead. Upload only clean source/resources/docs; exclude bank data, build outputs, caches and credentials. Repository name ultimate-slayer, private for review. Final remote verification and commit details accompany the delivered release.

Remaining work: expand explicitly modeled weapons/targets/effect combinations, independent in-game acceptance, full master/Mortimer and drop/profit data, broader transport/unlock/charge readers and supported Tile Packs integration. Keep data/reference scope distinct from combat validity. Retain third-party notices and pinned data hashes; no Wiki calculator implementation code was ported.
