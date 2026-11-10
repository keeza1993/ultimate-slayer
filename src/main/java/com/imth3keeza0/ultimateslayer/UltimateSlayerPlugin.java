package com.imth3keeza0.ultimateslayer;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.StatChanged;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.DBTableID;
import java.util.List;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.api.ItemComposition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(name = "Ultimate Slayer", internalName = "ultimate-slayer", description = "Compact modular Slayer information sidebar", tags = {"slayer", "equipment", "inventory"})
public final class UltimateSlayerPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private ItemManager itemManager;
    @Inject private UltimateSlayerConfig config;
    @Inject private ConfigManager configManager;
    @Inject private net.runelite.client.ui.overlay.OverlayManager overlayManager;
    @Inject private BankGearOverlay bankGearOverlay;
    private WikiStrategyService wikiService;
    private volatile BankSnapshotController bank;
    private volatile UltimateSlayerPanel panel;
    private NavigationButton navigation;
    private javax.swing.JDialog welcomeDialog;
    private volatile long generation;
    private volatile boolean active;
    private boolean refreshQueued;
    private final Map<Skill, Integer> observedBoosts = new java.util.EnumMap<>(Skill.class);
    private final Map<Skill, Integer> observedLevels = new java.util.EnumMap<>(Skill.class);
    private volatile long catalogRevision;
    private volatile OwnedGearCatalog ownedCatalog;
    private volatile BankSnapshot planningSnapshot;

    @Provides
    UltimateSlayerConfig provideConfig(ConfigManager manager) { return manager.getConfig(UltimateSlayerConfig.class); }

    @Override
    protected void startUp()
    {
        ReferenceCatalog.get(); // Load bundled data once, before constructing the sidebar.
        active = true;
        wikiService = new WikiStrategyService(this::getPluginDirectory);
        overlayManager.add(bankGearOverlay);
        ++generation;
        if (config.bankEnabled()) { startBank(); }
        SwingUtilities.invokeLater(() -> {
            if (!active) { return; }
            panel = new UltimateSlayerPanel(config, this::manualRefresh);
            panel.setRankListener(bankGearOverlay::setRanks);
            panel.setWikiService(wikiService);
            panel.setInfoAction(() -> showWelcome(true));
            panel.setBankStatus(bankMessage, this::clearBank);
            panel.setOwnedGear(ownedCatalog);
            panel.setPlanningSnapshot(planningSnapshot);
            navigation = NavigationButton.builder().tooltip("Ultimate Slayer").icon(createIcon()).priority(6).panel(panel).build();
            toolbar.addNavigation(navigation);
            requestRefresh();
            showWelcome(false);
        });
    }

    @Override
    protected void shutDown()
    {
        active = false;
        bankGearOverlay.setRanks(Map.of()); overlayManager.remove(bankGearOverlay);
        if (wikiService != null) { wikiService.close(); wikiService = null; }
        ++catalogRevision;
        ownedCatalog = null;
        planningSnapshot = null;
        if (bank != null) { bank.stop(); bank = null; }
        ++generation;
        SwingUtilities.invokeLater(() -> {
            if (navigation != null) { toolbar.removeNavigation(navigation); }
            navigation = null;
            if (welcomeDialog != null) { welcomeDialog.dispose(); welcomeDialog = null; }
            if (panel != null) { panel.disposePanel(); }
            panel = null;
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        // Clear immediately on account transitions, including connection loss/hops.
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            bankGearOverlay.setRanks(Map.of());
            final long token = ++generation;
            if (bank != null) { bank.accountTransition(); }
            SwingUtilities.invokeLater(() -> {
                if (token == generation && panel != null) { panel.update(SidebarState.empty(), new HashMap<>()); }
            });
        }
        else { if (bank != null) { bank.loggedIn(); } requestRefresh(); }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (((config.equipmentEnabled() || config.requiredItemsEnabled()) && event.getContainerId() == InventoryID.EQUIPMENT.getId())
            || ((config.inventoryEnabled() || config.requiredItemsEnabled()) && event.getContainerId() == InventoryID.INVENTORY.getId()))
        {
            requestRefresh();
        }
        // Bank capture uses the bank interface's build-complete event, not container polling.
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Integer previous = observedLevels.put(event.getSkill(), event.getLevel());
        Integer previousBoost = observedBoosts.put(event.getSkill(), event.getBoostedLevel());
        boolean damageSkill = event.getSkill() == Skill.ATTACK || event.getSkill() == Skill.STRENGTH || event.getSkill() == Skill.RANGED || event.getSkill() == Skill.MAGIC;
        if (previous != null && previous == event.getLevel() && (!damageSkill || previousBoost != null && previousBoost == event.getBoostedLevel())) { return; } // XP-only changes do not rebuild the sidebar.
        if (config.equipmentEnabled() || config.requiredItemsEnabled() || config.spellsEnabled()) { requestRefresh(); }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        int varp = event.getVarpId();
        int varbit = event.getVarbitId();
        if ((config.equipmentEnabled() || config.requiredItemsEnabled() || config.travelEnabled() || config.spellsEnabled()) &&
            (varp == VarPlayerID.DRAGONQUEST || varp == VarPlayerID.FEVER_QUEST || varp == VarPlayerID.PRIESTPERIL || varp == VarPlayerID.ELEMENTAL_WORKSHOP_BITS
            || varp == VarPlayerID.ELEMENTAL_QUEST_2_BITS || varp == VarPlayerID.QUEST_VIKINGEXILE
            || varbit == VarbitID.SPELLBOOK || varbit == VarbitID.FAIRY2_QUEENCURE_QUEST || varbit == VarbitID.ELEMENTAL_QUEST_2_MAIN)) { requestRefresh(); }
        if (config.requiredItemsEnabled() && varbit == VarbitID.KOUREND_DIARY_ELITE_COMPLETE) { requestRefresh(); }
        if (config.taskEnabled() && (varp == VarPlayerID.SLAYER_COUNT
            || varp == VarPlayerID.SLAYER_TARGET || varp == VarPlayerID.SLAYER_AREA
            || varp == VarPlayerID.SLAYER_COUNT_ORIGINAL || varbit == VarbitID.SLAYER_TARGET_BOSSID
            || varbit == VarbitID.SLAYER_MODIFIER_ID || varbit == VarbitID.SLAYER_MODIFIER_VALUE
            || varbit == VarbitID.SLAYER_MODIFIER_NEGATIVE)) { requestRefresh(); }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (UltimateSlayerConfig.GROUP.equals(event.getGroup()))
        {
            if ("combatEnabled".equals(event.getKey())) { SwingUtilities.invokeLater(() -> { if (panel != null) { panel.combatSettingsChanged(); } }); }
            if ("showWelcome".equals(event.getKey()) && "true".equals(event.getNewValue()))
            { configManager.setConfiguration(UltimateSlayerConfig.GROUP, "showWelcome", false); showWelcome(true); }
            if ("bankEnabled".equals(event.getKey()))
            {
                if (bank != null) { bank.stop(); bank = null; }
                if (active && config.bankEnabled()) { startBank(); }
            }
            if ("ownedGearEnabled".equals(event.getKey()) || "equipmentEnabled".equals(event.getKey()))
            {
                rebuildOwnedCatalog(bank == null ? null : bank.getSnapshot());
            }
            requestRefresh();
        }
    }

    private volatile String bankMessage = "Open your bank to capture items.";

    private void startBank()
    {
        bank = new BankSnapshotController(client, clientThread, configManager, this::getPluginDirectory, message -> {
            bankMessage = message;
            final long token = generation;
            SwingUtilities.invokeLater(() -> {
                if (active && token == generation && panel != null) { panel.setBankStatus(message, this::clearBank); }
            });
        });
        bank.setSnapshotListener(snapshot -> {
            planningSnapshot = snapshot;
            final long token = generation;
            SwingUtilities.invokeLater(() -> { if (active && token == generation && panel != null) { panel.setPlanningSnapshot(snapshot); } });
            rebuildOwnedCatalog(snapshot);
        });
        bank.start();
    }

    private void rebuildOwnedCatalog(BankSnapshot snapshot)
    {
        bankGearOverlay.setRanks(Map.of());
        final long token = ++catalogRevision;
        ownedCatalog = null;
        SwingUtilities.invokeLater(() -> {
            if (token == catalogRevision && panel != null) { panel.setOwnedGear(null); }
        });
        if (!active || !config.ownedGearEnabled() || !config.equipmentEnabled() || snapshot == null) { return; }
        Set<Integer> ids = new HashSet<>(snapshot.bank.keySet());
        ids.addAll(snapshot.inventory.keySet());
        ids.addAll(snapshot.equipment.keySet());
        buildCatalogBatch(snapshot, ids.iterator(), token, new ArrayList<>(), new HashMap<>(), new HashSet<>(), new int[1]);
    }

    private void buildCatalogBatch(BankSnapshot snapshot, Iterator<Integer> ids, long token,
                                   List<OwnedGearPlanner.Gear> gear, Map<Integer, UltimateSlayerPanel.ItemDisplay> icons,
                                   Set<Integer> seen, int[] unknown)
    {
        clientThread.invokeLater(() -> {
            if (!active || token != catalogRevision || !config.ownedGearEnabled() || !config.equipmentEnabled()) { return; }
            // Resolve at most 48 cached IDs per callback. Never re-enumerates bank contents.
            for (int n = 0; n < 48 && ids.hasNext(); n++)
            {
                int id = ids.next();
                ItemComposition definition = itemManager.getItemComposition(id);
                if (definition.getPlaceholderTemplateId() >= 0) { continue; }
                if (definition.getNote() >= 0)
                {
                    id = definition.getLinkedNoteId();
                    if (id <= 0) { continue; }
                    definition = itemManager.getItemComposition(id);
                }
                if (!seen.add(id)) { continue; }
                ItemStats stats = itemManager.getItemStats(id);
                if (stats == null) { stats = itemManager.getItemStats(ItemVariationMapping.map(id)); }
                if (stats == null) { unknown[0]++; continue; }
                ItemEquipmentStats equipment = stats.getEquipment();
                if (!stats.isEquipable() || equipment == null) { continue; }
                gear.add(new OwnedGearPlanner.Gear(id, ItemVariationMapping.map(id), definition.getName(), equipment.getSlot(), equipment.isTwoHanded(),
                    Math.max(equipment.getAstab(), Math.max(equipment.getAslash(), equipment.getAcrush())), equipment.getArange(), equipment.getAmagic(),
                    equipment.getStr(), equipment.getRstr(), equipment.getMdmg(), equipment.getAstab(), equipment.getAslash(), equipment.getAcrush()));
                net.runelite.client.util.AsyncBufferedImage image = itemManager.getImage(id);
                image.onLoaded(() -> SwingUtilities.invokeLater(() -> { if (token == catalogRevision && panel != null) { panel.repaint(); } }));
                icons.put(id, new UltimateSlayerPanel.ItemDisplay(definition.getName(), new ImageIcon(image)));
            }
            if (ids.hasNext()) { buildCatalogBatch(snapshot, ids, token, gear, icons, seen, unknown); }
            else
            {
                OwnedGearCatalog catalog = new OwnedGearCatalog(snapshot.capturedAt, gear, icons, unknown[0], seen);
                ownedCatalog = catalog;
                SwingUtilities.invokeLater(() -> { if (active && token == catalogRevision && panel != null) { panel.setOwnedGear(catalog); } });
            }
        });
    }

    private void clearBank() { if (bank != null) { bank.clear(); } }

    private void manualRefresh()
    {
        clientThread.invokeLater(() -> {
            if (!active) { return; }
            rebuildOwnedCatalog(bank == null ? null : bank.getSnapshot());
            requestRefresh();
        });
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (bank != null && event.getGroupId() == InterfaceID.BANKMAIN) { bank.opened(); }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        if (bank != null && event.getGroupId() == InterfaceID.BANKMAIN) { bank.closed(); }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == ScriptID.QUESTLIST_INIT) { requestRefresh(); }
        if (bank != null && event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING) { bank.bankBuilt(); }
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        bankGearOverlay.setRanks(Map.of());
        ++generation;
        SwingUtilities.invokeLater(() -> { if (panel != null) { panel.resetAccountContext(); panel.refreshAppearance(); } });
        if (bank != null) { bank.accountTransition(); bank.loggedIn(); }
        requestRefresh();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        bankGearOverlay.setRanks(Map.of());
        ++generation;
        SwingUtilities.invokeLater(() -> { if (panel != null) { panel.resetAccountContext(); panel.refreshAppearance(); } });
        if (bank != null) { bank.stop(); bank = null; }
        if (active && config.bankEnabled()) { startBank(); }
        requestRefresh();
    }

    private void requestRefresh()
    {
        clientThread.invokeLater(() -> {
            if (!active || panel == null || refreshQueued) { return; }
            refreshQueued = true;
            clientThread.invokeAtTickEnd(() -> {
                refreshQueued = false;
                capture();
            });
        });
    }

    private void capture()
    {
        if (!active || panel == null) { return; }
        final long token = generation;
        boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
        int[] equipment = new int[14];
        int[] inventory = new int[28];
        int[] quantities = new int[28];
        Arrays.fill(equipment, -1);
        Arrays.fill(inventory, -1);
        Map<Integer, UltimateSlayerPanel.ItemDisplay> display = new HashMap<>();
        if (loggedIn)
        {
            if (config.equipmentEnabled() || config.requiredItemsEnabled()) { copy(client.getItemContainer(InventoryID.EQUIPMENT), equipment, null, display); }
            if (config.inventoryEnabled() || config.requiredItemsEnabled()) { copy(client.getItemContainer(InventoryID.INVENTORY), inventory, quantities, display); }
        }
        String task = null;
        String location = null;
        int remaining = 0;
        if (loggedIn && config.taskEnabled())
        {
            remaining = Math.max(0, client.getVarpValue(VarPlayerID.SLAYER_COUNT));
            if (remaining == 0)
            {
                task = "No active Slayer task";
            }
            else
            {
                int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
                task = resolveTaskName(taskId);
                int areaId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
                if (areaId > 0)
                {
                    List<Integer> areas = client.getDBRowsByValue(DBTableID.SlayerArea.ID,
                        DBTableID.SlayerArea.COL_AREA_ID, 0, areaId);
                    if (!areas.isEmpty())
                    {
                        Object[] field = client.getDBTableField(areas.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER, 0);
                        if (field.length > 0 && field[0] instanceof String) { location = (String) field[0]; }
                    }
                }
            }
        }
        Map<Skill, Integer> levels = new java.util.EnumMap<>(Skill.class);
        Map<Skill, Integer> boosts = new java.util.EnumMap<>(Skill.class);
        Map<Quest, Boolean> quests = new java.util.EnumMap<>(Quest.class);
        if (loggedIn && (config.equipmentEnabled() || config.requiredItemsEnabled() || config.travelEnabled() || config.spellsEnabled()))
        {
            for (Skill skill : Skill.values())
            { if (skill != Skill.OVERALL) { levels.put(skill, client.getRealSkillLevel(skill)); if (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.RANGED || skill == Skill.MAGIC) { boosts.put(skill, client.getBoostedSkillLevel(skill)); } } }
            for (Quest quest : ProgressionChecks.QUESTS)
            { QuestState status = quest.getState(client); if (status != null) { quests.put(quest, status == QuestState.FINISHED); } }
        }
        final AccountContext account = new AccountContext(levels, quests, loggedIn && config.spellsEnabled() ? client.getVarbitValue(VarbitID.SPELLBOOK) : -1, boosts);
        final List<OwnedGearPlanner.Gear> worn = new ArrayList<>();
        if (loggedIn && config.equipmentEnabled() && config.combatEnabled())
        {
            for (int id : equipment)
            {
                if (id <= 0) { continue; }
                ItemStats stats = itemManager.getItemStats(id); ItemEquipmentStats e = stats == null ? null : stats.getEquipment();
                if (e != null) { worn.add(new OwnedGearPlanner.Gear(id, ItemVariationMapping.map(id), itemManager.getItemComposition(id).getName(), e.getSlot(), e.isTwoHanded(),
                    Math.max(e.getAstab(), Math.max(e.getAslash(), e.getAcrush())), e.getArange(), e.getAmagic(), e.getStr(), e.getRstr(), e.getMdmg(), e.getAstab(), e.getAslash(), e.getAcrush())); }
            }
        }
        SidebarState next = new SidebarState(task, location, remaining, loggedIn, equipment, inventory, quantities);
        final boolean diaryObserved = loggedIn && config.requiredItemsEnabled() && client.getVarbitValue(VarbitID.KOUREND_DIARY_ELITE_COMPLETE) > 0;
        SwingUtilities.invokeLater(() -> {
            if (token == generation && panel != null) { panel.setAccountContext(account); panel.setWornGear(worn); panel.setDiaryObserved(diaryObserved); panel.update(next, display); }
        });
    }

    /** Resolve the assignment using the game's tables, without a cross-plugin service. */
    private String resolveTaskName(int taskId)
    {
        List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID,
            DBTableID.SlayerTask.COL_ID, 0, taskId);
        // Boss assignments resolve to their specific subtask through the same game table.
        final int bossAssignmentId = 98; // helper_slayer_current_assignment, as used by core Slayer
        if (taskId == bossAssignmentId)
        {
            List<Integer> bossRows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
                DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0,
                client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
            if (!bossRows.isEmpty())
            {
                Object[] field = client.getDBTableField(bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0);
                if (field.length > 0 && field[0] instanceof Integer)
                {
                    return taskNameFromRow((Integer) field[0], taskId);
                }
            }
        }
        return rows.isEmpty() ? "Unknown assignment (" + taskId + ")" : taskNameFromRow(rows.get(0), taskId);
    }

    private String taskNameFromRow(int row, int taskId)
    {
        Object[] field = client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
        return field.length > 0 && field[0] instanceof String ? (String) field[0] : "Unknown assignment (" + taskId + ")";
    }

    private void copy(ItemContainer container, int[] ids, int[] quantities, Map<Integer, UltimateSlayerPanel.ItemDisplay> display)
    {
        if (container == null) { return; }
        Item[] entries = container.getItems();
        for (int i = 0; i < Math.min(entries.length, ids.length); i++)
        {
            Item item = entries[i];
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0) { continue; }
            int id = item.getId();
            ids[i] = id;
            if (quantities != null) { quantities[i] = item.getQuantity(); }
            if (!display.containsKey(id))
            {
                net.runelite.client.util.AsyncBufferedImage image = itemManager.getImage(id);
                image.onLoaded(() -> SwingUtilities.invokeLater(() -> {
                    if (active && panel != null) { panel.repaint(); }
                }));
                display.put(id, new UltimateSlayerPanel.ItemDisplay(itemManager.getItemComposition(id).getName(), new ImageIcon(image)));
            }
        }
    }

    private void showWelcome(boolean force)
    {
        SwingUtilities.invokeLater(() -> {
            if (!active || java.awt.GraphicsEnvironment.isHeadless()) { return; }
            if (!force && Boolean.TRUE.equals(configManager.getConfiguration(UltimateSlayerConfig.GROUP, "welcomeSeen", Boolean.class))) { return; }
            if (welcomeDialog != null && welcomeDialog.isDisplayable()) { welcomeDialog.toFront(); return; }
            welcomeDialog = WelcomeDialog.create(SwingUtilities.getWindowAncestor(client.getCanvas()));
            welcomeDialog.setVisible(true); configManager.setConfiguration(UltimateSlayerConfig.GROUP, "welcomeSeen", true);
        });
    }

    private static BufferedImage createIcon()
    {
        try
        {
            BufferedImage logo = javax.imageio.ImageIO.read(UltimateSlayerPlugin.class.getResource("branding/logo.png"));
            BufferedImage icon = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D paint = icon.createGraphics(); paint.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            paint.drawImage(logo, 0, 0, 24, 24, null); paint.dispose(); return icon;
        }
        catch (Exception ignored) { /* The existing sword remains a safe fallback. */ }
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(235, 189, 91));
        g.fillPolygon(new int[]{3, 6, 14, 11}, new int[]{12, 13, 3, 2}, 4);
        g.setColor(Color.WHITE);
        g.drawLine(3, 9, 8, 14);
        g.drawLine(2, 14, 5, 11);
        g.dispose();
        return image;
    }
}

