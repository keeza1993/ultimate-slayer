# Drop markers

Ultimate Slayer 0.11.0 adds optional ground-drop tile outlines alongside RuneLite Ground Items. It does not change Ground Items settings, hide items or click/loot anything. Ground Items can keep showing its labels. Ultimate Slayer's additional item labels default to off to avoid duplicate text.

Open RuneLite Settings > Ultimate Slayer > Drop markers. The master toggle, rune/herb categories and each value band are independently selectable. Every colour has a picker. Only my/group drops defaults to on; disable it to mark all visible ground items. Bank stars are independently selectable under Star bank badges. Saved setup bank highlights and Bank gear ranks can each be disabled as before.

| Matched stack | Default colour |
| --- | --- |
| GE value at least 500,000 gp | Gold |
| GE value at least 100,000 and below 500,000 gp | Purple |
| GE value at least 15,000 and below 100,000 gp | Blue |
| Rune below 15,000 gp | Pink |
| Clean/grimy herb below 15,000 gp | Green |

Value always takes priority, including on runes and herbs. Disabling a value band suppresses that band's marker; expensive runes/herbs do not fall back to a cheaper colour. Disabling rune/herb markers does not disable the value bands. Ordinary items below 15k are unmarked.

Matching IDs on the same tile are combined before valuing their quantity. Different items are never added together to reach a threshold. Noted items use their linked real item's name and GE price. Coins count as one gp each. The highest-value enabled match determines the tile outline when several matching items share it; optional labels show the individual matches in their own colours. Unknown GE prices count as zero, so an unknown-priced rune/herb can still receive its category colour. Untradeables have no invented market value. Values are cached RuneLite GE estimates, not live sale quotes or high-alchemy values.

Spawn, despawn, quantity and ownership changes update at the next game tick. Visible scene drops are seeded once on enable/login, then maintained using ground-item events. Logout/loading/hops and plugin shutdown clear all in-memory markers. The master switch stops collection and clears markers. This module reads ground items only and never scans a bank. It stores no loot history, adds no HTTP requests, and changes no other plugin's settings. Other plugins' labels/menu colours retain their own rules; Ultimate Slayer controls its tile outlines and optional labels only.

The initial visible-scene seed covers the top-level world view. Nested world views are not explicitly seeded; event-delivered ground drops can still be observed. In-game acceptance of marker positioning, ownership behavior and Ground Items coexistence is pending.
