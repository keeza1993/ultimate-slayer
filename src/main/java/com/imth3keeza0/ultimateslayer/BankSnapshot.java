package com.imth3keeza0.ultimateslayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Raw IDs retain charged, degraded and other functional variants. */
public final class BankSnapshot
{
    public final long capturedAt;
    public final Map<Integer, Long> bank;
    public final Map<Integer, Long> inventory;
    public final Map<Integer, Long> equipment;

    public BankSnapshot(long capturedAt, Map<Integer, Long> bank, Map<Integer, Long> inventory, Map<Integer, Long> equipment)
    {
        if (capturedAt <= 0 || capturedAt > System.currentTimeMillis() + 60_000) { throw new IllegalArgumentException("Invalid capture time"); }
        this.capturedAt = capturedAt;
        this.bank = checkedCopy(bank);
        this.inventory = checkedCopy(inventory);
        this.equipment = checkedCopy(equipment);
    }

    private static Map<Integer, Long> checkedCopy(Map<Integer, Long> source)
    {
        if (source.size() > 2000) { throw new IllegalArgumentException("Too many entries"); }
        Map<Integer, Long> copy = new LinkedHashMap<>();
        source.forEach((id, count) -> {
            if (id == null || count == null || id <= 0 || count <= 0 || count > Integer.MAX_VALUE * 2000L)
            { throw new IllegalArgumentException("Invalid item entry"); }
            copy.put(id, count);
        });
        return Collections.unmodifiableMap(copy);
    }
}
