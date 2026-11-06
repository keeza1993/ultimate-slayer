package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Explainable bonus shortlist, deliberately not a DPS or eligibility engine. */
public final class OwnedGearPlanner
{
    public static final class Gear
    {
        public final int id, base, slot, accuracyMelee, accuracyRanged, accuracyMagic, strength, rangedStrength;
        public final double magicDamage;
        public final boolean twoHanded;
        public final int stab, slash, crush, speed;
        public final String category;
        public final String name;
        public Gear(int id, int base, String name, int slot, boolean twoHanded, int melee, int ranged,
                    int magic, int strength, int rangedStrength, double magicDamage)
        {
            this(id, base, name, slot, twoHanded, melee, ranged, magic, strength, rangedStrength, magicDamage, melee, melee, melee);
        }
        public Gear(int id, int base, String name, int slot, boolean twoHanded, int melee, int ranged,
                    int magic, int strength, int rangedStrength, double magicDamage, int stab, int slash, int crush)
        {
            this.id = id; this.base = base; this.name = name; this.slot = slot; this.twoHanded = twoHanded;
            this.accuracyMelee = melee; this.accuracyRanged = ranged; this.accuracyMagic = magic;
            this.strength = strength; this.rangedStrength = rangedStrength; this.magicDamage = magicDamage;
            ReferenceCatalog.Equipment reference = ReferenceCatalog.get().equipment.get(id);
            this.stab = stab;
            this.slash = slash;
            this.crush = crush;
            this.speed = reference == null ? 0 : reference.speed;
            this.category = reference == null ? "" : reference.category;
        }
        public double damage(String style) { return "Magic".equals(style) ? magicDamage : "Ranged".equals(style) ? rangedStrength : strength; }
        public int accuracy(String style) { if ("Stab".equals(style)) { return stab; } if ("Slash".equals(style)) { return slash; } if ("Crush".equals(style)) { return crush; } return "Magic".equals(style) ? accuracyMagic : "Ranged".equals(style) ? accuracyRanged : accuracyMelee; }
        public String describe(String style)
        {
            return name + " - " + ("Magic".equals(style) ? magicDamage + "% magic damage" : ("Ranged".equals(style) ? rangedStrength : strength) + " strength")
                + ", " + accuracy(style) + " attack bonus" + (speed > 0 ? "; " + speed + " ticks" : "");
        }
    }

    public static final class Draft
    {
        public final Map<Integer, Gear> selected;
        public final Map<Integer, List<Gear>> alternatives;
        public final String requirement;
        public final boolean protectionMissing;
        public final List<String> omitted;
        public final Map<Integer, String> unverified;
        Draft(Map<Integer, Gear> selected, Map<Integer, List<Gear>> alternatives, String requirement, boolean protectionMissing, List<String> omitted, Map<Integer, String> unverified)
        {
            this.selected = Collections.unmodifiableMap(new HashMap<>(selected));
            this.alternatives = Collections.unmodifiableMap(new HashMap<>(alternatives));
            this.requirement = requirement;
            this.protectionMissing = protectionMissing;
            this.omitted = List.copyOf(omitted);
            this.unverified = Map.copyOf(unverified);
        }
    }

    public static Draft shortlist(List<Gear> owned, String style, String task)
    {
        return shortlist(owned, style, task, TaskReferenceData.Location.UNKNOWN, TaskReferenceData.Exemption.UNKNOWN);
    }

    public static Draft shortlist(List<Gear> owned, String style, String task, TaskReferenceData.Location location, TaskReferenceData.Exemption exemption)
    {
        return shortlist(owned, style, task, location, exemption, null);
    }

