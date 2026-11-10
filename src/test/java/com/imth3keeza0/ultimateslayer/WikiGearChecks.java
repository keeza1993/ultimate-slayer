package com.imth3keeza0.ultimateslayer;

import java.awt.Container;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Regression cases for the reported Araxxor bank and task/profile lifecycle. */
public final class WikiGearChecks
{
    public static void main(String[] args) throws Exception
    {
        String html = "<h2>Equipment</h2><table><caption>Recommended equipment for Melee</caption>"
            + row("Head slot", "Slayer helmet (i)") + row("Weapon slot", "Abyssal bludgeon", "Zombie axe")
            + row("Shield slot", "Dragon defender") + row("Special attack", "Bandos godsword") + "</table>"
            + "<table><caption>Recommended equipment for Ranged</caption>" + row("Head slot", "Slayer helmet (i)")
            + row("Weapon slot", "Twisted bow") + row("Ammo slot", "Dragon arrow") + "</table>";
        WikiStrategy.Profile parsed = WikiStrategy.parse("Araxxor/Strategies", html, 123);
        require(parsed.methods.size() == 2, "Separate Wiki strategies retain their boundaries");
        require(parsed.methods.get(0).slots.get(3).equals(List.of("Abyssal bludgeon", "Zombie axe")), "Icon and text links deduplicate without reordering");
        require(parsed.methods.get(0).specials.equals(List.of("Bandos godsword")), "Special weapons cannot leak into main-weapon row");
        require(parsed.methods.get(1).style.equals("Ranged"), "Style is inferred from the table caption");
        require(WikiStrategy.parse("Unknown", "<table>" + row("Weapon slot", "Bandos godsword") + "</table>", 123).methods.isEmpty(), "Unlabelled tables fail closed");
        WikiStrategy.Profile restored = WikiStrategy.fromJson(WikiStrategy.toJson(parsed), "cache");
        require(restored.methods.get(0).slots.equals(parsed.methods.get(0).slots), "Public cache round trip preserves slots and order");
        require(!WikiStrategyService.sameFamily("Araxxor", "Araxytes"), "Boss never silently inherits ordinary task strategy");
        require(WikiStrategyService.sameFamily("Hydra", "Hydras"), "Ordinary singular monster can use task page");

        WikiStrategy.Method araxxor = WikiStrategy.bundled("Araxxor").methods.get(0);
        OwnedGearPlanner.Gear bludgeon = gear(13263, "Abyssal bludgeon", 3, true, 85);
        OwnedGearPlanner.Gear axe = gear(28810, "Zombie axe", 3, false, 107);
        OwnedGearPlanner.Gear godsword = gear(11804, "Bandos godsword", 3, true, 132);
        OwnedGearPlanner.Gear defender = gear(12954, "Dragon defender", 5, false, 6);
        List<OwnedGearPlanner.Gear> bank = List.of(godsword, axe, bludgeon, defender);
        OwnedGearPlanner.Draft draft = plan(bank, araxxor, "Araxytes", CombatChecks.account(99,0));
        require(draft.selected.get(3) == bludgeon, "Owned bludgeon wins over higher raw-strength BGS");
        require(draft.alternatives.get(3).equals(List.of(bludgeon, axe)), "Exactly two suitable owned choices; no invented third");
        require(!draft.selected.containsKey(5) && !draft.alternatives.containsKey(5), "Two-handed main clears offhand and its highlights");
        require(WikiGearPlanner.specials(bank, araxxor, CombatChecks.account(99,0)).equals(List.of(godsword)), "Owned BGS is listed separately for special attacks");
        require(plan(List.of(axe, defender), araxxor, "Araxytes", CombatChecks.account(99,0)).selected.get(5) == defender, "One-handed fallback includes owned defender");
        require(!plan(List.of(gear(10,"Broken zombie axe",3,false,200)), araxxor,"Araxytes",CombatChecks.account(99,0)).selected.containsKey(3), "Broken state never recommended");
        require(araxxor.rank(gear(11,"Red slayer helmet (i)",0,false,0)) == 0, "Recoloured imbued helmet matches Wiki family");
        require(araxxor.rank(gear(12,"Slayer helmet",0,false,0)) == -1, "Unimbued state is not silently equated to imbued");
        Map<Skill,Integer> low = new EnumMap<>(Skill.class); low.put(Skill.ATTACK,1); low.put(Skill.STRENGTH,1);
        require(!plan(bank,araxxor,"Araxytes",new AccountContext(low,Map.of())).selected.containsKey(3), "Known unmet level excludes both main-weapon choices");
        WikiStrategy.Method protectedMethod = new WikiStrategy.Method("Melee", Map.of(3,List.of("Zombie axe","Abyssal bludgeon"),5,List.of("Dragon defender")),List.of());
        OwnedGearPlanner.Gear mirror = gear(ItemID.SLAYER_MIRROR_SHIELD,"Mirror shield",5,false,0);
        OwnedGearPlanner.Draft protectedDraft = plan(List.of(axe,bludgeon,defender,mirror),protectedMethod,"Basilisks",CombatChecks.account(99,0));
        require(protectedDraft.selected.get(3) == axe && protectedDraft.selected.get(5) == mirror && !protectedDraft.protectionMissing, "Required protection wins over Wiki offhand, excluding two-handed weapons");
        require(plan(List.of(axe),protectedMethod,"Basilisks",CombatChecks.account(99,0)).protectionMissing, "Missing required protection stays explicit");

        AtomicReference<Map<Integer,Integer>> ranks = new AtomicReference<>(Map.of());
        SwingUtilities.invokeAndWait(() -> {
            UltimateSlayerPanel panel = new UltimateSlayerPanel(new UltimateSlayerConfig() {}, () -> {});
            try
            {
                panel.setRankListener(ranks::set); panel.setAccountContext(CombatChecks.account(99,0));
                panel.update(new SidebarState("Araxxor",null,100,true,new int[14],new int[28],new int[28]),Map.of());
                panel.setOwnedGear(new OwnedGearCatalog(123,bank,Map.of(),0));
                require(ranks.get().get(bludgeon.id) == 1 && ranks.get().get(axe.id) == 2 && !ranks.get().containsKey(godsword.id), "Bank colours use the displayed owned strategy ranking");
                render(panel);
                panel.update(new SidebarState("No active Slayer task",null,0,true,new int[14],new int[28],new int[28]),Map.of());
                require(ranks.get().isEmpty(), "Task completion removes bank ranks");
                panel.resetAccountContext(); panel.refreshAppearance(); require(ranks.get().isEmpty(), "Account transition cannot restore previous bank ranks");
            }
            finally { panel.disposePanel(); }
        });
        System.out.println("Wiki parser, Araxxor owned gear, protection, special role, cache and bank rank lifecycle checks passed.");
    }
    private static String row(String slot, String... items)
    {
        StringBuilder value = new StringBuilder("<tr><th><img alt=\"").append(slot).append("\"></th>");
        for (String item : items) { value.append("<td><a title=\"").append(item).append("\" href=\"/w/Item\"><img alt=\"icon\"></a><a title=\"").append(item).append("\" href=\"/w/Item\">").append(item).append("</a></td>"); }
        return value.append("</tr>").toString();
    }
    private static OwnedGearPlanner.Draft plan(List<OwnedGearPlanner.Gear> bank, WikiStrategy.Method method, String task, AccountContext account)
    { return WikiGearPlanner.plan(bank,method,task,TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,account,null); }
    private static OwnedGearPlanner.Gear gear(int id,String name,int slot,boolean twoHand,int str)
    { return new OwnedGearPlanner.Gear(id,id,name,slot,twoHand,100,0,0,str,0,0); }
    private static void require(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void render(UltimateSlayerPanel panel)
    {
        try
        {
            panel.setSize(225,panel.getPreferredSize().height); layout(panel);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(225,panel.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            javax.imageio.ImageIO.write(image,"png",new java.io.File("build/wiki-gear-preview.png"));
        }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    private static void layout(Container container)
    { container.doLayout(); for (java.awt.Component child : container.getComponents()) { if (child instanceof Container) { layout((Container)child); } } }
}
