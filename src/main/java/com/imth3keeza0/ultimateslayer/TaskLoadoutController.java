package com.imth3keeza0.ultimateslayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/** Client-thread ownership gate around asynchronous, local-only per-profile presets. */
public final class TaskLoadoutController
{
    private final Client client;
    private final ClientThread clientThread;
    private final ConfigManager config;
    private final BankSnapshotController.Directory directory;
    private final Consumer<Map<String, TaskLoadout>> listener;
    private final Consumer<String> status;
    private volatile boolean running;
    private volatile long revision;
    private String character;
    private boolean ready;
    private Map<String, TaskLoadout> loadouts = Map.of();

    public TaskLoadoutController(Client client, ClientThread clientThread, ConfigManager config,
                                 BankSnapshotController.Directory directory,
                                 Consumer<Map<String, TaskLoadout>> listener, Consumer<String> status)
    {
        this.client = client; this.clientThread = clientThread; this.config = config;
        this.directory = directory; this.listener = listener; this.status = status;
    }

    public void start() { running = true; clientThread.invokeLater(this::loggedIn); }
    public void stop()
    {
        running = false; ++revision; character = null; ready = false;
        loadouts = Map.of(); listener.accept(Map.of());
    }
    public void accountTransition()
    {
        ++revision; character = null; ready = false;
        loadouts = Map.of(); listener.accept(Map.of());
    }
    public void loggedIn() { if (running) { ensureProfile(); } }

    private boolean ensureProfile()
    {
        if (!running || client.getGameState() != GameState.LOGGED_IN) { return false; }
        String profile = config.getRSProfileKey();
        if (profile == null || profile.isEmpty()) { return false; }
        String key = BankSnapshotController.hash(profile);
        if (key.equals(character)) { return true; }
        character = key; ready = false; loadouts = Map.of(); listener.accept(Map.of());
        final long token = ++revision;
        status.accept("Loading task presets...");
        if (!LocalSnapshotIo.submit(() -> {
            try
            {
                Map<String, TaskLoadout> loaded = new TaskLoadoutStore(directory.get()).load(key);
                clientThread.invokeLater(() -> {
                    if (running && token == revision && key.equals(character))
                    { loadouts = loaded; ready = true; listener.accept(loaded); status.accept(loaded.size() + " task presets ready."); }
                });
            }
            catch (IOException | RuntimeException ex)
            {
                clientThread.invokeLater(() -> {
                    if (running && token == revision)
                    { status.accept("Could not load task presets. Existing presets have NOT been overwritten."); }
                });
            }
        })) { status.accept("Storage queue busy. Restart the plugin to retry loading presets."); }
        return true;
    }

    public void save(String task, TaskLoadout preset)
    {
        clientThread.invokeLater(() -> {
            if (!ensureProfile() || !ready) { status.accept("Task presets still loading; try again."); return; }
            String key = TaskLoadoutStore.taskKey(task);
            if (key.isEmpty()) { status.accept("Select an active Slayer task first."); return; }
            Map<String, TaskLoadout> next = new HashMap<>(loadouts);
            next.put(key, preset);
            persist(next, "Saved current equipment and inventory for " + task + ".");
        });
    }

    public void remove(String task)
    {
        clientThread.invokeLater(() -> {
            if (!ensureProfile() || !ready) { status.accept("Task presets still loading; try again."); return; }
            String key = TaskLoadoutStore.taskKey(task);
            if (key.isEmpty() || !loadouts.containsKey(key)) { return; }
            Map<String, TaskLoadout> next = new HashMap<>(loadouts);
            next.remove(key);
            persist(next, "Removed saved setup for " + task + ".");
        });
    }

    private void persist(Map<String, TaskLoadout> next, String message)
    {
        Map<String, TaskLoadout> captured = Map.copyOf(next);
        String key = character;
        loadouts = captured; listener.accept(captured);
        final long token = ++revision;
        status.accept("Saving task setup locally...");
        if (!LocalSnapshotIo.submit(() -> {
            if (!running || token != revision) { return; }
            try
            {
                new TaskLoadoutStore(directory.get()).save(key, captured);
                clientThread.invokeLater(() -> { if (running && token == revision) { status.accept(message); } });
            }
            catch (IOException | RuntimeException ex)
            {
                clientThread.invokeLater(() -> { if (running && token == revision)
                { status.accept("Task setup could not be saved to disk. Check storage permissions."); } });
            }
        })) { status.accept("Storage busy. Task setup has not been saved to disk."); }
    }
}
