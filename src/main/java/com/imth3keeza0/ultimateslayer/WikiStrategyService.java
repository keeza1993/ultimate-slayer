package com.imth3keeza0.ultimateslayer;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import net.runelite.client.util.Filepath;

/** Bounded background reads from a fixed public Wiki host. Never receives bank/account data. */
public final class WikiStrategyService implements AutoCloseable
{
    private static final long TTL = TimeUnit.DAYS.toMillis(7);
    private final BankSnapshotController.Directory directory;
    private final Map<String, WikiStrategy.Profile> memory = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 20, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1), action -> { Thread t = new Thread(action, "ultimate-slayer-wiki"); t.setDaemon(true); return t; }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile boolean closed;
    public WikiStrategyService(BankSnapshotController.Directory directory) { this.directory = directory; }
    public void load(String target, String task, boolean online, boolean force, Consumer<WikiStrategy.Profile> callback)
    {
        if (closed) { return; }
        worker.execute(() -> {
            WikiStrategy.Profile result = loadNow(target, task, online, force);
            if (!closed) { callback.accept(result); }
        });
    }
    private WikiStrategy.Profile loadNow(String target, String task, boolean online, boolean force)
    {
        String key = WikiStrategy.normalize(target);
        WikiStrategy.Profile cached = memory.get(key);
        if (cached == null)
        {
            try
            {
                Filepath path = path(key);
                if (path.exists() && path.size() < 512_000)
                {
                    try (Reader reader = path.openBufferedReader())
                    {
                        JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                        cached = WikiStrategy.fromJson(root, "Cached public Wiki table.");
                        if (!root.has("inventory") && online) { cached = null; }
                    }
                }
            }
            catch (IOException | RuntimeException ignored) { /* A missing/corrupt cache does not block planning. */ }
        }
        if (cached == null) { cached = WikiStrategy.bundled(target); }
        if (cached != null && !force && (!online || System.currentTimeMillis() - cached.fetchedAt < TTL))
        { memory.put(key, cached); return cached; }
        if (!online) { return cached != null ? cached : unavailable(target, "No cached strategy. Enable Wiki table updates or open the source page."); }
        LinkedHashSet<String> pages = new LinkedHashSet<>();
        pages.add(target + "/Strategies");
        // Task pages describe the ordinary task, never silently substitute them for a selected boss.
        if (sameFamily(target, task))
        {
            String canonical = task;
            for (String name : ReferenceCatalog.get().tasks.keySet()) { if (name.equalsIgnoreCase(task)) { canonical = name; break; } }
            pages.add("Slayer task/" + canonical);
        }
        pages.add(target);
        for (String page : pages)
        {
            if (closed || Thread.currentThread().isInterrupted()) { break; }
            try
            {
                WikiStrategy.Profile found = fetch(page);
                if (!found.methods.isEmpty())
                {
                    memory.put(key, found);
                    try
                    {
                        Filepath root = directory.get().joinSegment("wiki-strategies"); root.createDirectories();
                        Filepath temp = root.createTempFile("wiki-", ".tmp");
                        try { temp.write(WikiStrategy.toJson(found).toString()); temp.moveTo(path(key), StandardCopyOption.REPLACE_EXISTING); }
                        finally { temp.deleteIfExists(); }
                    }
                    catch (IOException | RuntimeException ignored) { /* Session result is still usable. */ }
                    return found;
                }
            }
            catch (IOException | RuntimeException ignored) { /* Try another public page, then labelled cache. */ }
        }
        if (cached != null)
        {
            WikiStrategy.Profile fallback = cached.withStatus("Wiki update unavailable; using cached/bundled baseline. Review source notes.");
            memory.put(key, fallback); return fallback;
        }
        // Negative results are cached for this session to prevent retrying on every sidebar rebuild.
        WikiStrategy.Profile missing = unavailable(target, "Wiki table unavailable or unsupported. No task-specific bank highlights; use the source page or retry.");
        memory.put(key, missing); return missing;
    }
    static boolean sameFamily(String target, String task)
    {
        String a = WikiStrategy.normalize(target), b = WikiStrategy.normalize(task == null ? "" : task);
        return a.equals(b) || a.equals(b.replaceFirst("s$", "")) || a.replaceFirst("s$", "").equals(b);
    }
    private static WikiStrategy.Profile unavailable(String target, String message)
    { return new WikiStrategy.Profile(target, System.currentTimeMillis(), List.of(), message); }
    private Filepath path(String key) throws IOException
    {
        String filename = Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        return directory.get().joinSegment("wiki-strategies").joinSegment(filename + ".json");
    }
    static WikiStrategy.Profile fetch(String title) throws IOException
    {
        String query = "https://oldschool.runescape.wiki/api.php?action=parse&format=json&formatversion=2&redirects=1&prop=text&page="
            + URLEncoder.encode(title, StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(query).openConnection();
        connection.setConnectTimeout(5000); connection.setReadTimeout(7000); connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "UltimateSlayer/0.9.0 (RuneLite equipment planning; github.com/keeza1993/ultimate-slayer)");
        try
        {
            if (connection.getResponseCode() != 200) { throw new IOException("Wiki request unavailable"); }
            byte[] bytes;
            try (InputStream input = connection.getInputStream()) { bytes = input.readNBytes(2_000_001); }
            if (bytes.length > 2_000_000) { throw new IOException("Wiki response too large"); }
            JsonObject root = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("parse")) { throw new IOException("No Wiki parse result"); }
            JsonObject parsed = root.getAsJsonObject("parse");
            String actualTitle = parsed.get("title").getAsString();
            return WikiStrategy.parse(actualTitle, parsed.get("text").getAsString(), System.currentTimeMillis());
        }
        finally { connection.disconnect(); }
    }
    @Override public void close() { closed = true; worker.shutdownNow(); memory.clear(); }
}

