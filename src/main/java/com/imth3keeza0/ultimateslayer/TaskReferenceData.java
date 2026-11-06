package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;

/** Bundled factual requirements/routes; unknown content is never declared safe. */
public final class TaskReferenceData
{
    public enum Exemption
    {
        UNKNOWN("Unknown - keep protective boots"), REWARDS_CLAIMED("Rewards claimed"), NOT_CLAIMED("Not claimed - keep protective boots");
        private final String label;
        Exemption(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum Location
    {
        UNKNOWN("Choose / unverified location", "", "Location coverage is incomplete. Check the task Wiki page.", ""),
        KARUULM("Karuulm Slayer Dungeon", "Fairy ring CIR, then travel up Mount Karuulm. Alternatively use a Mount Karuulm teleport from Rada's blessing 3 or 4.",
            "Fairy ring access or the appropriate diary reward is required for those methods. Check your charges/daily teleports. Enter the dungeon at the summit.", "Karuulm_Slayer_Dungeon"),
        TOWER("Slayer Tower", "Slayer ring teleport to the tower; or fairy ring CKS, then travel north to the tower.",
            "Morytania access is required. Ring charges and fairy ring access are not verified. Basement/task-only and boss areas have additional restrictions.", "Slayer_Tower"),
        FREMENNIK("Fremennik Slayer Dungeon", "Slayer ring teleport to the cave; or fairy ring AJR next to the dungeon entrance.",
            "Check ring charges or fairy ring access. Each monster has its own Slayer requirement; no cannon use in this cave.", "Fremennik_Slayer_Dungeon"),
        CATACOMBS("Catacombs of Kourend", "Travel to Kourend Castle and enter through the statue. Options include Xeric's Heart or Kourend Castle Teleport.",
            "The relevant talisman teleport unlock or spell requirements must be met. Unlocks, spellbook and charges are not verified.", "Catacombs_of_Kourend"),
        SMOKE_DEVIL("Smoke Devil Dungeon", "Ring of dueling to Castle Wars, then travel south-west; or fairy ring BKP near the dungeon.",
            "Smoke devils require 93 Slayer and an appropriate active assignment. Previewing a task does not grant access or task credit. Wear smoke protection.", "Smoke_Devil_Dungeon"),
        SMOKE("Smoke Dungeon", "Travel to Pollnivneach, then find the smokey well to the west and climb down.",
            "Desert Treasure I must be progressed to the diamond search. Wear smoke protection; check transport and desert supplies for the approach.", "Smoke_Dungeon"),
        KRAKEN("Kraken Cove", "Travel to Piscatoris and enter Kraken Cove; check a suitable fairy ring or other approach.", "87 Slayer, 50 Magic and a matching assignment are needed. Bring appropriate magic supplies.", "Kraken_Cove"),
        NEXUS("Abyssal Nexus", "Use fairy ring DIP for the Abyssal Nexus if you have fairy ring access.", "Abyssal Sire needs 85 Slayer and a matching eligible assignment. Boss mechanics are not planned here.", "Abyssal_Nexus"),
        FOSSIL("Wyvern Cave", "Travel to Fossil Island after Bone Voyage; use unlocked island transport to the cave.", "Standard wyverns need 66 Slayer; ancient wyverns need 82. Bring icy-breath protection.", "Wyvern_Cave"),
        PRISON("Jormungand's Prison", "Travel from Rellekka to the Island of Stone after The Fremennik Exiles.", "Basilisk Knights need 60 Slayer, the quest and a mirror shield or V's shield.", "Jormungand%27s_Prison"),
        HORROR("Mos Le'Harmless Cave", "Travel to Mos Le'Harmless after Cabin Fever, then enter the cave.", "Bring a light source and consider poison protection; wear a witchwood icon for scream protection.", "Mos_Le%27Harmless_Cave"),
        ICE("Asgarnian Ice Dungeon", "Enter the dungeon south of Port Sarim and continue to the wyvern area.", "Skeletal wyverns need 72 Slayer. Ordinary antifire protection does not stop icy breath.", "Asgarnian_Ice_Dungeon"),
        SWAMP("Lumbridge Swamp Caves", "Enter the cave in Lumbridge Swamp with a suitable light source.", "Check gas, darkness, poison and wall-beast hazards. A covered light source is safer.", "Lumbridge_Swamp_Caves"),
        DESERT("Kharidian Desert", "Choose a desert lizard hunting area; plan your approach from Shantay Pass.", "Bring desert travel supplies. Ice coolers apply to desert lizards, not every lizard variant.", "Desert_lizard"),
        WYRMSCRAIG("Wyrmscraig Cavern", "Use the Wyrmscraig Slayer ring teleport if unlocked.",
            "Check Fallen From Grace access and the teleport unlock. Karuulm's hot-floor boots rule does not apply to this different dungeon.", "Wyrmscraig_Cavern");

        public final String label, route, access, wiki;
        Location(String label, String route, String access, String wiki)
        { this.label = label; this.route = route; this.access = access; this.wiki = wiki; }
        @Override public String toString() { return label; }
    }

    public static final class Rule
    {
        public final String text;
        public final int slot; // -1 means carry/tool check only.
        public final Set<Integer> allowed;
        public final String source = "https://oldschool.runescape.wiki/w/Slayer_equipment";
        public final String reviewedOn = "2026-11-05";
        public Rule(String text, int slot, Integer... allowed)
        { this.text = text; this.slot = slot; this.allowed = Set.of(allowed); }
        public boolean accepts(int id, int base) { return allowed.contains(id) || allowed.contains(base); }
        public boolean accepts(int id) { return accepts(id, ItemVariationMapping.map(id)); }
    }

    public static String[] tasks()
    {
        return ReferenceCatalog.get().taskNames();
    }

    static String key(String task)
    {
        String key = ReferenceCatalog.normalize(task);
        switch (key)
        {
            case "choke devil": return "dust devils";
            case "nuclear smoke devil": return "smoke devils";
            case "screaming banshee": case "twisted banshee": case "screaming twisted banshee": return "banshees";
            case "abhorrent spectre": case "deviant spectre": case "repugnant spectre": return "aberrant spectres";
            case "cockathrice": return "cockatrice";
            case "monstrous basilisk": return "basilisks";
            case "basilisk sentinel": return "basilisk knights";
            case "spiked turoth": return "turoth";
            case "king kurask": return "kurask";
            case "marble gargoyle": return "gargoyles";
            case "colossal hydra": return "hydras";
            case "shadow wyrm": return "wyrms";
            case "guardian drake": return "drakes";
            case "spitting wyvern": case "long-tailed wyvern": case "taloned wyvern": return "fossil island wyverns";
            default: break;
        }
        if (key.equals("cave kraken")) { return "cave krakens"; }
        for (String candidate : ReferenceCatalog.get().tasks.keySet())
        {
            String label = ReferenceCatalog.normalize(candidate);
            if (label.equals(key) || (label.endsWith("s") && label.substring(0, label.length() - 1).equals(key))) { return label; }
        }
        switch (key)
        {
            case "desert lizard": return "desert lizards";
            case "basilisk knight": return "basilisk knights";
            case "ancient wyvern": return "ancient wyverns";
            case "skeletal wyvern": return "skeletal wyverns";
            default: return key;
        }
    }

    public static Location[] locations(String task)
    {
        switch (key(task))
        {
            case "hydras": case "hydra": case "the alchemical hydra": case "alchemical hydra": case "drakes": return new Location[]{Location.KARUULM};
            case "wyrms": return new Location[]{Location.KARUULM, Location.WYRMSCRAIG};
            case "dust devils": return new Location[]{Location.CATACOMBS, Location.SMOKE};
            case "smoke devils": case "the thermonuclear smoke devil": case "thermonuclear smoke devil": return new Location[]{Location.SMOKE_DEVIL};
            case "cave krakens": case "kraken": return new Location[]{Location.KRAKEN};
            case "abyssal sire": case "the abyssal sire": return new Location[]{Location.NEXUS};
            case "fossil island wyverns": case "ancient wyverns": return new Location[]{Location.FOSSIL};
            case "basilisk knights": return new Location[]{Location.PRISON};
            case "cave horrors": return new Location[]{Location.HORROR};
            case "skeletal wyverns": return new Location[]{Location.ICE};
            case "wall beasts": return new Location[]{Location.SWAMP};
            case "desert lizards": case "lizards": return new Location[]{Location.DESERT};
            case "rockslugs": case "cave crawlers": return new Location[]{Location.FREMENNIK, Location.SWAMP};
            case "basilisks": return new Location[]{Location.FREMENNIK, Location.PRISON};
            case "cockatrice": case "turoth": case "kurask": return new Location[]{Location.FREMENNIK};
            case "banshees": case "aberrant spectres": case "bloodveld": case "nechryael": case "abyssal demons": return new Location[]{Location.TOWER, Location.CATACOMBS};
            case "gargoyles": case "crawling hands": return new Location[]{Location.TOWER};
            default: return new Location[]{Location.UNKNOWN};
        }
    }

    public static Location assignedLocation(String assigned)
    {
        if (assigned == null || assigned.isEmpty()) { return Location.UNKNOWN; }
        if (assigned.equalsIgnoreCase("Mount Karuulm")) { return Location.KARUULM; }
        if (assigned.equalsIgnoreCase("Fremennik Slayer Cave")) { return Location.FREMENNIK; }
        for (Location location : Location.values())
        {
            if (location != Location.UNKNOWN && (assigned.equalsIgnoreCase(location.label)
                || (location == Location.TOWER && assigned.equalsIgnoreCase("Morytania Slayer Tower")))) { return location; }
        }
        return Location.UNKNOWN;
    }

    public static List<Rule> rules(String task, Location location, Exemption exemption, String style)
    {
        List<Rule> result = new ArrayList<>();
        switch (key(task))
        {
            case "dust devils": case "smoke devils": case "the thermonuclear smoke devil": case "thermonuclear smoke devil":
                result.add(head("Wear a facemask or Slayer helmet.", ItemID.SLAYER_FACEMASK)); break;
            case "banshees": result.add(head("Wear earmuffs or a Slayer helmet.", ItemID.SLAYER_EARMUFFS)); break;
            case "aberrant spectres": result.add(head("Wear a nose peg or Slayer helmet.", ItemID.SLAYER_NOSEPEG)); break;
            case "basilisk knights": case "basilisks": case "cockatrice":
                result.add(new Rule("Equip a mirror shield or V's shield.", 5, ItemID.SLAYER_MIRROR_SHIELD, ItemID.V_SHIELD)); break;
            case "wall beasts":
                result.add(head("Wear a spiny helmet or Slayer helmet.", ItemID.WALLBEAST_SPIKE_HELMET)); break;
            case "cave horrors":
                result.add(new Rule("Witchwood icon: conservative scream protection; prayer/safespot alternatives are not evaluated.", 2, ItemID.WITCHWOOD_ICON)); break;
            case "fossil island wyverns": case "ancient wyverns": case "skeletal wyverns":
                result.add(new Rule("Equip an elemental, mind or wyvern-breath shield. Ordinary anti-dragon shields and antifire are insufficient.", 5,
                    ItemID.ELEMENTAL_SHIELD, ItemID.ELEMENTAL_MIND_SHIELD, ItemID.DRAGONFIRE_SHIELD, ItemID.DRAGONFIRE_SHIELD_UNCHARGED,
                    ItemID.DRAGONFIRE_WARD, ItemID.DRAGONFIRE_WARD_UNCHARGED, ItemID.WYVERN_SHIELD, ItemID.WYVERN_SHIELD_UNCHARGED)); break;
            case "rockslugs":
                result.add(new Rule("Carry bags of salt for finishing blows; quantities below assume no alternative finishing weapon.", -1, ItemID.SLAYER_BAG_OF_SALT)); break;
            case "desert lizards": case "lizards":
                result.add(new Rule("Desert lizards: carry ice coolers for finishing blows. Other lizard variants differ.", -1, ItemID.SLAYER_ICY_WATER)); break;
            case "gargoyles":
                result.add(new Rule("Carry a rock hammer, rock thrownhammer or granite hammer to finish standard gargoyles. Consumable quantities are not planned.", -1,
                    ItemID.SLAYER_ROCK_HAMMER, ItemID.SLAYER_ROCK_THROWNHAMMER, ItemID.GRANITE_HAMMER)); break;
            case "turoth": case "kurask":
                if (style.equals("Melee")) { result.add(new Rule("Use leaf-bladed melee weaponry.", 3, ItemID.SLAYER_LEAFBLADED_SPEAR, ItemID.LEAFBLADED_SWORD, ItemID.LEAFBLADED_BATTLEAXE)); }
                else if (style.equals("Ranged")) { result.add(new Rule("Use compatible broad arrows, broad bolts or amethyst broad bolts.", 13, 4160, 11875, 21316)); }
                else { result.add(new Rule("Magic Dart is required; its damage model is not implemented.", 3)); }
                break;
            default: break;
        }
        if (location == Location.SMOKE && result.stream().noneMatch(r -> r.slot == 0))
        { result.add(head("Wear smoke protection in the Smoke Dungeon.", ItemID.SLAYER_FACEMASK)); }
        boolean possibleHeat = location == Location.KARUULM || (location == Location.UNKNOWN && Arrays.asList(locations(task)).contains(Location.KARUULM));
        if (possibleHeat && exemption != Exemption.REWARDS_CLAIMED)
        {
            result.add(new Rule("Karuulm heat: wear boots of stone, boots of brimstone or granite boots. Elite Kourend & Kebos rewards remove this restriction.", 10,
                ItemID.BOOTS_OF_STONE, ItemID.BOOTS_OF_BRIMSTONE, ItemID.GRANITE_BOOTS));
        }
        return result;
    }

    private static Rule head(String text, int specific)
    { return new Rule(text, 0, specific, ItemID.SLAYER_HELM, ItemID.SLAYER_HELM_I); }
}
