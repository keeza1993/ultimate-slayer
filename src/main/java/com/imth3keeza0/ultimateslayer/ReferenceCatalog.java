package com.imth3keeza0.ultimateslayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;


/** Versioned bundled data only. Never contacts the Wiki or uploads account data. */
public final class ReferenceCatalog
{
    private static final ReferenceCatalog INSTANCE = new ReferenceCatalog();
    public static ReferenceCatalog get() { return INSTANCE; }
    public final Map<Integer, Equipment> equipment;
    public final List<Monster> monsters;
    public final Map<Integer, Map<String, Integer>> requirements;
    private final Map<String, List<Monster>> byName;
    public final Map<String, List<String>> tasks;
    public final String error;
    public final Map<String, String> spells;

    public static final class Equipment
    {
        public final int id, speed, stab, slash, crush;
        public final String name, category, slot, version;
        Equipment(JsonObject o)
        {
            id = number(o, "id"); speed = number(o, "speed"); name = string(o, "name");
            category = string(o, "category"); slot = string(o, "slot"); version = string(o, "version");
            JsonObject attack = o.getAsJsonObject("offensive");
            stab = number(attack, "stab"); slash = number(attack, "slash"); crush = number(attack, "crush");
        }
    }
    public static final class Monster
    {
        public final int id, level, hp, defence, magic, speed, stab, slash, crush, light, standard, heavy, magicDefence, armour;
        public final String name, version, weakness, weaknessElement;
        public final int weaknessPercent;
        public final java.util.Set<String> attributes;
        public final boolean ordinaryCombat;
        Monster(JsonObject o)
        {
            id = number(o, "id"); level = number(o, "level"); name = string(o, "name"); version = string(o, "version"); speed = number(o, "speed");
            JsonObject skills = o.getAsJsonObject("skills"), d = o.getAsJsonObject("defensive");
            hp = number(skills, "hp"); defence = number(skills, "def"); magic = number(skills, "magic");
            stab = number(d, "stab"); slash = number(d, "slash"); crush = number(d, "crush");
            light = number(d, "light"); standard = number(d, "standard"); heavy = number(d, "heavy"); magicDefence = number(d, "magic"); armour = number(d, "flat_armour");
            JsonObject w = o.has("weakness") && o.get("weakness").isJsonObject() ? o.getAsJsonObject("weakness") : null;
            java.util.Set<String> tags = new java.util.HashSet<>();
            if (o.has("attributes")) { for (JsonElement tag : o.getAsJsonArray("attributes")) { tags.add(tag.getAsString().toLowerCase(Locale.ROOT)); } }
            attributes = java.util.Set.copyOf(tags);
            weaknessElement = w == null ? "" : string(w, "element"); weaknessPercent = w == null ? 0 : number(w, "severity");
            ordinaryCombat = java.util.Set.of("abyssal demon","aberrant spectre","deviant spectre","banshee","twisted banshee","bloodveld","mutated bloodveld","dust devil","smoke devil","nechryael","greater nechryael","gargoyle","hydra","wyrm","drake","cave horror","cave crawler","cockatrice","basilisk","basilisk knight","rockslug","wall beast","turoth","kurask","skeletal wyvern","spitting wyvern","long-tailed wyvern","taloned wyvern","ancient wyvern","cave bug","cave slime","dark beast","brine rat","warped terrorbird","warped tortoise","desert lizard","hellhound","black demon","greater demon","lesser demon","red dragon","blue dragon","green dragon","black dragon","bronze dragon","iron dragon","steel dragon","frost dragon","adamant dragon","rune dragon","brutal black dragon","brutal blue dragon","brutal red dragon","goblin","cow","chicken","zombie","skeleton","hill giant","moss giant","fire giant","ice giant","troll","ice troll","ankou","dagannoth","suqah","kalphite worker","kalphite soldier","kalphite guardian","guard","man","woman","giant spider","scorpion","giant rat","giant bat","rat","wolf","bear","giant frog").contains(name.toLowerCase(Locale.ROOT))
                && !version.toLowerCase(Locale.ROOT).matches(".*(quest|deadman|league|beta|challenge|hard mode).*" );
            weakness = w == null ? "No elemental weakness recorded" : string(w, "element") + " " + number(w, "severity") + "%";
        }
        @Override public String toString() { return name + (version.isEmpty() ? "" : " — " + version) + " (lvl " + level + ", #" + id + ")"; }
        public String summary()
        {
            return "HP " + hp + " · Defence " + defence + " · Magic " + magic + "\nDefence bonuses: stab " + stab + ", slash " + slash + ", crush " + crush
                + "; ranged light/standard/heavy " + light + "/" + standard + "/" + heavy + "; magic " + magicDefence
                + "\nElemental weakness: " + weakness + ". Flat armour " + armour + ".\nReference stats only; phase/scaling and special immunities may change the fight.";
        }
    }
    private ReferenceCatalog()
    {
        Map<Integer, Equipment> items = new HashMap<>(); List<Monster> npcs = new ArrayList<>();
        Map<Integer, Map<String, Integer>> req = new HashMap<>(); Map<String, List<String>> taskData = new LinkedHashMap<>(); Map<String, String> spellData = new java.util.TreeMap<>(); String problem = "";
        try
        {
            for (JsonElement e : read("equipment").getAsJsonArray()) { Equipment item = new Equipment(e.getAsJsonObject()); if (item.id > 0) { items.put(item.id, item); } }
            for (JsonElement e : read("monsters").getAsJsonArray()) { npcs.add(new Monster(e.getAsJsonObject())); }
            for (Map.Entry<String, JsonElement> entry : read("requirements").getAsJsonObject().entrySet())
            {
                Map<String, Integer> levels = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> level : entry.getValue().getAsJsonObject().entrySet()) { levels.put(level.getKey(), level.getValue().getAsInt()); }
                req.put(Integer.parseInt(entry.getKey()), Map.copyOf(levels));
            }
            for (JsonElement e : read("tasks").getAsJsonArray())
            {
                JsonObject task = e.getAsJsonObject(); List<String> targets = new ArrayList<>();
                for (JsonElement target : task.getAsJsonArray("targets")) { targets.add(target.getAsString()); }
                taskData.put(string(task, "name"), List.copyOf(targets));
            }
            for (JsonElement e : read("spells").getAsJsonArray()) { JsonObject spell = e.getAsJsonObject(); spellData.put(string(spell, "name"), string(spell, "spellbook")); }
            if (items.size() < 1000 || npcs.size() < 1000) { throw new IllegalStateException("Incomplete bundled catalog"); }
        }
        catch (RuntimeException e) { items.clear(); npcs.clear(); req.clear(); problem = "Reference data unavailable; live item suggestions remain available."; }
        equipment = Map.copyOf(items); monsters = List.copyOf(npcs); requirements = Map.copyOf(req); error = problem; tasks = Collections.unmodifiableMap(taskData); spells = Collections.unmodifiableMap(spellData);
        Map<String, List<Monster>> index = new HashMap<>();
        for (Monster monster : monsters) { index.computeIfAbsent(normalize(monster.name), ignored -> new ArrayList<>()).add(monster); }
        index.replaceAll((key, value) -> List.copyOf(value)); byName = Map.copyOf(index);
    }
    private static JsonElement read(String name)
    {
        InputStream stream = ReferenceCatalog.class.getResourceAsStream("data/" + name + ".json");
        if (stream == null) { throw new IllegalStateException("Missing bundled " + name); }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) { return new JsonParser().parse(reader); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    static String normalize(String name)
    { return name == null ? "" : name.toLowerCase(Locale.ROOT).trim().replaceFirst("^the ", ""); }
    private static String singular(String name)
    { return name.endsWith("ies") ? name.substring(0, name.length() - 3) + "y" : name.endsWith("s") && !name.endsWith("ss") ? name.substring(0, name.length() - 1) : name; }
    public List<Monster> forTask(String name)
    {
        String key = normalize(name); TreeSet<String> targets = new TreeSet<>(); targets.add(key); targets.add(singular(key));
        for (Map.Entry<String, List<String>> task : tasks.entrySet())
        {
            if (normalize(task.getKey()).equals(key) || singular(normalize(task.getKey())).equals(singular(key)))
            { for (String target : task.getValue()) { targets.add(normalize(target)); } }
        }
        List<Monster> result = new ArrayList<>();
        for (Monster monster : monsters)
        {
            String candidate = normalize(monster.name);
            if (targets.contains(candidate) || targets.contains(singular(candidate))) { result.add(monster); }
        }
        return List.copyOf(result);
    }
    public List<Monster> search(String query, int limit)
    {
        String key = normalize(query); if (key.length() < 2) { return List.of(); }
        List<Monster> results = new ArrayList<>();
        for (Monster monster : monsters) { if ((normalize(monster.name) + " " + normalize(monster.version)).contains(key)) { results.add(monster); if (results.size() >= limit) { break; } } }
        return List.copyOf(results);
    }
    public String[] taskNames()
    {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(tasks.keySet());
        for (Monster monster : monsters) { names.add(monster.name); }
        names.remove("Current task"); List<String> result = new ArrayList<>(); result.add("Current task"); result.addAll(names); return result.toArray(new String[0]);
    }
    private static int number(JsonObject object, String name) { return object == null || !object.has(name) || object.get(name).isJsonNull() ? 0 : object.get(name).getAsInt(); }
    private static String string(JsonObject object, String name) { return object == null || !object.has(name) || object.get(name).isJsonNull() ? "" : object.get(name).getAsString(); }
}
