package com.imth3keeza0.ultimateslayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OwnedGearCatalog
{
    public final long capturedAt;
    public final List<OwnedGearPlanner.Gear> gear;
    public final Map<Integer, UltimateSlayerPanel.ItemDisplay> icons;
    public final int unknownItems;
    public final java.util.Set<Integer> itemIds;
    public OwnedGearCatalog(long capturedAt, List<OwnedGearPlanner.Gear> gear,
                            Map<Integer, UltimateSlayerPanel.ItemDisplay> icons, int unknownItems)
    {
        this(capturedAt, gear, icons, unknownItems, gear.stream().map(g -> g.id).collect(java.util.stream.Collectors.toSet()));
    }

    public OwnedGearCatalog(long capturedAt, List<OwnedGearPlanner.Gear> gear,
                            Map<Integer, UltimateSlayerPanel.ItemDisplay> icons, int unknownItems, java.util.Set<Integer> itemIds)
    {
        this.capturedAt = capturedAt;
        this.gear = Collections.unmodifiableList(new ArrayList<>(gear));
        this.icons = Collections.unmodifiableMap(new HashMap<>(icons));
        this.unknownItems = unknownItems;
        this.itemIds = java.util.Set.copyOf(itemIds);
    }
}
