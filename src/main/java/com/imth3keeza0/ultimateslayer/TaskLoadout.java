package com.imth3keeza0.ultimateslayer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** An immutable, user-selected 14-slot equipment and 28-slot inventory preset. */
public final class TaskLoadout
{
    private final int[] equipment;
    private final int[] inventory;
    private final int[] quantities;

    public TaskLoadout(int[] equipment, int[] inventory, int[] quantities)
    {
        if (equipment.length != 14 || inventory.length != 28 || quantities.length != 28)
        { throw new IllegalArgumentException("Invalid loadout dimensions"); }
        this.equipment = equipment.clone();
        this.inventory = inventory.clone();
        this.quantities = quantities.clone();
        for (int id : this.equipment) { if (id < -1) { throw new IllegalArgumentException("Invalid equipment ID"); } }
        for (int i = 0; i < 28; i++)
        {
            if (this.inventory[i] < -1 || this.quantities[i] < 0)
            { throw new IllegalArgumentException("Invalid inventory item"); }
            if (this.inventory[i] <= 0) { this.quantities[i] = 0; }
        }
    }

    public static TaskLoadout capture(SidebarState state)
    {
        int[] equipment = new int[14], inventory = new int[28], quantities = new int[28];
        for (int i = 0; i < 14; i++) { equipment[i] = state.equipmentAt(i); }
        for (int i = 0; i < 28; i++)
        {
            inventory[i] = state.inventoryAt(i);
            quantities[i] = state.quantityAt(i);
        }
        return new TaskLoadout(equipment, inventory, quantities);
    }

    public int equipmentAt(int index) { return equipment[index]; }
    public int inventoryAt(int index) { return inventory[index]; }
    public int quantityAt(int index) { return quantities[index]; }
    public int[] equipment() { return equipment.clone(); }
    public int[] inventory() { return inventory.clone(); }
    public int[] quantities() { return quantities.clone(); }

    /** IDs only. No bank reads, player details, or automatic withdrawals. */
    public Set<Integer> itemIds()
    {
        Set<Integer> ids = new HashSet<>();
        for (int id : equipment) { if (id > 0) { ids.add(id); } }
        for (int id : inventory) { if (id > 0) { ids.add(id); } }
        return Set.copyOf(ids);
    }

    @Override public boolean equals(Object other)
    {
        if (!(other instanceof TaskLoadout)) { return false; }
        TaskLoadout that = (TaskLoadout) other;
        return Arrays.equals(equipment, that.equipment) && Arrays.equals(inventory, that.inventory)
            && Arrays.equals(quantities, that.quantities);
    }
    @Override public int hashCode()
    { return 31 * (31 * Arrays.hashCode(equipment) + Arrays.hashCode(inventory)) + Arrays.hashCode(quantities); }
}
