# v0.8.0 combat model and search boundaries

## Meaning of a result

The estimate assumes uninterrupted ordinary PvM attacks against the selected supported reference variant. Attack roll, defence roll, accuracy, maximum hit and ticks are displayed. Mean successful damage accounts for a successful zero becoming one; missed attacks remain zero. DPS divides expected damage by 0.6 seconds per tick. It is not measured combat performance or an exact kill-time model.

Physical defence uses NPC Defence and the matching stab/slash/crush or light/standard/heavy ranged defence. Magic uses NPC Magic and its magic defence. Targets with special phases, flat armour or unmodeled ordinary-combat rules do not produce a numerical recommendation. The explicit ordinary-target name set is in ReferenceCatalog.Monster; no claim covers all 2,921 catalog records.

Actual live offensive bonuses feed item models. Reference data supplies weapon category/speed, and a separately attributed factual stance table limits melee attack types/stances. Base levels validate curated requirements; observed boosts may affect damage/accuracy without falsely raising base eligibility. Assumed prayers are None/Piety/Rigour/Augury, with base Prayer/Defence thresholds and a warning that unlock/activation is not verified. No prayer or potion is activated by the plugin.

## Modeled cases

- Ordinary metal melee weapons, whip/tentacle, selected standard high-tier weapons and leaf-bladed sword/spear, with the actual available stances.
- Supported ordinary bows/crossbows, ballistas, metal darts/knives, crystal bow and Bowfa. Supported external ammo must be compatible and observed in the ownership snapshot. Enchanted bolts are excluded from numerical search; no proc effect is guessed.
- Twenty standard elemental spells, including unlocked-tier base-damage scaling, and trident of the seas/swamp or Sanguinesti normal built-in attacks. Powered attacks need explicit charge/degradation assumptions. Standard-spell loadout search checks enough loose runes for one cast, allowing simple elemental staff substitutions; it does not prove trip endurance or autocast capability.
- Slayer helmet/black mask task bonuses only when the matching-task assumption is enabled. Imbued versions extend the effect to ranged/magic. Salve against recorded undead does not stack with task helmets.
- Full normal/elite Void and per-piece crystal armour bonuses with the applicable crystal bow. Incomplete sets do not receive full-set effects; protective constraints still win.
- Matching standard-element weakness, chaos-gauntlet bolt bonus and supported assumed prayers.

Additional supported ordinary weapons: zombie axe, Sarachnis cudgel, brine sabre, granite longsword, abyssal dagger, Voidwaker and noxious halberd, plus ordinary metal axes/pickaxes. Special attacks and venom remain excluded. Warped sceptre uses floor((8*observed Magic+96)/37), four ticks and an explicit charge assumption. Dragon hunter lance applies 20% accuracy/damage to recorded draconic ordinary targets; granite hammer applies 30% to recorded golems. Leaf-bladed battleaxe applies 17.5% damage to turoth/kurask. Target leafy immunity also constrains direct monster browsing; supported ranged tasks require compatible broad ammunition.

Added ordinary families include jellies, crawling hands, ogres, ogresses, hobgoblins, crabs, baby dragons, common humanoids and Stronghold training monsters. Unused/hidden/Nightmare Zone versions are excluded in addition to previous special-mode exclusions. This remains an explicit allowlist, not universal support.

## Withheld or outside scope

Fang rerolls, Scythe multi-hits, Twisted bow scaling, Shadow amplification, blowpipe internal darts, dragon hunter crossbow/demonbane effects, obsidian/Berserker necklace, Inquisitor bonuses, many other conditional effects and special boss mechanics are not modeled. These items remain visible in the ordinary bonus shortlist. Charged/degrading states are assumptions, never inferred from ownership. Explicit inactive states stay blocked. Pouches, combination/sunfire runes, tomes, smoke-staff stacking and most non-elemental spells are not resolved by this model.

Unknown equipment requirements remain CHECK conditions, not hidden items or verified eligibility. Imported skill-reference records can be outdated; their warnings are advisory. A modeled result may therefore be conditional on an unverified unlock even when the arithmetic is supported.

## Search method

The search creates complete owned seeds for weapon/stance pairs, including ordinary, Void and crystal templates. Shared required-slot rules filter the pool first. It then compares swaps using whole-loadout DPS, not isolated slot bonuses. Ammo is filtered for compatibility before selection and every result contains only snapshot-owned gear.

Six ordinary armour candidates per slot plus modeled effect representatives are retained; ammunition is retained across tiers/families. Up to 24 starting setups receive up to three improvement passes, with a hard 40,000-evaluation limit. Results are deterministic for fixed inputs. This multi-start local search is bounded and not exhaustive, so it is labelled best found rather than a guaranteed global best-in-slot setup.

The worker is cancellable; account, snapshot, target, style, spell and assumption changes invalidate old results. Applying a result changes the sidebar draft only. Current worn-item stats are kept separate from immutable snapshot ownership.

## Primary formula references

- https://oldschool.runescape.wiki/w/Damage_per_second/Melee
- https://oldschool.runescape.wiki/w/Magic#Magic_accuracy
- https://oldschool.runescape.wiki/w/Maximum_magic_hit
- https://oldschool.runescape.wiki/w/Void_mage_helm
- https://oldschool.runescape.wiki/w/Void_Knight_equipment
- https://oldschool.runescape.wiki/w/Slayer_helmet_(i)
- https://oldschool.runescape.wiki/w/Passive_effect
- https://oldschool.runescape.wiki/w/Augury

Reviewed 6 November 2026. CombatChecks includes independent fixed examples and brute-force dice comparisons, not merely calls that mirror implementation branches. No calculator implementation code was copied from these sources.
