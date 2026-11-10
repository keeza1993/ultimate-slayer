package com.imth3keeza0.ultimateslayer;

import java.util.*;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Pure trip checklist: live carried items and capture-time bank stock stay separate. */
public final class SupplyPlanner
{
    private SupplyPlanner() {}
    public enum Priority { HIGH, NORMAL, LOW }
    public enum Category
    {
        PROTECTION("Protection", "protectionPriority"), PRAYER("Prayer", "prayerPriority"), FOOD("Food", "foodPriority"),
        OUTBOUND("Getting there", "outboundPriority"), RETURN("Getting back", "returnPriority"), TOOLS("Task tools", "toolsPriority"), EXTRA("Other Wiki supplies", "extraSupplyPriority");
        public final String label, key;
        Category(String label,String key) { this.label=label; this.key=key; }
    }
    public static class Choice
    {
        public final String name;
        public final Map<Integer,Integer> weights;
        public Choice(String name,Map<Integer,Integer> weights) { this.name=name; this.weights=Map.copyOf(weights); }
        public long carried(SidebarState state) { return live(state, weights); }
        public long bank(BankSnapshot snapshot) { return snapshot == null ? 0 : weighted(snapshot.bank, weights); }
    }
    public static final class Need
    {
        public final String key,title,note,unit;
        public final Category category;
        public final List<Choice> choices;
        public final long minimum;
        public final boolean manual;
        Need(String key,String title,Category category,String note,String unit,List<Choice> choices,long minimum,boolean manual)
        { this.key=key; this.title=title; this.category=category; this.note=note; this.unit=unit; this.choices=List.copyOf(choices); this.minimum=minimum; this.manual=manual; }
        public long carried(SidebarState state) { return choices.stream().mapToLong(c -> c.carried(state)).sum(); }
        public long bank(BankSnapshot snapshot) { return choices.stream().mapToLong(c -> c.bank(snapshot)).sum(); }
        public String status(SidebarState state,BankSnapshot snapshot)
        {
            if (manual) { return "CHECK SOURCE / ACCESS"; }
            if (packed(state)) { return "CARRIED / WORN"; }
            if (snapshot == null) { return "BANK UNKNOWN"; }
            if (key.equals("return") && minimum==2 ? DUELING.bank(snapshot)>=1 || bank(snapshot)-DUELING.bank(snapshot)>=2 : bank(snapshot)>=minimum) { return "PACK FROM BANK"; }
            if (bank(snapshot)>0) { return "BANK STOCK - CHECK QUANTITY"; }
            return "NOT ENOUGH OBSERVED";
        }
        public boolean packed(SidebarState state)
        { return !manual && (key.equals("return") && minimum==2 ? carried(state)-DUELING.carried(state)>=2 || DUELING.carried(state)>=1 : carried(state)>=minimum); }
        public String summary(SidebarState state,BankSnapshot snapshot)
        { return status(state,snapshot) + (manual ? "" : " � now " + carried(state) + " " + unit + "; bank at capture " + (snapshot==null ? "unknown" : bank(snapshot))); }
    }
    public static final Choice PRAYER = doses("Prayer potions",ItemID._1DOSEPRAYERRESTORE,ItemID._2DOSEPRAYERRESTORE,ItemID._3DOSEPRAYERRESTORE,ItemID._4DOSEPRAYERRESTORE);
    public static final Choice RESTORE = doses("Super restores",ItemID._1DOSE2RESTORE,ItemID._2DOSE2RESTORE,ItemID._3DOSE2RESTORE,ItemID._4DOSE2RESTORE);
    public static final Choice EXTENDED_VENOM = doses("Extended anti-venom+",ItemID.EXTENDED_ANTIVENOM_1,ItemID.EXTENDED_ANTIVENOM_2,ItemID.EXTENDED_ANTIVENOM_3,ItemID.EXTENDED_ANTIVENOM_4);
    public static final Choice VENOM_PLUS = doses("Anti-venom+",ItemID.ANTIVENOM_1,ItemID.ANTIVENOM_2,ItemID.ANTIVENOM_3,ItemID.ANTIVENOM_4);
    public static final Choice VENOM = doses("Anti-venom",ItemID.ANTIVENOM1,ItemID.ANTIVENOM2,ItemID.ANTIVENOM3,ItemID.ANTIVENOM4);
    public static final Choice EXTENDED_SUPER_FIRE = doses("Extended super antifire",ItemID._1DOSE4ANTIDRAGON,ItemID._2DOSE4ANTIDRAGON,ItemID._3DOSE4ANTIDRAGON,ItemID._4DOSE4ANTIDRAGON);
    public static final Choice SUPER_FIRE = doses("Super antifire",ItemID._1DOSE3ANTIDRAGON,ItemID._2DOSE3ANTIDRAGON,ItemID._3DOSE3ANTIDRAGON,ItemID._4DOSE3ANTIDRAGON);
    public static final Choice EXTENDED_FIRE = doses("Extended antifire (shield combination)",ItemID._1DOSE2ANTIDRAGON,ItemID._2DOSE2ANTIDRAGON,ItemID._3DOSE2ANTIDRAGON,ItemID._4DOSE2ANTIDRAGON);
    public static final Choice FIRE = doses("Antifire (shield combination)",ItemID._1DOSE1ANTIDRAGON,ItemID._2DOSE1ANTIDRAGON,ItemID._3DOSE1ANTIDRAGON,ItemID._4DOSE1ANTIDRAGON);
    public static final Choice HOUSE = items("House teleport tablets",ItemID.POH_TABLET_TELEPORTTOHOUSE);
    public static final Choice DUELING = items("Charged rings of dueling",ItemID.RING_OF_DUELING_1,ItemID.RING_OF_DUELING_2,ItemID.RING_OF_DUELING_3,ItemID.RING_OF_DUELING_4,ItemID.RING_OF_DUELING_5,ItemID.RING_OF_DUELING_6,ItemID.RING_OF_DUELING_7,ItemID.RING_OF_DUELING_8);
    private static final Choice DRAGON_SHIELD = items("Dragonfire protection shields",ItemID.ANTIDRAGONBREATHSHIELD,ItemID.DRAGONFIRE_SHIELD,ItemID.DRAGONFIRE_SHIELD_UNCHARGED,ItemID.DRAGONFIRE_WARD,ItemID.DRAGONFIRE_WARD_UNCHARGED);
    private static Choice doses(String name,int one,int two,int three,int four) { return new Choice(name,Map.of(one,1,two,2,three,3,four,4)); }
    private static Choice houseSpell(AccountContext account)
    {
        final boolean usable=account.level(Skill.MAGIC)>=40 && account.spellbook==0;
        return new Choice("House spell: loose law/air/earth or dust runes (standard book, 40 Magic)",Map.of())
        {
            private long casts(java.util.function.IntToLongFunction count)
            {return Math.min(count.applyAsLong(ItemID.LAWRUNE),count.applyAsLong(ItemID.RUNE_DUST)+Math.min(count.applyAsLong(ItemID.AIRRUNE),count.applyAsLong(ItemID.EARTHRUNE)));}
            @Override public long carried(SidebarState state){return usable?casts(id->live(state,Map.of(id,1))):0;}
            @Override public long bank(BankSnapshot snapshot){return usable&&snapshot!=null?casts(id->snapshot.bank.getOrDefault(id,0L)):0;}
        };
    }
    private static Choice items(String name,int... ids) { Map<Integer,Integer> weights=new HashMap<>(); for(int id:ids){weights.put(id,1);} return new Choice(name,weights); }
    private static long weighted(Map<Integer,Long> stock,Map<Integer,Integer> weights)
    { long total=0; for(Map.Entry<Integer,Integer> e:weights.entrySet()){total+=stock.getOrDefault(e.getKey(),0L)*e.getValue();} return total; }
    private static long live(SidebarState state,Map<Integer,Integer> weights)
    {
        if (state == null || !state.loggedIn) { return 0; }
        long total=0;
        for(int i=0;i<28;i++){total+=(long)Math.max(0,state.quantityAt(i))*weights.getOrDefault(state.inventoryAt(i),0);}
        for(int i=0;i<14;i++){total+=weights.getOrDefault(state.equipmentAt(i),0);}
        return total;
    }
    private static Need need(String key,String title,Category category,String note,String unit,Choice... choices)
    { return new Need(key,title,category,note,unit,List.of(choices),1,false); }
    public static List<Need> plan(String task,String target,TaskReferenceData.Location location,TaskReferenceData.Exemption exemption,
        String style,SidebarState live,BankSnapshot snapshot,OwnedGearCatalog catalog,WikiStrategy.Profile wiki,
        AccountContext account,boolean preferHouse,boolean houseFairyRing,boolean stafflessFairyRings)
    {
        List<Need> result=new ArrayList<>();
        String name=WikiStrategy.normalize(target==null ? (task==null ? "" : task) : target);
        if (name.isBlank() || live == null || !live.loggedIn) { return List.of(); }
        List<String> inventory=wiki==null ? List.of() : wiki.inventory;
        boolean venom=name.contains("araxx") || name.contains("araxyt") || name.equals("zulrah") || name.equals("vorkath")
            || inventory.stream().anyMatch(n -> WikiStrategy.normalize(n).contains("anti-venom"));
        if(venom){result.add(need("venom","Venom protection",Category.PROTECTION,"Prefer extended anti-venom+ for Araxxor. Other anti-venoms have different protection durations. Plain antipoison is not equivalent.","doses",EXTENDED_VENOM,VENOM_PLUS,VENOM));}
        boolean wyvern=name.contains("wyvern");
        boolean dragon=!wyvern && name.contains("dragon") && !name.startsWith("baby ");
        if(dragon || inventory.stream().anyMatch(n -> WikiStrategy.normalize(n).contains("antifire")))
        {
            List<Choice> fireChoices=new ArrayList<>(List.of(EXTENDED_SUPER_FIRE,SUPER_FIRE));
            if(DRAGON_SHIELD.carried(live)+DRAGON_SHIELD.bank(snapshot)>0){fireChoices.add(EXTENDED_FIRE);fireChoices.add(FIRE);}
            result.add(new Need("fire","Dragonfire protection",Category.PROTECTION,
                "Extended super antifire is the preferred standalone potion for ordinary dragonfire. Ordinary/extended antifire is counted as an alternative only if a suitable shield was observed; equip BOTH. Protect from Magic alone does not cover metal-dragon breath. Boss-specific breath still needs its own strategy checks.","doses",fireChoices,1,false));
            if(EXTENDED_FIRE.carried(live)+FIRE.carried(live)+EXTENDED_FIRE.bank(snapshot)+FIRE.bank(snapshot)>0)
            {
                result.add(need("dragon-shield","Shield for the ordinary-antifire alternative",Category.PROTECTION,"Required only if using the ordinary/extended-antifire combination; do not count shield ownership as wearing it.","items",DRAGON_SHIELD));
            }
        }
        if(!venom && (name.contains("cave crawler") || name.contains("cave horror") || name.equals("hydra") || name.equals("hydras")))
        {result.add(need("poison","Poison protection",Category.PROTECTION,"Check your chosen monster's poison attacks. These are poison supplies, not a substitute for venom immunity.","doses",
            doses("Antidote++",ItemID.ANTIDOTE__1,ItemID.ANTIDOTE__2,ItemID.ANTIDOTE__3,ItemID.ANTIDOTE__4),
            doses("Superantipoison",ItemID._1DOSE2ANTIPOISON,ItemID._2DOSE2ANTIPOISON,ItemID._3DOSE2ANTIPOISON,ItemID._4DOSE2ANTIPOISON),
            doses("Antipoison",ItemID._1DOSEANTIPOISON,ItemID._2DOSEANTIPOISON,ItemID._3DOSEANTIPOISON,ItemID._4DOSEANTIPOISON)));}
        result.add(need("prayer","Prayer restoration",Category.PRAYER,"Prayer potions OR super restores. Counts include all 1-4 dose bottles. Quantity depends on prayer use and trip length; this checks availability, not a full-trip estimate.","doses",PRAYER,RESTORE));
        result.add(need("food","Healing food",Category.FOOD,"Tracked examples; other food, brews and storage are not declared absent. Plan enough healing for your trip.","items",
            items("Sharks, manta rays, karambwans, monkfish, anglers, dark crabs, swordfish and lobsters",ItemID.SHARK,ItemID.MANTARAY,ItemID.TBWT_COOKED_KARAMBWAN,ItemID.MONKFISH,ItemID.ANGLERFISH,ItemID.DARK_CRAB,ItemID.SWORDFISH,ItemID.LOBSTER)));
        String context=target==null ? task : target;
        int index=0;
        for(TaskReferenceData.Rule rule:TaskReferenceData.rules(context,location,exemption,style))
        {
            if(rule.allowed.isEmpty()){continue;}
            Choice choice=items(rule.text,rule.allowed.stream().mapToInt(Integer::intValue).toArray());
            long min=rule.slot<0 && (TaskReferenceData.key(context).equals("rockslugs") || TaskReferenceData.key(context).equals("desert lizards")) ? Math.max(1,live.remaining) : 1;
            result.add(new Need("rule"+(index++),rule.text,rule.slot<0?Category.TOOLS:Category.PROTECTION,
                (wyvern?"Antifire does not replace icy-breath protection. ":"")+(min>1?"One-use baseline for the current remaining count; allow extras. ":"")+"See required-item and access checks.","items",List.of(choice),min,false));
        }
        addTravel(result,name,location,live,snapshot,account,preferHouse,houseFairyRing,stafflessFairyRings);
        Set<Integer> covered=new HashSet<>(); for(Need need:result){for(Choice choice:need.choices){covered.addAll(choice.weights.keySet());}}
        // Inventory links from this exact public strategy extend coverage beyond curated hazard rules.
        if(catalog!=null)
        {
            int extra=0;
            for(String item:inventory)
            {
                Map<Integer,Integer> ids=new HashMap<>();
                String wanted=family(item);
                for(Map.Entry<Integer,String> observed:catalog.itemNames.entrySet())
                {
                    if(!covered.contains(observed.getKey()) && family(observed.getValue()).equals(wanted))
                    {
                        java.util.regex.Matcher dose=java.util.regex.Pattern.compile(".*\\(([1-4])\\)$").matcher(observed.getValue());
                        ids.put(observed.getKey(),isPotion(item)&&dose.matches()?Integer.parseInt(dose.group(1)):1);
                    }
                }
                if(!ids.isEmpty() && extra++<8)
                {result.add(need("wiki-"+wanted,"Wiki: "+item,Category.EXTRA,"Observed item matches this strategy's Inventory section. Check source notes, charges and quantities; it is not automatically mandatory.",isPotion(item)?"doses":"items",new Choice(item,ids)));covered.addAll(ids.keySet());}
            }
        }
        return List.copyOf(result);
    }
    private static boolean isPotion(String item)
    {String n=WikiStrategy.normalize(item);return n.contains("potion")||n.contains("restore")||n.contains("antifire")||n.contains("anti-venom")||n.contains("antipoison")||n.contains("brew")||n.contains("serum");}
    private static String family(String name) {return WikiStrategy.normalize(name).replaceFirst("\\s*\\([1-8]\\)$","").trim();}
    private static void addTravel(List<Need> result,String target,TaskReferenceData.Location location,SidebarState live,BankSnapshot snapshot,AccountContext account,
        boolean preferHouse,boolean houseFairyRing,boolean staffless)
    {
        boolean spider=target.contains("araxx")||target.contains("araxyt");
        String ring=spider?"ALQ":location==TaskReferenceData.Location.TOWER?"CKS":location==TaskReferenceData.Location.FREMENNIK?"AJR":location==TaskReferenceData.Location.KARUULM?"CIR":location==TaskReferenceData.Location.SMOKE_DEVIL?"BKP":location==TaskReferenceData.Location.NEXUS?"DIP":"";
        boolean usingHouse=preferHouse && houseFairyRing && !ring.isEmpty();
        if(usingHouse)
        {result.add(need("out","Outbound: POH fairy ring "+ring,Category.OUTBOUND,"Your POH fairy ring is manually confirmed in settings. House teleport, then ring "+ring+". "+(spider?"Run south past the bridge to the spider cave. ":"")+"Loose spell runes count only with standard spellbook and 40 Magic; rune pouches and staff substitutions are not read. POH furniture and transport access are not scanned.","teleports",HOUSE,houseSpell(account)));}
        else if(spider)
        {result.add(need("out","Outbound: spider cave",Category.OUTBOUND,"Spider cave scroll teleports to the cave; requires Priest in Peril and Drezel access. Ectophial is a different approach; fairy ring ALQ is another option. Run south past the bridge. Choose one route, not all.","items",items("Spider cave teleports",ItemID.TELEPORTSCROLL_SPIDERCAVE),items("Filled ectophial (Ghosts Ahoy access required)",ItemID.ECTOPHIAL)));}
        else if(location==TaskReferenceData.Location.TOWER||location==TaskReferenceData.Location.FREMENNIK)
        {result.add(need("out","Outbound: Slayer ring / fairy ring "+ring,Category.OUTBOUND,"Slayer ring to the destination, or fairy ring "+ring+". Charged/eternal rings counted as items, not a verified teleport unlock.","items",items("Slayer rings",ItemID.SLAYER_RING_1,ItemID.SLAYER_RING_2,ItemID.SLAYER_RING_3,ItemID.SLAYER_RING_4,ItemID.SLAYER_RING_5,ItemID.SLAYER_RING_6,ItemID.SLAYER_RING_7,ItemID.SLAYER_RING_8,ItemID.SLAYER_RING_ETERNAL)));}
        else if(location==TaskReferenceData.Location.KARUULM)
        {result.add(need("out","Outbound: Mount Karuulm",Category.OUTBOUND,"Rada's blessing 3/4 Mount Karuulm teleport, or fairy ring CIR. Diary rewards, daily uses and fairy-ring access need checking.","items",items("Rada's blessing 3/4",ItemID.ZEAH_BLESSING_HARD,ItemID.ZEAH_BLESSING_ELITE)));}
        else if(location==TaskReferenceData.Location.SMOKE_DEVIL)
        {result.add(need("out","Outbound: Castle Wars / BKP",Category.OUTBOUND,"Ring of dueling to Castle Wars then south-west, or fairy ring BKP.","items",DUELING));}
        else
        {result.add(new Need("out","Outbound route",Category.OUTBOUND,location.route.isBlank()?"No verified route bundled for this destination. Select the location and review its Wiki travel section.":location.route,"items",List.of(),1,true));}
        Need outward=result.get(result.size()-1);
        boolean usingFairy=usingHouse || !ring.isEmpty() && outward.carried(live)+outward.bank(snapshot)==0;
        if(usingFairy && !usingHouse && !ring.isEmpty())
        {result.set(result.size()-1,new Need(outward.key,outward.title,outward.category,outward.note+" Fairy-ring route remains possible without the listed consumable; check access.",outward.unit,outward.choices,1,true));}
        if(usingFairy)
        {
            if(!staffless){result.add(need("fairy-staff","Fairy-ring route: staff",Category.TOOLS,"Only needed if choosing the fairy-ring route. Carry/equip a dramen or lunar staff unless you have the staffless diary reward; confirm that exemption in settings. Fairy-ring access is not inferred from owning a staff.","items",items("Dramen/lunar staff",ItemID.DRAMEN_STAFF,ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF)));}
        }
        List<Choice> back=new ArrayList<>(); if(preferHouse){back.add(HOUSE);back.add(houseSpell(account));} back.add(DUELING); if(!preferHouse){back.add(HOUSE);back.add(houseSpell(account));}
        result.add(new Need("return","Return / emergency teleport",Category.RETURN,"Choose a route back before leaving. House tablets work independently of your spellbook; house furniture is not verified. Dueling rings offer a bank route. If using the same tablet stack for outbound and return, reserve a SECOND tablet. Wilderness level/teleblock restrictions still apply.","items",back,usingHouse?2:1,false));
    }
    public static List<Need> sorted(List<Need> needs,UltimateSlayerConfig config,SidebarState live,BankSnapshot snapshot,boolean missingFirst)
    {
        List<Need> ordered=new ArrayList<>(needs);
        Comparator<Need> priority=Comparator.comparingInt(n->priority(config,n.category).ordinal());
        Comparator<Need> packed=Comparator.comparing(n->n.packed(live));
        ordered.sort(missingFirst?packed.thenComparing(priority):priority.thenComparing(packed));return List.copyOf(ordered);
    }
    public static Priority priority(UltimateSlayerConfig config,Category category)
    {switch(category){case PROTECTION:return config.protectionPriority();case PRAYER:return config.prayerPriority();case FOOD:return config.foodPriority();case OUTBOUND:return config.outboundPriority();case RETURN:return config.returnPriority();case TOOLS:return config.toolsPriority();default:return config.extraSupplyPriority();}}
}

