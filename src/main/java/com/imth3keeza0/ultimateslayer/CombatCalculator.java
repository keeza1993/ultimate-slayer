package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/** Original implementation of documented ordinary PvM attack equations. No game actions. */
public final class CombatCalculator
{
    private CombatCalculator() {}
    public enum PrayerBoost { NONE, PIETY, RIGOUR, AUGURY }
    public static final class Mode
    {
        public final String style, stance;
        public Mode(String style, String stance) { this.style = style; this.stance = stance; }
        @Override public String toString() { return style + " / " + stance; }
    }
    public static final class Inputs
    {
        public final AccountContext account;
        public final ReferenceCatalog.Monster target;
        public final Mode mode;
        public final String spell, task;
        public final boolean onTask, charged, boosted;
        public final PrayerBoost prayer;
        public final TaskReferenceData.Location location;
        public final TaskReferenceData.Exemption exemption;
        public Inputs(AccountContext account, ReferenceCatalog.Monster target, Mode mode, String spell, String task,
                      TaskReferenceData.Location location, TaskReferenceData.Exemption exemption, boolean onTask,
                      boolean charged, boolean boosted, PrayerBoost prayer)
        {
            this.account = account; this.target = target; this.mode = mode; this.spell = spell; this.task = task;
            this.location = location; this.exemption = exemption; this.onTask = onTask; this.charged = charged; this.boosted = boosted; this.prayer = prayer;
        }
        public Inputs withMode(Mode next)
        { return new Inputs(account, target, next, spell, task, location, exemption, onTask, charged, boosted, prayer); }
        int level(Skill skill) { return boosted ? account.boostedLevel(skill) : account.level(skill); }
    }
    public static final class Result
    {
        public final boolean available;
        public final String reason;
        public final double dps, accuracy, meanHit;
        public final int maxHit, ticks;
        public final long attackRoll, defenceRoll;
        public final List<String> effects, warnings;
        private Result(String reason, double dps, double accuracy, double meanHit, int maxHit, int ticks, long attackRoll, long defenceRoll, List<String> effects, List<String> warnings)
        {
            available = reason.isEmpty(); this.reason = reason; this.dps = dps; this.accuracy = accuracy; this.meanHit = meanHit;
            this.maxHit = maxHit; this.ticks = ticks; this.attackRoll = attackRoll; this.defenceRoll = defenceRoll;
            this.effects = List.copyOf(effects); this.warnings = List.copyOf(warnings);
        }
        static Result unavailable(String reason) { return new Result(reason, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of()); }
        public String summary() { return available ? String.format(Locale.ROOT, "%.2f normal-attack DPS · %.1f%% hit · max %d · %d ticks", dps, accuracy * 100, maxHit, ticks) : reason; }
    }
    public static double hitChance(long attack, long defence)
    {
        if (attack < 0 || defence < 0) { throw new IllegalArgumentException("Negative-roll mechanics are outside this model"); }
        return attack > defence ? 1.0 - (defence + 2.0) / (2.0 * (attack + 1.0)) : attack / (2.0 * (defence + 1.0));
    }
    /** Successful zero rolls become one; misses are handled separately by hitChance. */
    public static double normalMean(int maximum) { return maximum < 1 ? 0 : maximum / 2.0 + 1.0 / (maximum + 1.0); }
    static String name(OwnedGearPlanner.Gear item) { return item == null ? "" : item.name.toLowerCase(Locale.ROOT); }
    static boolean chargedItem(OwnedGearPlanner.Gear item)
    {
        String n = name(item);
        return n.matches(".*(trident|sanguinesti|crystal|faerdhinen|saeldor|tentacle|barrows|ahrim|dharok|guthan|karil|torag|verac).*" );
    }
    static String blockedItem(OwnedGearPlanner.Gear gear, Inputs input)
    {
        String n = name(gear); ReferenceCatalog.Equipment ref = ReferenceCatalog.get().equipment.get(gear.id);
        if (n.matches(".*(uncharged|broken|inactive|\\(empty\\)).*") || (ref != null && ref.version.toLowerCase(Locale.ROOT).matches(".*(inactive|uncharged|broken|empty).*"))) { return "Known inactive item: " + gear.name; }
        String rejection = EquipmentEligibility.rejection(gear, input.account);
        if (rejection != null && !EquipmentEligibility.unverified(rejection)) { return gear.name + ": " + rejection; }
        if (chargedItem(gear) && !input.charged) { return "Charge/degradation assumption required: " + gear.name; }
        if (n.contains("amulet of the damned") || n.contains("inquisitor") || n.contains("berserker necklace") || n.startsWith("obsidian ")) { return "Conditional effect not modeled: " + gear.name; }
        return null;
    }
    public static List<Mode> modes(OwnedGearPlanner.Gear weapon, String requested)
    {
        if (weapon == null) { return List.of(); }
        if (requested.equals("Magic")) { return List.of(new Mode("Magic", "Spell")); }
        if (requested.equals("Ranged"))
        {
            return List.of(new Mode("Ranged", "Rapid"), new Mode("Ranged", "Accurate"));
        }
        List<Mode> result = new ArrayList<>();
        for (WeaponStances.Stance stance : WeaponStances.forCategory(weapon.category))
        {
            if (!List.of("stab", "slash", "crush").contains(stance.type)) { continue; }
            String type = Character.toUpperCase(stance.type.charAt(0)) + stance.type.substring(1);
            if (!requested.equals("Melee") && !requested.equals(type)) { continue; }
            result.add(new Mode(type, stance.stance));
        }
        return List.copyOf(result);
    }
    private static boolean ordinaryWeapon(OwnedGearPlanner.Gear weapon, String style)
    {
        String n = name(weapon).replaceAll(" \\(p\\+*\\)| \\(or\\)", "");
        if (style.equals("Magic"))
        {
            return n.contains("trident of the seas") || n.contains("trident of the swamp") || n.contains("sanguinesti staff")
                || (List.of("Staff", "Bladed Staff").contains(weapon.category) && !n.matches(".*(harmonised|harmonized|dragon hunter|twinflame|purging|slayer's staff|sceptre|scepter|ancient).*"));
        }
        if (style.equals("Ranged"))
        {
            if (n.matches(".*(twisted bow|dark bow|venator|scorching|dragon hunter|craw's|webweaver|tonalztic|eclipse).*")) { return false; }
            if (n.contains("ballista") || n.startsWith("karil's crossbow") || n.equals("dorgeshuun crossbow") || n.equals("hunters' sunlight crossbow")) { return true; }
            if (weapon.category.equals("Crossbow")) { return List.of("crossbow","bronze crossbow","blurite crossbow","iron crossbow","steel crossbow","mithril crossbow","adamant crossbow","rune crossbow","dragon crossbow","armadyl crossbow","zaryte crossbow").contains(n); }
            if (weapon.category.equals("Bow")) { return n.matches("(oak |willow |maple |yew |magic )?(shortbow|longbow)( \\(i\\))?") || n.startsWith("bow of faerdhinen") || n.startsWith("crystal bow") || n.equals("3rd age bow"); }
            return weapon.category.equals("Thrown") && n.matches("(bronze|iron|steel|black|mithril|adamant|rune|amethyst|dragon) (dart|knife)");
        }
        if (n.matches("(bronze|iron|steel|black|white|mithril|adamant|rune|dragon) (dagger|sword|longsword|scimitar|mace|battleaxe|warhammer|2h sword|claws|spear|hasta)")) { return true; }
        return List.of("abyssal whip","abyssal tentacle","ghrazi rapier","blade of saeldor","zamorakian hasta","zamorakian spear","saradomin sword","saradomin's blessed sword","saradomin godsword","bandos godsword","armadyl godsword","zamorak godsword","ancient godsword","abyssal bludgeon","elder maul","granite hammer","granite maul","leaf-bladed sword","leaf-bladed spear").contains(n) || n.startsWith("blade of saeldor (");
    }
    public static Result evaluate(Map<Integer, OwnedGearPlanner.Gear> loadout, Inputs input)
    {
        if (input.target == null) { return Result.unavailable("Choose the actual monster variant first."); }
        if (input.target.hp <= 0 || input.target.armour != 0 || !input.target.ordinaryCombat) { return Result.unavailable("This target's special/phase mechanics are outside the modeled target subset."); }
        OwnedGearPlanner.Gear weapon = loadout.get(3); String style = input.mode.style;
        if (weapon == null) { return Result.unavailable("A supported owned weapon is needed."); }
        if (weapon.twoHanded && loadout.containsKey(5)) { return Result.unavailable("Two-handed weapon and shield cannot be combined."); }
        if (!ordinaryWeapon(weapon, style)) { return Result.unavailable("Weapon's normal attack or passive is not modeled: " + weapon.name); }
        if (!style.equals("Magic") && modes(weapon, style).stream().noneMatch(m -> m.style.equals(style) && m.stance.equals(input.mode.stance))) { return Result.unavailable("That attack type/stance is not available on this weapon."); }
        for (TaskReferenceData.Rule rule : TaskReferenceData.rules(input.task, input.location, input.exemption, OwnedGearPlanner.isMelee(style) ? "Melee" : style))
        {
            OwnedGearPlanner.Gear equipped = loadout.get(rule.slot);
            if (rule.slot >= 0 && (equipped == null || !rule.accepts(equipped.id, equipped.base))) { return Result.unavailable("Required slot is missing or incompatible: " + rule.text); }
        }
        List<String> effects = new ArrayList<>(), warnings = new ArrayList<>();
        int attackBonus = 0, strengthBonus = 0; double magicDamage = 0;
        for (Map.Entry<Integer, OwnedGearPlanner.Gear> entry : loadout.entrySet())
        {
            OwnedGearPlanner.Gear gear = entry.getValue();
            if (entry.getKey() != gear.slot) { return Result.unavailable("Item is in the wrong equipment slot."); }
            String blocked = blockedItem(gear, input); if (blocked != null) { return Result.unavailable(blocked); }
            String check = EquipmentEligibility.rejection(gear, input.account);
            if (check != null) { warnings.add(gear.name + ": " + check); }
            attackBonus += gear.accuracy(style); strengthBonus += style.equals("Ranged") ? gear.rangedStrength : gear.strength; magicDamage += gear.magicDamage;
        }
        boolean ranged = style.equals("Ranged"), magic = style.equals("Magic");
        Skill attackSkill = magic ? Skill.MAGIC : ranged ? Skill.RANGED : Skill.ATTACK;
        int level = input.level(attackSkill), strength = input.level(ranged ? Skill.RANGED : Skill.STRENGTH);
        if (level < 1 || (!magic && strength < 1)) { return Result.unavailable("Required combat levels are unknown."); }
        double attackPrayer = 1, strengthPrayer = 1, prayerMagicDamage = 0;
        if (input.prayer != PrayerBoost.NONE)
        {
            int need = input.prayer == PrayerBoost.PIETY ? 70 : input.prayer == PrayerBoost.RIGOUR ? 74 : 77;
            if (input.account.level(Skill.PRAYER) < need || input.account.level(Skill.DEFENCE) < 70) { return Result.unavailable("Selected prayer's base skill requirements are unmet/unknown."); }
            warnings.add("Prayer assumed active; unlock is not verified.");
            if (!magic && !ranged && input.prayer == PrayerBoost.PIETY) { attackPrayer = 1.20; strengthPrayer = 1.23; }
            else if (ranged && input.prayer == PrayerBoost.RIGOUR) { attackPrayer = 1.20; strengthPrayer = 1.23; }
            else if (magic && input.prayer == PrayerBoost.AUGURY) { attackPrayer = 1.25; prayerMagicDamage = .04; }
        }
        boolean fullVoid = isVoid(loadout, magic ? "mage" : ranged ? "ranger" : "melee");
        boolean eliteVoid = fullVoid && name(loadout.get(4)).startsWith("elite void") && name(loadout.get(7)).startsWith("elite void");
        String head = name(loadout.get(0)), neck = name(loadout.get(2)); boolean imbuedHead = head.contains("(i)");
        boolean helmet = head.contains("slayer helmet") || head.startsWith("black mask");
        boolean salve = input.target.attributes.contains("undead") && neck.startsWith("salve amulet");
        boolean salveImbued = neck.contains("(i)") || neck.contains("(ei)"); boolean salveEnhanced = neck.contains("(e)") || neck.contains("(ei)");
        double taskFactor = input.onTask && helmet && (!ranged && !magic || imbuedHead) && !salve ? (ranged || magic ? 1.15 : 7.0 / 6) : 1;
        double salveFactor = salve && (!ranged && !magic || salveImbued) ? (salveEnhanced ? 1.2 : magic ? 1.15 : 7.0 / 6) : 1;
        if (taskFactor > 1) { effects.add("Matching-task helmet/mask bonus (assumed on task)"); }
        if (salveFactor > 1) { effects.add("Salve undead bonus; does not stack with Slayer helmet"); }
        if (fullVoid) { effects.add(eliteVoid ? "Full elite Void set" : "Full Void set"); }
        boolean powered = magic && (name(weapon).contains("trident") || name(weapon).contains("sanguinesti"));
        int ticks = magic ? powered ? 4 : 5 : weapon.speed - (ranged && input.mode.stance.equals("Rapid") ? 1 : 0);
        if (ticks < 1 || ticks > 10) { return Result.unavailable("Weapon attack speed is unavailable."); }
        int accuracyStyle = magic ? powered ? 3 : 0 : input.mode.stance.equalsIgnoreCase("accurate") ? 3 : input.mode.stance.equalsIgnoreCase("controlled") ? 1 : 0;
        int strengthStyle = ranged ? accuracyStyle : input.mode.stance.equalsIgnoreCase("aggressive") ? 3 : input.mode.stance.equalsIgnoreCase("controlled") ? 1 : 0;
        int effectiveAttack = (int) Math.floor(level * attackPrayer) + accuracyStyle + 8;
        int effectiveStrength = magic ? 0 : (int) Math.floor(strength * strengthPrayer) + strengthStyle + 8;
        if (fullVoid)
        {
            if (!magic) { effectiveAttack = (int) Math.floor(effectiveAttack * 1.1); }
            if (!magic) { effectiveStrength = (int) Math.floor(effectiveStrength * (ranged && eliteVoid ? 1.125 : 1.1)); }
        }
        long attack = (long) effectiveAttack * (attackBonus + 64); int maximum;
        if (magic && fullVoid) { attack = (long) Math.floor(attack * 1.45); }
        long baseAccuracyRoll = attack;
        int defenceBonus = magic ? input.target.magicDefence : ranged ? rangedDefence(weapon, input.target) : style.equals("Stab") ? input.target.stab : style.equals("Slash") ? input.target.slash : input.target.crush;
        long defence = (long) ((magic ? input.target.magic : input.target.defence) + 9) * (defenceBonus + 64);
        if (magic)
        {
            int base;
            if (powered)
            {
                int needed = name(weapon).contains("swamp") ? 78 : name(weapon).contains("sanguinesti") ? 82 : 75;
                if (input.account.level(Skill.MAGIC) < needed) { return Result.unavailable("Powered staff base Magic requirement is unmet/unknown."); }
                base = level / 3 - (name(weapon).contains("swamp") ? 2 : name(weapon).contains("sanguinesti") ? 1 : 5);
                if (!input.charged) { return Result.unavailable("Powered staff charge assumption is needed."); }
                warnings.add("Powered staff charges assumed; no internal runes/charges inspected.");
            }
            else
            {
                int needed = SpellPlanning.requiredLevel(input.spell); base = SpellPlanning.baseMaxHit(input.spell, level);
                if (needed < 0 || base < 1) { return Result.unavailable("Choose a modeled standard elemental spell."); }
                if (level < needed || input.account.spellbook != 0) { return Result.unavailable("Spell level or current standard spellbook is unmet/unknown."); }
                if (name(loadout.get(9)).startsWith("chaos gauntlets") && input.spell.endsWith("Bolt")) { base += 3; effects.add("Chaos gauntlets"); }
                if (neck.contains("elemental amulet")) { return Result.unavailable("Elemental amulet stacking is not modeled."); }
            }
            double weakness = !powered && input.target.weaknessElement.equals(SpellPlanning.element(input.spell)) ? input.target.weaknessPercent / 100.0 : 0;
            maximum = (int) Math.floor(base * (1 + Math.min(1, magicDamage / 100) + prayerMagicDamage + (eliteVoid ? .05 : 0) + (salveFactor - 1))) + (int) Math.floor(base * weakness);
            maximum = (int) Math.floor(maximum * taskFactor);
            attack = (long) Math.floor(attack * (salveFactor > 1 ? salveFactor : taskFactor));
            if (weakness > 0) { attack += (long) Math.floor(baseAccuracyRoll * weakness); effects.add("Elemental weakness: " + input.target.weaknessElement); }
            if (name(loadout.get(5)).startsWith("tome of") || name(weapon).contains("smoke battlestaff")) { return Result.unavailable("Tome/smoke-staff modifier stacking is not modeled."); }
        }
        else
        {
            if (ranged)
            {
                boolean internal = name(weapon).startsWith("crystal bow") || name(weapon).startsWith("bow of faerdhinen") || weapon.category.equals("Thrown");
                OwnedGearPlanner.Gear ammo = loadout.get(13);
                if (!internal && (ammo == null || !AmmoCompatibility.matches(weapon, ammo))) { return Result.unavailable("Compatible ammunition is missing."); }
                if (ammo != null && !internal && name(ammo).contains("(e)")) { return Result.unavailable("Enchanted-bolt procs are not modeled."); }
                if (internal && ammo != null) { return Result.unavailable("Internal-ammo model requires an empty ammo slot; blessing effects are not modeled."); }
            }
            maximum = (int) Math.floor(.5 + effectiveStrength * (strengthBonus + 64) / 640.0);
            if (ranged && (name(weapon).startsWith("crystal bow") || name(weapon).startsWith("bow of faerdhinen")))
            {
                int points = (head.startsWith("crystal helm") ? 1 : 0) + (name(loadout.get(4)).startsWith("crystal body") ? 3 : 0) + (name(loadout.get(7)).startsWith("crystal legs") ? 2 : 0);
                attack = (long) Math.floor(attack * (1 + points * .05)); maximum = (int) Math.floor(maximum * (1 + points * .025));
                if (points > 0) { effects.add("Crystal armour pieces with crystal bow/Bowfa"); }
            }
            double targetFactor = salveFactor > 1 ? salveFactor : taskFactor;
            attack = (long) Math.floor(attack * targetFactor); maximum = (int) Math.floor(maximum * targetFactor);
        }
        if (attack < 0 || defence < 0 || maximum < 1) { return Result.unavailable("Negative-roll or zero-maximum edge case is not modeled."); }
        double accuracy = hitChance(attack, defence), mean = normalMean(maximum);
        warnings.add("Normal attacks only: no special attacks, poison/burn, AoE, regeneration, overkill or downtime.");
        warnings.add(input.boosted ? "Observed boosted levels; future potion decay not predicted." : "Base levels; potion boosts not assumed.");
        return new Result("", accuracy * mean / (ticks * .6), accuracy, mean, maximum, ticks, attack, defence, effects, warnings);
    }
    private static int rangedDefence(OwnedGearPlanner.Gear weapon, ReferenceCatalog.Monster target)
    { return weapon.category.equals("Crossbow") || name(weapon).contains("ballista") ? target.heavy : weapon.category.equals("Thrown") ? target.light : target.standard; }
    private static boolean isVoid(Map<Integer, OwnedGearPlanner.Gear> set, String type)
    {
        return name(set.get(0)).startsWith("void " + type + " helm") && name(set.get(9)).startsWith("void knight gloves")
            && (name(set.get(4)).startsWith("void knight top") || name(set.get(4)).startsWith("elite void top"))
            && (name(set.get(7)).startsWith("void knight robe") || name(set.get(7)).startsWith("elite void robe"));
    }
}
