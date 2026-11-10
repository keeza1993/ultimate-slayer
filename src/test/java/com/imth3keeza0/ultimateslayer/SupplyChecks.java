package com.imth3keeza0.ultimateslayer;

import java.util.*;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.gameval.ItemID;

/** Task hazards, stock provenance, shared travel resources and user priority regressions. */
public final class SupplyChecks
{
    public static void main(String[] args) throws Exception
    {
        BankSnapshot bank=new BankSnapshot(123,Map.of(ItemID._4DOSEPRAYERRESTORE,5L),Map.of(),Map.of());
        SidebarState live=state("Araxxor",Map.of(ItemID._2DOSEPRAYERRESTORE,1));
        List<SupplyPlanner.Need> araxxor=plan("Araxxor",live,bank,false,false);
        require(find(araxxor,"venom")!=null && find(araxxor,"fire")==null,"Araxxor asks for venom, not unrelated antifire");
        SupplyPlanner.Need prayer=find(araxxor,"prayer");
        require(prayer.carried(live)==2 && prayer.bank(bank)==20,"Live carried doses and capture-time bank doses are separate");
        require(SupplyPlanner.PRAYER.carried(state("Araxxor",Map.of(ItemID._2DOSEPRAYERRESTORE,2,ItemID._4DOSEPRAYERRESTORE,1)))==8,"All bottle doses are weighted");
        require(find(araxxor,"venom").carried(state("Araxxor",Map.of(ItemID._4DOSE2ANTIPOISON,1)))==0,"Superantipoison does not satisfy venom supplies");
        require(find(araxxor,"venom").carried(state("Araxxor",Map.of(ItemID.EXTENDED_ANTIVENOM_4,1)))==4,"Extended anti-venom+ counted by doses");
        List<SupplyPlanner.Need> steel=plan("Steel dragon",state("Steel dragons",Map.of(ItemID._4DOSE2ANTIDRAGON,1)),bank,false,false);
        require(find(steel,"fire").carried(state("Steel dragons",Map.of(ItemID._4DOSE2ANTIDRAGON,1)))==0,"Ordinary antifire alone does not satisfy the shield combination");
        BankSnapshot shieldBank=new BankSnapshot(123,Map.of(ItemID.ANTIDRAGONBREATHSHIELD,1L,ItemID._4DOSE2ANTIDRAGON,2L),Map.of(),Map.of());
        require(find(plan("Steel dragon",state("Steel dragons",Map.of()),shieldBank,false,false),"fire").bank(shieldBank)==8,"Owned shield permits the explicitly labelled ordinary-antifire alternative");
        List<SupplyPlanner.Need> wyverns=plan("Skeletal wyvern",state("Skeletal wyverns",Map.of()),bank,false,false);
        require(find(wyverns,"fire")==null && wyverns.stream().anyMatch(n->n.note.contains("icy-breath")),"Wyverns keep their shield requirement instead of antifire advice");
        SidebarState one=state("Araxxor",Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE,1));
        List<SupplyPlanner.Need> house=plan("Araxxor",one,bank,true,true);
        require(find(house,"out").packed(one) && !find(house,"return").packed(one),"One house tablet cannot cover both outbound and return");
        SidebarState two=state("Araxxor",Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE,2));
        require(find(plan("Araxxor",two,bank,true,true),"return").packed(two),"Two house tablets cover both uses");
        SidebarState ring=state("Araxxor",Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE,1,ItemID.RING_OF_DUELING_1,1));
        require(find(plan("Araxxor",ring,bank,true,true),"return").packed(ring),"Independent dueling ring can cover return");
        SidebarState runes=state("Araxxor",Map.of(ItemID.LAWRUNE,3,ItemID.RUNE_DUST,2));
        require(find(plan("Araxxor",runes,bank,true,true),"return").packed(runes),"Two loose-rune house casts can cover outbound and return");
        List<SupplyPlanner.Need> wrongBook=SupplyPlanner.plan("Araxxor","Araxxor",TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,"Melee",runes,bank,null,null,CombatChecks.account(99,1),true,true,true);
        require(!find(wrongBook,"return").packed(runes),"House spell not counted on another spellbook");
        SidebarState scroll=state("Araxxor",Map.of(ItemID.TELEPORTSCROLL_SPIDERCAVE,1));
        require(find(plan("Araxxor",scroll,bank,false,false),"fairy-staff")==null,"Direct cave scroll route does not demand an unnecessary staff");
        SidebarState recoloured=state("Dust devils",Map.of(ItemID.SLAYER_HELM_I_RED,1));
        require(plan("Dust devil",recoloured,bank,false,false).stream().anyMatch(n->n.key.startsWith("rule")&&n.carried(recoloured)>0),"Recoloured protective helmets satisfy carried supply checks");
        UltimateSlayerConfig priorities=new UltimateSlayerConfig(){@Override public SupplyPlanner.Priority prayerPriority(){return SupplyPlanner.Priority.LOW;}};
        List<SupplyPlanner.Need> sorted=SupplyPlanner.sorted(araxxor,priorities,live,bank,false);
        require(sorted.indexOf(find(sorted,"food"))<sorted.indexOf(find(sorted,"prayer")),"User category priorities change checklist order");
        require(SupplyPlanner.plan("Araxxor","Araxxor",TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,"Melee",SidebarState.empty(),bank,null,null,CombatChecks.account(99,0),true,true,true).isEmpty(),"Logout clears supply advice");
        String html="<h2>Inventory</h2><ul><li><a title='Extended anti-venom+' href='/w/Extended_anti-venom%2B'>Potion</a></li><li><a title='Super combat potion' href='/w/Super_combat_potion'>Boost</a></li></ul><h2>Fight</h2><ul><li><a title='Unrelated' href='/w/Unrelated'>No</a></li></ul>";
        WikiStrategy.Profile wiki=WikiStrategy.parse("Araxxor/Strategies",html,123);
        require(wiki.inventory.equals(List.of("Extended anti-venom+","Super combat potion")),"Wiki inventory stays in its section and is not confused with combat links");
        require(WikiStrategy.fromJson(WikiStrategy.toJson(wiki),"cache").inventory.equals(wiki.inventory),"Cached inventory links survive a round trip");
        OwnedGearCatalog catalog=new OwnedGearCatalog(123,List.of(),Map.of(),0,Set.of(ItemID._4DOSE2COMBAT),Map.of(ItemID._4DOSE2COMBAT,"Super combat potion(4)"));
        BankSnapshot boosts=new BankSnapshot(123,Map.of(ItemID._4DOSE2COMBAT,2L),Map.of(),Map.of());
        List<SupplyPlanner.Need> enriched=SupplyPlanner.plan("Araxxor","Araxxor",TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,"Melee",live,boosts,catalog,wiki,CombatChecks.account(99,0),false,false,true);
        require(enriched.stream().anyMatch(n->n.key.startsWith("wiki-")&&n.bank(boosts)==8),"Wiki-listed non-gear supplies match snapshot item metadata");
        SwingUtilities.invokeAndWait(()->{
            UltimateSlayerPanel panel=new UltimateSlayerPanel(new UltimateSlayerConfig(){},()->{});
            try{panel.setAccountContext(CombatChecks.account(99,0));panel.update(live,Map.of());panel.setPlanningSnapshot(bank);panel.setOwnedGear(catalog);render(panel);}
            finally{panel.disposePanel();}
        });
        System.out.println("Task supplies, dose variants, bank/live separation, travel reservation, Wiki inventory and priority checks passed.");
    }
    private static List<SupplyPlanner.Need> plan(String target,SidebarState state,BankSnapshot bank,boolean preferHouse,boolean houseRing)
    {return SupplyPlanner.plan(state.task,target,TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,"Melee",state,bank,null,null,CombatChecks.account(99,0),preferHouse,houseRing,true);}
    private static SupplyPlanner.Need find(List<SupplyPlanner.Need> needs,String key){return needs.stream().filter(n->n.key.equals(key)).findFirst().orElse(null);}
    private static SidebarState state(String task,Map<Integer,Integer> stock)
    {int[] worn=new int[14],items=new int[28],quantities=new int[28];Arrays.fill(worn,-1);Arrays.fill(items,-1);int i=0;for(Map.Entry<Integer,Integer> e:stock.entrySet()){items[i]=e.getKey();quantities[i++]=e.getValue();}return new SidebarState(task,null,50,true,worn,items,quantities);}
    private static void require(boolean value,String message){if(!value){throw new AssertionError(message);}}
    private static void render(UltimateSlayerPanel panel)
    {try{panel.setSize(225,panel.getPreferredSize().height);layout(panel);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,panel.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File("build/supplies-preview.png"));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private static void layout(java.awt.Container c){c.doLayout();for(java.awt.Component child:c.getComponents()){if(child instanceof java.awt.Container){layout((java.awt.Container)child);}}}
}
