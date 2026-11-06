# Offline catalogue schema 2

This is readable JSON with shared dictionaries, not compressed or executable content. It removes repeated field labels and unused reference fields. IDs, counts, current runtime values and item-state restrictions are preserved. Live item names, equipment slots and offensive bonuses continue to come from RuneLite metadata. No network access was added.

- equipment: each profile is `[speed, category, inactiveState]`; each group is `[profileIndex, [itemIds...]]`. `inactiveState` is `inactive` when the original version contains inactive/uncharged/broken/empty, otherwise empty. The runtime only used these three reference fields; unused reference names, slots, weights, images, bonuses and defences are omitted.
- monsters: rows are `[id, nameIndex, versionIndex, profileIndex]`. Profiles are `[combatLevel, attackSpeed, hp, defenceLevel, magicLevel, defenseIndex, attributesIndex, weaknessIndex]`. Defenses are `[stab, slash, crush, light, standard, heavy, magic, flatArmour]`. Names, versions, attributes and weaknesses are readable dictionaries. All fields previously read by the plugin are preserved.
- requirements: each group is `[profileIndex, [itemIds...]]`; profiles are the original skill/level objects.
- spells retain name/spellbook. Stances retain attack_type/attack_style for every original category/row. Other upstream fields were unused.

data/manifest.json retains pinned upstream commit identifiers, original packaged SHA-256 hashes and derived SHA-256 hashes. Licences and attribution remain in THIRD_PARTY.md and third-party/. Projection equivalence was checked for every record against the original local v0.7 resource files, separately from the Java invariant suites.

Schema expansion is implemented in ReferenceCatalog. Monster combat support still requires the explicit ordinary-target allowlist; catalogue membership alone never grants numerical DPS support.
