package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

/** Bounded multi-start joint-loadout search. Results never claim exhaustive optimality. */
public final class LoadoutOptimizer
{
    private static final int[] SLOTS = {0,1,2,4,5,7,9,10,12,13};
    private static final int MAX_EVALUATIONS = 40_000, MAX_STARTS = 24;
    public static final class Setup
    {
        public final Map<Integer, OwnedGearPlanner.Gear> gear;
        public final CombatCalculator.Inputs inputs;
        public final CombatCalculator.Result combat;
        Setup(Map<Integer, OwnedGearPlanner.Gear> gear, CombatCalculator.Inputs inputs, CombatCalculator.Result combat)
        { this.gear = Map.copyOf(gear); this.inputs = inputs; this.combat = combat; }
        public String title() { return inputs.mode + " · " + gear.get(3).name; }
        String identity()
        {
            StringBuilder value = new StringBuilder(inputs.mode.toString());
            gear.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> value.append(':').append(e.getKey()).append('=').append(e.getValue().id)); return value.toString();
        }
        public OwnedGearPlanner.Draft asDraft()
        {
            Map<Integer, List<OwnedGearPlanner.Gear>> alternatives = new HashMap<>(); Map<Integer, String> warnings = new HashMap<>();
            gear.forEach((slot, item) -> {
                alternatives.put(slot, List.of(item)); String check = EquipmentEligibility.rejection(item, inputs.account);
                if (check != null) { warnings.put(item.id, check); }
            });
            return new OwnedGearPlanner.Draft(gear, alternatives, "Best found modeled setup; " + combat.summary() + ". CHECK conditions still apply.", false, List.of(), warnings);
        }
    }
    public static final class SearchResult
    {
        public final List<Setup> setups;
        public final List<String> excluded;
        public final int evaluations;
        public final boolean capped;
        SearchResult(List<Setup> setups, Set<String> excluded, int evaluations)
        { this.setups = List.copyOf(setups); this.excluded = List.copyOf(excluded); this.evaluations = evaluations; capped = evaluations >= MAX_EVALUATIONS; }
    }
    private final List<OwnedGearPlanner.Gear> owned;
    private final BankSnapshot snapshot;
    private int evaluations;
    private final Set<String> excluded = new java.util.TreeSet<>();
    public LoadoutOptimizer(List<OwnedGearPlanner.Gear> owned, BankSnapshot snapshot) { this.owned = List.copyOf(owned); this.snapshot = snapshot; }
    public SearchResult search(CombatCalculator.Inputs template, String preferred, boolean compareStyles)
    {
        evaluations = 0; excluded.clear(); List<Setup> starts = new ArrayList<>();
        List<String> styles = compareStyles ? List.of("Melee", "Ranged", "Magic") : List.of(preferred);
        for (String requested : styles)
        {
            for (OwnedGearPlanner.Gear weapon : owned)
            {
                cancelled(); if (weapon.slot != 3) { continue; }
                List<CombatCalculator.Mode> modes = CombatCalculator.modes(weapon, requested);
                for (CombatCalculator.Mode mode : modes)
                {
                    CombatCalculator.Inputs inputs = template.withMode(mode);
                    Map<Integer, List<OwnedGearPlanner.Gear>> pool = pool(inputs);
                    for (String family : List.of("ordinary", "void", "crystal"))
                    {
                        Map<Integer, OwnedGearPlanner.Gear> seed = initial(weapon, pool, inputs, family);
                        if (seed == null) { continue; }
                        Setup candidate = evaluate(seed, inputs); if (candidate != null) { starts.add(candidate); }
                    }
                }
                if (evaluations >= MAX_EVALUATIONS) { break; }
            }
        }
        starts.sort(order()); List<Setup> finished = new ArrayList<>(); Set<String> used = new HashSet<>();
        for (Setup start : starts)
        {
            if (used.size() >= MAX_STARTS || evaluations >= MAX_EVALUATIONS) { break; }
            if (!used.add(start.identity())) { continue; }
            finished.add(improve(start, pool(start.inputs)));
        }
        finished.addAll(starts); finished.sort(order()); Set<String> identities = new HashSet<>(); List<Setup> result = new ArrayList<>();
        for (Setup setup : finished)
        { if (identities.add(setup.identity())) { result.add(setup); if (result.size() == 3) { break; } } }
        return new SearchResult(result, excluded.stream().limit(12).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)), evaluations);
    }
    private Map<Integer, List<OwnedGearPlanner.Gear>> pool(CombatCalculator.Inputs input)
    {
        Map<Integer, List<OwnedGearPlanner.Gear>> all = new HashMap<>();
        List<TaskReferenceData.Rule> rules = TaskReferenceData.rules(input.task, input.location, input.exemption, OwnedGearPlanner.isMelee(input.mode.style) ? "Melee" : input.mode.style);
        for (OwnedGearPlanner.Gear item : owned)
        {
            if (item.slot < 0 || item.slot > 13) { continue; }
            if (rules.stream().anyMatch(rule -> rule.slot == item.slot && !rule.accepts(item.id, item.base))) { continue; }
            String blocked = CombatCalculator.blockedItem(item, input);
            if (blocked != null) { excluded.add(blocked); continue; }
            if (item.slot == 13 && CombatCalculator.name(item).contains("(e)")) { excluded.add("Enchanted-bolt procs are not modeled."); continue; }
            all.computeIfAbsent(item.slot, ignored -> new ArrayList<>()).add(item);
        }
        Map<Integer, List<OwnedGearPlanner.Gear>> result = new HashMap<>();
        all.forEach((slot, candidates) -> {
            candidates.sort(Comparator.<OwnedGearPlanner.Gear>comparingDouble(g -> heuristic(g, input.mode.style)).reversed().thenComparingInt(g -> g.id));
            LinkedHashMap<Integer, OwnedGearPlanner.Gear> selected = new LinkedHashMap<>();
            candidates.stream().limit(slot == 3 || slot == 13 ? candidates.size() : 6).forEach(g -> selected.put(g.id, g));
            Set<String> effects = new HashSet<>();
            for (OwnedGearPlanner.Gear gear : candidates)
            {
                String tag = effectTag(gear);
                if (!tag.isEmpty() && effects.add(tag)) { selected.put(gear.id, gear); }
            }
            result.put(slot, List.copyOf(selected.values()));
        });
        return result;
    }
    private Map<Integer, OwnedGearPlanner.Gear> initial(OwnedGearPlanner.Gear weapon, Map<Integer, List<OwnedGearPlanner.Gear>> pool, CombatCalculator.Inputs inputs, String family)
    {
        if (CombatCalculator.blockedItem(weapon, inputs) != null) { return null; }
        Map<Integer, OwnedGearPlanner.Gear> result = new HashMap<>(); result.put(3, weapon);
        for (int slot : SLOTS)
        {
            if (slot == 5 && weapon.twoHanded || slot == 13 && !inputs.mode.style.equals("Ranged")) { continue; }
            for (OwnedGearPlanner.Gear item : pool.getOrDefault(slot, List.of()))
            { if (slot != 13 || needsAmmo(weapon) && AmmoCompatibility.matches(weapon, item)) { result.put(slot, item); break; } }
        }
        if (family.equals("void"))
        {
            String type = inputs.mode.style.equals("Magic") ? "mage" : inputs.mode.style.equals("Ranged") ? "ranger" : "melee";
            if (!force(result, pool, 0, "void " + type + " helm") || !force(result, pool, 9, "void knight gloves")
                || !forceVoidBody(result, pool, 4, "top") || !forceVoidBody(result, pool, 7, "robe")) { return null; }
        }
        if (family.equals("crystal"))
        {
            if (!inputs.mode.style.equals("Ranged") || !(CombatCalculator.name(weapon).startsWith("crystal bow") || CombatCalculator.name(weapon).startsWith("bow of faerdhinen"))) { return null; }
            // Required head protection is never replaced to complete a crystal set.
            force(result, pool, 0, "crystal helm"); force(result, pool, 4, "crystal body"); force(result, pool, 7, "crystal legs");
        }
        return result;
    }
    private static boolean forceVoidBody(Map<Integer, OwnedGearPlanner.Gear> set, Map<Integer, List<OwnedGearPlanner.Gear>> pool, int slot, String part)
    { return force(set, pool, slot, "elite void " + part) || force(set, pool, slot, "void knight " + part); }
    private static boolean force(Map<Integer, OwnedGearPlanner.Gear> set, Map<Integer, List<OwnedGearPlanner.Gear>> pool, int slot, String prefix)
    {
        for (OwnedGearPlanner.Gear item : pool.getOrDefault(slot, List.of()))
        { if (CombatCalculator.name(item).startsWith(prefix)) { set.put(slot, item); return true; } } return false;
    }
    private Setup improve(Setup start, Map<Integer, List<OwnedGearPlanner.Gear>> pool)
    {
        Setup best = start;
        for (int pass = 0; pass < 3; pass++)
        {
            boolean improved = false;
            for (int slot : SLOTS)
            {
                cancelled(); if (evaluations >= MAX_EVALUATIONS) { return best; }
                if (slot == 5 && best.gear.get(3).twoHanded || slot == 13 && !best.inputs.mode.style.equals("Ranged")) { continue; }
                List<OwnedGearPlanner.Gear> choices = new ArrayList<>(pool.getOrDefault(slot, List.of())); choices.add(null);
                for (OwnedGearPlanner.Gear item : choices)
                {
                    if (slot == 13 && item != null && (!needsAmmo(best.gear.get(3)) || !AmmoCompatibility.matches(best.gear.get(3), item))) { continue; }
                    Map<Integer, OwnedGearPlanner.Gear> variant = new HashMap<>(best.gear); if (item == null) { variant.remove(slot); } else { variant.put(slot, item); }
                    Setup candidate = evaluate(variant, best.inputs);
                    if (candidate != null && candidate.combat.dps > best.combat.dps + 1e-10) { best = candidate; improved = true; }
                    if (evaluations >= MAX_EVALUATIONS) { return best; }
                }
            }
            if (!improved) { break; }
        }
        return best;
    }
    private Setup evaluate(Map<Integer, OwnedGearPlanner.Gear> set, CombatCalculator.Inputs inputs)
    {
        cancelled(); if (evaluations >= MAX_EVALUATIONS) { return null; } evaluations++;
        CombatCalculator.Result result = CombatCalculator.evaluate(set, inputs);
        if (!result.available) { excluded.add(result.reason); return null; }
        OwnedGearPlanner.Gear weapon = set.get(3);
        if (inputs.mode.style.equals("Magic") && !(CombatCalculator.name(weapon).contains("trident") || CombatCalculator.name(weapon).contains("sanguinesti")))
        {
            if (snapshot == null) { excluded.add("Rune ownership unknown; capture the bank before searching magic loadouts."); return null; }
            for (Map.Entry<Integer, Integer> entry : SpellPlanning.costs(inputs.spell, weapon.name).entrySet())
            { if (TripPlanning.count(snapshot, entry.getKey()) < entry.getValue()) { excluded.add("Loose rune stock is insufficient for one modeled cast."); return null; } }
        }
        return new Setup(set, inputs, result);
    }
    static boolean needsAmmo(OwnedGearPlanner.Gear weapon)
    { return !(CombatCalculator.name(weapon).startsWith("crystal bow") || CombatCalculator.name(weapon).startsWith("bow of faerdhinen") || weapon.category.equals("Thrown")); }
    private static double heuristic(OwnedGearPlanner.Gear gear, String style) { return gear.damage(style) * 4 + gear.accuracy(style); }
    private static String effectTag(OwnedGearPlanner.Gear item)
    {
        String name = CombatCalculator.name(item);
        if (name.contains("slayer helmet") || name.startsWith("black mask")) { return "task" + name.contains("(i)"); }
        if (name.startsWith("salve amulet")) { return name; }
        for (String tag : List.of("void melee helm","void ranger helm","void mage helm","void knight gloves","void knight top","void knight robe","elite void top","elite void robe","crystal helm","crystal body","crystal legs"))
        { if (name.startsWith(tag)) { return tag; } } return "";
    }
    private static Comparator<Setup> order() { return Comparator.<Setup>comparingDouble(s -> s.combat.dps).reversed().thenComparing(Setup::identity); }
    private static void cancelled() { if (Thread.currentThread().isInterrupted()) { throw new CancellationException(); } }
}
