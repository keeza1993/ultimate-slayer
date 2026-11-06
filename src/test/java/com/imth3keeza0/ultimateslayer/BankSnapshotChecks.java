package com.imth3keeza0.ultimateslayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;

/** Offline invariants. No client launch, credentials, reflection or game input. */
public final class BankSnapshotChecks
{
    public static void main(String[] args) throws Exception
    {
        BankSessionGate gate = new BankSessionGate();
        require(!gate.claim(gate.session()), "Cannot capture while closed");
        gate.opened();
        long first = gate.session();
        gate.opened();
        require(gate.session() == first, "Duplicate loaded event must not re-arm");
        require(gate.claim(first), "First ready event can capture");
        for (int i = 0; i < 500; i++) { require(!gate.claim(first), "Only one scan per opening"); }
        gate.closed();
        gate.opened();
        require(!gate.claim(first), "Late callback from previous opening rejected");
        require(gate.claim(gate.session()), "Next opening permits one new scan");
        gate.closed();
        gate.opened();
        gate.suppressCurrentOpening();
        require(!gate.claim(gate.session()), "Clear/re-enable must not recapture current opening");

        Map<Integer, Long> entries = new LinkedHashMap<>();
        entries.put(100, 3L);
        BankSnapshot snapshot = new BankSnapshot(System.currentTimeMillis(), entries, Map.of(), Map.of());
        entries.put(100, 8L);
        require(snapshot.bank.get(100) == 3L, "Snapshot must not retain mutable input");
        try { snapshot.bank.put(100, 9L); throw new AssertionError("Snapshot is mutable"); }
        catch (UnsupportedOperationException expected) { }
        try { new BankSnapshot(1, Map.of(100, -1L), Map.of(), Map.of()); throw new AssertionError("Negative count accepted"); }
        catch (IllegalArgumentException expected) { }
        BankSnapshot empty = new BankSnapshot(System.currentTimeMillis(), Map.of(), Map.of(), Map.of());
        require(empty.bank.isEmpty(), "Empty bank is a valid snapshot");
        BankSnapshot original = new BankSnapshot(System.currentTimeMillis(), Map.of(100, 2L, 101, 1L), Map.of(100, 1L), Map.of(102, 1L));
        BankSnapshot restored = BankSnapshotStore.decode(new BufferedReader(new StringReader(BankSnapshotStore.encode(original))));
        require(restored.bank.equals(original.bank) && restored.inventory.equals(original.inventory)
            && restored.equipment.equals(original.equipment), "Local format preserves quantities, variants and container provenance");
        reject("unknown-schema\n1\n");
        reject("ultimate-slayer-bank-v1\n1\nB\t100\t-1\n");
        reject("ultimate-slayer-bank-v1\n1\nB\t100\t1\nB\t100\t2\n");
        reject("ultimate-slayer-bank-v1\n1\nE\tbroken\n");
        System.out.println("Bank session and immutable snapshot checks passed.");
    }

    private static void require(boolean condition, String message)
    {
        if (!condition) { throw new AssertionError(message); }
    }

    private static void reject(String data) throws Exception
    {
        try
        {
            BankSnapshotStore.decode(new BufferedReader(new StringReader(data)));
            throw new AssertionError("Corrupt snapshot accepted");
        }
        catch (IOException expected) { }
    }
}
