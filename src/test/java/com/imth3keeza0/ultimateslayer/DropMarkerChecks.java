package com.imth3keeza0.ultimateslayer;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;

/** Offline event-lifecycle and exact-boundary regression checks; no live client or game input. */
public final class DropMarkerChecks
{
    public static void main(String[] args) throws Exception
    {
        String[] names = {"Air rune", "Grimy ranarr weed", "Dragon platelegs"};
        for (String name : names)
        {
            require(DropHighlightRules.classify(name, 15_000) == DropHighlightRules.Kind.BLUE, "15k inclusive");
            require(DropHighlightRules.classify(name, 99_999) == DropHighlightRules.Kind.BLUE, "Blue upper edge");
            require(DropHighlightRules.classify(name, 100_000) == DropHighlightRules.Kind.PURPLE, "100k inclusive");
            require(DropHighlightRules.classify(name, 499_999) == DropHighlightRules.Kind.PURPLE, "Purple upper edge");
            require(DropHighlightRules.classify(name, 500_000) == DropHighlightRules.Kind.GOLD, "500k inclusive");
        }
        require(DropHighlightRules.classify("Air rune", 14_999) == DropHighlightRules.Kind.RUNE, "Low rune pink");
        require(DropHighlightRules.classify("Grimy irit leaf", 0) == DropHighlightRules.Kind.HERB, "Grimy herb");
        require(DropHighlightRules.classify("Torstol", 0) == DropHighlightRules.Kind.HERB, "Clean herb");
        for (String name : new String[]{"Rune platebody", "Rune arrow", "Pure essence", "Herb sack", "Ranarr seed", "Unfinished potion"})
        { require(DropHighlightRules.classify(name, 14_999) == null, "Not a rune/herb: " + name); }
        require(DropHighlightRules.stackValue(2_147_483_647L, 2_147_483_647L) > Integer.MAX_VALUE, "Long stack arithmetic");
        require(DropHighlightRules.stackValue(Long.MAX_VALUE, 2) == Long.MAX_VALUE, "Overflow saturates");
        require(DropHighlightRules.stackValue(-1, 5) == 0, "Unknown price");
        require(!DropHighlightRules.visibleOwnership(TileItem.OWNERSHIP_OTHER, true), "Other ownership excluded");
        require(DropHighlightRules.visibleOwnership(TileItem.OWNERSHIP_GROUP, true), "Group drops included");

        boolean[] enabled = {true}, gold = {true}, herbs = {true}, runes = {true}, ownOnly = {true};
        UltimateSlayerConfig config = new UltimateSlayerConfig() {
            public boolean dropMarkersEnabled() { return enabled[0]; }
            public boolean goldDropMarkers() { return gold[0]; }
            public boolean herbDropMarkers() { return herbs[0]; }
            public boolean runeDropMarkers() { return runes[0]; }
            public boolean ownDropMarkersOnly() { return ownOnly[0]; }
        };
        Map<String, Object> definitions = new HashMap<>();
        definitions.put("getName", "Air rune"); definitions.put("getNote", -1);
        ItemComposition rune = stub(ItemComposition.class, definitions);
        ItemComposition note = stub(ItemComposition.class, Map.of("getNote", 799, "getLinkedNoteId", 556));
        GameState[] state = {GameState.LOGGED_IN};
        Map<String, Object> tileMethods = new HashMap<>();
        tileMethods.put("getGroundItems", List.of());
        Tile tile = stub(Tile.class, tileMethods);
        Tile[][][] sceneTiles = new Tile[][][]{{{tile}}};
        Scene scene = stub(Scene.class, Map.of("getTiles", sceneTiles));
        WorldView view = stub(WorldView.class, Map.of("getScene", scene));
        Client client = stub(Client.class, Map.of("getGameState", (Supplier<GameState>) () -> state[0], "getTopLevelWorldView", view));
        DropMarkerOverlay overlay = new DropMarkerOverlay(client, config, id -> id == 557 ? note : rune, id -> id == 556 ? 10L : 0L);
        int[] quantity = {1_000}, owner = {TileItem.OWNERSHIP_SELF};
        TileItem first = item(556, quantity, owner);
        overlay.spawned(new ItemSpawned(tile, first)); overlay.rebuild();
        require(color(overlay).equals(config.runeDropColor()), "One low rune stack");
        TileItem second = item(556, new int[]{1_000}, owner);
        overlay.spawned(new ItemSpawned(tile, second)); overlay.rebuild();
        require(color(overlay).equals(config.blueDropColor()), "Same-ID stacks on one tile combine before valuation");
        overlay.despawned(new ItemDespawned(tile, second)); overlay.rebuild();
        require(color(overlay).equals(config.runeDropColor()), "Picking up a stack lowers the band");
        quantity[0] = 50_000;
        overlay.quantityChanged(new ItemQuantityChanged(first, tile, 1_000, 50_000)); overlay.rebuild();
        require(color(overlay).equals(config.goldDropColor()), "Increasing stack changes to gold at exact boundary");
        gold[0] = false; overlay.rebuild();
        require(markerCount(overlay) == 0, "Disabled gold does not fall back to pink");
        gold[0] = true; owner[0] = TileItem.OWNERSHIP_OTHER; overlay.rebuild();
        require(markerCount(overlay) == 0, "Own-drop filter");
        ownOnly[0] = false; overlay.rebuild();
        require(color(overlay).equals(config.goldDropColor()), "All-drops selection");
        overlay.despawned(new ItemDespawned(tile, first)); overlay.rebuild();
        require(markerCount(overlay) == 0, "Despawn clears the marker");
        Tile otherTile = stub(Tile.class, Map.of("getGroundItems", List.of()));
        TileItem lowA = item(556, new int[]{1_000}, new int[]{TileItem.OWNERSHIP_SELF});
        TileItem lowB = item(556, new int[]{1_000}, new int[]{TileItem.OWNERSHIP_SELF});
        overlay.spawned(new ItemSpawned(tile, lowA)); overlay.spawned(new ItemSpawned(otherTile, lowB)); overlay.rebuild();
        require(markerCount(overlay) == 2 && color(overlay).equals(config.runeDropColor()), "Separate tiles do not combine values");
        overlay.clear();
        TileItem noted = item(557, new int[]{10_000}, new int[]{TileItem.OWNERSHIP_SELF});
        overlay.spawned(new ItemSpawned(tile, noted)); overlay.rebuild();
        require(color(overlay).equals(config.purpleDropColor()), "Noted items use linked real-item GE price");
        overlay.clear(); require(markerCount(overlay) == 0, "Hop/logout clears state");
        tileMethods.put("getGroundItems", List.of(first));
        overlay.seedVisibleScene();
        require(markerCount(overlay) == 1, "Enabling mid-session seeds existing visible drops");
        enabled[0] = false; overlay.rebuild();
        require(markerCount(overlay) == 0, "Master toggle clears markers");
        overlay.spawned(new ItemSpawned(tile, noted));
        enabled[0] = true; overlay.rebuild();
        require(markerCount(overlay) == 0, "No tracking while master disabled");
        overlay.seedVisibleScene(); state[0] = GameState.LOGIN_SCREEN; overlay.rebuild();
        require(markerCount(overlay) == 0, "Logout safety");
        herbs[0] = false; runes[0] = false;
        require(DropHighlightRules.color(DropHighlightRules.Kind.HERB, config) == null, "Herb toggle");
        require(DropHighlightRules.color(DropHighlightRules.Kind.RUNE, config) == null, "Rune toggle");
        require(DropHighlightRules.color(DropHighlightRules.Kind.GOLD, config).equals(config.goldDropColor()), "Value independent of category toggles");
        require(BankGearOverlay.star(10, 10, 6, 2.7).contains(10, 10), "Vector star badge");
        System.out.println("Drop boundaries, category toggles, stack changes, ownership, noted prices and lifecycle checks passed.");
    }

