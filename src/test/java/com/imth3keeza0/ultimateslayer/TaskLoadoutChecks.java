package com.imth3keeza0.ultimateslayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;

/** Pure-file-format and privacy boundary checks. Run with -ea. */
public final class TaskLoadoutChecks
{
    public static void main(String[] args) throws Exception
    {
        int[] gear = new int[14], inventory = new int[28], quantities = new int[28];
        Arrays.fill(gear, -1); Arrays.fill(inventory, -1);
        gear[3] = 26219;
        inventory[0] = 27000; quantities[0] = 1;
        inventory[1] = 2434; quantities[1] = 1;
        inventory[2] = 2434; quantities[2] = 1;
        TaskLoadout preset = new TaskLoadout(gear, inventory, quantities);
        gear[3] = -1; inventory[0] = -1; // No aliasing.
        assert preset.equipmentAt(3) == 26219;
        assert preset.inventoryAt(0) == 27000;
        assert preset.itemIds().contains(2434);
        assert preset.itemIds().size() == 3;

        Map<String, TaskLoadout> original = Map.of("Yama", preset, "Gargoyles", preset);
        String encoded = TaskLoadoutStore.encode(original);
        assert !encoded.contains("KEEZA0"); // Profile/player name is not serialized.
        Map<String, TaskLoadout> decoded = TaskLoadoutStore.decode(new BufferedReader(new StringReader(encoded)));
        assert decoded.size() == 2;
        assert preset.equals(decoded.get("yama"));
        assert preset.equals(decoded.get("gargoyles"));
        assert TaskLoadoutStore.taskKey("  YaMa  ").equals("yama");
        assert TaskLoadoutStore.taskKey("No active Slayer task").isEmpty();
        assert TaskLoadoutStore.taskKey(null).isEmpty();

        String corrupt = encoded.replaceFirst("E\\t0\\t-1", "E\\t0\\t-99");
        try
        {
            TaskLoadoutStore.decode(new BufferedReader(new StringReader(corrupt)));
            throw new AssertionError("Invalid negative item ID accepted");
        }
        catch (IOException expected) { /* Invalid files must not silently overwrite good presets. */ }

        String missing = encoded.replaceFirst("I\\t27\\t-1\\t0\\n", "");
        try
        {
            TaskLoadoutStore.decode(new BufferedReader(new StringReader(missing)));
            throw new AssertionError("Incomplete preset accepted");
        }
        catch (IOException expected) { }
    }
}
