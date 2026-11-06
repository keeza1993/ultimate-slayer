package com.imth3keeza0.ultimateslayer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

/** Bundled factual stance table, not a cross-plugin/private API. */
final class WeaponStances
{
    static final class Stance
    {
        final String type, stance;
        Stance(String type, String stance) { this.type = type; this.stance = stance; }
    }
    private static final Map<String, List<Stance>> STANCES = load();
    private static Map<String, List<Stance>> load()
    {
        Map<String, List<Stance>> result = new HashMap<>();
        try (InputStreamReader reader = new InputStreamReader(WeaponStances.class.getResourceAsStream("data/stances.json"), StandardCharsets.UTF_8))
        {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet())
            {
                List<Stance> values = new ArrayList<>();
                for (JsonElement e : entry.getValue().getAsJsonArray())
                {
                    JsonObject row = e.getAsJsonObject();
                    if (!row.get("attack_type").isJsonNull() && !row.get("attack_style").isJsonNull()) { values.add(new Stance(row.get("attack_type").getAsString(), row.get("attack_style").getAsString())); }
                }
                result.put(entry.getKey(), List.copyOf(values));
            }
        }
        catch (Exception e) { return Map.of(); }
        return Map.copyOf(result);
    }
    static List<Stance> forCategory(String category) { return STANCES.getOrDefault(category.toLowerCase(Locale.ROOT).replace(' ', '_'), List.of()); }
}
