package com.imth3keeza0.ultimateslayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.util.Filepath;

/** Local-only, per-RuneLite-profile task presets; never stored in synced config. */
public final class TaskLoadoutStore
{
    private static final String HEADER = "ultimate-slayer-loadouts-v1";
    private final Filepath root;
    public TaskLoadoutStore(Filepath root) { this.root = root; }

    public static String taskKey(String task)
    {
        if (task == null) { return ""; }
        String key = task.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return key.equals("no active slayer task") || key.startsWith("unknown assignment") || key.length() > 100 ? "" : key;
    }

    private Filepath file(String profile)
    {
        if (profile == null || !profile.matches("[a-f0-9]{64}"))
        { throw new IllegalArgumentException("Invalid profile key"); }
        return root.joinSegment(profile + ".loadouts");
    }

    public Map<String, TaskLoadout> load(String profile) throws IOException
    {
        Filepath f = file(profile);
        if (!f.exists()) { return Map.of(); }
        if (f.size() > 512_000) { throw new IOException("Presets file too large"); }
        try (BufferedReader reader = f.openBufferedReader()) { return decode(reader); }
    }

    static Map<String, TaskLoadout> decode(BufferedReader reader) throws IOException
    {
        if (!HEADER.equals(reader.readLine())) { throw new IOException("Unknown presets format"); }
        Map<String, TaskLoadout> result = new LinkedHashMap<>();
        String line;
        int lineCount = 0;
        String task = null;
        int[] equipment = null, inventory = null, quantities = null;
        boolean[] seenEquipment = null, seenInventory = null;
        while ((line = reader.readLine()) != null)
        {
            if (++lineCount > 7000) { throw new IOException("Too many loadout entries"); }
            String[] f = line.split("\\t", -1);
            try
            {
                if (f.length == 2 && "T".equals(f[0]))
                {
                    if (task != null) { put(result, task, equipment, inventory, quantities, seenEquipment, seenInventory); }
                    task = taskKey(new String(Base64.getUrlDecoder().decode(f[1]), StandardCharsets.UTF_8));
                    if (task.isEmpty() || result.containsKey(task) || result.size() >= 100) { throw new IOException("Invalid task"); }
                    equipment = new int[14]; inventory = new int[28]; quantities = new int[28];
                    java.util.Arrays.fill(equipment, -1); java.util.Arrays.fill(inventory, -1);
                    seenEquipment = new boolean[14]; seenInventory = new boolean[28];
                }
                else if (task != null && f.length == 3 && "E".equals(f[0]))
                {
                    int index = Integer.parseInt(f[1]), id = Integer.parseInt(f[2]);
                    if (index < 0 || index >= 14 || seenEquipment[index] || id < -1) { throw new IOException("Invalid equipment slot"); }
                    equipment[index] = id; seenEquipment[index] = true;
                }
                else if (task != null && f.length == 4 && "I".equals(f[0]))
                {
                    int index = Integer.parseInt(f[1]), id = Integer.parseInt(f[2]), count = Integer.parseInt(f[3]);
                    if (index < 0 || index >= 28 || seenInventory[index] || id < -1 || count < 0)
                    { throw new IOException("Invalid inventory slot"); }
                    inventory[index] = id; quantities[index] = count; seenInventory[index] = true;
                }
                else { throw new IOException("Invalid preset entry"); }
            }
            catch (IllegalArgumentException e) { throw new IOException("Malformed preset", e); }
        }
        if (task != null) { put(result, task, equipment, inventory, quantities, seenEquipment, seenInventory); }
        return Map.copyOf(result);
    }

    private static void put(Map<String, TaskLoadout> result, String task, int[] equipment, int[] inventory,
                            int[] quantities, boolean[] seenEquipment, boolean[] seenInventory) throws IOException
    {
        for (boolean seen : seenEquipment) { if (!seen) { throw new IOException("Incomplete equipment preset"); } }
        for (boolean seen : seenInventory) { if (!seen) { throw new IOException("Incomplete inventory preset"); } }
        if (result.putIfAbsent(task, new TaskLoadout(equipment, inventory, quantities)) != null)
        { throw new IOException("Duplicate task"); }
    }

    public void save(String profile, Map<String, TaskLoadout> loadouts) throws IOException
    {
        if (loadouts.size() > 100) { throw new IOException("Too many presets"); }
        Filepath destination = file(profile);
        root.createDirectories();
        Filepath temporary = root.createTempFile("loadouts-", ".tmp");
        try
        {
            temporary.write(encode(loadouts));
            try { temporary.moveTo(destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { temporary.moveTo(destination, StandardCopyOption.REPLACE_EXISTING); }
        }
        finally { temporary.deleteIfExists(); }
    }

    static String encode(Map<String, TaskLoadout> loadouts)
    {
        StringBuilder result = new StringBuilder(HEADER).append('\n');
        loadouts.forEach((task, loadout) -> {
            String key = taskKey(task);
            if (key.isEmpty()) { throw new IllegalArgumentException("Invalid task"); }
            result.append("T\t").append(Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8))).append('\n');
            for (int i = 0; i < 14; i++) { result.append("E\t").append(i).append('\t').append(loadout.equipmentAt(i)).append('\n'); }
            for (int i = 0; i < 28; i++)
            { result.append("I\t").append(i).append('\t').append(loadout.inventoryAt(i)).append('\t').append(loadout.quantityAt(i)).append('\n'); }
        });
        return result.toString();
    }
}
