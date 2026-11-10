package com.imth3keeza0.ultimateslayer;

import java.awt.*;
import java.util.Map;
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
    @Inject public BankGearOverlay(Client client, UltimateSlayerConfig config, ItemManager items)
    { this.client = client; this.config = config; this.items = items; drawAfterLayer(InterfaceID.Bankmain.ITEMS); }
    public void setRanks(Map<Integer, Integer> next) { ranks = Map.copyOf(next); }
    public static Color rankColor(int rank)
    { return rank == 1 ? new Color(75, 220, 115) : rank == 2 ? new Color(250, 210, 65) : new Color(240, 90, 85); }
    @Override public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!config.bankRanksEnabled() || !config.wikiGearEnabled() || !config.ownedGearEnabled()
            || !config.equipmentEnabled() || client.getGameState() != GameState.LOGGED_IN
            || widgetItem.getWidget().getId() != InterfaceID.Bankmain.ITEMS
            || items.getItemComposition(itemId).getPlaceholderTemplateId() >= 0) { return; }
        Integer rank = ranks.get(itemId); if (rank == null) { return; }
        Rectangle bounds = widgetItem.getCanvasBounds();
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(rankColor(rank)); g.setStroke(new BasicStroke(2));
            g.drawRoundRect(bounds.x + 1, bounds.y + 1, Math.max(1,bounds.width-2), Math.max(1,bounds.height-2), 5, 5);
            g.setColor(new Color(0,0,0,210)); g.fillOval(bounds.x + bounds.width-14, bounds.y+1, 13, 14);
            g.setColor(rankColor(rank)); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.drawString(rank.toString(), bounds.x + bounds.width-11, bounds.y+12);
        }
        finally { g.dispose(); }
    }
}
