package com.imth3keeza0.ultimateslayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntToLongFunction;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.QuantityFormatter;

/** Tracks visible ground-item events only. Never reads bank/inventory or changes Ground Items settings. */
public final class DropMarkerOverlay extends Overlay
{
    private final Client client;
    private final IntFunction<ItemComposition> definitions;
    private final IntToLongFunction prices;
    private final UltimateSlayerConfig config;
    private final Map<TileItem, ObservedDrop> observed = new IdentityHashMap<>();
    private List<Marker> markers = List.of();

    @Inject
    public DropMarkerOverlay(Client client, ItemManager items, UltimateSlayerConfig config)
    { this(client, config, items::getItemComposition, items::getItemPrice); }

    DropMarkerOverlay(Client client, UltimateSlayerConfig config,
                      IntFunction<ItemComposition> definitions, IntToLongFunction prices)
    {
        this.client = client; this.config = config; this.definitions = definitions; this.prices = prices;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    void clear() { observed.clear(); markers = List.of(); }

    /** One visible-scene seed on enable/login; subsequent updates use spawn/despawn/quantity events. */
    void seedVisibleScene()
    {
        clear();
        if (!config.dropMarkersEnabled() || client.getGameState() != GameState.LOGGED_IN) { return; }
        WorldView view = client.getTopLevelWorldView();
        if (view == null || view.getScene() == null) { return; }
        for (Tile[][] plane : view.getScene().getTiles())
        {
            for (Tile[] row : plane)
            {
                for (Tile tile : row)
                {
                    if (tile == null || tile.getGroundItems() == null) { continue; }
                    for (TileItem item : tile.getGroundItems()) { observe(tile, item); }
                }
            }
        }
        rebuild();
    }

    void spawned(ItemSpawned event)
    {
        if (config.dropMarkersEnabled()) { observe(event.getTile(), event.getItem()); }
    }

    void despawned(ItemDespawned event) { observed.remove(event.getItem()); }

    void quantityChanged(ItemQuantityChanged event)
    {
        if (event.getNewQuantity() <= 0) { observed.remove(event.getItem()); }
        else if (config.dropMarkersEnabled() && !observed.containsKey(event.getItem()))
        { observe(event.getTile(), event.getItem()); }
    }

    private void observe(Tile tile, TileItem item)
    {
        if (tile == null || item == null) { return; }
        ItemComposition definition = definitions.apply(item.getId());
        int priceId = item.getId();
        if (definition.getNote() >= 0)
        {
            priceId = definition.getLinkedNoteId();
            if (priceId <= 0) { return; }
            definition = definitions.apply(priceId);
        }
        observed.put(item, new ObservedDrop(tile, item, priceId, definition.getName()));
    }

    /** Client-thread tick snapshot: combines matching IDs on each tile before pricing the stack. */
    void rebuild()
    {
        if (!config.dropMarkersEnabled() || client.getGameState() != GameState.LOGGED_IN)
        { clear(); return; }
        Map<Tile, Map<Integer, Stack>> grouped = new IdentityHashMap<>();
        for (ObservedDrop drop : observed.values())
        {
            if (drop.item.getQuantity() <= 0 || !DropHighlightRules.visibleOwnership(
                drop.item.getOwnership(), config.ownDropMarkersOnly())) { continue; }
            Map<Integer, Stack> tile = grouped.computeIfAbsent(drop.tile, ignored -> new HashMap<>());
            Stack stack = tile.computeIfAbsent(drop.item.getId(), ignored -> new Stack(drop));
            stack.quantity += drop.item.getQuantity();
        }
        List<Marker> next = new ArrayList<>();
        for (Map.Entry<Tile, Map<Integer, Stack>> entry : grouped.entrySet())
        {
            List<Stack> highlighted = new ArrayList<>();
            for (Stack stack : entry.getValue().values())
            {
                long unitPrice = stack.drop.priceId == 995 ? 1 : prices.applyAsLong(stack.drop.priceId);
                stack.value = DropHighlightRules.stackValue(unitPrice, stack.quantity);
                stack.kind = DropHighlightRules.classify(stack.drop.name, stack.value);
                stack.color = DropHighlightRules.color(stack.kind, config);
                if (stack.color != null) { highlighted.add(stack); }
            }
            if (highlighted.isEmpty()) { continue; }
            highlighted.sort(Comparator.comparingLong((Stack stack) -> stack.value).reversed()
                .thenComparing(stack -> stack.drop.name));
            next.add(new Marker(entry.getKey(), highlighted));
        }
        markers = List.copyOf(next);
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        if (!config.dropMarkersEnabled() || client.getGameState() != GameState.LOGGED_IN) { return null; }
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setStroke(new BasicStroke(2));
            for (Marker marker : markers)
            {
                if (marker.tile.getPlane() != client.getPlane()) { continue; }
                Polygon tile = Perspective.getCanvasTilePoly(client, marker.tile.getLocalLocation());
                if (tile == null) { continue; }
                Color color = marker.stacks.get(0).color;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
                g.fillPolygon(tile);
                g.setColor(color); g.drawPolygon(tile);
                if (!config.dropMarkerLabels()) { continue; }
                int offset = 20;
                for (Stack stack : marker.stacks)
                {
                    String text = stack.drop.name + (stack.quantity > 1 ? " x " + stack.quantity : "")
                        + " (" + QuantityFormatter.quantityToStackSize(stack.value) + " gp)";
                    net.runelite.api.Point point = Perspective.getCanvasTextLocation(
                        client, g, marker.tile.getLocalLocation(), text, offset);
                    if (point != null) { OverlayUtil.renderTextLocation(g, point, text, stack.color); }
                    offset += g.getFontMetrics().getHeight();
                }
            }
        }
        finally { g.dispose(); }
        return null;
    }

    private static final class ObservedDrop
    {
        final Tile tile; final TileItem item; final int priceId; final String name;
        ObservedDrop(Tile tile, TileItem item, int priceId, String name)
        { this.tile = tile; this.item = item; this.priceId = priceId; this.name = name; }
    }

    private static final class Stack
    {
        final ObservedDrop drop; long quantity; long value; DropHighlightRules.Kind kind; Color color;
        Stack(ObservedDrop drop) { this.drop = drop; }
    }

    private static final class Marker
    {
        final Tile tile; final List<Stack> stacks;
        Marker(Tile tile, List<Stack> stacks) { this.tile = tile; this.stacks = List.copyOf(stacks); }
    }
}
