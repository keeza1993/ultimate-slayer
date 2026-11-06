package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

/** Pure cached-item arithmetic. No Client, container access or gameplay actions. */
public final class TripPlanning
{
    private TripPlanning() {}
    public static long count(BankSnapshot snapshot, int id)
    {
        if (snapshot == null) { return 0; }
        return snapshot.bank.getOrDefault(id, 0L) + snapshot.inventory.getOrDefault(id, 0L) + snapshot.equipment.getOrDefault(id, 0L);
    }
    private static long weighted(BankSnapshot snapshot, Map<Integer, Integer> weights)
    {
        long total = 0;
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) { total += count(snapshot, entry.getKey()) * entry.getValue(); }
        return total;
    }
    private static final Map<Integer, Integer> SLAYER_RINGS = Map.of(
        ItemID.SLAYER_RING_1, 1, ItemID.SLAYER_RING_2, 2, ItemID.SLAYER_RING_3, 3, ItemID.SLAYER_RING_4, 4,
        ItemID.SLAYER_RING_5, 5, ItemID.SLAYER_RING_6, 6, ItemID.SLAYER_RING_7, 7, ItemID.SLAYER_RING_8, 8);
    private static final Map<Integer, Integer> DUEL_RINGS = Map.of(
        ItemID.RING_OF_DUELING_1, 1, ItemID.RING_OF_DUELING_2, 2, ItemID.RING_OF_DUELING_3, 3, ItemID.RING_OF_DUELING_4, 4,
        ItemID.RING_OF_DUELING_5, 5, ItemID.RING_OF_DUELING_6, 6, ItemID.RING_OF_DUELING_7, 7, ItemID.RING_OF_DUELING_8, 8);
    public static List<String> travel(TaskReferenceData.Location location, AccountContext account, BankSnapshot snapshot)
    {
        List<String> lines = new ArrayList<>();
        if (location == TaskReferenceData.Location.TOWER || location == TaskReferenceData.Location.FREMENNIK)
        {
            lines.add(snapshot == null ? "Slayer ring ownership unknown - capture a bank snapshot." :
                count(snapshot, ItemID.SLAYER_RING_ETERNAL) > 0 ? "Eternal Slayer ring observed at snapshot; withdraw before travelling." :
                "Slayer ring teleports at snapshot: " + weighted(snapshot, SLAYER_RINGS) + ". Withdraw a charged ring if available.");
        }
        if (location == TaskReferenceData.Location.SMOKE_DEVIL)
        { lines.add(snapshot == null ? "Dueling ring ownership unknown." : "Castle Wars option: " + weighted(snapshot, DUEL_RINGS) + " dueling-ring charges observed at snapshot."); }
        if (location == TaskReferenceData.Location.KARUULM || location == TaskReferenceData.Location.TOWER
            || location == TaskReferenceData.Location.FREMENNIK || location == TaskReferenceData.Location.SMOKE_DEVIL)
        {
            lines.add(Boolean.TRUE.equals(account.completed(Quest.FAIRYTALE_II__CURE_A_QUEEN)) ?
                "Fairytale II complete; fairy ring route available subject to required staff/equipment." :
                "Fairy ring access unknown: partial Fairytale II progress may suffice; completion alone is not required.");
        }
        lines.add("Snapshot availability can be stale. Area access and transport use must still be checked.");
        return List.copyOf(lines);
    }
    public static List<String> supplies(String task, int remaining, String style, BankSnapshot snapshot)
    {
        if (snapshot == null) { return List.of("Open your bank to capture supply quantities. No trip quantity can be verified yet."); }
        List<String> lines = new ArrayList<>();
        String key = task == null ? "" : task.toLowerCase(Locale.ROOT).trim();
        if (key.equals("rockslugs")) { lines.add(finisher("Bags of salt", count(snapshot, ItemID.SLAYER_BAG_OF_SALT), remaining)); }
        if (key.equals("desert lizards") || key.equals("lizards")) { lines.add(finisher("Ice coolers (desert variant)", count(snapshot, ItemID.SLAYER_ICY_WATER), remaining)); }
        long prayer = weighted(snapshot, Map.of(ItemID._1DOSEPRAYERRESTORE, 1, ItemID._2DOSEPRAYERRESTORE, 2,
            ItemID._3DOSEPRAYERRESTORE, 3, ItemID._4DOSEPRAYERRESTORE, 4));
        lines.add("Prayer potion doses: " + prayer + ". Other restores/storage are not included.");
        lines.add("Food stocks: sharks " + count(snapshot, ItemID.SHARK) + ", lobsters " + count(snapshot, ItemID.LOBSTER)
            + ", swordfish " + count(snapshot, ItemID.SWORDFISH) + ", manta rays " + count(snapshot, ItemID.MANTARAY)
            + ", cooked karambwan " + count(snapshot, ItemID.TBWT_COOKED_KARAMBWAN) + ". Other foods not counted.");
        lines.add("Super restore doses: " + weighted(snapshot, Map.of(ItemID._1DOSE2RESTORE, 1, ItemID._2DOSE2RESTORE, 2, ItemID._3DOSE2RESTORE, 3, ItemID._4DOSE2RESTORE, 4))
            + ". Plan doses around prayer usage; no invented trip duration.");
        if (key.equals("cave crawlers") || key.equals("cave horrors"))
        {
            long doses = weighted(snapshot, Map.of(ItemID._1DOSEANTIPOISON, 1, ItemID._2DOSEANTIPOISON, 2,
                ItemID._3DOSEANTIPOISON, 3, ItemID._4DOSEANTIPOISON, 4));
            lines.add("Basic antipoison doses: " + doses + ". Stronger antidotes not counted; duration not estimated.");
        }
        if ("Ranged".equals(style)) { lines.add("The draft matches supported weapon/ammo families; verify quantity and CHECK warnings. Consumption is not estimated."); }
        if ("Magic".equals(style)) { lines.add("Check spellbook, runes and weapon charges; rune pouch and internal stores are not counted."); }
        lines.add("Capture-time totals only; noted items excluded. Food/prayer needs depend on your fight and trip length; no invented per-kill estimate.");
        return List.copyOf(lines);
    }
    private static String finisher(String name, long owned, int remaining)
    {
        return name + ": " + owned + " at snapshot. " + (remaining <= 0 ? "Set a live assignment for a quantity target." :
            "One-use baseline for " + remaining + " remaining: " + remaining + "; shortfall " + Math.max(0L, remaining - owned) + ". Allow extras for wasted uses; alternatives not evaluated.");
    }
}
