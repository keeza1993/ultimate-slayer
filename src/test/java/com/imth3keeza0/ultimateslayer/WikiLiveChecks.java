package com.imth3keeza0.ultimateslayer;

/** Optional external availability probe, separate from deterministic offline checks. */
public final class WikiLiveChecks
{
    public static void main(String[] args) throws Exception
    {
        okhttp3.OkHttpClient http = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(7, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        try { verify(http); }
        finally
        {
            http.dispatcher().executorService().shutdownNow();
            http.connectionPool().evictAll();
        }
    }

    private static void verify(okhttp3.OkHttpClient http) throws Exception
    {
        WikiStrategy.Profile live;
        try { live = WikiStrategyService.fetch(http, "Araxxor/Strategies"); }
        catch (java.io.IOException error)
        { System.out.println("LIVE_WIKI_UNAVAILABLE: " + error.getMessage() + "; offline baseline remains available."); return; }
        if (live.methods.isEmpty()) { throw new AssertionError("Live Wiki returned HTML but no supported equipment table"); }
        boolean found = false;
        for (WikiStrategy.Method method : live.methods)
        {
            if (!method.style.equals("Melee")) { continue; }
            java.util.List<String> main = method.slots.getOrDefault(3,java.util.List.of());
            if (WikiStrategy.rankName(main,"Abyssal bludgeon") < 0 || WikiStrategy.rankName(main,"Zombie axe") < 0)
            { throw new AssertionError("Live Araxxor main-weapon table is incomplete"); }
            if (WikiStrategy.rankName(main,"Bandos godsword") >= 0 || WikiStrategy.rankName(method.specials,"Bandos godsword") < 0)
            { throw new AssertionError("Live Wiki special-attack row was not separated"); }
            found = true;
        }
        if (!found) { throw new AssertionError("Live Araxxor melee method missing"); }
        if (live.inventory.stream().noneMatch(n -> WikiStrategy.normalize(n).contains("anti-venom")))
        { throw new AssertionError("Actual Wiki inventory venom supplies were not captured"); }
        System.out.println("LIVE_WIKI_PARSED: " + live.methods.size() + " actual equipment tables; Araxxor main and special rows validated.");
    }
}

