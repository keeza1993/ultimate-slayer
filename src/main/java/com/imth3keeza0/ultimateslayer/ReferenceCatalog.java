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
        public final int id, speed;
        public final String category, version;
        Equipment(JsonObject o)
        {
            id = number(o, "id"); speed = number(o, "speed");
            category = string(o, "category"); version = string(o, "version");
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
            ordinaryCombat = java.util.Set.of("abyssal demon","aberrant spectre","deviant spectre","banshee","twisted banshee","bloodveld","mutated bloodveld","dust devil","smoke devil","nechryael","greater nechryael","gargoyle","hydra","wyrm","drake","cave horror","cave crawler","cockatrice","basilisk","basilisk knight","rockslug","wall beast","turoth","kurask","skeletal wyvern","spitting wyvern","long-tailed wyvern","taloned wyvern","ancient wyvern","cave bug","cave slime","dark beast","brine rat","warped terrorbird","warped tortoise","desert lizard","hellhound","black demon","greater demon","lesser demon","red dragon","blue dragon","green dragon","black dragon","bronze dragon","iron dragon","steel dragon","frost dragon","adamant dragon","rune dragon","brutal black dragon","brutal blue dragon","brutal red dragon","goblin","cow","chicken","zombie","skeleton","hill giant","moss giant","fire giant","ice giant","troll","ice troll","ankou","dagannoth","suqah","kalphite worker","kalphite soldier","kalphite guardian","guard","man","woman","giant spider","scorpion","giant rat","giant bat","rat","wolf","bear","giant frog","crawling hand","jelly","twisted jelly","ogre","ogress warrior","ogress shaman","hobgoblin","rock crab","sand crab","ammonite crab","giant rock crab","baby blue dragon","baby green dragon","baby red dragon","baby black dragon","mammoth","ice warrior","chaos dwarf","dwarf","earth warrior","chaos druid","dark warrior","pirate","bandit","rogue","thug","monk of zamorak","minotaur","flesh crawler","catablepon").contains(name.toLowerCase(Locale.ROOT))
                && !version.toLowerCase(Locale.ROOT).matches(".*(quest|deadman|league|beta|challenge|hard mode|unused|hidden|nightmare zone).*" );
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
            JsonObject equipmentData = read("equipment").getAsJsonObject();
            for (JsonElement group : equipmentData.getAsJsonArray("groups"))
            {
                JsonArray g = group.getAsJsonArray(), p = equipmentData.getAsJsonArray("profiles").get(g.get(0).getAsInt()).getAsJsonArray();
                for (JsonElement id : g.get(1).getAsJsonArray())
                {
                    JsonObject row = new JsonObject(); row.add("id", id); row.add("speed", p.get(0)); row.add("category", p.get(1)); row.add("version", p.get(2));
                    Equipment item = new Equipment(row); items.put(item.id, item);
                }
            }
            JsonObject monsterData = read("monsters").getAsJsonObject();
            for (JsonElement e : monsterData.getAsJsonArray("rows")) { npcs.add(new Monster(expandMonster(monsterData, e.getAsJsonArray()))); }
            JsonObject requirementData = read("requirements").getAsJsonObject();
            for (JsonElement group : requirementData.getAsJsonArray("groups"))
            {
                JsonArray g = group.getAsJsonArray(); Map<String, Integer> levels = new LinkedHashMap<>();
                JsonObject profile = requirementData.getAsJsonArray("profiles").get(g.get(0).getAsInt()).getAsJsonObject();
                for (Map.Entry<String, JsonElement> level : profile.entrySet()) { levels.put(level.getKey(), level.getValue().getAsInt()); }
                Map<String, Integer> immutable = Map.copyOf(levels);
                for (JsonElement id : g.get(1).getAsJsonArray()) { req.put(id.getAsInt(), immutable); }
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
    /** Schema 2 is transparent dictionary normalization; no encoded or executable data. */
    private static JsonObject expandMonster(JsonObject data, JsonArray row)
    {
        JsonObject o = new JsonObject(); o.add("id", row.get(0));
        o.add("name", data.getAsJsonArray("names").get(row.get(1).getAsInt()));
        o.add("version", data.getAsJsonArray("versions").get(row.get(2).getAsInt()));
        JsonArray p = data.getAsJsonArray("profiles").get(row.get(3).getAsInt()).getAsJsonArray();
        o.add("level", p.get(0)); o.add("speed", p.get(1));
        JsonObject skills = new JsonObject(); skills.add("hp", p.get(2)); skills.add("def", p.get(3)); skills.add("magic", p.get(4)); o.add("skills", skills);
        JsonArray d = data.getAsJsonArray("defenses").get(p.get(5).getAsInt()).getAsJsonArray();
        String[] keys = {"stab", "slash", "crush", "light", "standard", "heavy", "magic", "flat_armour"};
        JsonObject defense = new JsonObject(); for (int i = 0; i < keys.length; i++) { defense.add(keys[i], d.get(i)); } o.add("defensive", defense);
        o.add("attributes", data.getAsJsonArray("attributes").get(p.get(6).getAsInt()));
        o.add("weakness", data.getAsJsonArray("weaknesses").get(p.get(7).getAsInt()));
        return o;
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
        // The bundled task aliases omit the boss that can fulfil a hydra assignment.
        if (singular(key).equals("hydra")) { targets.add("alchemical hydra"); }
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
