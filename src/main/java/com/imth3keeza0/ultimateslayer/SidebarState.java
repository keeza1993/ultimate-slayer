package com.imth3keeza0.ultimateslayer;

import java.util.Arrays;

/** Immutable client-thread copy. No widgets, NPCs, credentials or bank data. */
public final class SidebarState
{
    public final String task;
    public final String location;
    public final int remaining;
    public final boolean loggedIn;
    private final int[] equipment;
    private final int[] inventory;
    private final int[] quantities;

    public SidebarState(String task, String location, int remaining, boolean loggedIn,
                        int[] equipment, int[] inventory, int[] quantities)
    {
        if (equipment.length != 14 || inventory.length != 28 || quantities.length != 28)
        {
            throw new IllegalArgumentException("Unexpected container dimensions");
        }
        this.task = task;
        this.location = location;
        this.remaining = remaining;
        this.loggedIn = loggedIn;
        this.equipment = equipment.clone();
        this.inventory = inventory.clone();
        this.quantities = quantities.clone();
    }

    public int equipmentAt(int slot) { return equipment[slot]; }
    public int inventoryAt(int slot) { return inventory[slot]; }
    public int quantityAt(int slot) { return quantities[slot]; }

    public static SidebarState empty()
    {
        int[] equipment = new int[14];
        int[] inventory = new int[28];
        Arrays.fill(equipment, -1);
        Arrays.fill(inventory, -1);
        return new SidebarState(null, null, 0, false, equipment, inventory, new int[28]);
    }
}
