# v0.7.0 coverage

| Feature | Working | Boundary |
|---|---|---|
| Catalog | 2,921 monster records, 5,096 equipment records, 152 tasks, 2,135 advisory skill entries, 61 spell names/books | Source snapshot is not every mechanic/entity in OSRS |
| Combat | Supported ordinary PvM hit chance, max hit, weapon/spell ticks and normal-attack DPS | Explicit target/weapon subset; see COMBAT_MODEL.md |
| Joint gear search | Owned complete layouts, compatible ammo, required slots, two-handed rules, modeled set seeds and style comparison | Bounded best-found search, not global optimum |
| Effects | Slayer/mask, Salve non-stacking, Void/elite Void, crystal armour, supported elemental weakness and assumed prayers | Many weapon/target effects withheld |
| Magic | Twenty standard elemental plans/models; trident/seas/swamp/Sanguinesti attacks with charge assumptions | Pouches, most other spells, internal charge quantities and autocast unresolved |
| Gear visibility | Unknown gear stays visible with CHECK; curated known failures excluded | Imported requirements advisory; all unlocks not guaranteed |
| Bank/privacy | One capture per opening, local character-specific maps, clear and ordered IO | Live edge cases and crash durability need manual acceptance |
| Information page | First-run/reopen basics, adapted logo and optional original clan invite | No browser opens automatically |
| Future integrations | Clear coverage text instead of fake results | Full master/Mortimer/drop/profit/Tile Packs features remain unfinished |

All result assumptions are explicit. No gameplay actions or bank upload occur. Build/offline/UI checks do not constitute in-game acceptance.