    private static TileItem item(int id, int[] quantity, int[] owner)
    {
        return stub(TileItem.class, Map.of("getId", id, "getQuantity", (Supplier<Integer>) () -> quantity[0],
            "getOwnership", (Supplier<Integer>) () -> owner[0]));
    }

    private static List<?> markers(DropMarkerOverlay overlay) throws Exception
    {
        Field field = DropMarkerOverlay.class.getDeclaredField("markers"); field.setAccessible(true);
        return (List<?>) field.get(overlay);
    }
    private static int markerCount(DropMarkerOverlay overlay) throws Exception { return markers(overlay).size(); }
    private static Color color(DropMarkerOverlay overlay) throws Exception
    {
        Object marker = markers(overlay).get(0);
        Field stacks = marker.getClass().getDeclaredField("stacks"); stacks.setAccessible(true);
        Object stack = ((List<?>) stacks.get(marker)).get(0);
        Field color = stack.getClass().getDeclaredField("color"); color.setAccessible(true);
        return (Color) color.get(stack);
    }
    private static <T> T stub(Class<T> type, Map<String, Object> methods)
    {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("hashCode")) { return System.identityHashCode(proxy); }
            if (method.getName().equals("equals")) { return proxy == args[0]; }
            if (method.getName().equals("toString")) { return type.getSimpleName(); }
            Object value = methods.get(method.getName());
            if (value instanceof Supplier) { return ((Supplier<?>) value).get(); }
            if (value != null) { return value; }
            throw new AssertionError("Unexpected client access: " + type.getSimpleName() + "." + method.getName());
        }));
    }
    private static void require(boolean condition, String message)
    { if (!condition) { throw new AssertionError(message); } }
}
