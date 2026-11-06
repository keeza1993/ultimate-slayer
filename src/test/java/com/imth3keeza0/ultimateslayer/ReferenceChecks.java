package com.imth3keeza0.ultimateslayer;

import java.util.List;
import net.runelite.api.gameval.ItemID;

public final class ReferenceChecks
{
    public static void main(String[] args)
    {
        OwnedGearPlanner.Gear brimstone = gear(ItemID.BOOTS_OF_BRIMSTONE, "Boots of brimstone", 10, 0);
        OwnedGearPlanner.Gear stronger = gear(1, "Synthetic offensive boots", 10, 50);
        for (TaskReferenceData.Exemption status : new TaskReferenceData.Exemption[]{TaskReferenceData.Exemption.UNKNOWN, TaskReferenceData.Exemption.NOT_CLAIMED})
        {
            OwnedGearPlanner.Draft draft = OwnedGearPlanner.shortlist(List.of(brimstone, stronger), "Melee", "Hydras", TaskReferenceData.Location.KARUULM, status);
            require(draft.selected.get(10).id == ItemID.BOOTS_OF_BRIMSTONE, "Unknown/unclaimed elite reward must preserve protective boots");
        }
        OwnedGearPlanner.Draft exempt = OwnedGearPlanner.shortlist(List.of(brimstone, stronger), "Melee", "Hydras", TaskReferenceData.Location.KARUULM, TaskReferenceData.Exemption.REWARDS_CLAIMED);
        require(exempt.selected.get(10).id == 1, "Explicit claimed-reward exemption unlocks footwear ranking");
        OwnedGearPlanner.Draft absent = OwnedGearPlanner.shortlist(List.of(stronger), "Melee", "Hydras", TaskReferenceData.Location.KARUULM, TaskReferenceData.Exemption.UNKNOWN);
        require(!absent.selected.containsKey(10) && absent.protectionMissing, "Never replace missing protective boots with offensive boots");
        require(TaskReferenceData.rules("Wyrms", TaskReferenceData.Location.WYRMSCRAIG, TaskReferenceData.Exemption.UNKNOWN, "Melee").stream().noneMatch(r -> r.slot == 10), "Heat rule belongs to Karuulm, not every wyrm location");
        require(TaskReferenceData.rules("Dust devils", TaskReferenceData.Location.KARUULM, TaskReferenceData.Exemption.UNKNOWN, "Melee").stream().filter(r -> r.slot >= 0).count() == 2, "Multiple protected slots supported");
        require(TaskReferenceData.rules("Gargoyles", TaskReferenceData.Location.TOWER, TaskReferenceData.Exemption.UNKNOWN, "Melee").get(0).accepts(ItemID.GRANITE_HAMMER), "Finishing tool alternatives recognised");
        require(!OwnedGearPlanner.shortlist(List.of(gear(2, "Generic sword", 3, 99)), "Melee", "Turoth").selected.containsKey(3), "Leafy tasks reject incompatible melee weapons");
        require(TaskReferenceData.assignedLocation("Unmapped assigned dungeon") == TaskReferenceData.Location.UNKNOWN, "Unknown assigned area cannot silently become a different dungeon");
        require(TaskReferenceData.assignedLocation("Mount Karuulm") == TaskReferenceData.Location.KARUULM, "Client area alias resolves");
        require(TaskReferenceData.rules("Hydras", TaskReferenceData.Location.UNKNOWN, TaskReferenceData.Exemption.UNKNOWN, "Melee").stream().anyMatch(r -> r.slot == 10), "Unverified location cannot silently remove potential heat protection");
        require(TaskReferenceData.Location.KARUULM.route.contains("CIR") && TaskReferenceData.Location.SMOKE_DEVIL.route.contains("BKP"), "Bundled route codes");
        System.out.println("Required-item, diary-exemption and travel context checks passed.");
    }
    private static OwnedGearPlanner.Gear gear(int id, String name, int slot, int strength)
    { return new OwnedGearPlanner.Gear(id, id, name, slot, false, 10, 10, 10, strength, strength, 0); }
    private static void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
