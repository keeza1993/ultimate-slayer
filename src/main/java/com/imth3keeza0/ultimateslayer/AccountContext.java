package com.imth3keeza0.ultimateslayer;

import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/** Session-only immutable progression copy. Never persisted with bank items. */
public final class AccountContext
{
    public static final AccountContext UNKNOWN = new AccountContext(Map.of(), Map.of());
    public final int spellbook;
    private final Map<Skill, Integer> levels;
    private final Map<Quest, Boolean> quests;
    private final Map<Skill, Integer> boosted;
    public AccountContext(Map<Skill, Integer> levels, Map<Quest, Boolean> quests)
    { this(levels, quests, -1); }
    public AccountContext(Map<Skill, Integer> levels, Map<Quest, Boolean> quests, int spellbook)
    { this(levels, quests, spellbook, levels); }
    public AccountContext(Map<Skill, Integer> levels, Map<Quest, Boolean> quests, int spellbook, Map<Skill, Integer> boosted)
    { this.levels = Map.copyOf(levels); this.quests = Map.copyOf(quests); this.spellbook = spellbook; this.boosted = Map.copyOf(boosted); }
    public int boostedLevel(Skill skill) { return boosted.getOrDefault(skill, level(skill)); }
    public boolean sameAs(AccountContext other) { return other != null && levels.equals(other.levels) && boosted.equals(other.boosted) && quests.equals(other.quests) && spellbook == other.spellbook; }
    public int signature() { return java.util.Objects.hash(levels, quests, boosted, spellbook); }
    public int level(Skill skill) { return levels.getOrDefault(skill, -1); }
    public Boolean completed(Quest quest) { return quests.get(quest); }
    public String levelStatus(Skill skill, int needed)
    {
        int actual = level(skill);
        return skill.getName() + " " + needed + ": " + (actual < 0 ? "unknown" : actual >= needed ? "base level met (" + actual + ")" : "base level too low (" + actual + "); boosts not evaluated");
    }
    public String questStatus(Quest quest)
    { Boolean done = completed(quest); return quest.getName() + ": " + (done == null ? "unknown" : done ? "complete" : "not complete"); }
}