    public static Draft shortlist(List<Gear> owned, String style, String task, TaskReferenceData.Location location,
                                  TaskReferenceData.Exemption exemption, AccountContext account)
    {
        int omitted = 0;
        List<String> reasons = new ArrayList<>();
        Map<Integer, String> unverified = new HashMap<>();
        List<TaskReferenceData.Rule> rules = TaskReferenceData.rules(task, location, exemption, isMelee(style) ? "Melee" : style);
        String requirement = rules.isEmpty() ? "Task equipment coverage incomplete - check the Wiki for your monster/variant."
            : "Required-item rules applied; coverage incomplete.";
        boolean requiredShield = rules.stream().anyMatch(r -> r.slot == 5);
        Map<Integer, List<Gear>> bySlot = new HashMap<>();
        for (Gear gear : owned)
        {
            if (gear.slot < 0 || gear.slot > 13 || gear.slot == 13) { continue; } // Ammo is matched after selecting the weapon.
            String rejection = account == null ? null : EquipmentEligibility.rejection(gear, account);
            if (EquipmentEligibility.unverified(rejection)) { unverified.put(gear.id, rejection); }
            else if (rejection != null) { omitted++; if (reasons.size() < 12) { reasons.add(gear.name + ": " + rejection); } continue; }
            String name = gear.name.toLowerCase(Locale.ROOT);
            if (name.contains("uncharged") || name.contains("broken") || name.contains("inactive")) { continue; }
            boolean constrained = rules.stream().anyMatch(r -> r.slot == gear.slot);
            if (rules.stream().anyMatch(r -> r.slot == gear.slot && !r.accepts(gear.id, gear.base))) { continue; }
            if (requiredShield && gear.slot == 3 && gear.twoHanded) { continue; }
            if (!constrained && gear.damage(style) <= 0 && gear.accuracy(style) <= 0) { continue; }
            // Weapons must offer the selected attack style, rather than only a hybrid strength bonus.
            if (gear.slot == 3 && gear.accuracy(style) <= 0) { continue; }
            bySlot.computeIfAbsent(gear.slot, ignored -> new ArrayList<>()).add(gear);
        }
        Comparator<Gear> order = Comparator.<Gear>comparingDouble(g -> g.damage(style)).reversed()
            .thenComparing(Comparator.comparingInt((Gear g) -> g.accuracy(style)).reversed()).thenComparingInt(g -> g.id);
        Map<Integer, Gear> selected = new HashMap<>();
        Map<Integer, List<Gear>> alternatives = new HashMap<>();
        bySlot.forEach((slot, candidates) -> {
            candidates.sort(order);
            selected.put(slot, candidates.get(0));
            alternatives.put(slot, Collections.unmodifiableList(new ArrayList<>(candidates.subList(0, Math.min(3, candidates.size())))));
        });
        Gear weapon = selected.get(3);
        if (weapon != null && weapon.twoHanded) { selected.remove(5); alternatives.remove(5); }
        if ("Ranged".equals(style) && weapon != null)
        {
            List<Gear> ammo = new ArrayList<>();
            for (Gear candidate : owned)
            {
                if (candidate.slot != 13 || !AmmoCompatibility.matches(weapon, candidate)) { continue; }
                String rejection = account == null ? null : EquipmentEligibility.rejection(candidate, account);
                if (rejection != null && !EquipmentEligibility.unverified(rejection)) { continue; }
                if (rules.stream().anyMatch(rule -> rule.slot == 13 && !rule.accepts(candidate.id, candidate.base))) { continue; }
                if (rejection != null) { unverified.put(candidate.id, rejection); }
                ammo.add(candidate);
            }
            ammo.sort(order);
            if (!ammo.isEmpty()) { selected.put(13, ammo.get(0)); alternatives.put(13, List.copyOf(ammo.subList(0, Math.min(3, ammo.size())))); }
            requirement += " " + AmmoCompatibility.explain(weapon, !ammo.isEmpty());
        }
        return new Draft(selected, alternatives, requirement + (account == null ? "" : " " + omitted + " blocked; " + selected.values().stream().filter(g -> unverified.containsKey(g.id)).count() + " displayed choices need checks."), rules.stream().anyMatch(r -> r.slot >= 0 && !selected.containsKey(r.slot)), reasons, unverified);
    }

    public static boolean isMelee(String style) { return "Melee".equals(style) || "Stab".equals(style) || "Slash".equals(style) || "Crush".equals(style); }
}
