package com.imth3keeza0.ultimateslayer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class UltimateSlayerLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(UltimateSlayerPlugin.class);
        RuneLite.main(args);
    }
}
