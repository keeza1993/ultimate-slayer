package com.imth3keeza0.ultimateslayer;

/** One claim per genuine bank opening. No client reads or timers. */
public final class BankSessionGate
{
    private boolean open;
    private boolean claimed;
    private long session;

    public void opened()
    {
        if (!open) { open = true; claimed = false; ++session; }
    }

    public void closed() { open = false; claimed = false; ++session; }
    public void suppressCurrentOpening() { if (open) { claimed = true; } }
    public long session() { return session; }
    public boolean pending() { return open && !claimed; }
    public boolean claim(long expected)
    {
        if (!pending() || expected != session) { return false; }
        claimed = true;
        return true;
    }
}
