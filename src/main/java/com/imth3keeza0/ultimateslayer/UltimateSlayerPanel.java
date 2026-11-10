package com.imth3keeza0.ultimateslayer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

/** Swing-only view. Data collection and item-definition access stay outside it. */
public final class UltimateSlayerPanel extends PluginPanel
{
    private static final Color BACKGROUND = new Color(35, 36, 39);
    private static final Color CARD = new Color(44, 46, 50);
    private static final Color MUTED = new Color(185, 190, 198);
    private static final Color GOLD = new Color(235, 189, 91);
    private final UltimateSlayerConfig config;
    private java.util.function.BiConsumer<String,SupplyPlanner.Priority> supplyPriorityAction=(key,priority)->{};
    private boolean missingSuppliesFirst;
    public void setSupplyPriorityAction(java.util.function.BiConsumer<String,SupplyPlanner.Priority> action){supplyPriorityAction=action;}
    private WikiStrategyService wikiService;
    private WikiStrategy.Profile wikiProfile;
    private WikiStrategy.Method wikiMethod;
    private String wikiKey = "";
    private long wikiRevision;
    private java.util.function.Consumer<Map<Integer, Integer>> rankListener = ranks -> {};
    public void setRankListener(java.util.function.Consumer<Map<Integer, Integer>> listener) { rankListener = listener; }
    public void setWikiService(WikiStrategyService service) { wikiService = service; rebuild(); }
    private String strategyTarget()
    {
        if (monsterChoice != null) { return monsterChoice.name; }
        String task = contextTask(); if (task == null) { return null; }
        for (ReferenceCatalog.Monster monster : ReferenceCatalog.get().forTask(task))
        { if (WikiStrategyService.sameFamily(monster.name, task)) { return monster.name; } }
        for (String name : ReferenceCatalog.get().tasks.keySet()) { if (name.equalsIgnoreCase(task)) { return name; } }
        return task;
    }
    private final Runnable refresh;
    private final Map<String, Boolean> expanded = new HashMap<>();
    private SidebarState state = SidebarState.empty();
    private Map<Integer, ItemDisplay> items = new HashMap<>();
    private String preferredStyle = "Melee";
    private String plannedSpell = "Choose spell";
    private String meleeType = "Melee";
    private ReferenceCatalog.Monster monsterChoice;
    private String selectedStyle() { return "Melee".equals(preferredStyle) ? meleeType : preferredStyle; }
    private String bankStatus = "Open your bank to capture items.";
    private Runnable clearBank = () -> {};
    private OwnedGearCatalog owned;
    private boolean showOwned = true;
    private int selectedOwnedSlot = 3;
    private final Map<String, OwnedGearPlanner.Draft> draftCache = new HashMap<>();
    private String previewTask;
    private TaskReferenceData.Location locationChoice;
    private TaskReferenceData.Exemption exemption = TaskReferenceData.Exemption.UNKNOWN;
    private boolean diaryObserved;
    private Runnable infoAction = () -> {};
    public void setInfoAction(Runnable action) { infoAction = action; }
    private final java.util.concurrent.ThreadPoolExecutor combatWorker = new java.util.concurrent.ThreadPoolExecutor(1, 1, 20,
        java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1), action -> {
            Thread thread = new Thread(action, "ultimate-slayer-loadout-search"); thread.setDaemon(true); return thread;
        });
    private java.util.concurrent.Future<?> searchJob;
    private long searchRevision;
    private boolean disposed, onTaskAssumed, chargedAssumed, useBoosts = true, allStyles = true;
    private CombatCalculator.PrayerBoost prayerAssumed = CombatCalculator.PrayerBoost.NONE;
    private LoadoutOptimizer.SearchResult combatResults;
    private OwnedGearPlanner.Draft optimizedDraft;
    private LoadoutOptimizer.Setup displayedSetup;
    private String combatStatus = "Choose a monster variant, then compare your owned gear.";
    private List<OwnedGearPlanner.Gear> wornGear = List.of();
    public void setWornGear(List<OwnedGearPlanner.Gear> gear) { wornGear = List.copyOf(gear); }
    public void disposePanel() { disposed = true; ++wikiRevision; rankListener.accept(Map.of()); invalidateCombat(); combatWorker.shutdownNow(); }
    public void combatSettingsChanged() { if (!config.combatEnabled()) { invalidateCombat(); } rebuild(); }
    private void invalidateCombat()
    {
        draftCache.clear();
        searchRevision++; if (searchJob != null) { searchJob.cancel(true); searchJob = null; combatWorker.purge(); }
        if (combatResults != null || optimizedDraft != null) { combatStatus = "Inputs changed; run the comparison again."; }
        combatResults = null; optimizedDraft = null; displayedSetup = null;
    }
    private AccountContext account = AccountContext.UNKNOWN;
    public void setAccountContext(AccountContext next) { if (!account.sameAs(next)) { invalidateCombat(); draftCache.clear(); } account = next; }
    private BankSnapshot planningSnapshot;
    public void setPlanningSnapshot(BankSnapshot next) { if (planningSnapshot != next) { invalidateCombat(); } planningSnapshot = next; rebuild(); }

    public void setDiaryObserved(boolean observed) { diaryObserved = observed; }

    public void resetAccountContext()
    {
        onTaskAssumed = false; chargedAssumed = false; prayerAssumed = CombatCalculator.PrayerBoost.NONE; useBoosts = true;
        wornGear = List.of();
        owned = null;
        account = AccountContext.UNKNOWN;
        monsterChoice = null;
        wikiProfile = null; wikiMethod = null; wikiKey = ""; ++wikiRevision; rankListener.accept(Map.of());
        plannedSpell = "Choose spell";
        planningSnapshot = null;
        previewTask = null;
        locationChoice = null;
        exemption = TaskReferenceData.Exemption.UNKNOWN;
        diaryObserved = false;
        draftCache.clear(); invalidateCombat();
    }

    private String contextTask() { return previewTask == null ? state.task : previewTask; }

    private TaskReferenceData.Location contextLocation()
    {
        if (previewTask == null && state.location != null && !state.location.isEmpty())
        { return TaskReferenceData.assignedLocation(state.location); }
        return locationChoice == null ? TaskReferenceData.locations(contextTask())[0] : locationChoice;
    }

    private OwnedGearPlanner.Draft contextDraft()
    {
        if (optimizedDraft != null && config.combatEnabled() && showOwned) { return optimizedDraft; }
        if (owned == null) { return null; }
        if (config.wikiGearEnabled() && wikiMethod != null)
        {
            return draftCache.computeIfAbsent("wiki:" + wikiKey + ":" + wikiMethod.name + ":" + contextLocation() + ":" + exemption,
                ignored -> WikiGearPlanner.plan(owned.gear, wikiMethod, contextTask(), contextLocation(), exemption, account, combatInputs()));
        }
        return draftCache.computeIfAbsent(selectedStyle() + ":" + contextTask() + ":" + contextLocation() + ":" + exemption,
            ignored -> OwnedGearPlanner.shortlist(owned.gear, selectedStyle(), contextTask(), contextLocation(), exemption, account));
    }

    public void setOwnedGear(OwnedGearCatalog catalog)
    {
        owned = catalog;
        draftCache.clear(); invalidateCombat();
        rebuild();
    }

    public void setBankStatus(String status, Runnable clearAction)
    {
        bankStatus = status;
        clearBank = clearAction;
        rebuild();
    }

    public static final class ItemDisplay
    {
        final String name;
        final ImageIcon icon;
        public ItemDisplay(String name, ImageIcon icon)
        {
            this.name = name;
            this.icon = icon;
        }
    }

    public UltimateSlayerPanel(UltimateSlayerConfig config, Runnable refresh)
    {
        this.config = config;
        this.refresh = refresh;
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 7, 10, 7));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        rebuild();
    }

    public void update(SidebarState next, Map<Integer, ItemDisplay> nextItems)
    {
        if (!java.util.Objects.equals(state.task, next.task) || !java.util.Objects.equals(state.location, next.location) || state.loggedIn != next.loggedIn)
        { draftCache.clear(); invalidateCombat(); if (previewTask == null) { locationChoice = null; } }
        if (!next.loggedIn) { resetAccountContext(); }
        state = next;
        items = new HashMap<>(nextItems);
        rebuild();
    }

    public void refreshAppearance() { rebuild(); }

    private void rebuild()
    {
        removeAll();
        JPanel header = row();
        JLabel title = new JLabel("ULTIMATE SLAYER");
        title.setForeground(GOLD);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        header.add(title);
        JButton button = new JButton("Refresh");
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        button.setMargin(new java.awt.Insets(3, 4, 3, 4));
        styleButton(button);
        button.setToolTipText("Refresh task and carried items. Does not read the bank.");
        button.addActionListener(e -> refresh.run());
        header.add(button);
        JButton info = new JButton("Info"); styleButton(info); info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        info.addActionListener(e -> infoAction.run()); header.add(info);
        addBlock(header);
        addBlock(label("Version 0.10.0 - Information only", MUTED));

        JPanel context = card("TASK / LOCATION");
        JComboBox<String> preview = new JComboBox<>(TaskReferenceData.tasks());
        preview.setEditable(true);
        preview.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        preview.setMaximumSize(new Dimension(188, 28));
        preview.setAlignmentX(LEFT_ALIGNMENT);
        preview.setSelectedItem(previewTask == null ? "Current task" : previewTask);
        preview.addActionListener(e -> {
            Object value = preview.getSelectedItem();
            String selected = value == null ? "" : value.toString().trim();
            if (selected.isEmpty() || selected.length() > 80) { return; }
            previewTask = selected.equals("Current task") ? null : selected;
            locationChoice = null;
            draftCache.clear(); invalidateCombat();
            rebuild();
        });
        context.add(preview);
        if (previewTask != null)
        {
            context.add(label("PREVIEW ONLY - " + previewTask, GOLD));
            context.add(label("Your real assignment is unchanged.", MUTED));
        }
        boolean lockedLocation = previewTask == null && state.location != null && !state.location.isEmpty();
        JComboBox<TaskReferenceData.Location> locations = new JComboBox<>(lockedLocation
            ? new TaskReferenceData.Location[]{contextLocation()} : TaskReferenceData.locations(contextTask()));
        locations.setSelectedItem(contextLocation());
        locations.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        locations.setMaximumSize(new Dimension(188, 28));
        locations.setAlignmentX(LEFT_ALIGNMENT);
        locations.setEnabled(!lockedLocation);
        locations.addActionListener(e -> { locationChoice = (TaskReferenceData.Location) locations.getSelectedItem(); draftCache.clear(); invalidateCombat(); rebuild(); });
        context.add(locations);
        if (lockedLocation) { context.add(label("Assigned area: " + state.location, GOLD)); }
        addBlock(context);

        if (config.taskEnabled())
        {
            JPanel task = card("YOUR TASK");
            task.add(label(state.task == null || state.task.isEmpty() ? "No task information" : state.task, Color.WHITE));
            task.add(label(state.task == null ? (state.loggedIn ? "Task data unavailable � refresh" : "Log in to see your task")
                : state.remaining + " remaining", MUTED));
            if (state.location != null && !state.location.isEmpty())
            {
                task.add(label(state.location, GOLD));
            }
            addBlock(task);
        }

        if (config.monstersEnabled()) { addMonsterReference(); }
        requestWiki(false);
        if (config.wikiGearEnabled()) { addWikiStrategy(); }
        if (config.suppliesEnabled()) { addSupplies(); }
        if (config.requiredItemsEnabled()) { addRequiredItems(); }
        if (config.spellsEnabled() && "Magic".equals(preferredStyle)) { addSpellPlan(); }
        if (config.combatEnabled()) { addCombatComparison(); }
        if (config.equipmentEnabled())
        {
            JPanel gear = card("EQUIPMENT");
            if (config.ownedGearEnabled())
            {
                JPanel modes = row();
                ButtonGroup modeGroup = new ButtonGroup();
                for (boolean mode : new boolean[]{true, false})
                {
                    JToggleButton choice = new JToggleButton(mode ? "Owned draft" : "Currently worn", showOwned == mode);
                    choice.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                    styleButton(choice);
                    choice.addActionListener(e -> { showOwned = mode; rebuild(); });
                    modeGroup.add(choice);
                    modes.add(choice);
                }
                gear.add(modes);
            }
            JPanel styles = row();
            ButtonGroup group = new ButtonGroup();
            for (String style : new String[]{"Melee", "Ranged", "Magic"})
            {
                JToggleButton choice = new JToggleButton(style, style.equals(preferredStyle));
                choice.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                choice.setMargin(new java.awt.Insets(4, 4, 4, 4));
                styleButton(choice);
                choice.setToolTipText("Choose a style for the bonus draft and optional modeled comparison");
                choice.addActionListener(e -> {
                    preferredStyle = style; wikiMethod = null;
                    if (wikiProfile != null) { for (WikiStrategy.Method method : wikiProfile.methods) { if (method.style.equals(style)) { wikiMethod = method; break; } } }
                    draftCache.clear(); invalidateCombat(); rebuild();
                });
                group.add(choice);
                styles.add(choice);
            }
            gear.add(styles);
            if ("Melee".equals(preferredStyle) && !(config.wikiGearEnabled() && wikiMethod != null))
            {
                JComboBox<String> subtype = new JComboBox<>(new String[]{"Melee", "Stab", "Slash", "Crush"});
                subtype.setSelectedItem(meleeType); subtype.setAlignmentX(LEFT_ALIGNMENT); subtype.setPreferredSize(new Dimension(176, 28)); subtype.setMaximumSize(new Dimension(176, 28));
                subtype.addActionListener(e -> { meleeType = (String) subtype.getSelectedItem(); draftCache.clear(); invalidateCombat(); rebuild(); }); gear.add(subtype);
            }
            boolean ownedMode = config.ownedGearEnabled() && showOwned;
            OwnedGearPlanner.Draft draft = ownedMode ? contextDraft() : null;
            gear.add(label(ownedMode ? "Owned setup draft - check marked items" : "Currently worn", MUTED));
            if (ownedMode && config.wikiGearEnabled() && wikiMethod == null)
            { gear.add(label("No Wiki method selected: the grid below is a generic bonus shortlist. Task-specific bank ranks are off.", GOLD)); }
            if (ownedMode && owned == null) { gear.add(label("Open your bank to load owned options. Bank snapshots must be enabled.", GOLD)); }
            if (draft != null)
            {
                gear.add(label(draft.requirement + (draft.protectionMissing ? " Required slot unfilled - see Required items." : ""), draft.protectionMissing ? new Color(255, 145, 125) : GOLD));
            }
            JPanel grid = new JPanel(new GridLayout(5, 3, 4, 4));
            grid.setOpaque(false);
            // Standard equipment container slots, arranged like the worn-items screen.
            int[] slots = {-1, 0, -1, 1, 2, 13, 3, 4, 5, -1, 7, -1, 9, 10, 12};
            String[] names = {"", "Head", "", "Cape", "Neck", "Ammo", "Weapon", "Body", "Shield", "", "Legs", "", "Hands", "Feet", "Ring"};
            for (int i = 0; i < slots.length; i++)
            {
                int slot = slots[i];
                OwnedGearPlanner.Gear option = draft == null ? null : draft.selected.get(slot);
                JComponent cell = slot < 0 ? spacer() : tile(ownedMode ? (option == null ? -1 : option.id) : state.equipmentAt(slot), 1, names[i]);
                if (draft != null && slot >= 0)
                {
                    String check = option == null ? null : draft.unverified.get(option.id);
                    cell.setToolTipText((option == null ? names[i] : option.name) + (check == null ? "" : " - CHECK: " + check) + "; click for alternatives");
                    if (check != null && cell instanceof JPanel)
                    {
                        JLabel badge = new JLabel("CHECK", SwingConstants.CENTER);
                        badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9)); badge.setForeground(GOLD);
                        ((JPanel) cell).add(badge, BorderLayout.SOUTH);
                    }
                    cell.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    cell.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent event) { selectedOwnedSlot = slot; rebuild(); }
                    });
                }
                grid.add(cell);
            }
            JPanel centred = row();
            centred.add(grid);
            gear.add(centred);
            if (draft != null)
            {
                List<OwnedGearPlanner.Gear> alternatives = draft.alternatives.get(selectedOwnedSlot);
                gear.add(label("Click a gear slot for its top 3 options.", MUTED));
                if (alternatives != null) {
                    for (int rank = 0; rank < alternatives.size(); rank++) {
                        OwnedGearPlanner.Gear option = alternatives.get(rank);
                        gear.add(label((rank + 1) + ". " + option.describe(selectedStyle()) + (draft.unverified.containsKey(option.id) ? " - CHECK: " + draft.unverified.get(option.id) : ""), BankGearOverlay.rankColor(rank + 1)));
                    }
                }
                else { gear.add(label("No compatible ranked option observed for this slot.", MUTED)); }
                boolean explain = expanded.getOrDefault("ranking", false);
                JButton explanation = new JButton((explain ? "Hide" : "Show") + " ranking details");
                styleButton(explanation);
                explanation.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                explanation.setAlignmentX(LEFT_ALIGNMENT);
                explanation.addActionListener(e -> { expanded.put("ranking", !explain); rebuild(); });
                gear.add(explanation);
                if (explain) { gear.add(label(config.wikiGearEnabled() && wikiMethod != null && optimizedDraft == null
                    ? "Source: " + wikiProfile.title + " / " + wikiMethod.name + ". Only owned table entries and required protection are ranked. Whole-loadout DPS refines a slot when every candidate is supported; otherwise Wiki order. Conditional choices and charges still need checking. Colour means relative owned rank, not verified exact DPS."
                    : optimizedDraft != null ? "Joint modeled setup: the combat panel explains the chosen stance, speed, effects and assumptions. Bounded search, not an exhaustive optimum. CHECK items still need verification."
                    : "Generic bonus shortlist only: damage bonus, then attack bonus. This is not task-specific Wiki guidance or DPS. Bank rank highlights are disabled without a matching Wiki strategy.", MUTED)); }
                gear.add(label("Snapshot: " + java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(owned.capturedAt))
                    + ". " + owned.unknownItems + " item IDs lack stat data.", MUTED));
            }
            addBlock(gear);
        }

        if (config.inventoryEnabled())
        {
            JPanel inventory = card("INVENTORY");
            inventory.add(label("Currently carried � 28 slots", MUTED));
            JPanel grid = new JPanel(new GridLayout(7, 4, 4, 4));
            grid.setOpaque(false);
            for (int i = 0; i < 28; i++)
            {
                grid.add(tile(state.inventoryAt(i), state.quantityAt(i), "Empty"));
            }
            JPanel centred = row();
            centred.add(grid);
            inventory.add(centred);
            addBlock(inventory);
        }

        if (config.equipmentEnabled() && config.ownedGearEnabled() && showOwned && contextDraft() != null && !contextDraft().omitted.isEmpty())
        { disclosure("Why gear was omitted", String.join("\n", contextDraft().omitted) + "\nFirst 12 blocked items shown. CHECK items remain visible; verify them before use."); }
        if (config.travelEnabled()) { addTravel(); }

        if (config.detailsEnabled())
        {
            disclosure("Feature coverage", "Monster/equipment browsing, bonus drafts, supported ammo matching, spellbook/rune plans and cached supplies work offline. The combat panel estimates supported normal attacks and set effects with a bounded joint search. Unsupported mechanics, full master weighting, drop/profit tables and Tile Packs remain outside this release.");
            if (contextTask() != null)
            {
                JButton wiki = new JButton("Check task on OSRS Wiki");
                styleButton(wiki);
                wiki.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                wiki.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse("https://oldschool.runescape.wiki/w/Special:Search?search="
                    + java.net.URLEncoder.encode(contextTask(), java.nio.charset.StandardCharsets.UTF_8)));
                addBlock(wiki);
            }
        }
        if (config.bankEnabled())
        {
            JPanel status = card("BANK DATA");
            status.add(label(bankStatus, MUTED));
            JButton clear = new JButton("Clear this character's data");
            styleButton(clear);
            clear.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            clear.addActionListener(e -> clearBank.run());
            status.add(clear);
            addBlock(status);
        }
        publishRanks();
        revalidate();
        repaint();
    }

    private CombatCalculator.Inputs combatInputs()
    {
        return new CombatCalculator.Inputs(account, monsterChoice, new CombatCalculator.Mode(selectedStyle(), "accurate"), plannedSpell,
            contextTask(), contextLocation(), exemption, onTaskAssumed, chargedAssumed, useBoosts, prayerAssumed);
    }
    private void startComparison()
    {
        invalidateCombat();
        if (monsterChoice == null || owned == null || planningSnapshot == null || owned.capturedAt != planningSnapshot.capturedAt)
        { combatStatus = "Choose a target and wait for this bank snapshot's gear metadata."; rebuild(); return; }
        final long revision = searchRevision;
        final CombatCalculator.Inputs inputs = combatInputs(); final OwnedGearCatalog captured = owned; final BankSnapshot snapshot = planningSnapshot;
        final WikiStrategy.Method capturedMethod = config.wikiGearEnabled() ? wikiMethod : null;
        final String style = selectedStyle(); final boolean compare = allStyles && capturedMethod == null;
        combatStatus = "Comparing complete loadouts in the background...";
        searchJob = combatWorker.submit(() -> {
            try
            {
                List<OwnedGearPlanner.Gear> candidates = captured.gear;
                if (capturedMethod != null)
                {
                    candidates = WikiGearPlanner.candidates(captured.gear, capturedMethod, inputs.task, inputs.location, inputs.exemption);
                }
                LoadoutOptimizer.SearchResult result = new LoadoutOptimizer(candidates, snapshot).search(inputs, style, compare);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (disposed || revision != searchRevision) { return; }
                    searchJob = null; combatResults = result;
                    combatStatus = result.evaluations + " complete combinations checked. Bounded search; not exhaustive." + (result.capped ? " Evaluation limit reached." : ""); rebuild();
                });
            }
            catch (java.util.concurrent.CancellationException ignored) { }
            catch (RuntimeException error)
            {
                javax.swing.SwingUtilities.invokeLater(() -> { if (!disposed && revision == searchRevision) { searchJob = null; combatStatus = "Comparison unavailable. Bonus draft remains available; refresh and retry."; rebuild(); } });
            }
        });
        rebuild();
    }
    private void combatToggle(JPanel section, String text, boolean selected, java.util.function.Consumer<Boolean> changed)
    {
        javax.swing.JCheckBox box = new javax.swing.JCheckBox(text, selected); box.setOpaque(false); box.setForeground(MUTED);
        box.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10)); box.setAlignmentX(LEFT_ALIGNMENT);
        box.addActionListener(e -> { changed.accept(box.isSelected()); invalidateCombat(); rebuild(); }); section.add(box);
    }
    private void addCombatComparison()
    {
        JPanel section = card("COMBAT COMPARISON");
        section.add(label("Supported normal-attack estimates. Choose the actual monster variant above.", MUTED));
        combatToggle(section, "Compare all supported styles", allStyles, value -> allStyles = value);
        combatToggle(section, "Use observed level boosts", useBoosts, value -> useBoosts = value);
        combatToggle(section, "On matching task (assume)", onTaskAssumed, value -> onTaskAssumed = value);
        combatToggle(section, "Assume charged items usable", chargedAssumed, value -> chargedAssumed = value);
        JComboBox<CombatCalculator.PrayerBoost> prayer = new JComboBox<>(CombatCalculator.PrayerBoost.values());
        prayer.setSelectedItem(prayerAssumed); prayer.setPreferredSize(new Dimension(176, 28)); prayer.setMaximumSize(new Dimension(176, 28)); prayer.setAlignmentX(LEFT_ALIGNMENT);
        prayer.setToolTipText("Assume this prayer is active. Unlocks are not automatically verified.");
        prayer.addActionListener(e -> { prayerAssumed = (CombatCalculator.PrayerBoost) prayer.getSelectedItem(); invalidateCombat(); rebuild(); }); section.add(prayer);
        JButton calculate = new JButton("Find best modeled setup"); styleButton(calculate);
        calculate.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10)); calculate.setEnabled(owned != null && monsterChoice != null && searchJob == null);
        calculate.addActionListener(e -> startComparison()); section.add(calculate); section.add(label(combatStatus, GOLD));
        if (combatResults != null)
        {
            if (combatResults.setups.isEmpty()) { section.add(label("No complete modeled setup found. Check required items, supported attacks, spell/runes and charge assumptions. Your bonus draft is still available.", GOLD)); }
            for (LoadoutOptimizer.Setup setup : combatResults.setups)
            {
                section.add(label(setup.title(), Color.WHITE)); section.add(label(setup.combat.summary(), GOLD));
                JButton show = new JButton("Show in draft"); styleButton(show); show.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                show.addActionListener(e -> {
                    OwnedGearPlanner.Draft baseline = contextDraft();
                    optimizedDraft = config.wikiGearEnabled() && wikiMethod != null && baseline != null
                        ? WikiGearPlanner.withModeledSelection(baseline, setup) : setup.asDraft();
                    displayedSetup = setup; showOwned = true;
                    if (OwnedGearPlanner.isMelee(setup.inputs.mode.style)) { preferredStyle = "Melee"; meleeType = setup.inputs.mode.style; }
                    else { preferredStyle = setup.inputs.mode.style; }
                    rebuild();
                }); section.add(show);
            }
        }
        if (monsterChoice != null && !wornGear.isEmpty())
        {
            Map<Integer, OwnedGearPlanner.Gear> worn = new HashMap<>(); wornGear.forEach(item -> worn.put(item.slot, item));
            int observed = 0; for (int slot = 0; slot < 14; slot++) { if (state.equipmentAt(slot) > 0) { observed++; } }
            CombatCalculator.Result best = null;
            if (observed == worn.size())
            {
                OwnedGearPlanner.Gear weapon = worn.get(3);
                for (CombatCalculator.Mode mode : CombatCalculator.modes(weapon, selectedStyle()))
                { CombatCalculator.Result result = CombatCalculator.evaluate(worn, combatInputs().withMode(mode)); if (result.available && (best == null || result.dps > best.dps)) { best = result; } }
            }
            section.add(label(best == null ? "Worn gear: no complete supported estimate for these inputs." : "Worn gear, best supported stance: " + best.summary(), MUTED));
        }
        section.add(label("CHECK items make results conditional. No special attacks, poison/burn, AoE, overkill or downtime. Assumptions reset on account changes.", MUTED));
        addBlock(section);
        if (combatResults != null)
        {
            StringBuilder explanation = new StringBuilder("Search: up to 6 ordinary candidates per slot plus modeled effect pieces, 24 starting setups, 3 improvement passes, 40,000 evaluations. Not a global optimum proof.\n");
            if (!combatResults.setups.isEmpty())
            {
                CombatCalculator.Result result = displayedSetup == null ? combatResults.setups.get(0).combat : displayedSetup.combat;
                explanation.append("Result: attack roll ").append(result.attackRoll).append(", defence roll ").append(result.defenceRoll).append(".\n");
                for (String effect : result.effects) { explanation.append(effect).append('\n'); }
                for (String warning : result.warnings) { explanation.append(warning).append('\n'); }
            }
            for (String exclusion : combatResults.excluded) { explanation.append("Not compared: ").append(exclusion).append('\n'); }
            disclosure("Why this setup / limits", explanation.toString());
        }
    }

    private void requestWiki(boolean force)
    {
        String target = strategyTarget();
        boolean valid = config.wikiGearEnabled() && config.equipmentEnabled() && config.ownedGearEnabled()
            && owned != null && target != null && !target.isBlank()
            && (previewTask != null || state.loggedIn && state.remaining > 0);
        String next = valid ? target + ":" + contextTask() + ":" + (monsterChoice == null ? 0 : monsterChoice.id)
            + ":" + config.wikiOnlineEnabled() : "";
        if (!force && next.equals(wikiKey)) { return; }
        wikiKey = next; wikiProfile = null; wikiMethod = null; draftCache.clear(); invalidateCombat();
        rankListener.accept(Map.of()); long revision = ++wikiRevision;
        if (!valid) { return; }
        wikiProfile = WikiStrategy.bundled(target); chooseWikiMethod();
        if (wikiService != null)
        {
            wikiService.load(target, contextTask(), config.wikiOnlineEnabled(), force, result -> javax.swing.SwingUtilities.invokeLater(() -> {
                if (disposed || revision != wikiRevision || !config.wikiGearEnabled()) { return; }
                String previous = wikiMethod == null ? "" : wikiMethod.name;
                wikiProfile = result; wikiMethod = null;
                for (WikiStrategy.Method method : result.methods) { if (method.name.equals(previous)) { wikiMethod = method; break; } }
                chooseWikiMethod(); draftCache.clear(); invalidateCombat(); rebuild();
            }));
        }
    }
    private void chooseWikiMethod()
    {
        if (wikiProfile == null) { return; }
        if (wikiMethod == null)
        {
            for (WikiStrategy.Method method : wikiProfile.methods) { if (method.style.equals(preferredStyle)) { wikiMethod = method; break; } }
            if (wikiMethod == null && !wikiProfile.methods.isEmpty()) { wikiMethod = wikiProfile.methods.get(0); }
        }
        if (wikiMethod != null)
        {
            preferredStyle = wikiMethod.style;
            String n = wikiMethod.name.toLowerCase(java.util.Locale.ROOT);
            meleeType = n.contains("crush") ? "Crush" : n.contains("stab") ? "Stab" : n.contains("slash") ? "Slash" : "Melee";
        }
    }
    private void addWikiStrategy()
    {
        JPanel section = card("WIKI STRATEGY - OWNED GEAR");
        section.add(label(wikiProfile == null ? (wikiKey.isEmpty() ? "Open your bank and choose a task or monster." : "Loading public Wiki equipment tables...") : wikiProfile.status, GOLD));
        if (wikiProfile != null)
        {
            section.add(label("Source: " + wikiProfile.title + "\nReviewed/fetched: " + java.time.Instant.ofEpochMilli(wikiProfile.fetchedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate(), MUTED));
            if (!wikiProfile.methods.isEmpty())
            {
                JComboBox<WikiStrategy.Method> methods = new JComboBox<>(wikiProfile.methods.toArray(new WikiStrategy.Method[0]));
                methods.setSelectedItem(wikiMethod); methods.setAlignmentX(LEFT_ALIGNMENT); methods.setMaximumSize(new Dimension(188, 28));
                methods.addActionListener(e -> { wikiMethod = (WikiStrategy.Method) methods.getSelectedItem(); chooseWikiMethod(); draftCache.clear(); invalidateCombat(); rebuild(); });
                section.add(methods);
                section.add(label("1 green: best owned; 2 yellow: alternative; 3 red: fallback. Wiki baseline, not a guaranteed DPS optimum. Required protection overrides rankings.", MUTED));
                if (owned != null && wikiMethod != null)
                {
                    List<OwnedGearPlanner.Gear> specials = WikiGearPlanner.specials(owned.gear, wikiMethod, account);
                    section.add(label("Special attack weapons (separate role): " + (specials.isEmpty() ? "none observed from this table" : specials.stream().map(g -> g.name).collect(java.util.stream.Collectors.joining(", "))), MUTED));
                }
            }
            JButton source = new JButton("Open strategy and conditions"); styleButton(source);
            String url = wikiProfile.url(); source.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse(url)); section.add(source);
        }
        JButton retry = new JButton("Refresh Wiki table"); styleButton(retry);
        retry.setEnabled(!wikiKey.isEmpty()); retry.addActionListener(e -> { requestWiki(true); rebuild(); }); section.add(retry);
        section.add(label(config.wikiOnlineEnabled() ? "Public page names only are requested. Tables are cached for 7 days." : "Online updates disabled; cached/bundled tables only.", MUTED));
        addBlock(section);
    }
    private void publishRanks()
    {
        if (!config.bankRanksEnabled() || !config.wikiGearEnabled() || !config.equipmentEnabled()
            || !config.ownedGearEnabled() || !showOwned || wikiMethod == null || owned == null || !state.loggedIn
            || previewTask == null && state.remaining <= 0)
        { rankListener.accept(Map.of()); return; }
        OwnedGearPlanner.Draft draft = contextDraft();
        if (draft == null || draft.protectionMissing || !draft.selected.containsKey(3)) { rankListener.accept(Map.of()); return; }
        Map<Integer, Integer> ranks = new HashMap<>();
        draft.alternatives.forEach((slot, candidates) -> {
            for (int i = 0; i < candidates.size() && i < 3; i++) {
                OwnedGearPlanner.Gear candidate = candidates.get(i); final int rank = i + 1;
                for (OwnedGearPlanner.Gear item : owned.gear) {
                    if (item.base == candidate.base && wikiMethod.rank(item) == wikiMethod.rank(candidate)
                        && !WikiStrategy.normalize(item.name).matches(".*(uncharged|broken|inactive|\\(empty\\)).*")) { ranks.merge(item.id, rank, Math::min); }
                }
            }
        });
        rankListener.accept(Map.copyOf(ranks));
    }

    private void addSpellPlan()
    {
        JPanel section = card("SPELL PLAN"); JComboBox<String> spells = new JComboBox<>(); spells.addItem("Choose spell");
        for (String name : ReferenceCatalog.get().spells.keySet()) { spells.addItem(name); }
        spells.setAlignmentX(LEFT_ALIGNMENT); spells.setPreferredSize(new Dimension(176, 28)); spells.setMaximumSize(new Dimension(176, 28)); spells.setSelectedItem(plannedSpell);
        spells.addActionListener(e -> { plannedSpell = (String) spells.getSelectedItem(); invalidateCombat(); rebuild(); }); section.add(spells);
        OwnedGearPlanner.Draft draft = contextDraft();
        for (String line : SpellPlanning.describe(plannedSpell, account, planningSnapshot, draft == null || !showOwned ? null : draft.selected.get(3))) { section.add(label(line, MUTED)); }
        addBlock(section);
    }

    private void addMonsterReference()
    {
        ReferenceCatalog catalog = ReferenceCatalog.get();
        JPanel section = card("MONSTER REFERENCE");
        List<ReferenceCatalog.Monster> matches = catalog.forTask(contextTask());
        if (matches.isEmpty()) { matches = catalog.search(contextTask(), 40); }
        if (monsterChoice != null && !matches.contains(monsterChoice)) { monsterChoice = null; }
        if (!catalog.error.isEmpty()) { section.add(label(catalog.error, GOLD)); }
        else if (matches.isEmpty()) { section.add(label("No matching variant. Type a monster name in Task / location and press Enter to search.", MUTED)); }
        else
        {
            JComboBox<Object> variants = new JComboBox<>(); variants.addItem("Choose monster / variant");
            for (ReferenceCatalog.Monster monster : matches) { variants.addItem(monster); }
            variants.setAlignmentX(LEFT_ALIGNMENT); variants.setPreferredSize(new Dimension(176, 28)); variants.setMaximumSize(new Dimension(176, 28)); variants.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            variants.setToolTipText(monsterChoice == null ? "Choose the exact monster and variant" : monsterChoice.toString());
            variants.setSelectedItem(monsterChoice == null ? "Choose monster / variant" : monsterChoice);
            variants.addActionListener(e -> { Object choice = variants.getSelectedItem(); monsterChoice = choice instanceof ReferenceCatalog.Monster ? (ReferenceCatalog.Monster) choice : null; invalidateCombat(); rebuild(); });
            section.add(variants);
            section.add(label(matches.size() + " matching records (search capped at 40). Select the actual variant; task/location restrictions still apply.", MUTED));
            if (monsterChoice != null)
            {
                section.add(label(monsterChoice.toString(), GOLD)); section.add(label(monsterChoice.summary(), Color.WHITE));
                JButton wiki = new JButton("Monster details on Wiki"); styleButton(wiki);
                String monsterName = monsterChoice.name;
                wiki.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse("https://oldschool.runescape.wiki/w/" + java.net.URLEncoder.encode(monsterName.replace(' ', '_'), java.nio.charset.StandardCharsets.UTF_8)));
                section.add(wiki);
            }
        }
        section.add(label(catalog.monsters.size() + " monster variants � " + catalog.equipment.size() + " equipment records. Offline reference snapshot: 5 Nov 2026.", MUTED));
        addBlock(section);
    }

    private void addRequiredItems()
    {
        JPanel section = card("REQUIRED ITEMS");
        for (String check : ProgressionChecks.describe(contextTask(), contextLocation(), account))
        { section.add(label(check, GOLD)); }
        if (contextLocation() == TaskReferenceData.Location.KARUULM)
        {
            section.add(label(diaryObserved ? "Elite diary completion observed; reward claim not verified." : "Elite Kourend reward exemption not verified.", MUTED));
            section.add(label("Elite rewards claimed (this character/session):", MUTED));
            JComboBox<TaskReferenceData.Exemption> reward = new JComboBox<>(TaskReferenceData.Exemption.values());
            reward.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            reward.setMaximumSize(new Dimension(188, 28));
            reward.setAlignmentX(LEFT_ALIGNMENT);
            reward.setSelectedItem(exemption);
            reward.addActionListener(e -> { exemption = (TaskReferenceData.Exemption) reward.getSelectedItem(); draftCache.clear(); invalidateCombat(); rebuild(); });
            section.add(reward);
            if (exemption == TaskReferenceData.Exemption.REWARDS_CLAIMED)
            { section.add(label("Heat exemption confirmed by you. Footwear slot unrestricted.", GOLD)); }
        }
        List<TaskReferenceData.Rule> rules = TaskReferenceData.rules(contextTask(), contextLocation(), exemption, preferredStyle);
        if (rules.isEmpty()) { section.add(label("No specific item checks bundled for this context. This does not mean no items are required.", MUTED)); }
        for (TaskReferenceData.Rule rule : rules)
        {
            section.add(label(rule.text, GOLD));
            String status = requirementStatus(rule);
            section.add(label(status, status.startsWith("Not observed") ? new Color(255, 145, 125) : MUTED));
            section.add(Box.createVerticalStrut(5));
        }
        section.add(label("Core checks only; verify the full monster and variant requirements. Snapshot ownership does not prove item eligibility or supply quantity.", MUTED));
        addBlock(section);
    }

    private String requirementStatus(TaskReferenceData.Rule rule)
    {
        if (rule.allowed.isEmpty()) { return "Compatibility not verified."; }
        for (int i = 0; i < 14; i++)
        {
            if (state.equipmentAt(i) > 0 && rule.accepts(state.equipmentAt(i)))
            { return rule.slot < 0 || rule.slot == i ? "Worn now." : "Owned in another slot - verify."; }
        }
        for (int i = 0; i < 28; i++)
        {
            if (state.inventoryAt(i) > 0 && rule.accepts(state.inventoryAt(i)))
            { return rule.slot < 0 ? "Carried now." : "Carried now - equip for protection."; }
        }
        if (planningSnapshot == null && owned == null) { return "Unknown - open bank to load ownership."; }
        if ((owned != null && owned.itemIds.stream().anyMatch(rule::accepts)) || (planningSnapshot != null && java.util.stream.Stream.of(planningSnapshot.bank, planningSnapshot.inventory, planningSnapshot.equipment).flatMap(m -> m.keySet().stream()).anyMatch(rule::accepts))) { return "Owned at snapshot - withdraw/equip as needed."; }
        return "Not observed in the last snapshot.";
    }

    private void addTravel()
    {
        TaskReferenceData.Location location = contextLocation();
        JPanel section = card("TRAVEL & LOCATIONS");
        section.add(label(location.label, GOLD));
        for (String hint : TripPlanning.travel(location, account, planningSnapshot)) { section.add(label(hint, GOLD)); }
        if (!location.route.isEmpty()) { section.add(label(location.route, Color.WHITE)); }
        section.add(label(location.access, MUTED));
        section.add(label("Reference routes with the limited checks above. Location coverage is incomplete.", MUTED));
        if (!location.wiki.isEmpty())
        {
            JButton wiki = new JButton("Open location Wiki");
            styleButton(wiki);
            wiki.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            wiki.setAlignmentX(LEFT_ALIGNMENT);
            wiki.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse("https://oldschool.runescape.wiki/w/" + location.wiki));
            section.add(wiki);
        }
        addBlock(section);
    }

    private void addSupplies()
    {
        JPanel section=card("TRIP CHECKLIST");
        if(!state.loggedIn || previewTask==null && state.remaining<=0)
        {section.add(label("Choose a task or preview to plan supplies.",MUTED));addBlock(section);return;}
        section.add(label("Carried/worn counts are live. Bank counts are from the last opening; they can be stale. Availability is not a calculated trip quantity.",MUTED));
        JComboBox<String> sort=new JComboBox<>(new String[]{"Priority first","Unpacked first"});
        sort.setSelectedIndex(missingSuppliesFirst?1:0);sort.setMaximumSize(new Dimension(188,28));sort.setAlignmentX(LEFT_ALIGNMENT);
        sort.addActionListener(e->{missingSuppliesFirst=sort.getSelectedIndex()==1;rebuild();});section.add(sort);
        boolean priorities=expanded.getOrDefault("supply-priorities",false);
        JButton settings=new JButton(priorities?"Hide priorities":"Set supply priorities");styleButton(settings);
        settings.addActionListener(e->{expanded.put("supply-priorities",!priorities);rebuild();});section.add(settings);
        if(priorities)
        {
            for(SupplyPlanner.Category category:SupplyPlanner.Category.values())
            {
                section.add(label(category.label,MUTED));
                JComboBox<SupplyPlanner.Priority> priority=new JComboBox<>(SupplyPlanner.Priority.values());
                priority.setSelectedItem(SupplyPlanner.priority(config,category));priority.setMaximumSize(new Dimension(188,28));priority.setAlignmentX(LEFT_ALIGNMENT);
                priority.addActionListener(e->{supplyPriorityAction.accept(category.key,(SupplyPlanner.Priority)priority.getSelectedItem());});section.add(priority);
            }
        }
        SidebarState planningState=state;
        if(previewTask!=null){int[] worn=new int[14],carried=new int[28],quantities=new int[28];for(int i=0;i<14;i++){worn[i]=state.equipmentAt(i);}for(int i=0;i<28;i++){carried[i]=state.inventoryAt(i);quantities[i]=state.quantityAt(i);}planningState=new SidebarState(previewTask,null,0,state.loggedIn,worn,carried,quantities);}
        Map<Integer,String> supplyNames=new HashMap<>(owned==null?Map.of():owned.itemNames);
        items.forEach((id,display)->supplyNames.put(id,display.name));
        OwnedGearCatalog supplyMetadata=new OwnedGearCatalog(owned==null?System.currentTimeMillis():owned.capturedAt,List.of(),Map.of(),0,supplyNames.keySet(),supplyNames);
        List<SupplyPlanner.Need> needs=SupplyPlanner.plan(contextTask(),monsterChoice==null?contextTask():monsterChoice.name,
            contextLocation(),exemption,preferredStyle,planningState,planningSnapshot,supplyMetadata,wikiProfile,account,
            config.preferHouseTravel(),config.houseFairyRingConfirmed(),config.stafflessFairyRingsConfirmed());
        for(SupplyPlanner.Need need:SupplyPlanner.sorted(needs,config,planningState,planningSnapshot,missingSuppliesFirst))
        {
            section.add(Box.createVerticalStrut(7));section.add(label(SupplyPlanner.priority(config,need.category)+" � "+need.title,GOLD));
            section.add(label(need.summary(planningState,planningSnapshot),need.packed(planningState)?BankGearOverlay.rankColor(1):MUTED));
            boolean details=expanded.getOrDefault("supply:"+need.key,false);
            JButton show=new JButton(details?"Hide details":"Choices / why");styleButton(show);show.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10));
            show.addActionListener(e->{expanded.put("supply:"+need.key,!details);rebuild();});section.add(show);
            if(details)
            {
                section.add(label(need.note,MUTED));
                for(SupplyPlanner.Choice choice:need.choices){section.add(label(choice.name+": now "+choice.carried(planningState)+"; bank at capture "+(planningSnapshot==null?"unknown":choice.bank(planningSnapshot)),MUTED));}
            }
        }
        if(wikiProfile!=null&&!wikiProfile.inventory.isEmpty())
        {
            boolean references=expanded.getOrDefault("wiki-inventory",false);
            JButton show=new JButton(references?"Hide Wiki inventory list":"Show Wiki inventory list");styleButton(show);
            show.addActionListener(e->{expanded.put("wiki-inventory",!references);rebuild();});section.add(show);
            if(references){section.add(label("Wiki inventory references (check source conditions): "+String.join(", ",wikiProfile.inventory),MUTED));}
        }
        addBlock(section);
    }

    private void disclosure(String title, String text)
    {
        boolean open = expanded.getOrDefault(title, false);
        JPanel section = card(null);
        JButton toggle = new JButton((open ? "? " : "? ") + title);
        styleButton(toggle);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setAlignmentX(LEFT_ALIGNMENT);
        toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        toggle.addActionListener(e -> { expanded.put(title, !open); rebuild(); });
        section.add(toggle);
        if (open) { section.add(label(text, MUTED)); }
        addBlock(section);
    }

    private JComponent tile(int id, int quantity, String emptyName)
    {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setPreferredSize(new Dimension(40, 42));
        tile.setBackground(new Color(31, 33, 36));
        tile.setBorder(BorderFactory.createLineBorder(config.highContrast() ? MUTED : new Color(65, 69, 76)));
        ItemDisplay display = items.get(id);
        if (display == null && owned != null) { display = owned.icons.get(id); }
        JLabel icon = new JLabel(display == null ? "�" : "", SwingConstants.CENTER);
        icon.setForeground(MUTED);
        if (display != null) { icon.setIcon(display.icon); }
        tile.add(icon, BorderLayout.CENTER);
        if (id > 0 && quantity > 1)
        {
            JLabel count = new JLabel(formatQuantity(quantity), SwingConstants.LEFT);
            count.setForeground(GOLD);
            count.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            tile.add(count, BorderLayout.NORTH);
        }
        tile.setToolTipText(display == null ? emptyName : display.name + (quantity > 1 ? " � " + quantity : ""));
        return tile;
    }

    static String formatQuantity(int amount)
    {
        if (amount >= 10_000_000) { return amount / 1_000_000 + "M"; }
        if (amount >= 100_000) { return amount / 1_000 + "K"; }
        return Integer.toString(amount);
    }

    private JPanel card(String title)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(config.highContrast() ? MUTED : new Color(58, 61, 67)), BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        if (title != null)
        {
            panel.add(label(title, GOLD));
            panel.add(Box.createVerticalStrut(5));
        }
        return panel;
    }

    private JLabel label(String text, Color color)
    {
        JLabel label = new JLabel("<html><body style='width:132px'>" + escape(text).replace("\n", "<br><br>") + "</body></html>");
        label.setForeground(config.highContrast() && color.equals(MUTED) ? Color.WHITE : color);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, config.largeText() ? 14 : 12));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JPanel row()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static void styleButton(javax.swing.AbstractButton button)
    {
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setBackground(new Color(56, 59, 65));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(91, 96, 105)), BorderFactory.createEmptyBorder(5, 4, 5, 4)));
    }

    private JComponent spacer()
    {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private void addBlock(JComponent component)
    {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        add(component);
        add(Box.createVerticalStrut(8));
    }
}


