package com.imth3keeza0.ultimateslayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.client.util.Filepath;

/** Called exclusively on the plugin's serial IO worker. No synced config. */
public final class BankSnapshotStore
{
    private final Filepath root;
    public BankSnapshotStore(Filepath root) { this.root = root; }

    private Filepath file(String key)
    {
        if (!key.matches("[a-f0-9]{64}")) { throw new IllegalArgumentException("Invalid character key"); }
        return root.joinSegment(key + ".snapshot");
    }

    public BankSnapshot load(String key) throws IOException
    {
        Filepath file = file(key);
        if (!file.exists()) { return null; }
        if (file.size() > 262_144) { throw new IOException("Snapshot too large"); }
        try (BufferedReader reader = file.openBufferedReader())
        {
            return decode(reader);
        }
    }

    static BankSnapshot decode(BufferedReader reader) throws IOException
    {
        try
        {
            if (!"ultimate-slayer-bank-v1".equals(reader.readLine())) { throw new IOException("Unknown schema"); }
            long captured = Long.parseLong(reader.readLine());
            Map<Integer, Long> bank = new LinkedHashMap<>();
            Map<Integer, Long> inventory = new LinkedHashMap<>();
            Map<Integer, Long> equipment = new LinkedHashMap<>();
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null)
            {
                if (++lines > 2100) { throw new IOException("Too many entries"); }
                String[] fields = line.split("\t", -1);
                if (fields.length != 3) { throw new IOException("Invalid entry"); }
                Map<Integer, Long> target;
                switch (fields[0])
                {
                    case "B": target = bank; break;
                    case "I": target = inventory; break;
                    case "E": target = equipment; break;
                    default: throw new IOException("Invalid container");
                }
                if (target.put(Integer.parseInt(fields[1]), Long.parseLong(fields[2])) != null)
                { throw new IOException("Duplicate entry"); }
            }
            return new BankSnapshot(captured, bank, inventory, equipment);
        }
        catch (IllegalArgumentException e) { throw new IOException("Invalid snapshot", e); }
    }

    public void save(String key, BankSnapshot snapshot) throws IOException
    {
        root.createDirectories();
        Filepath temporary = root.createTempFile("bank-", ".tmp");
        try
        {
            temporary.write(encode(snapshot));
            try { temporary.moveTo(file(key), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { temporary.moveTo(file(key), StandardCopyOption.REPLACE_EXISTING); }
        }
        finally { temporary.deleteIfExists(); }
    }

    static String encode(BankSnapshot snapshot)
    {
        StringBuilder data = new StringBuilder("ultimate-slayer-bank-v1\n").append(snapshot.capturedAt).append('\n');
        append(data, "B", snapshot.bank);
        append(data, "I", snapshot.inventory);
        append(data, "E", snapshot.equipment);
        return data.toString();
    }

    private static void append(StringBuilder data, String container, Map<Integer, Long> entries)
    {
        entries.forEach((id, count) -> data.append(container).append('\t').append(id).append('\t').append(count).append('\n'));
    }

    public void clear(String key) throws IOException { file(key).deleteIfExists(); }
}
