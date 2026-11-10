# Wiki strategy equipment and bank ranks (0.9.0)

The normal owned view uses the equipment tables on the selected monster's Wiki strategy page, filtered against the character's immutable bank-opening snapshot (bank, inventory and worn items). Choose the actual monster/boss variant, then a Wiki method. Ordinary task pages are tried only for the same monster family; a boss never silently inherits an ordinary task setup. Assignment-area constraints remain separate.

Each equipment slot shows up to three different owned families, numbered and coloured: 1 green, 2 yellow, 3 red. Bank borders and badges use those same lists. These are relative owned choices, not safety ratings. A slot with two suitable choices has two ranks. Main weapons and special-attack weapons are separate roles. The bundled Araxxor example ranks an owned abyssal bludgeon above a zombie axe and does not use a Bandos godsword as the main weapon.

Rank order starts with the Wiki table. Where every candidate for a slot can be evaluated, the existing ordinary-attack calculator compares complete loadouts (including the corresponding offhand and ammo) to refine that slot's order. If a weapon, effect or target is unmodeled, the order stays Wiki-based; unavailable estimates do not count as zero DPS. This is a bounded equipment plan, not a guaranteed global optimum, measured kill time or full encounter simulation. Independent conditional Wiki choices and footnotes are not automatically resolved. Partial Void setups are omitted; crystal armour is paired only with a crystal bow/Bowfa. Review the source page, charge assumptions and CHECK items.

Required protection overrides table ranks, two-handed weapons clear the shield slot, and supported ranged ammo is matched to the selected weapon. Missing required protection suppresses bank highlights. Inactive/broken items and curated known unmet requirements are excluded. Cosmetic families are deduplicated, without equating imbued/unimbued states. Unknown item stat metadata is still reported; the plugin cannot promise to compare equipment it could not resolve.

## Public Wiki updates

Public tables load on a bounded background worker from the fixed `oldschool.runescape.wiki` MediaWiki parse API. Only public monster/task page titles are sent. No account name, bank contents, item quantities, credentials or PINs are sent. Parsed equipment names and table order are cached locally for seven days. HTML/scripts are never executed. Requests have timeouts and response-size limits; account/task changes discard stale UI completions.

Araxxor has a bundled factual baseline reviewed 10 November 2026, so the reported case works offline. Other monsters depend on an available parseable Wiki table or a previously cached table. Unsupported/blocked/changed tables show an explicit message; a generic bonus shortlist is never coloured as Wiki guidance. Use **Refresh Wiki table** to retry. Disable **Update public Wiki tables** to use cached/bundled data only. **Wiki strategy gear** and **Bank gear ranks** are independently optional. Public tables may contain conditional choices that need manual checking; follow the visible source-page link.

This changes the previous offline-only networking policy for optional public strategy retrieval. Bank capture remains local and once per legitimate bank opening. The overlay paints visible personal-bank widgets; it does not enumerate the bank or read shared/group storage. Logging out, changing accounts, clearing snapshots, completing a task or disabling relevant settings clears the ranks.

## Sources

- https://oldschool.runescape.wiki/w/Araxxor/Strategies - factual equipment order and separate special-attack role; reviewed from indexed Wiki table excerpts on 10 November 2026.
- https://oldschool.runescape.wiki/w/Abyssal_bludgeon - 70 Attack and Strength requirements.
- https://oldschool.runescape.wiki/w/Zombie_axe - 65 Attack requirement.
- https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/overlay/WidgetItemOverlay.java - public bank widget overlay API reference; original implementation here.

The source Wiki page and retrieval/review date are shown in the sidebar. The application retains equipment facts rather than article prose or calculator implementation code. The Wiki and its contributors are not affiliated with this plugin.

## Validation

`verifyWikiGear` checks rendered-table parsing, separate strategies/special rows, cache serialization, the reported owned Araxxor bank, two-handed/offhand pairing, no invented third choice, known requirements, cosmetic state distinctions, protection precedence, and bank-rank removal on task/account changes. Existing checks cover bank snapshots, task planning, combat arithmetic and the asynchronous Swing comparison. Live Wiki availability and real in-game rendering still require environment testing; fixture parsing is not a claim of universal Wiki-table coverage.
