package com.imth3keeza0.ultimateslayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import java.awt.Color;

@ConfigGroup(UltimateSlayerConfig.GROUP)
public interface UltimateSlayerConfig extends Config
{
    @ConfigSection(name="Drop markers", description="Optional coloured ground-drop tiles alongside Ground Items", position=40)
    String dropMarkersSection = "dropMarkers";

    @ConfigItem(keyName="starBankBadges", name="Star bank badges", description="Show a star on saved setup items and rank-one Wiki bank choices; disable for the original badges", position=29)
    default boolean starBankBadges() { return true; }

    @ConfigItem(keyName="dropMarkersEnabled", name="Enable drop markers", description="Colour ground-item tiles using stack GE value first, then runes/herbs. No looting or game actions", section=dropMarkersSection, position=0)
    default boolean dropMarkersEnabled() { return true; }
    @ConfigItem(keyName="ownDropMarkersOnly", name="Only my/group drops", description="Only mark drops owned by you or your group; disable to include all visible ground items", section=dropMarkersSection, position=1)
    default boolean ownDropMarkersOnly() { return true; }
    @ConfigItem(keyName="dropMarkerLabels", name="Show marker item labels", description="Optional labels in addition to tile outlines. Leave off when using Ground Items labels", section=dropMarkersSection, position=2)
    default boolean dropMarkerLabels() { return false; }

    @ConfigItem(keyName="runeDropMarkers", name="Runes below 15k", description="Mark rune stacks below 15,000 gp; value bands take priority", section=dropMarkersSection, position=3)
    default boolean runeDropMarkers() { return true; }
    @ConfigItem(keyName="runeDropColor", name="Rune colour", description="Default pink", section=dropMarkersSection, position=4)
    default Color runeDropColor() { return new Color(255, 105, 180); }
    @ConfigItem(keyName="herbDropMarkers", name="Herbs below 15k", description="Mark clean, grimy and noted herb stacks below 15,000 gp", section=dropMarkersSection, position=5)
    default boolean herbDropMarkers() { return true; }
    @ConfigItem(keyName="herbDropColor", name="Herb colour", description="Default green", section=dropMarkersSection, position=6)
    default Color herbDropColor() { return new Color(75, 220, 115); }
    @ConfigItem(keyName="blueDropMarkers", name="15k to under 100k", description="Mark stacks worth at least 15,000 but less than 100,000 gp", section=dropMarkersSection, position=7)
    default boolean blueDropMarkers() { return true; }
    @ConfigItem(keyName="blueDropColor", name="15k band colour", description="Default blue", section=dropMarkersSection, position=8)
    default Color blueDropColor() { return new Color(75, 150, 255); }
    @ConfigItem(keyName="purpleDropMarkers", name="100k to under 500k", description="Mark stacks worth at least 100,000 but less than 500,000 gp", section=dropMarkersSection, position=9)
    default boolean purpleDropMarkers() { return true; }
    @ConfigItem(keyName="purpleDropColor", name="100k band colour", description="Default purple", section=dropMarkersSection, position=10)
    default Color purpleDropColor() { return new Color(185, 95, 255); }
    @ConfigItem(keyName="goldDropMarkers", name="500k and above", description="Mark stacks worth at least 500,000 gp", section=dropMarkersSection, position=11)
    default boolean goldDropMarkers() { return true; }
    @ConfigItem(keyName="goldDropColor", name="500k band colour", description="Default gold", section=dropMarkersSection, position=12)
    default Color goldDropColor() { return new Color(255, 215, 0); }

    @ConfigItem(keyName="protectionPriority",name="Protection priority",description="Checklist order; does not remove protection requirements",position=18)
    default SupplyPlanner.Priority protectionPriority(){return SupplyPlanner.Priority.HIGH;}
    @ConfigItem(keyName="prayerPriority",name="Prayer supply priority",description="Priority of prayer potions and super restores",position=19)
    default SupplyPlanner.Priority prayerPriority(){return SupplyPlanner.Priority.HIGH;}
    @ConfigItem(keyName="foodPriority",name="Food priority",description="Priority of healing supplies",position=20)
    default SupplyPlanner.Priority foodPriority(){return SupplyPlanner.Priority.HIGH;}
    @ConfigItem(keyName="outboundPriority",name="Outbound travel priority",description="Priority of the journey to the task",position=21)
    default SupplyPlanner.Priority outboundPriority(){return SupplyPlanner.Priority.HIGH;}
    @ConfigItem(keyName="returnPriority",name="Return teleport priority",description="Priority of the journey back",position=22)
    default SupplyPlanner.Priority returnPriority(){return SupplyPlanner.Priority.HIGH;}
    @ConfigItem(keyName="toolsPriority",name="Task tools priority",description="Priority of task tools; does not remove required tools",position=23)
    default SupplyPlanner.Priority toolsPriority(){return SupplyPlanner.Priority.HIGH;}
    @ConfigItem(keyName="extraSupplyPriority",name="Other Wiki supplies priority",description="Priority of extra observed inventory items from the selected Wiki strategy",position=24)
    default SupplyPlanner.Priority extraSupplyPriority(){return SupplyPlanner.Priority.NORMAL;}
    @ConfigItem(keyName="preferHouseTravel",name="Prefer POH travel",description="Prefer house return teleports and confirmed POH fairy-ring routes",position=25)
    default boolean preferHouseTravel(){return true;}
    @ConfigItem(keyName="houseFairyRingConfirmed",name="POH fairy ring confirmed",description="You confirm your house has a usable fairy ring. The plugin does not scan POH furniture or access",position=26)
    default boolean houseFairyRingConfirmed(){return false;}
    @ConfigItem(keyName="stafflessFairyRingsConfirmed",name="Staffless fairy rings confirmed",description="You confirm the Lumbridge elite diary staffless reward; removes only the conditional staff reminder",position=27)
    default boolean stafflessFairyRingsConfirmed(){return false;}
    @ConfigItem(keyName = "wikiGearEnabled", name = "Wiki strategy gear", description = "Use monster-specific Wiki equipment tables and your owned items; incomplete tables are labelled", position = 15)
    default boolean wikiGearEnabled() { return true; }

