package com.imth3keeza0.ultimateslayer;

import java.util.List;
import net.runelite.api.gameval.ItemID;

public final class OwnedGearChecks
{
    public static void main(String[] args)
    {
        OwnedGearPlanner.Gear oneHand = gear(1, 1, "One hand", 3, false, 30, 10);
        OwnedGearPlanner.Gear twoHand = gear(2, 2, "Two hand", 3, true, 50, 20);
        OwnedGearPlanner.Gear shield = gear(ItemID.SLAYER_MIRROR_SHIELD, ItemID.SLAYER_MIRROR_SHIELD, "Mirror shield", 5, false, 0, 0);
        OwnedGearPlanner.Draft protectedDraft = OwnedGearPlanner.shortlist(List.of(oneHand, twoHand, shield), "Melee", "Basilisks");
        require(protectedDraft.selected.get(3).id == 1, "Required shield excludes a two-handed weapon");
        require(protectedDraft.selected.get(5) == shield && !protectedDraft.protectionMissing, "Required shield retained despite zero offence");
        OwnedGearPlanner.Draft unprotected = OwnedGearPlanner.shortlist(List.of(oneHand, twoHand, shield), "Melee", "Ankou");
        require(unprotected.selected.get(3).id == 2 && !unprotected.selected.containsKey(5), "Two handed weapon clears shield");
        OwnedGearPlanner.Draft missing = OwnedGearPlanner.shortlist(List.of(oneHand, twoHand), "Melee", "Basilisks");
        require(missing.protectionMissing && missing.selected.get(3).id == 1, "Missing required protection stays explicit");
        OwnedGearPlanner.Gear face = gear(ItemID.SLAYER_FACEMASK, ItemID.SLAYER_FACEMASK, "Facemask", 0, false, 0, 0);
        OwnedGearPlanner.Gear unsafeHead = gear(3, 3, "Offensive helmet", 0, false, 50, 50);
        OwnedGearPlanner.Draft dust = OwnedGearPlanner.shortlist(List.of(face, unsafeHead, oneHand), "Melee", "Dust devils");
        require(dust.selected.get(0) == face, "Required protection beats offensive bonuses");
        OwnedGearPlanner.Gear imbued = gear(ItemID.SLAYER_HELM_I_RED, net.runelite.client.game.ItemVariationMapping.map(ItemID.SLAYER_HELM_I_RED), "Red Slayer helmet (i)", 0, false, 0, 0);
        require(!OwnedGearPlanner.shortlist(List.of(imbued), "Magic", "Smoke devils").protectionMissing, "Recoloured helmet protection preserved");
        OwnedGearPlanner.Gear uncharged = gear(4, 4, "Uncharged weapon", 3, false, 100, 100);
        require(!OwnedGearPlanner.shortlist(List.of(uncharged), "Melee", "Ankou").selected.containsKey(3), "Uncharged states excluded");
        require(!OwnedGearPlanner.shortlist(List.of(oneHand), "Ranged", "Ankou").selected.containsKey(3), "Melee-only weapon not ranked for ranged");
        require(OwnedGearPlanner.shortlist(List.of(oneHand), "Magic", "Unknown").requirement.contains("incomplete"), "Unknown task never declared safe");
        System.out.println("Owned gear constraints and task-protection checks passed.");
    }

    private static OwnedGearPlanner.Gear gear(int id, int base, String name, int slot, boolean twoHanded, int attack, int strength)
    {
        return new OwnedGearPlanner.Gear(id, base, name, slot, twoHanded, attack, 0, 0, strength, 0, 0);
    }

    private static void require(boolean condition, String message)
    {
        if (!condition) { throw new AssertionError(message); }
    }
}
