package com.imth3keeza0.ultimateslayer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

public final class PlanningChecks
{
    public static void main(String[] args)
    {
        require(ProgressionChecks.describe(null, TaskReferenceData.Location.UNKNOWN, AccountContext.UNKNOWN).get(0).contains("not bundled"), "Logged-out/unknown task has a safe fallback");
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 99); }
        Map<Quest, Boolean> quests = new EnumMap<>(Quest.class);
        for (Quest quest : ProgressionChecks.QUESTS) { quests.put(quest, true); }
        AccountContext high = new AccountContext(levels, quests);
        levels.put(Skill.ATTACK, 1);
        require(high.level(Skill.ATTACK) == 99, "Progression is an immutable copy");
        OwnedGearPlanner.Gear scim = gear(1, "Dragon scimitar", 3, 100);
        require(EquipmentEligibility.rejection(scim, high) == null, "Known quest/level unlock permits scimitar");
        require(EquipmentEligibility.rejection(scim, AccountContext.UNKNOWN) != null, "Unknown progression never passes");
        quests.put(Quest.MONKEY_MADNESS_I, false);
        require(EquipmentEligibility.rejection(scim, new AccountContext(levels, quests)) != null, "Level and quest failures excluded");
        levels.put(Skill.ATTACK, 99);
        require(EquipmentEligibility.rejection(scim, new AccountContext(levels, quests)) != null, "Quest failure remains with high levels");
        require(EquipmentEligibility.rejection(gear(2, "Trident of the seas", 3, 150), high) != null, "Internal charge names do not prove usability");
        require(EquipmentEligibility.rejection(gear(3, "Unknown sword", 3, 200), high) != null, "Unbundled gear stays unknown");
        require(EquipmentEligibility.rejection(gear(4, "Rune scimitar", 3, 40), high) == null, "Ordinary metal weapon requirements supported");
        levels.put(Skill.MAGIC, 69);
        require(EquipmentEligibility.rejection(gear(5, "Boots of brimstone", 10, 10), new AccountContext(levels, quests)) != null,
            "Brimstone boots need all four skill requirements");
        OwnedGearPlanner.Draft draft = OwnedGearPlanner.shortlist(List.of(scim, gear(3, "Unknown sword", 3, 200)), "Melee", "Dust devils",
            TaskReferenceData.Location.CATACOMBS, TaskReferenceData.Exemption.UNKNOWN, high);
        require(draft.selected.get(3).id == 3 && draft.unverified.containsKey(3) && draft.protectionMissing, "Unknown gear remains visible and marked; missing mask remains explicit");
        OwnedGearPlanner.Draft blocked = OwnedGearPlanner.shortlist(List.of(scim), "Melee", "Unknown", TaskReferenceData.Location.UNKNOWN, TaskReferenceData.Exemption.UNKNOWN, new AccountContext(levels, quests));
        require(!blocked.selected.containsKey(3), "Known failed quest must still block gear");
        OwnedGearPlanner.Draft charge = OwnedGearPlanner.shortlist(List.of(gear(8, "Crystal sword", 3, 90)), "Melee", "Unknown", TaskReferenceData.Location.UNKNOWN, TaskReferenceData.Exemption.UNKNOWN, high);
        require(charge.selected.containsKey(3) && charge.unverified.containsKey(8), "Unverified charge states remain visible with a warning");
        OwnedGearPlanner.Draft inactive = OwnedGearPlanner.shortlist(List.of(gear(9, "Uncharged trident", 3, 90)), "Melee", "Unknown", TaskReferenceData.Location.UNKNOWN, TaskReferenceData.Exemption.UNKNOWN, high);
        require(!inactive.selected.containsKey(3), "Known inactive states stay excluded");
        require(TaskReferenceData.rules("Basilisk Knights", TaskReferenceData.Location.PRISON, TaskReferenceData.Exemption.UNKNOWN, "Melee")
            .stream().anyMatch(r -> r.slot == 5), "Knight variant retains shield constraint");
        require(TaskReferenceData.rules("Skeletal wyverns", TaskReferenceData.Location.ICE, TaskReferenceData.Exemption.UNKNOWN, "Melee")
            .stream().anyMatch(r -> r.accepts(ItemID.ELEMENTAL_SHIELD)), "Wyvern ice-breath protection supported");
        Map<Integer, Long> bank = new HashMap<>(); bank.put(ItemID.SLAYER_BAG_OF_SALT, 7L);
        bank.put(ItemID._4DOSEPRAYERRESTORE, 2L); bank.put(ItemID.SLAYER_RING_8, 2L);
        BankSnapshot snapshot = new BankSnapshot(System.currentTimeMillis(), bank,
            Map.of(ItemID.SLAYER_BAG_OF_SALT, 2L, ItemID._1DOSEPRAYERRESTORE, 3L), Map.of(ItemID.SLAYER_RING_1, 1L));
        require(TripPlanning.count(snapshot, ItemID.SLAYER_BAG_OF_SALT) == 9, "Coherent containers sum exactly once");
        String supplies = String.join(" ", TripPlanning.supplies("Rockslugs", 15, "Melee", snapshot));
        require(supplies.contains("shortfall 6") && supplies.contains("doses: 11"), "Finisher deficit and potion doses use quantities, not item types");
        require(String.join(" ", TripPlanning.supplies("Rockslugs", 0, "Melee", snapshot)).contains("live assignment"), "Preview does not invent task quantity");
        require(String.join(" ", TripPlanning.travel(TaskReferenceData.Location.TOWER, high, snapshot)).contains("17"), "Ring charges use exact functional IDs and quantities");
        require(TripPlanning.supplies("Rockslugs", 15, "Melee", null).get(0).contains("No trip quantity"), "Missing bank remains unknown");
        require(String.join(" ", ProgressionChecks.describe("Basilisk Knights", TaskReferenceData.Location.PRISON, AccountContext.UNKNOWN)).contains("unknown"), "Unknown quest/level state labelled");
        System.out.println("Progression, eligibility, charged-state, variant and cached-trip planning checks passed.");
    }
    private static OwnedGearPlanner.Gear gear(int id, String name, int slot, int bonus)
    { return new OwnedGearPlanner.Gear(id, id, name, slot, false, bonus, 0, 0, bonus, 0, 0); }
    private static void require(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
