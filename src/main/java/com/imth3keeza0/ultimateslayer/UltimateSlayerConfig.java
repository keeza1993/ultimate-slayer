package com.imth3keeza0.ultimateslayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(UltimateSlayerConfig.GROUP)
public interface UltimateSlayerConfig extends Config
{
    @ConfigItem(keyName = "suppliesEnabled", name = "Supply planning", description = "Show cached supply counts and finishing-item estimates; never rescans the bank", position = 10)
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
