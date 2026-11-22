package com.imth3keeza0.ultimateslayer;

import java.awt.Color;
import java.util.Locale;
import java.util.Set;

/** Pure, inclusive GE stack-value rules. A disabled band never falls back to a cheaper colour. */
final class DropHighlightRules
{
    enum Kind { HERB, RUNE, BLUE, PURPLE, GOLD }
    private static final Set<String> HERBS = Set.of(
        "guam", "guam leaf", "marrentill", "tarromin", "harralander", "ranarr", "ranarr weed",
        "toadflax", "irit", "irit leaf", "avantoe", "kwuarm", "snapdragon", "cadantine", "lantadyme",
        "dwarf weed", "torstol", "snake weed", "ardrigal", "sito foil", "volencia moss", "rogue's purse");

    static Kind classify(String name, long value)
    {
        if (value >= 500_000) { return Kind.GOLD; }
        if (value >= 100_000) { return Kind.PURPLE; }
        if (value >= 15_000) { return Kind.BLUE; }
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
        if (normalized.endsWith(" rune")) { return Kind.RUNE; }
        if (normalized.startsWith("grimy ")) { normalized = normalized.substring(6); }
        return HERBS.contains(normalized) ? Kind.HERB : null;
    }

    static Color color(Kind kind, UltimateSlayerConfig config)
    {
        if (kind == null || !config.dropMarkersEnabled()) { return null; }
        switch (kind)
        {
            case GOLD: return config.goldDropMarkers() ? config.goldDropColor() : null;
            case PURPLE: return config.purpleDropMarkers() ? config.purpleDropColor() : null;
            case BLUE: return config.blueDropMarkers() ? config.blueDropColor() : null;
            case RUNE: return config.runeDropMarkers() ? config.runeDropColor() : null;
            case HERB: return config.herbDropMarkers() ? config.herbDropColor() : null;
            default: return null;
        }
    }

    static long stackValue(long unitPrice, long quantity)
    {
        if (unitPrice <= 0 || quantity <= 0) { return 0; }
        return quantity > Long.MAX_VALUE / unitPrice ? Long.MAX_VALUE : unitPrice * quantity;
    }

    static boolean visibleOwnership(int ownership, boolean ownOnly)
    {
        return !ownOnly || ownership == net.runelite.api.TileItem.OWNERSHIP_SELF
            || ownership == net.runelite.api.TileItem.OWNERSHIP_GROUP;
    }
}
