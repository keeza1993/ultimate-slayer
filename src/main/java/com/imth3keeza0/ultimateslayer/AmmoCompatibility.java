package com.imth3keeza0.ultimateslayer;

import java.util.Locale;
import java.util.Map;

/** Explicit supported weapon/ammo families. Unknown pairings never become matches. */
public final class AmmoCompatibility
{
    private AmmoCompatibility() {}
    public static boolean matches(OwnedGearPlanner.Gear weapon, OwnedGearPlanner.Gear ammo)
    {
        String w = clean(weapon.name), a = clean(ammo.name);
        if (w.startsWith("karil's crossbow")) { return a.equals("bolt rack"); }
        if (w.equals("dorgeshuun crossbow")) { return a.equals("bone bolts"); }
        if (w.contains("ballista")) { return a.contains("javelin"); }
        if (w.equals("hunters' sunlight crossbow")) { return a.equals("sunlight antler bolts") || a.equals("moonlight antler bolts"); }
        if (w.endsWith("crossbow"))
        {
            if (!a.contains("bolts") || a.contains("antler") || a.equals("bone bolts") || a.equals("bolt rack")) { return false; }
            int max = w.equals("rune crossbow") ? 7 : w.equals("adamant crossbow") ? 6 : w.equals("mithril crossbow") ? 5
                : w.equals("steel crossbow") ? 4 : w.equals("iron crossbow") ? 3 : w.equals("blurite crossbow") ? 2
                : w.equals("bronze crossbow") || w.equals("crossbow") ? 1
                : w.equals("dragon crossbow") || w.equals("armadyl crossbow") || w.equals("dragon hunter crossbow") || w.equals("zaryte crossbow") ? 8 : 0;
            int tier = boltTier(a); return tier > 0 && tier <= max;
        }
        if (!a.contains("arrow") || a.contains("ogre") || a.contains("brutal") || a.contains("ice") || a.contains("fire")) { return false; }
        int tier = a.startsWith("broad") ? 5 : metalTier(a);
        int max = 0;
        if (w.equals("shortbow") || w.equals("longbow")) { max = 2; }
        else if (w.startsWith("oak ")) { max = 3; } else if (w.startsWith("willow ")) { max = 4; }
        else if (w.startsWith("maple ")) { max = 5; } else if (w.startsWith("yew ")) { max = 6; }
        else if (w.startsWith("magic shortbow") || w.startsWith("magic longbow") || w.equals("venator bow")) { max = 7; }
        else if (w.equals("twisted bow") || w.equals("dark bow") || w.equals("3rd age bow")) { max = 8; }
        return tier > 0 && tier <= max;
    }
    private static String clean(String name) { return name.toLowerCase(Locale.ROOT).trim().replaceAll("\\(p\\+*\\)", "").trim(); }
    private static int metalTier(String name)
    {
        String[] metals = {"bronze", "iron", "steel", "mithril", "adamant", "rune", "amethyst", "dragon"};
        for (int i = 0; i < metals.length; i++) { if (name.startsWith(metals[i] + " ")) { return i + 1; } } return 0;
    }
    private static int boltTier(String name)
    {
        if (name.contains("dragon bolts")) { return 8; }
        if (name.startsWith("opal")) { return 1; } if (name.startsWith("jade")) { return 2; }
        if (name.startsWith("pearl")) { return 3; } if (name.startsWith("topaz")) { return 4; }
        if (name.startsWith("sapphire") || name.startsWith("emerald")) { return 5; }
        if (name.startsWith("ruby") || name.startsWith("diamond")) { return 6; }
        if (name.startsWith("dragonstone") || name.startsWith("onyx") || name.startsWith("broad") || name.startsWith("amethyst broad")) { return 7; }
        String[] metals = {"bronze", "blurite", "iron", "steel", "mithril", "adamant", "runite", "dragon"};
        for (int i = 0; i < metals.length; i++) { if (name.startsWith(metals[i] + " ")) { return i + 1; } }
        return name.startsWith("rune ") ? 7 : 0;
    }
    public static String explain(OwnedGearPlanner.Gear weapon, boolean found)
    {
        String n = clean(weapon.name);
        if (n.contains("blowpipe")) { return "Blowpipe darts/scales are internal: inspect charges manually."; }
        if (n.contains("crystal bow") || n.contains("faerdhinen") || weapon.category.equals("Thrown") || weapon.category.equals("Chinchompas"))
        { return "No separate arrows/bolts suggested; verify ammunition or internal charges."; }
        return found ? "Compatible owned ammo selected; quantities and special effects are not optimised." : "No supported compatible ammo observed. Check weapon/ammunition before leaving.";
    }
}
