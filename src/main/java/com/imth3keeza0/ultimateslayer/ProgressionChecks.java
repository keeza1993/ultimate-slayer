package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/** Curated checks, not a complete access resolver. Provenance: SOURCES.md. */
public final class ProgressionChecks
{
    public static final Quest[] QUESTS = {Quest.MONKEY_MADNESS_I, Quest.BONE_VOYAGE, Quest.DRAGON_SLAYER_I, Quest.THE_FREMENNIK_EXILES,
        Quest.ELEMENTAL_WORKSHOP_I, Quest.ELEMENTAL_WORKSHOP_II, Quest.PRIEST_IN_PERIL,
        Quest.CABIN_FEVER, Quest.FAIRYTALE_II__CURE_A_QUEEN};
    private static final Map<String, Integer> SLAYER = Map.ofEntries(
        Map.entry("cave krakens", 87), Map.entry("kraken", 87), Map.entry("abyssal sire", 85),
        Map.entry("fossil island wyverns", 66), Map.entry("ancient wyverns", 82), Map.entry("crawling hands", 5), Map.entry("cave crawlers", 10), Map.entry("banshees", 15),
        Map.entry("rockslugs", 20), Map.entry("desert lizards", 22), Map.entry("lizards", 22),
        Map.entry("cockatrice", 25), Map.entry("wall beasts", 35), Map.entry("basilisks", 40),
        Map.entry("basilisk knights", 60), Map.entry("bloodveld", 50), Map.entry("turoth", 55),
        Map.entry("cave horrors", 58), Map.entry("aberrant spectres", 60), Map.entry("dust devils", 65),
        Map.entry("kurask", 70), Map.entry("skeletal wyverns", 72), Map.entry("gargoyles", 75),
        Map.entry("nechryael", 80), Map.entry("abyssal demons", 85), Map.entry("wyrms", 62),
        Map.entry("drakes", 84), Map.entry("hydras", 95), Map.entry("alchemical hydra", 95),
        Map.entry("smoke devils", 93), Map.entry("thermonuclear smoke devil", 93));
    public static List<String> describe(String task, TaskReferenceData.Location location, AccountContext account)
    {
        List<String> lines = new ArrayList<>();
        String key = TaskReferenceData.key(task);
        Integer needed = location == TaskReferenceData.Location.PRISON ? Integer.valueOf(60) : SLAYER.get(key);
        lines.add(needed == null ? "Slayer requirement not bundled for this task/variant." : account.levelStatus(Skill.SLAYER, needed));
        if (key.equals("basilisk knights") || location == TaskReferenceData.Location.PRISON)
        { lines.add(account.questStatus(Quest.THE_FREMENNIK_EXILES)); }
        if (location == TaskReferenceData.Location.TOWER) { lines.add(account.questStatus(Quest.PRIEST_IN_PERIL)); }
        if (location == TaskReferenceData.Location.HORROR) { lines.add(account.questStatus(Quest.CABIN_FEVER)); }
        if (location == TaskReferenceData.Location.FOSSIL) { lines.add(account.questStatus(Quest.BONE_VOYAGE)); }
        if (key.equals("kraken") || key.equals("cave krakens")) { lines.add(account.levelStatus(Skill.MAGIC, 50)); }
        if (key.equals("abyssal sire")) { lines.add("Abyssal demons/Sire assignment required; Wilderness assignments do not qualify."); }
        if (key.equals("kraken") || key.equals("cave krakens")) { lines.add("A matching active assignment is required; preview does not grant access."); }
        if (key.equals("alchemical hydra") || key.contains("smoke devil"))
        { lines.add("Active matching assignment required; preview does not verify task eligibility."); }
        if (key.equals("wall beasts")) { lines.add("Wear a spiny helmet or Slayer helmet to initiate combat; bring a safe light source."); }
        if (key.equals("cave crawlers") || key.equals("cave horrors")) { lines.add("Plan poison protection and healing; supplies are not a safety guarantee."); }
        return List.copyOf(lines);
    }
}
