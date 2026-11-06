package com.imth3keeza0.ultimateslayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Filepath;

/** Personal-bank capture only. No input, PIN, login widget, or HTTP access. */
public final class BankSnapshotController
{
    public interface Directory { Filepath get() throws IOException; }
    private final Client client;
    private final ClientThread clientThread;
    private final ConfigManager config;
    private final Directory directory;
    private final Consumer<String> status;
    private final BankSessionGate gate = new BankSessionGate();
    private volatile long revision;
    private volatile boolean running;
    private String character;
    private boolean scheduled;
    private BankSnapshot snapshot;
    private Consumer<BankSnapshot> snapshotListener = ignored -> {};
    public void setSnapshotListener(Consumer<BankSnapshot> listener) { snapshotListener = listener; }
    public BankSnapshot getSnapshot() { return snapshot; } // Client thread only.

    public BankSnapshotController(Client client, ClientThread clientThread, ConfigManager config,
                                  Directory directory, Consumer<String> status)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.directory = directory;
        this.status = status;
    }

    public void start()
    {
        running = true;
        clientThread.invokeLater(() -> {
            if (!running) { return; }
            ensureCharacter();
            // Enabling while already banking does not create a new bank opening.
            if (bankVisible()) { gate.opened(); gate.suppressCurrentOpening(); }
        });
    }

    public void stop()
    {
        running = false;
        ++revision;
        gate.closed();
        character = null;
        snapshot = null;
        snapshotListener.accept(null);
        // Drain explicit deletions even when disabled; stale writes check running/revision.
        // Shared IO queue survives disable/re-enable so a new load cannot overtake a clear.
    }

    public void accountTransition()
    {
        if (!running) { return; }
        ++revision;
        character = null;
        snapshot = null;
        snapshotListener.accept(null);
        gate.closed();
        status.accept("Log in and open your bank.");
    }

    public void loggedIn() { if (running) { ensureCharacter(); } }

    private boolean ensureCharacter()
    {
        if (!running || client.getGameState() != GameState.LOGGED_IN) { return false; }
        String profile = config.getRSProfileKey();
        if (profile == null || profile.isEmpty()) { return false; }
        String key = hash(profile);
        if (key.equals(character)) { return true; }
        character = key;
        snapshot = null;
        snapshotListener.accept(null);
        final long token = ++revision;
        status.accept("Loading this character's local snapshot…");
        submit(() -> {
            try
            {
                BankSnapshot loaded = new BankSnapshotStore(directory.get()).load(key);
                complete(token, () -> { snapshot = loaded; snapshotListener.accept(loaded); showSnapshot(loaded == null ? "Open your bank to capture items." : "Saved locally"); });
            }
            catch (IOException | RuntimeException e) { complete(token, () -> status.accept("Saved data unavailable. Reopen the bank to replace it.")); }
        });
        return true;
    }

    public void opened()
    {
        if (!running) { return; }
        ensureCharacter();
        gate.opened();
    }

    public void closed() { if (running) { gate.closed(); } }

    public void bankBuilt()
    {
        if (!running || !gate.pending() || scheduled) { return; }
        final long opening = gate.session();
        scheduled = true;
        clientThread.invokeAtTickEnd(() -> {
            scheduled = false;
            if (!running || !gate.pending() || opening != gate.session() || !bankVisible() || !ensureCharacter()) { return; }
            if (!gate.claim(opening)) { return; }
            // A visible health bar is a conservative recent-combat guard.
            if (client.getLocalPlayer() == null || client.getLocalPlayer().getHealthRatio() >= 0
                || client.getLocalPlayer().getInteracting() instanceof net.runelite.api.Player
                || (client.getLocalPlayer().getInteracting() instanceof NPC
                    && ((NPC) client.getLocalPlayer().getInteracting()).getCombatLevel() > 0))
            {
                status.accept("Capture skipped after recent combat. Reopen the bank once safe.");
                return;
            }
            ItemContainer bank = client.getItemContainer(InventoryID.BANK);
            if (bank == null)
            {
                status.accept("Bank contents were not ready. Close and reopen the bank.");
                return;
            }
            Map<Integer, Long> bankItems = copy(bank);
            Map<Integer, Long> carried = copy(client.getItemContainer(InventoryID.INVENTORY));
            Map<Integer, Long> worn = copy(client.getItemContainer(InventoryID.EQUIPMENT));
            snapshot = new BankSnapshot(System.currentTimeMillis(), bankItems, carried, worn);
            snapshotListener.accept(snapshot);
            final BankSnapshot captured = snapshot;
            final String key = character;
            final long token = ++revision; // Prevent an older disk load replacing this capture.
            showSnapshot("Saving locally…");
            submit(() -> {
                if (!running || token != revision) { return; }
                try
                {
                    new BankSnapshotStore(directory.get()).save(key, captured);
                    complete(token, () -> showSnapshot("Saved locally"));
                }
                catch (IOException | RuntimeException e) { complete(token, () -> showSnapshot("Local save failed; current session only")); }
            });
        });
    }

    private boolean bankVisible()
    {
        Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        return bank != null && !bank.isHidden();
    }

    private Map<Integer, Long> copy(ItemContainer container)
    {
        Map<Integer, Long> result = new LinkedHashMap<>();
        if (container == null) { return result; }
        Item[] entries = container.getItems();
        if (entries.length > 2000) { throw new IllegalArgumentException("Unexpected container size"); }
        for (Item item : entries)
        {
            if (item != null && item.getId() > 0 && item.getId() != ItemID.BANK_FILLER && item.getQuantity() > 0)
            {
                result.merge(item.getId(), (long) item.getQuantity(), Long::sum);
            }
        }
        return result;
    }

    public void clear()
    {
        clientThread.invokeLater(() -> {
            if (!running || !ensureCharacter()) { return; }
            final String key = character;
            final long token = ++revision;
            snapshot = null;
            snapshotListener.accept(null);
            gate.suppressCurrentOpening();
            status.accept("Clearing this character's saved items…");
            // Serial IO ensures an in-flight save finishes before this deletion.
            submit(() -> {
                try
                {
                    new BankSnapshotStore(directory.get()).clear(key);
                    complete(token, () -> status.accept("Saved items cleared. Close and reopen the bank to capture again."));
                }
                catch (IOException | RuntimeException e) { complete(token, () -> status.accept("Could not clear saved data. Please try again.")); }
            });
        });
    }

    private void showSnapshot(String note)
    {
        if (snapshot == null) { status.accept(note); return; }
        String time = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(snapshot.capturedAt));
        status.accept(snapshot.bank.size() + " bank item types · " + time + ". " + note + ". Changes appear next opening.");
    }

    private void complete(long token, Runnable action)
    {
        clientThread.invokeLater(() -> { if (running && token == revision) { action.run(); } });
    }

    private void submit(Runnable action)
    {
        if (!LocalSnapshotIo.submit(action) && running) { status.accept("Local storage busy. Please try again."); }
    }

    private static String hash(String profile)
    {
        try
        {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(profile.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) { result.append(String.format("%02x", value & 255)); }
            return result.toString();
        }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
