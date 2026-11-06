package com.imth3keeza0.ultimateslayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

public final class CatalogChecks
{
    public static void main(String[] args) throws Exception
    {
        ReferenceCatalog catalog = ReferenceCatalog.get();
        require(catalog.error.isEmpty(), "Bundled data loads without falling back");
        require(catalog.equipment.size() == 5096 && catalog.monsters.size() == 2921, "Packaged dataset counts match manifest");
        require(catalog.requirements.size() == 2135 && catalog.tasks.size() == 152 && catalog.spells.size() == 61, "Requirements, task and spell resources are packaged");
        require(catalog.forTask("Dust devils").size() >= 3 && catalog.forTask("Dust devil").size() >= 3, "Task and singular monster names find multiple variants");
        require(catalog.search("dust", 2).size() == 2 && catalog.search("x", 20).isEmpty(), "Search is bounded and rejects tiny queries");
        require(catalog.forTask(null).isEmpty() && catalog.forTask("No such monster").isEmpty(), "Unknown tasks never choose a substitute monster");
        require(List.of(catalog.taskNames()).contains("Current task") && List.of(catalog.taskNames()).contains("Zulrah"), "Whole reference catalog is browsable");
        for (ReferenceCatalog.Monster monster : catalog.monsters) { require(monster.name != null && !monster.name.isEmpty(), "Every variant has an identifiable name"); }
        OwnedGearPlanner.Gear runeCrossbow = gear(9185, "Rune crossbow", 3, 90, 0, 0, 0);
        OwnedGearPlanner.Gear magicBow = gear(861, "Magic shortbow", 3, 69, 0, 0, 0);
        OwnedGearPlanner.Gear runeBolts = ammo(9144, "Runite bolts", 115), dragonBolts = ammo(21905, "Dragon bolts", 122);
        OwnedGearPlanner.Gear amethystArrows = ammo(21326, "Amethyst arrow", 55), dragonArrows = ammo(11212, "Dragon arrow", 60);
        require(AmmoCompatibility.matches(runeCrossbow, runeBolts) && !AmmoCompatibility.matches(runeCrossbow, dragonBolts), "Rune crossbow rejects dragon bolts");
        require(!AmmoCompatibility.matches(runeCrossbow, dragonArrows) && !AmmoCompatibility.matches(magicBow, runeBolts), "Arrow/bolt families cannot cross-match");
        require(!AmmoCompatibility.matches(runeCrossbow, ammo(999995, "Dragonstone dragon bolts (e)", 122))
            && AmmoCompatibility.matches(runeCrossbow, ammo(9244, "Dragonstone bolts (e)", 117)), "Gem name never disguises a dragon bolt base");
        require(AmmoCompatibility.matches(magicBow, amethystArrows) && !AmmoCompatibility.matches(magicBow, dragonArrows), "Bow tier limits are respected");
        require(!AmmoCompatibility.matches(gear(999999, "Unknown crossbow", 3, 200, 0, 0, 0), runeBolts), "Unknown weapon compatibility stays unknown");
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class); for (Skill skill : Skill.values()) { levels.put(skill, 99); }
        AccountContext account = new AccountContext(levels, Map.of(), 0);
        OwnedGearPlanner.Draft ranged = OwnedGearPlanner.shortlist(List.of(runeCrossbow, runeBolts, dragonBolts), "Ranged", "Unknown",
            TaskReferenceData.Location.UNKNOWN, TaskReferenceData.Exemption.UNKNOWN, account);
        require(ranged.selected.get(13).id == runeBolts.id, "Production draft selects compatible weaker ammo instead of incompatible stronger ammo");
        OwnedGearPlanner.Gear stab = new OwnedGearPlanner.Gear(999991,999991,"Stabbing test blade",3,false,80,0,0,50,0,0,80,10,1);
        OwnedGearPlanner.Gear crush = new OwnedGearPlanner.Gear(999992,999992,"Crushing test blade",3,false,80,0,0,50,0,0,1,10,80);
        require(OwnedGearPlanner.shortlist(List.of(stab, crush), "Stab", "Unknown").selected.get(3) == stab
            && OwnedGearPlanner.shortlist(List.of(stab, crush), "Crush", "Unknown").selected.get(3) == crush, "Exact live subtype bonuses change selection");
        require(SpellPlanning.costs("Fire Surge", "").get(ItemID.FIRERUNE) == 10 && SpellPlanning.costs("Fire Surge", "").get(ItemID.AIRRUNE) == 7, "Surge rune costs are correct");
        require(!SpellPlanning.costs("Fire Surge", "Fire battlestaff").containsKey(ItemID.FIRERUNE)
            && SpellPlanning.costs("Fire Surge", "Fire battlestaff").containsKey(ItemID.WRATHRUNE), "Staff substitution removes only the supplied elemental rune");
        require(SpellPlanning.costs("Ice Barrage", "").isEmpty(), "Unsupported rune recipes are not invented");
        BankSnapshot snapshot = new BankSnapshot(System.currentTimeMillis(), Map.of(ItemID.AIRRUNE, 70L, ItemID.FIRERUNE, 100L, ItemID.WRATHRUNE, 5L), Map.of(), Map.of());
        require(String.join(" ", SpellPlanning.describe("Fire Surge", account, snapshot, null)).contains("loose runes: 5"), "Limited core rune determines casts");
        require(String.join(" ", SpellPlanning.describe("Fire Surge", new AccountContext(levels, Map.of(), 1), snapshot, null)).contains("does not match"), "Wrong spellbook is explicit");
        require(SpellPlanning.describe("Fire Surge", AccountContext.UNKNOWN, null, null).stream().anyMatch(s -> s.contains("unknown")), "Unknown progression/ownership is not zero certainty");
        verifyIoOrdering();
        System.out.println("Catalog, exact subtype, compatible ammo, spell/rune and cross-controller IO ordering checks passed.");
    }
    private static void verifyIoOrdering() throws Exception
    {
        Path dir = Files.createTempDirectory("ultimate-slayer-ordering-"); Path file = dir.resolve("synthetic.snapshot");
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        require(LocalSnapshotIo.submit(() -> { try { started.countDown(); if (!release.await(5, TimeUnit.SECONDS)) { throw new AssertionError("Write release timed out"); } Files.writeString(file, "synthetic old snapshot"); } catch (Throwable e) { failure.set(e); } }), "Queue first-controller save");
        require(started.await(5, TimeUnit.SECONDS), "Save starts");
        require(LocalSnapshotIo.submit(() -> { try { Files.deleteIfExists(file); } catch (Throwable e) { failure.set(e); } }), "Queue explicit clear");
        require(LocalSnapshotIo.submit(() -> { try { if (Files.exists(file)) { throw new AssertionError("A re-enabled controller loaded before queued deletion"); } } catch (Throwable e) { failure.set(e); } finally { completed.countDown(); } }), "Queue re-enabled controller load");
        release.countDown(); require(completed.await(5, TimeUnit.SECONDS), "Queued operations complete");
        try { if (failure.get() != null) { throw new AssertionError("IO contract failed", failure.get()); } }
        finally { Files.deleteIfExists(file); Files.deleteIfExists(dir); }
    }
    private static OwnedGearPlanner.Gear gear(int id, String name, int slot, int ranged, int melee, int magic, int strength)
    { return new OwnedGearPlanner.Gear(id,id,name,slot,false,melee,ranged,magic,strength,0,0); }
    private static OwnedGearPlanner.Gear ammo(int id, String name, int strength)
    { return new OwnedGearPlanner.Gear(id,id,name,13,false,0,0,0,0,strength,0); }
    private static void require(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
