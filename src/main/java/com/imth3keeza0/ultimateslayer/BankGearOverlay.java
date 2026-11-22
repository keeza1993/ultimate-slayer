package com.imth3keeza0.ultimateslayer;

import java.awt.*;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Paints already-computed ranks on visible personal-bank widgets; never enumerates a container. */
public final class BankGearOverlay extends WidgetItemOverlay
{
    private final Client client;
    private final UltimateSlayerConfig config;
    private final ItemManager items;
    private volatile Map<Integer, Integer> ranks = Map.of();
    private volatile Set<Integer> savedItems = Set.of();
    @Inject public BankGearOverlay(Client client, UltimateSlayerConfig config, ItemManager items)
    { this.client = client; this.config = config; this.items = items; drawAfterLayer(InterfaceID.Bankmain.ITEMS); }
    public void setRanks(Map<Integer, Integer> next) { ranks = Map.copyOf(next); }
    public void setSavedItems(Set<Integer> next) { savedItems = Set.copyOf(next); }
    public static Color rankColor(int rank)
    { return rank == 1 ? new Color(75, 220, 115) : rank == 2 ? new Color(250, 210, 65) : new Color(240, 90, 85); }
    @Override public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (client.getGameState() != GameState.LOGGED_IN
            || widgetItem.getWidget().getId() != InterfaceID.Bankmain.ITEMS
            || items.getItemComposition(itemId).getPlaceholderTemplateId() >= 0) { return; }
        boolean personal = config.loadoutBankHighlightsEnabled() && savedItems.contains(itemId);
        Integer rank = !personal && config.bankRanksEnabled() && config.wikiGearEnabled()
            && config.ownedGearEnabled() && config.equipmentEnabled() ? ranks.get(itemId) : null;
        if (!personal && rank == null) { return; }
        Color color = personal ? new Color(75, 200, 255) : rankColor(rank);
        Rectangle bounds = widgetItem.getCanvasBounds();
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(color); g.setStroke(new BasicStroke(3));
            g.drawRoundRect(bounds.x + 1, bounds.y + 1, Math.max(1,bounds.width-2), Math.max(1,bounds.height-2), 5, 5);
            g.setColor(new Color(0,0,0,210)); g.fillOval(bounds.x + bounds.width-16, bounds.y+1, 15, 15);
            g.setColor(color); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            if (config.starBankBadges() && (personal || rank == 1))
            {
                g.fill(star(bounds.x + bounds.width - 8.5, bounds.y + 8.5, 6, 2.7));
            }
            else { g.drawString(personal ? "✓" : rank.toString(), bounds.x + bounds.width-13, bounds.y+12); }
        }
        finally { g.dispose(); }
    }

    /** Vector badge stays readable even when the OS font has no star glyph. */
    static java.awt.geom.Path2D star(double x, double y, double outer, double inner)
    {
        java.awt.geom.Path2D path = new java.awt.geom.Path2D.Double();
        for (int i = 0; i < 10; i++)
        {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double radius = i % 2 == 0 ? outer : inner;
            double px = x + Math.cos(angle) * radius, py = y + Math.sin(angle) * radius;
            if (i == 0) { path.moveTo(px, py); } else { path.lineTo(px, py); }
        }
        path.closePath();
        return path;
    }
}
