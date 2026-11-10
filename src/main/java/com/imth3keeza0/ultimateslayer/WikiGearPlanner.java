package com.imth3keeza0.ultimateslayer;

import java.util.*;

/** Owned-only Wiki order, optionally refined using supported complete-loadout estimates. */
public final class WikiGearPlanner
{
    private WikiGearPlanner() {}
    public static List<OwnedGearPlanner.Gear> candidates(List<OwnedGearPlanner.Gear> owned, WikiStrategy.Method method,
        String task, TaskReferenceData.Location location, TaskReferenceData.Exemption exemption)
    {
        List<TaskReferenceData.Rule> rules = TaskReferenceData.rules(task, location, exemption, method.style);
        List<OwnedGearPlanner.Gear> result = new ArrayList<>();
        for (OwnedGearPlanner.Gear item : owned)
        { if (method.rank(item) >= 0 || rules.stream().anyMatch(r -> r.slot == item.slot && r.accepts(item.id, item.base))) { result.add(item); } }
        return List.copyOf(result);
    }
    public static OwnedGearPlanner.Draft plan(List<OwnedGearPlanner.Gear> owned, WikiStrategy.Method method,
        String task, TaskReferenceData.Location location, TaskReferenceData.Exemption exemption,
        AccountContext account, CombatCalculator.Inputs inputs)
    {
        Map<Integer, List<OwnedGearPlanner.Gear>> pool = new HashMap<>();
        Map<Integer, String> warnings = new HashMap<>(); List<String> omitted = new ArrayList<>();
        List<TaskReferenceData.Rule> rules = TaskReferenceData.rules(task, location, exemption, method.style);
        boolean needsShield = rules.stream().anyMatch(r -> r.slot == 5);
        for (OwnedGearPlanner.Gear item : owned)
        {
            String rejection = EquipmentEligibility.rejection(item, account);
            if (rejection != null && !EquipmentEligibility.unverified(rejection)) { omitted.add(item.name + ": " + rejection); continue; }
            String n = WikiStrategy.normalize(item.name);
            if (n.matches(".*(uncharged|broken|inactive|\\(empty\\)).*")) { continue; }
            if (rules.stream().anyMatch(r -> r.slot == item.slot && !r.accepts(item.id, item.base))) { continue; }
            if (item.slot == 3 && needsShield && item.twoHanded) { continue; }
            boolean protection = rules.stream().anyMatch(r -> r.slot == item.slot && r.accepts(item.id, item.base));
            if (method.rank(item) < 0 && !protection) { continue; }
            if (rejection != null) { warnings.put(item.id, rejection); }
            pool.computeIfAbsent(item.slot, ignored -> new ArrayList<>()).add(item);
        }
        Comparator<OwnedGearPlanner.Gear> wikiOrder = Comparator.comparingInt((OwnedGearPlanner.Gear g) -> Math.max(0, method.rank(g))).thenComparingInt(g -> g.id);
        pool.values().forEach(list -> list.sort(wikiOrder));
        Map<Integer, OwnedGearPlanner.Gear> selected = leaders(pool);
        // Enforce full-set dependencies instead of suggesting loose Void/crystal armour with an unrelated weapon.
        pruneDependencies(pool, selected);
        selected = leaders(pool);
        Map<Integer, List<OwnedGearPlanner.Gear>> alternatives = new HashMap<>();
        final Map<Integer, OwnedGearPlanner.Gear> initial = Map.copyOf(selected);
        for (Map.Entry<Integer, List<OwnedGearPlanner.Gear>> entry : pool.entrySet())
        {
            int slot = entry.getKey(); List<OwnedGearPlanner.Gear> candidates = new ArrayList<>(entry.getValue());
            if (slot == 13 && method.style.equals("Ranged"))
            {
                OwnedGearPlanner.Gear weapon = initial.get(3);
                candidates.removeIf(item -> weapon == null || !AmmoCompatibility.matches(weapon, item));
            }
            // A model never demotes an unmodeled Wiki option merely because it lacks a numerical estimate.
            if (inputs != null && !candidates.isEmpty() && slot != 13)
            {
                Map<Integer, Double> scores = new HashMap<>(); boolean allSupported = true;
                for (OwnedGearPlanner.Gear candidate : candidates)
                {
                    Map<Integer, OwnedGearPlanner.Gear> combination = new HashMap<>(initial);
                    combination.put(slot, candidate); completeWeapon(combination, pool, method.style);
                    double score = score(combination, inputs, method);
                    if (score < 0) { allSupported = false; break; } scores.put(candidate.id, score);
                }
                if (allSupported) { candidates.sort(Comparator.<OwnedGearPlanner.Gear>comparingDouble(g -> scores.get(g.id)).reversed().thenComparing(wikiOrder)); }
            }
            // Cosmetic copies are one option, not all three ranks.
            Set<Integer> families = new HashSet<>(); List<OwnedGearPlanner.Gear> top = new ArrayList<>();
            for (OwnedGearPlanner.Gear candidate : candidates)
            { if (families.add(candidate.base)) { top.add(candidate); if (top.size() == 3) { break; } } }
            if (!top.isEmpty()) { alternatives.put(slot, List.copyOf(top)); selected.put(slot, top.get(0)); }
            else { selected.remove(slot); }
        }
        completeWeapon(selected, pool, method.style);
        pruneDependencies(pool, selected);
        for (Map.Entry<Integer, List<OwnedGearPlanner.Gear>> entry : new ArrayList<>(alternatives.entrySet()))
        {
            List<OwnedGearPlanner.Gear> kept = new ArrayList<>();
            for (OwnedGearPlanner.Gear item : entry.getValue()) { if (pool.getOrDefault(entry.getKey(), List.of()).contains(item)) { kept.add(item); } }
            if (kept.isEmpty()) { alternatives.remove(entry.getKey()); selected.remove(entry.getKey()); }
            else { alternatives.put(entry.getKey(), List.copyOf(kept)); selected.put(entry.getKey(), kept.get(0)); }
        }
        completeWeapon(selected, pool, method.style);
        if (selected.get(3) != null && selected.get(3).twoHanded) { alternatives.remove(5); }
        // Ammo ranks must belong to the final weapon, not the initial seed.
        if (method.style.equals("Ranged"))
        {
            OwnedGearPlanner.Gear weapon = selected.get(3); List<OwnedGearPlanner.Gear> compatible = new ArrayList<>();
            for (OwnedGearPlanner.Gear ammo : pool.getOrDefault(13, List.of()))
            { if (weapon != null && AmmoCompatibility.matches(weapon, ammo) && compatible.size() < 3) { compatible.add(ammo); } }
            alternatives.remove(13); if (!compatible.isEmpty()) { alternatives.put(13, List.copyOf(compatible)); }
        }
        boolean missing = false;
        for (TaskReferenceData.Rule rule : rules) { if (rule.slot >= 0 && !selected.containsKey(rule.slot)) { missing = true; } }
        String summary = "Wiki-based owned setup: " + method.name + ". Whole-loadout DPS refines a slot only when every candidate is supported; otherwise Wiki order. Conditional/set/charge notes still need checking.";
        return new OwnedGearPlanner.Draft(selected, alternatives, summary, missing, omitted, warnings);
    }
    private static Map<Integer, OwnedGearPlanner.Gear> leaders(Map<Integer, List<OwnedGearPlanner.Gear>> pool)
    { Map<Integer, OwnedGearPlanner.Gear> result = new HashMap<>(); pool.forEach((slot, items) -> { if (!items.isEmpty()) { result.put(slot, items.get(0)); } }); return result; }
    private static void pruneDependencies(Map<Integer, List<OwnedGearPlanner.Gear>> pool, Map<Integer, OwnedGearPlanner.Gear> selected)
    {
        OwnedGearPlanner.Gear weapon = selected.get(3);
        boolean crystal = weapon != null && (WikiStrategy.normalize(weapon.name).contains("faerdhinen") || WikiStrategy.normalize(weapon.name).startsWith("crystal bow"));
        boolean fullVoid = true;
        for (int slot : new int[]{0,4,7,9})
        { if (selected.get(slot) == null || !WikiStrategy.normalize(selected.get(slot).name).contains("void ")) { fullVoid = false; } }
        final boolean preserveVoid = fullVoid;
        // Keep a complete coordinated Void template, never isolated pieces mixed with ordinary gear.
        pool.values().forEach(items -> items.removeIf(item -> {
            String n = WikiStrategy.normalize(item.name);
            boolean voidPiece = n.startsWith("void ") || n.startsWith("elite void ");
            boolean setSlot = item.slot == 0 || item.slot == 4 || item.slot == 7 || item.slot == 9;
            return setSlot && (preserveVoid ? !voidPiece : voidPiece) || (!crystal && n.matches("crystal (helm|body|legs)"));
        }));
    }
    private static void completeWeapon(Map<Integer, OwnedGearPlanner.Gear> setup, Map<Integer, List<OwnedGearPlanner.Gear>> pool, String style)
    {
        OwnedGearPlanner.Gear weapon = setup.get(3);
        if (weapon == null) { setup.remove(5); setup.remove(13); return; }
        if (weapon.twoHanded) { setup.remove(5); }
        else if (!setup.containsKey(5) && !pool.getOrDefault(5, List.of()).isEmpty()) { setup.put(5, pool.get(5).get(0)); }
        if (style.equals("Ranged"))
        {
            setup.remove(13);
            for (OwnedGearPlanner.Gear ammo : pool.getOrDefault(13, List.of()))
            { if (AmmoCompatibility.matches(weapon, ammo)) { setup.put(13, ammo); break; } }
        }
    }
    private static double score(Map<Integer, OwnedGearPlanner.Gear> setup, CombatCalculator.Inputs inputs, WikiStrategy.Method method)
    {
        double best = -1;
        String requested = method.name.toLowerCase(Locale.ROOT).contains("crush") ? "Crush"
            : method.name.toLowerCase(Locale.ROOT).contains("stab") ? "Stab" : method.style;
        for (CombatCalculator.Mode mode : CombatCalculator.modes(setup.get(3), requested))
        { CombatCalculator.Result result = CombatCalculator.evaluate(setup, inputs.withMode(mode)); if (result.available) { best = Math.max(best, result.dps); } }
        return best;
    }
    public static List<OwnedGearPlanner.Gear> specials(List<OwnedGearPlanner.Gear> owned, WikiStrategy.Method method, AccountContext account)
    {
        List<OwnedGearPlanner.Gear> result = new ArrayList<>();
        for (OwnedGearPlanner.Gear item : owned)
        {
            String rejection = EquipmentEligibility.rejection(item, account);
            if (item.slot == 3 && WikiStrategy.rankName(method.specials, item.name) >= 0 && (rejection == null || EquipmentEligibility.unverified(rejection))) { result.add(item); }
        }
        result.sort(Comparator.comparingInt(g -> WikiStrategy.rankName(method.specials, g.name)));
        return List.copyOf(result.subList(0, Math.min(3, result.size())));
    }
}
