package com.imth3keeza0.ultimateslayer;

import java.util.Locale;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/** Conservative curated subset; unknown requirements are never an eligibility pass. */
public final class EquipmentEligibility
{
    private EquipmentEligibility() {}
    public static boolean unverified(String reason)
    { return reason != null && (reason.startsWith("Unknown ") || reason.equals("equip requirements not bundled") || reason.equals("charges/degradation not verified") || reason.startsWith("Reference ") || reason.startsWith("Other equip")); }
    private static String levelCheck(AccountContext account, Skill skill, int needed)
    {
        int actual = account.level(skill);
        return actual < 0 ? "Unknown " + skill.getName() + " level" : actual < needed ? "Requires " + skill.getName() + " " + needed : null;
    }
    public static String rejection(OwnedGearPlanner.Gear gear, AccountContext account)
    {
        String n = gear.name.toLowerCase(Locale.ROOT);
        if (n.contains("uncharged") || n.contains("broken") || n.contains("inactive") || n.contains("(empty)"))
        { return "inactive item state"; }
        // These names do not establish ammunition, internal charge or degradation state.
        if (n.matches(".*(trident|sanguinesti|sang staff|tumeken|blowpipe|crystal|bow of faerdhinen|scythe|soulreaper|ursine|craw's|viggora|thammaron|webweaver|accursed|barrows|ahrim|dharok|guthan|karil|torag|verac).*"))
        { return "charges/degradation not verified"; }
        Skill skill = null; int level = 0; int slayer = 0; Quest quest = null;
        switch (n)
        {
            case "dragon scimitar": skill = Skill.ATTACK; level = 60; quest = Quest.MONKEY_MADNESS_I; break;
            case "twisted bow": skill = Skill.RANGED; level = 85; break;
            case "magic shortbow": case "magic shortbow (i)": skill = Skill.RANGED; level = 50; break;
            case "rune crossbow": skill = Skill.RANGED; level = 61; break;
            case "abyssal whip": skill = Skill.ATTACK; level = 70; break;
            case "abyssal bludgeon":
                String bludgeonStrength = levelCheck(account, Skill.STRENGTH, 70);
                if (bludgeonStrength != null) { return bludgeonStrength; }
                skill = Skill.ATTACK; level = 70; break;
            case "zombie axe": skill = Skill.ATTACK; level = 65; break;
            case "leaf-bladed spear": skill = Skill.ATTACK; level = 50; slayer = 55; break;
            case "leaf-bladed sword": skill = Skill.ATTACK; level = 50; slayer = 55; break;
            case "leaf-bladed battleaxe": skill = Skill.ATTACK; level = 65; slayer = 55; break;
            case "mirror shield": skill = Skill.DEFENCE; level = 20; slayer = 25; break;
            case "v's shield": skill = Skill.DEFENCE; level = 20; quest = Quest.THE_FREMENNIK_EXILES; break;
            case "facemask": slayer = 10; break;
            case "earmuffs": slayer = 15; break;
            case "nose peg": slayer = 60; break;
            case "spiny helmet": skill = Skill.DEFENCE; level = 5; break;
            case "witchwood icon": slayer = 35; break;
            case "boots of stone": slayer = 44; break;
            case "boots of brimstone": skill = Skill.DEFENCE; level = 70; slayer = 44; break;
            case "granite boots": skill = Skill.DEFENCE; level = 50; break;
            case "elemental shield": quest = Quest.ELEMENTAL_WORKSHOP_I; break;
            case "mind shield": quest = Quest.ELEMENTAL_WORKSHOP_II; break;
            case "amulet of strength": case "amulet of power": case "amulet of accuracy": break;
            default:
                java.util.regex.Matcher metal = java.util.regex.Pattern.compile("(bronze|iron|steel|black|mithril|adamant|rune) (dagger|sword|longsword|scimitar|mace|battleaxe|2h sword|med helm|full helm|chainbody|platebody|platelegs|plateskirt|sq shield|kiteshield)").matcher(n);
                if (metal.matches())
                {
                    String type = metal.group(2);
                    skill = type.matches("dagger|sword|longsword|scimitar|mace|battleaxe|2h sword") ? Skill.ATTACK : Skill.DEFENCE;
                    level = java.util.Map.of("bronze", 1, "iron", 1, "steel", 5, "black", 10, "mithril", 20, "adamant", 30, "rune", 40).get(metal.group(1));
                    if (n.equals("rune platebody")) { quest = Quest.DRAGON_SLAYER_I; }
                }
                else if (n.matches("(black |green |red |purple |turquoise |hydra |twisted |tztok |vampyric |tzkal )?slayer helmet( \\(i\\))?"))
                { skill = Skill.DEFENCE; level = 10; }
                else
                {
                    java.util.Map<String, Integer> requirements = ReferenceCatalog.get().requirements.get(gear.id);
                    if (requirements == null) { return "equip requirements not bundled"; }
                    for (java.util.Map.Entry<String, Integer> entry : requirements.entrySet())
                    {
                        Skill requiredSkill = null;
                        for (Skill candidate : Skill.values()) { if (candidate.getName().equalsIgnoreCase(entry.getKey()) || candidate.name().equalsIgnoreCase(entry.getKey())) { requiredSkill = candidate; break; } }
                        if (requiredSkill == null || account.level(requiredSkill) < 0) { return "Reference " + entry.getKey() + " " + entry.getValue() + ": current level unknown"; }
                        if (account.level(requiredSkill) < entry.getValue()) { return "Reference " + entry.getKey() + " " + entry.getValue() + " not met; confirm current requirements"; }
                    }
                    return "Other equip unlocks unverified; reference skill levels met";
                }
        }
        if (n.equals("boots of brimstone"))
        {
            String magic = levelCheck(account, Skill.MAGIC, 70), ranged = levelCheck(account, Skill.RANGED, 70);
            if (magic != null && !unverified(magic)) { return magic; }
            if (ranged != null && !unverified(ranged)) { return ranged; }
            if (magic != null || ranged != null) { return magic != null ? magic : ranged; }
        }
        if (n.equals("granite boots") && levelCheck(account, Skill.STRENGTH, 50) != null) { return levelCheck(account, Skill.STRENGTH, 50); }
        if (skill != null && levelCheck(account, skill, level) != null) { return levelCheck(account, skill, level); }
        if (slayer > 0 && levelCheck(account, Skill.SLAYER, slayer) != null) { return levelCheck(account, Skill.SLAYER, slayer); }
        if (quest != null && !Boolean.TRUE.equals(account.completed(quest))) { return (account.completed(quest) == null ? "Unknown quest completion: " : "Requires quest: ") + quest.getName(); }
        return null;
    }
}

