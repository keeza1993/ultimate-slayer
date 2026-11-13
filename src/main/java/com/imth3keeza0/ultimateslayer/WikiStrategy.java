package com.imth3keeza0.ultimateslayer;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;

/** Public Wiki equipment tables only. No player data or executable HTML is retained. */
public final class WikiStrategy
{
    private WikiStrategy() {}
    public static final class Method
    {
        public final String name, style;
        public final Map<Integer, List<String>> slots;
        public final List<String> specials;
        public Method(String name, Map<Integer, List<String>> slots, List<String> specials)
        {
            this.name = name;
            String n = normalize(name);
            style = n.contains("magic") || n.contains("barrage") ? "Magic" : n.contains("ranged") ? "Ranged" : "Melee";
            Map<Integer, List<String>> copy = new LinkedHashMap<>();
            slots.forEach((k,v) -> copy.put(k, List.copyOf(v)));
            this.slots = Collections.unmodifiableMap(copy); this.specials = List.copyOf(specials);
        }
        @Override public String toString() { return name; }
        public int rank(OwnedGearPlanner.Gear item)
        { return rankName(slots.getOrDefault(item.slot, List.of()), item.name); }
    }
    public static final class Profile
    {
        public final String title, status;
        public final long fetchedAt;
        public final List<Method> methods;
        public final List<String> inventory;
        public Profile(String title, long fetchedAt, List<Method> methods, String status)
        { this(title, fetchedAt, methods, status, List.of()); }
        public Profile(String title, long fetchedAt, List<Method> methods, String status, List<String> inventory)
        { this.title = title; this.fetchedAt = fetchedAt; this.methods = List.copyOf(methods); this.status = status; this.inventory = List.copyOf(inventory); }
        public String url() { return "https://oldschool.runescape.wiki/w/" + java.net.URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8); }
        public Profile withStatus(String next) { return new Profile(title, fetchedAt, methods, next, inventory); }
    }
    static String normalize(String name)
    { return name.toLowerCase(Locale.ROOT).replace('\u2019', '\'').replace('\u00a0', ' ').replaceAll("\\s+", " ").trim(); }
    static int rankName(List<String> names, String name)
    {
        String key = normalize(name);
        // Only known cosmetic/degradation suffixes; never collapse imbued, inactive or empty states.
        String cosmetic = key.replaceFirst(" \\((or|l|g|t)\\)$", "").replaceFirst(" (100|75|50|25)$", "");
        for (int i = 0; i < names.size(); i++)
        {
            String candidate = normalize(names.get(i));
            if (candidate.equals(key) || candidate.equals(cosmetic)) { return i; }
            if (candidate.equals("slayer helmet (i)") && key.matches(".*slayer helmet \\(i\\).*")) { return i; }
            if (candidate.equals("slayer helmet") && key.endsWith("slayer helmet")) { return i; }
            if (candidate.equals("black mask (i)") && key.matches("black mask( \\(\\d+\\))? \\(i\\)")) { return i; }
            if (candidate.equals("imbued god cape") && key.matches("imbued (saradomin|guthix|zamorak|max) cape")) { return i; }
            if (candidate.equals("god blessing") && key.matches("(holy|unholy|peaceful|honourable|war|ancient) blessing")) { return i; }
            if (candidate.equals("bow of faerdhinen") && key.startsWith("bow of faerdhinen (c)")) { return i; }
        }
        return -1;
    }
    /** Parse actual table/row/cell structure, preserving method and slot boundaries. */
    public static Profile parse(String title, String html, long timestamp) throws IOException
    {
        List<Method> methods = new ArrayList<>();
        LinkedHashSet<String> inventory = new LinkedHashSet<>();
        new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback()
        {
            int depth, cellIndex = -1, slot = -2;
            boolean caption, inCell, inRow;
            boolean inventorySection, inHeading;
            int listDepth;
            String method = "", recent = "", heading = "";
            StringBuilder text = new StringBuilder(), captionText = new StringBuilder();
            LinkedHashSet<String> links = new LinkedHashSet<>();
            Map<Integer, List<String>> slots = new LinkedHashMap<>();
            List<String> specials = new ArrayList<>();
            String attribute(MutableAttributeSet a, HTML.Attribute key) { Object v = a.getAttribute(key); return v == null ? "" : v.toString(); }
            void identify(String value)
            {
                String n = normalize(value).replace('_',' ');
                if (n.contains("special attack")) { slot = -1; return; }
                if (n.equals("one-handed weapon") || n.equals("two-handed weapon") || n.equals("weapon slot")) { slot = 3; return; }
                String[] names = {"head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "feet", "ring", "ammo"};
                int[] ids = {0,1,2,3,4,5,7,9,10,12,13};
                for (int i = 0; i < names.length; i++)
                { if (n.equals(names[i]) || n.equals(names[i] + " slot") || n.equals(names[i] + " slot icon")) { slot = ids[i]; return; } }
                if (n.equals("ammunition slot")) { slot = 13; }
            }
            @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet a, int pos)
            {
                if (tag == HTML.Tag.TABLE)
                {
                    if (depth++ == 0) { method = ""; slots = new LinkedHashMap<>(); specials = new ArrayList<>(); }
                    return;
                }
                if (depth == 0)
                {
                    if (tag == HTML.Tag.H2 || tag == HTML.Tag.H3 || tag == HTML.Tag.H4) { heading = ""; inHeading = true; }
                    if (tag == HTML.Tag.UL || tag == HTML.Tag.OL) { listDepth++; }
                    if (inventorySection && listDepth > 0 && tag == HTML.Tag.A && inventory.size() < 40)
                    {
                        String item = attribute(a, HTML.Attribute.TITLE), href = attribute(a, HTML.Attribute.HREF);
                        if (!item.isBlank() && !item.contains(":") && !href.contains("#") && !href.contains("redlink=1")) { inventory.add(item); }
                    }
                    return;
                }
                if (depth != 1) { return; }
                if (tag == HTML.Tag.CAPTION) { caption = true; captionText.setLength(0); }
                if (tag == HTML.Tag.TR) { inRow = true; cellIndex = -1; slot = -2; }
                if (tag == HTML.Tag.TH || tag == HTML.Tag.TD) { inCell = true; cellIndex++; text.setLength(0); links.clear(); }
                if (tag == HTML.Tag.A && inCell)
                {
                    String target = attribute(a, HTML.Attribute.TITLE);
                    String href = attribute(a, HTML.Attribute.HREF);
                    if (cellIndex == 0) { identify(target); }
                    else if (!target.isEmpty() && !target.contains(":") && !href.contains("#") && !href.contains("redlink=1") && !target.endsWith(" slot"))
                    { links.add(target); }
                }
            }
            @Override public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet a, int pos)
            {
                if (depth == 1 && inCell && cellIndex == 0 && tag == HTML.Tag.IMG)
                {
                    identify(attribute(a, HTML.Attribute.ALT));
                    String src = attribute(a, HTML.Attribute.SRC);
                    if (src.contains("/")) { identify(src.substring(src.lastIndexOf('/')+1).replaceFirst("(?i)\\.png.*$", "").replace('_',' ')); }
                }
            }
            @Override public void handleText(char[] chars, int pos)
            {
                String value = new String(chars);
                if (depth == 0) { if (inHeading) { heading += value; } recent = (recent + " " + value); if (recent.length() > 180) { recent = recent.substring(recent.length()-180); } }
                if (caption) { captionText.append(value); }
                if (inCell && depth == 1) { text.append(value); }
            }
            @Override public void handleEndTag(HTML.Tag tag, int pos)
            {
                if (depth == 0)
                {
                    if (tag == HTML.Tag.UL || tag == HTML.Tag.OL) { listDepth = Math.max(0,listDepth-1); }
                    if (tag == HTML.Tag.H2 || tag == HTML.Tag.H3 || tag == HTML.Tag.H4)
                    { inventorySection = normalize(heading).startsWith("inventory") || normalize(heading).startsWith("recommended inventory"); inHeading = false; }
                }
                if (tag == HTML.Tag.CAPTION && depth == 1)
                { caption = false; String c = captionText.toString().trim(); if (normalize(c).contains("recommended equipment")) { method = c.replaceFirst("(?i).*recommended equipment(?: for)?\\s*", ""); } }
                if ((tag == HTML.Tag.TD || tag == HTML.Tag.TH) && depth == 1 && inCell)
                {
                    String cellText = text.toString().trim();
                    if (normalize(cellText).contains("recommended equipment for"))
                    { method = cellText.replaceFirst("(?is).*recommended equipment for\\s*", "").trim(); }
                    if (cellIndex == 0) { identify(text.toString()); }
                    else if (slot >= -1 && !links.isEmpty())
                    {
                        List<String> values = slot < 0 ? specials : slots.computeIfAbsent(slot, ignored -> new ArrayList<>());
                        for (String link : links) { if (!values.contains(link)) { values.add(link); } }
                    }
                    inCell = false;
                }
                if (tag == HTML.Tag.TR && depth == 1) { inRow = false; }
                if (tag == HTML.Tag.TABLE && depth > 0 && --depth == 0)
                {
                    // Wiki normally uses a caption; support its heading variant without guessing ordinary tables.
                    if (method.isEmpty() && normalize(recent).contains("recommended equipment for"))
                    { method = recent.replaceFirst("(?is).*recommended equipment for\\s*", "").trim(); }
                    if (!method.isEmpty() && slots.containsKey(3) && slots.size() >= 3)
                    { methods.add(new Method(method, slots, specials)); }
                    caption = false; inCell = false; recent = "";
                }
            }
        }, true);
        return new Profile(title, timestamp, methods, methods.isEmpty() ? "No supported Wiki equipment table found." : "Live Wiki equipment table; review conditional notes on the source page.", List.copyOf(inventory));
    }
    static JsonObject toJson(Profile profile)
    {
        JsonObject root = new JsonObject(); root.addProperty("title", profile.title); root.addProperty("fetchedAt", profile.fetchedAt);
        JsonArray methods = new JsonArray();
        for (Method method : profile.methods)
        {
            JsonObject row = new JsonObject(); row.addProperty("name", method.name); JsonObject slots = new JsonObject();
            method.slots.forEach((slot, names) -> slots.add(slot.toString(), jsonStrings(names)));
            row.add("slots", slots); row.add("specials", jsonStrings(method.specials)); methods.add(row);
        }
        root.add("methods", methods); root.add("inventory", jsonStrings(profile.inventory)); return root;
    }
    private static JsonArray jsonStrings(List<String> values)
    { JsonArray result=new JsonArray(); for(String value:values){result.add(value);} return result; }
    static Profile fromJson(JsonObject root, String status)
    {
        List<Method> methods = new ArrayList<>();
        for (JsonElement entry : root.getAsJsonArray("methods"))
        {
            JsonObject row = entry.getAsJsonObject(); Map<Integer, List<String>> slots = new LinkedHashMap<>();
            row.getAsJsonObject("slots").entrySet().forEach(e -> slots.put(Integer.parseInt(e.getKey()), strings(e.getValue().getAsJsonArray())));
            methods.add(new Method(row.get("name").getAsString(), slots, strings(row.getAsJsonArray("specials"))));
        }
        return new Profile(root.get("title").getAsString(), root.get("fetchedAt").getAsLong(), methods, status,
            root.has("inventory") ? strings(root.getAsJsonArray("inventory")) : List.of());
    }
    private static List<String> strings(JsonArray array)
    { List<String> result = new ArrayList<>(); for (JsonElement e : array) { result.add(e.getAsString()); } return result; }
    static Profile bundled(String target)
    {
        if (!normalize(target).equals("araxxor")) { return null; }
        try (Reader reader = new InputStreamReader(WikiStrategy.class.getResourceAsStream("data/araxxor-strategy.json"), StandardCharsets.UTF_8))
        { return fromJson(new JsonParser().parse(reader).getAsJsonObject(), "Bundled Wiki baseline, reviewed 10 Nov 2026. Conditional choices need source-page checks."); }
        catch (Exception ignored) { return null; }
    }
}