    @ConfigItem(keyName = "wikiOnlineEnabled", name = "Update public Wiki tables", description = "Fetch public monster/task page names only from OSRS Wiki in the background; caches for seven days. No bank or account data is sent", position = 16)
    default boolean wikiOnlineEnabled() { return true; }

    @ConfigItem(keyName = "bankRanksEnabled", name = "Bank gear ranks", description = "Mark owned Wiki choices: star/1 green, 2 yellow, 3 red. Requires an active task or explicit preview", position = 17)
    default boolean bankRanksEnabled() { return true; }

    @ConfigItem(keyName = "loadoutBankHighlightsEnabled", name = "Saved setup bank highlights", description = "Highlight your chosen task preset items in your personal bank; does not move or withdraw items", position = 28)
    default boolean loadoutBankHighlightsEnabled() { return true; }

    @ConfigItem(keyName = "suppliesEnabled", name = "Supply planning", description = "Task trip checklist with priorities, live carried counts and capture-time bank stock; never rescans the bank", position = 10)
    default boolean suppliesEnabled() { return true; }

    @ConfigItem(keyName = "monstersEnabled", name = "Monster reference", description = "Browse bundled monster variants and combat stats; no network requests", position = 11)
    default boolean monstersEnabled() { return true; }

    @ConfigItem(keyName = "spellsEnabled", name = "Spell planning", description = "Spellbook checks and loose-rune plans; no automatic spell casting", position = 12)
    default boolean spellsEnabled() { return true; }

    @ConfigItem(keyName = "combatEnabled", name = "Combat comparison", description = "Compare supported normal-attack DPS and bounded owned loadouts; no automation", position = 13)
    default boolean combatEnabled() { return true; }
    @ConfigItem(keyName = "showWelcome", name = "Show welcome", description = "Reopen the basics, clan logo and optional Discord invite", position = 14)
    default boolean showWelcome() { return false; }

    String GROUP = "ultimate-slayer";

    @ConfigItem(keyName = "bankEnabled", name = "Bank snapshots", description = "Capture once each time the personal bank opens; store IDs and quantities locally for this character", position = 6)
    default boolean bankEnabled() { return true; }

    @ConfigItem(keyName = "ownedGearEnabled", name = "Owned gear shortlist", description = "Rank saved owned equipment by offensive bonuses. Not DPS or a verified usable loadout", position = 7)
    default boolean ownedGearEnabled() { return true; }

    @ConfigItem(keyName = "taskEnabled", name = "Task information", description = "Show your assignment and remaining count directly from game task values", position = 0)
    default boolean taskEnabled() { return true; }

    @ConfigItem(keyName = "equipmentEnabled", name = "Equipment section", description = "Show currently worn gear and optional owned bonus drafts", position = 1)
    default boolean equipmentEnabled() { return true; }

    @ConfigItem(keyName = "inventoryEnabled", name = "Inventory section", description = "Show currently carried items separately from snapshot supply planning", position = 2)
    default boolean inventoryEnabled() { return true; }

    @ConfigItem(keyName = "detailsEnabled", name = "Additional information", description = "Show feature coverage and task Wiki links", position = 3)
    default boolean detailsEnabled() { return true; }

    @ConfigItem(keyName = "largeText", name = "Larger text", description = "Use larger labels in the sidebar", position = 4)
    default boolean largeText() { return false; }

    @ConfigItem(keyName = "highContrast", name = "High contrast", description = "Use stronger borders and brighter labels", position = 5)
    default boolean highContrast() { return false; }

    @ConfigItem(keyName = "requiredItemsEnabled", name = "Required items section", description = "Show task/location requirements and ownership checks. Protective draft constraints remain enforced", position = 8)
    default boolean requiredItemsEnabled() { return true; }

    @ConfigItem(keyName = "travelEnabled", name = "Travel section", description = "Show reference travel routes, access notes and location links", position = 9)
    default boolean travelEnabled() { return true; }
}


