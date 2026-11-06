package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Explicit standard elemental spell costs; other spells remain labelled references. */
public final class SpellPlanning
{
    private SpellPlanning() {}
    private static final int[][] LEVELS = {{1,5,9,13},{17,23,29,35},{41,47,53,59},{62,65,70,75},{81,85,90,95}};
    private static final int[][] AIR = {{1,1,1,2},{2,2,2,3},{3,3,3,4},{5,5,5,5},{7,7,7,7}};
    private static final int[][] ELEMENT = {{0,1,2,3},{0,2,3,4},{0,3,4,5},{0,7,7,7},{0,10,10,10}};
    private static final int[] CORE = {ItemID.MINDRUNE, ItemID.CHAOSRUNE, ItemID.DEATHRUNE, ItemID.BLOODRUNE, ItemID.WRATHRUNE};
    private static final int[] ELEMENT_IDS = {ItemID.AIRRUNE, ItemID.WATERRUNE, ItemID.EARTHRUNE, ItemID.FIRERUNE};
    public static Map<Integer, Integer> costs(String spell, String plannedWeapon)
    {
        int[] index = indices(spell); if (index == null) { return Map.of(); }
        int tier = index[0], element = index[1]; Map<Integer, Integer> costs = new LinkedHashMap<>();
        costs.put(ItemID.AIRRUNE, AIR[tier][element]);
        if (element != 0) { costs.put(ELEMENT_IDS[element], ELEMENT[tier][element]); }
        costs.put(CORE[tier], 1);
        String weapon = plannedWeapon == null ? "" : plannedWeapon.toLowerCase(Locale.ROOT);
        String[] names = {"air", "water", "earth", "fire"};
        for (int i = 0; i < names.length; i++)
        {
            if (weapon.equals("staff of " + names[i]) || weapon.equals(names[i] + " battlestaff") || weapon.equals("mystic " + names[i] + " staff")) { costs.remove(ELEMENT_IDS[i]); }
        }
        return java.util.Collections.unmodifiableMap(costs);
    }
    public static int requiredLevel(String spell) { int[] ix = indices(spell); return ix == null ? -1 : LEVELS[ix[0]][ix[1]]; }
    public static String element(String spell) { int[] ix = indices(spell); return ix == null ? "" : new String[]{"air", "water", "earth", "fire"}[ix[1]]; }
    public static int baseMaxHit(String spell, int magic)
    {
        int[] ix = indices(spell); if (ix == null || magic < LEVELS[ix[0]][ix[1]]) { return -1; }
        int highest = 0; for (int i = 0; i < 4; i++) { if (magic >= LEVELS[ix[0]][i]) { highest = i; } }
        return new int[][]{{2,4,6,8},{9,10,11,12},{13,14,15,16},{17,18,19,20},{21,22,23,24}}[ix[0]][highest];
    }
    private static int[] indices(String spell)
    {
        if (spell == null) { return null; }
        String[] parts = spell.toLowerCase(Locale.ROOT).split(" "); if (parts.length != 2) { return null; }
        int element = List.of("wind", "water", "earth", "fire").indexOf(parts[0]);
        int tier = List.of("strike", "bolt", "blast", "wave", "surge").indexOf(parts[1]);
        return element < 0 || tier < 0 ? null : new int[]{tier, element};
    }
    public static List<String> describe(String spell, AccountContext account, BankSnapshot snapshot, OwnedGearPlanner.Gear weapon)
    {
        List<String> lines = new ArrayList<>();
        if (spell == null || spell.equals("Choose spell")) { return List.of("Choose a spell to check its book and supported rune requirements."); }
        String book = ReferenceCatalog.get().spells.getOrDefault(spell, "unknown");
        String actualBook = account.spellbook == 0 ? "standard" : account.spellbook == 1 ? "ancient" : account.spellbook == 2 ? "lunar" : account.spellbook == 3 ? "arceuus" : "unknown";
        lines.add("Spellbook: " + book + "; current: " + actualBook + (actualBook.equals("unknown") ? " (not observed)" : actualBook.equals(book) ? " (matches)" : " (does not match)"));
        int[] indices = indices(spell);
        if (indices == null) { lines.add("Rune/level planning is not bundled for this spell. Check its Wiki page before using it."); return List.copyOf(lines); }
        lines.add(account.levelStatus(Skill.MAGIC, LEVELS[indices[0]][indices[1]]));
        if (weapon != null && (weapon.category.startsWith("Powered") || weapon.name.toLowerCase(Locale.ROOT).contains("trident")))
        { lines.add("Selected weapon uses a built-in attack; this manual spell plan does not describe its internal charges."); return List.copyOf(lines); }
        Map<Integer, Integer> costs = costs(spell, weapon == null ? "" : weapon.name);
        if (snapshot == null) { lines.add("Rune stocks unknown: capture your bank first."); return List.copyOf(lines); }
        long casts = Long.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : costs.entrySet())
        {
            long stock = TripPlanning.count(snapshot, entry.getKey()); casts = Math.min(casts, stock / entry.getValue());
            lines.add(runeName(entry.getKey()) + ": " + entry.getValue() + "/cast; " + stock + " at snapshot.");
        }
        lines.add("Planned casts from these loose runes: " + (casts == Long.MAX_VALUE ? 0 : casts) + ". Simple elemental staff substitutions applied to the planned weapon only.");
        lines.add("Rune-stock plan only: pouches, combo/sunfire runes, tomes and autocast compatibility not checked. See Combat comparison for supported damage estimates.");
        return List.copyOf(lines);
    }
    private static String runeName(int id)
    {
        if (id == ItemID.AIRRUNE) { return "Air"; } if (id == ItemID.WATERRUNE) { return "Water"; } if (id == ItemID.EARTHRUNE) { return "Earth"; }
        if (id == ItemID.FIRERUNE) { return "Fire"; } if (id == ItemID.MINDRUNE) { return "Mind"; } if (id == ItemID.CHAOSRUNE) { return "Chaos"; }
        if (id == ItemID.DEATHRUNE) { return "Death"; } if (id == ItemID.BLOODRUNE) { return "Blood"; } return "Wrath";
    }
}
