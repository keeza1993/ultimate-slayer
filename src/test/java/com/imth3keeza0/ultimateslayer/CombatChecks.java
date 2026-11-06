package com.imth3keeza0.ultimateslayer;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/** Independent numeric fixtures, brute-force dice proof and complete-search regressions. */
public final class CombatChecks
{
    public static void main(String[] args)
    {
        for (int attack = 0; attack < 30; attack++)
        {
            for (int defence = 0; defence < 30; defence++)
            {
                int wins = 0; for (int a = 0; a <= attack; a++) { for (int d = 0; d <= defence; d++) { if (a > d) { wins++; } } }
                near(CombatCalculator.hitChance(attack, defence), wins / (double) ((attack + 1) * (defence + 1)), "Attack must strictly beat defence, including ties and zero rolls");
            }
        }
        for (int maximum = 1; maximum < 100; maximum++)
        { double sum = 0; for (int roll = 0; roll <= maximum; roll++) { sum += Math.max(1, roll); } near(CombatCalculator.normalMean(maximum), sum / (maximum + 1), "Successful zero becomes one"); }
        AccountContext account = account(99, 0);
        ReferenceCatalog.Monster target = target("Abyssal demon", "", false, 0, 1, 0, 0);
        OwnedGearPlanner.Gear whip = melee(4151, "Abyssal whip", false, 82, 82, 0, 82, 0);
        CombatCalculator.Inputs input = inputs(account, target, "Slash", "accurate", "Unknown", false, false);
        CombatCalculator.Result normal = CombatCalculator.evaluate(Map.of(3, whip), input);
        require(normal.available && normal.attackRoll == 16060 && normal.defenceRoll == 576 && normal.maxHit == 24 && normal.ticks == 4, "Whip numeric fixture: levels, rolls, max hit and speed");
        near(normal.dps, (1 - 578.0 / 32122) * 12.04 / 2.4, "Whip DPS includes successful-zero correction");
        OwnedGearPlanner.Gear slow = melee(7158, "Dragon 2h sword", true, 100, 120, 0, 100, 0);
        CombatCalculator.Result slowResult = CombatCalculator.evaluate(Map.of(3, slow), input);
        require(slowResult.available && slowResult.maxHit > normal.maxHit && slowResult.dps < normal.dps, "Higher strength does not automatically beat a faster weapon");
        OwnedGearPlanner.Gear helmet = armor(11865, "Slayer helmet (i)", 0, 0);
        CombatCalculator.Result onTask = CombatCalculator.evaluate(Map.of(3, whip, 0, helmet), inputs(account, target, "Slash", "accurate", "Unknown", true, false));
        require(onTask.available && onTask.maxHit == 28 && onTask.attackRoll == 18736, "Matching-task melee bonus uses 7/6 with integer floors");
        OwnedGearPlanner.Gear salve = armor(99990, "Salve amulet (ei)", 2, 0);
        CombatCalculator.Result undead = CombatCalculator.evaluate(Map.of(3, whip, 0, helmet, 2, salve), inputs(account, target("Abyssal demon", "", true, 0, 1, 0, 0), "Slash", "accurate", "Unknown", true, false));
        require(undead.available && undead.attackRoll == 19272 && undead.maxHit == 28 && undead.effects.size() == 1, "Salve replaces, rather than stacks with, task helmet");
        OwnedGearPlanner.Gear crossbow = new OwnedGearPlanner.Gear(9185,9185,"Rune crossbow",3,false,0,90,0,0,0,0);
        OwnedGearPlanner.Gear bolts = new OwnedGearPlanner.Gear(9144,9144,"Runite bolts",13,false,0,0,0,0,115,0);
        CombatCalculator.Result range = CombatCalculator.evaluate(Map.of(3,crossbow,13,bolts), inputs(account, target, "Ranged", "Rapid", "Unknown", false, false));
        require(range.available && range.maxHit == 30 && range.ticks == 5, "Rapid ranged fixture uses range strength and a one-tick speed change");
        OwnedGearPlanner.Gear arrow = new OwnedGearPlanner.Gear(892,892,"Rune arrow",13,false,0,0,0,0,49,0);
        require(!CombatCalculator.evaluate(Map.of(3,crossbow,13,arrow), inputs(account,target,"Ranged","Rapid","Unknown",false,false)).available, "An incompatible stronger ammo slot cannot make a result");
        Map<Integer, OwnedGearPlanner.Gear> voidSet = new HashMap<>(Map.of(3,crossbow,13,bolts));
        voidSet.put(0, armor(11664,"Void ranger helm",0,0)); voidSet.put(4,armor(8839,"Void knight top",4,0));
        voidSet.put(7,armor(8840,"Void knight robe",7,0)); voidSet.put(9,armor(8842,"Void knight gloves",9,0));
        CombatCalculator.Result voidRange = CombatCalculator.evaluate(voidSet, inputs(account,target,"Ranged","Rapid","Unknown",false,false));
        require(voidRange.available && voidRange.maxHit == 33 && voidRange.attackRoll == 18018, "Full zero-offence Void set is applied jointly");
        voidSet.remove(9);
        require(CombatCalculator.evaluate(voidSet, inputs(account,target,"Ranged","Rapid","Unknown",false,false)).maxHit == 30, "An incomplete set gets no set bonus");
        OwnedGearPlanner.Gear staff = new OwnedGearPlanner.Gear(1387,1387,"Staff of fire",3,false,0,0,10,0,0,0);
        CombatCalculator.Result magic = CombatCalculator.evaluate(Map.of(3,staff), inputs(account,target,"Magic","Spell","Unknown",false,false));
        require(magic.available && magic.maxHit == 24 && magic.ticks == 5 && magic.attackRoll == 7918 && magic.defenceRoll == 640, "Standard magic uses target Magic, spell damage and five-tick manual casting");
        Map<Integer,OwnedGearPlanner.Gear> voidMagic = new HashMap<>(Map.of(3,staff,0,armor(11663,"Void mage helm",0,0),4,armor(8839,"Void knight top",4,0),7,armor(8840,"Void knight robe",7,0),9,armor(8842,"Void knight gloves",9,0)));
        CombatCalculator.Result mageSet = CombatCalculator.evaluate(voidMagic,inputs(account,target,"Magic","Spell","Unknown",false,false));
        require(mageSet.available && mageSet.attackRoll==11481,"Void mage bonus rounds the attack roll, not the effective level");
        OwnedGearPlanner.Gear crystalBow = new OwnedGearPlanner.Gear(23983,23983,"Crystal bow",3,true,0,100,0,0,78,0);
        Map<Integer,OwnedGearPlanner.Gear> crystal = Map.of(3,crystalBow,0,armor(23971,"Crystal helm",0,0),4,armor(23975,"Crystal body",4,0),7,armor(23979,"Crystal legs",7,0));
        require(!CombatCalculator.evaluate(crystal,inputs(account,target,"Ranged","Rapid","Unknown",false,false)).available,"Crystal state must not silently count as charged");
        CombatCalculator.Result crystalSet=CombatCalculator.evaluate(crystal,inputs(account,target,"Ranged","Rapid","Unknown",false,true));
        require(crystalSet.available&&crystalSet.attackRoll==22812&&crystalSet.maxHit==27,"Crystal piece weights affect a crystal bow only when states are assumed usable");
        OwnedGearPlanner.Gear trident=new OwnedGearPlanner.Gear(11905,11905,"Trident of the seas",3,false,0,0,25,0,0,0);
        require(!CombatCalculator.evaluate(Map.of(3,trident),inputs(account,target,"Magic","Spell","Unknown",false,false)).available,"Powered staff charges remain an explicit assumption");
        CombatCalculator.Result powered=CombatCalculator.evaluate(Map.of(3,trident),inputs(account,target,"Magic","Spell","Unknown",false,true));
        require(powered.available&&powered.maxHit==28&&powered.ticks==4,"Trident base damage and attack interval fixture");
        Map<Skill,Integer> baseLevels=new EnumMap<>(Skill.class);for(Skill skill:Skill.values()){baseLevels.put(skill,99);}
        AccountContext boosted=new AccountContext(baseLevels,Map.of(),0,Map.of(Skill.STRENGTH,118));
        CombatCalculator.Inputs boostInput=new CombatCalculator.Inputs(boosted,target,new CombatCalculator.Mode("Slash","accurate"),"Fire Surge","Unknown",TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,false,false,true,CombatCalculator.PrayerBoost.NONE);
        require(CombatCalculator.evaluate(Map.of(3,whip),boostInput).maxHit==29,"Observed boosts are separated from base eligibility levels");
        require(SpellPlanning.baseMaxHit("Wind Strike",99) == 8 && SpellPlanning.baseMaxHit("Wind Strike",1) == 2, "Elemental spell scaling uses the highest unlocked element in the tier");
        require(!CombatCalculator.evaluate(Map.of(3,staff), inputs(account(99,1),target,"Magic","Spell","Unknown",false,false)).available, "Wrong book cannot become a magic recommendation");
        require(!CombatCalculator.evaluate(Map.of(3,whip), inputs(AccountContext.UNKNOWN,target,"Slash","accurate","Unknown",false,false)).available, "Unknown levels cannot become numeric certainty");
        require(!CombatCalculator.evaluate(Map.of(3,whip), inputs(account,target("Kraken","",false,0,1,0,0),"Slash","accurate","Unknown",false,false)).available, "Unmodeled target mechanics withhold numeric recommendations");
        OwnedGearPlanner.Gear face = armor(net.runelite.api.gameval.ItemID.SLAYER_FACEMASK,"Facemask",0,0);
        CombatCalculator.Inputs dust = inputs(account,target("Dust devil","Smoke Dungeon",false,0,1,0,0),"Slash","accurate","Dust devils",false,false);
        require(!CombatCalculator.evaluate(Map.of(3,whip),dust).available, "Missing protection blocks an otherwise strong numeric loadout");
        LoadoutOptimizer.SearchResult result = new LoadoutOptimizer(List.of(whip,slow,face), snapshot()).search(dust,"Melee",false);
        require(!result.setups.isEmpty() && result.setups.get(0).gear.get(3).id == whip.id && result.setups.get(0).gear.get(0).id == face.id, "Joint search chooses a faster complete protected setup");
        require(result.evaluations <= 40000 && result.setups.get(0).gear.values().stream().allMatch(g -> List.of(whip,slow,face).contains(g)), "Search is bounded and cannot invent unowned equipment");
        List<OwnedGearPlanner.Gear> rangedOwned = new ArrayList<>(List.of(crossbow,bolts,armor(11664,"Void ranger helm",0,0),armor(8839,"Void knight top",4,0),armor(8840,"Void knight robe",7,0),armor(8842,"Void knight gloves",9,0),armor(99991,"Synthetic offensive body",4,20)));
        LoadoutOptimizer.SearchResult rangedResult = new LoadoutOptimizer(rangedOwned,snapshot()).search(inputs(account,target,"Ranged","Rapid","Unknown",false,false),"Ranged",false);
        require(!rangedResult.setups.isEmpty() && rangedResult.setups.get(0).combat.effects.stream().anyMatch(e -> e.contains("Void")), "Multi-start search finds a full set that independent slot leaders would miss");
        boolean immutable = false; try { result.setups.get(0).gear.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        require(immutable, "Search results are immutable across the worker/UI boundary");
        OwnedGearPlanner.Gear zombieAxe = melee(28810,"Zombie axe",false,105,107,0,105,90);
        CombatCalculator.Result zombieHit = CombatCalculator.evaluate(Map.of(3,zombieAxe), inputs(account,target,"Slash","aggressive","Unknown",false,false));
        require(zombieHit.available && zombieHit.ticks == 5 && zombieHit.maxHit == 29, "Zombie axe uses five ticks and aggressive strength");
        OwnedGearPlanner.Gear leafAxe = melee(20727,"Leaf-bladed battleaxe",false,92,92,0,72,92);
        CombatCalculator.Result leafBase = CombatCalculator.evaluate(Map.of(3,leafAxe), inputs(account,target,"Crush","aggressive","Unknown",false,false));
        CombatCalculator.Result leafKurask = CombatCalculator.evaluate(Map.of(3,leafAxe), inputs(account,target("Kurask","",false,0,1,0,0),"Crush","aggressive","Kurasks",false,false));
        require(leafBase.available && leafKurask.available && leafBase.maxHit == 27 && leafKurask.maxHit == 31, "Leaf battleaxe gets 17.5 percent damage only on leafy monsters");
        require(leafBase.attackRoll == leafKurask.attackRoll, "Leaf battleaxe bonus does not increase accuracy");
        require(!CombatCalculator.evaluate(Map.of(3,whip),inputs(account,target("Kurask","",false,0,1,0,0),"Slash","accurate","Unknown",false,false)).available, "Direct monster browsing cannot bypass leafy immunity");
        OwnedGearPlanner.Gear broad = new OwnedGearPlanner.Gear(11875,11875,"Broad bolts",13,false,0,0,0,0,100,0);
        require(CombatCalculator.evaluate(Map.of(3,crossbow,13,broad),inputs(account,target("Kurask","",false,0,1,0,0),"Ranged","Rapid","Kurasks",false,false)).available, "Compatible broad ammunition supports real kurask tasks");
        OwnedGearPlanner.Gear lance = melee(22978,"Dragon hunter lance",false,85,70,85,65,65);
        com.google.gson.JsonObject dragonJson = new JsonParser().parse("{\"id\":99998,\"name\":\"Blue dragon\",\"skills\":{\"hp\":100,\"def\":0,\"magic\":1},\"defensive\":{},\"attributes\":[\"dragon\"]}").getAsJsonObject();
        CombatCalculator.Result lanceNormal = CombatCalculator.evaluate(Map.of(3,lance),inputs(account,target,"Stab","controlled","Unknown",false,false));
        CombatCalculator.Result lanceDragon = CombatCalculator.evaluate(Map.of(3,lance),inputs(account,new ReferenceCatalog.Monster(dragonJson),"Stab","controlled","Unknown",false,false));
        require(lanceNormal.available && lanceDragon.available && lanceNormal.maxHit == 23 && lanceDragon.maxHit == 27 && lanceDragon.attackRoll == 19310, "Lance draconic damage and accuracy fixture");
        OwnedGearPlanner.Gear graniteHammer = melee(21742,"Granite hammer",false,57,56,0,0,57);
        com.google.gson.JsonObject golemJson = dragonJson.deepCopy(); golemJson.addProperty("name","Gargoyle");
        com.google.gson.JsonArray golemTags = new com.google.gson.JsonArray(); golemTags.add("golem"); golemJson.add("attributes",golemTags);
        CombatCalculator.Result golemHit = CombatCalculator.evaluate(Map.of(3,graniteHammer),inputs(account,new ReferenceCatalog.Monster(golemJson),"Crush","accurate","Unknown",false,false));
        require(golemHit.available && golemHit.maxHit == 26 && golemHit.attackRoll == 17303, "Granite hammer golem accuracy and damage fixture");
        OwnedGearPlanner.Gear warped = new OwnedGearPlanner.Gear(28585,28585,"Warped sceptre",3,false,0,0,12,0,0,0);
        CombatCalculator.Result warpedHit = CombatCalculator.evaluate(Map.of(3,warped),inputs(account,target,"Magic","Spell","Unknown",false,true));
        require(warpedHit.available && warpedHit.maxHit == 24 && warpedHit.ticks == 4, "Warped sceptre uses floor((8*Magic+96)/37) and four ticks");
        require(!CombatCalculator.evaluate(Map.of(3,warped),inputs(account,target,"Magic","Spell","Unknown",false,false)).available, "Warped charges must be explicitly assumed");
        OwnedGearPlanner.Gear emptyWarped = new OwnedGearPlanner.Gear(28583,28583,"Warped sceptre",3,false,0,0,12,0,0,0);
        require(!CombatCalculator.evaluate(Map.of(3,emptyWarped),inputs(account,target,"Magic","Spell","Unknown",false,true)).available, "Known uncharged reference cannot be overridden by assumption");
        require(!new LoadoutOptimizer(List.of(warped),new BankSnapshot(1,Map.of(),Map.of(),Map.of())).search(inputs(account,target,"Magic","Spell","Unknown",false,true),"Magic",false).setups.isEmpty(), "Warped built-in attack does not require unrelated loose spell runes");
        require(target("Jelly","",false,0,1,0,0).ordinaryCombat && !target("Jelly","Nightmare Zone",false,0,1,0,0).ordinaryCombat, "Expanded ordinary target coverage excludes special versions");
        System.out.println("Combat dice proof, numeric fixtures, supported sets, protected joint-search and ownership checks passed.");
    }
    static AccountContext account(int level, int book)
    {
        Map<Skill,Integer> levels = new EnumMap<>(Skill.class); for (Skill skill : Skill.values()) { levels.put(skill,level); }
        Map<Quest,Boolean> quests = new EnumMap<>(Quest.class); for (Quest quest : ProgressionChecks.QUESTS) { quests.put(quest,true); }
        return new AccountContext(levels,quests,book);
    }
    static ReferenceCatalog.Monster target(String name, String version, boolean undead, int defence, int magic, int physicalBonus, int magicBonus)
    {
        return new ReferenceCatalog.Monster(new JsonParser().parse("{\"id\":99999,\"name\":\""+name+"\",\"version\":\""+version+"\",\"level\":1,\"speed\":4,\"skills\":{\"hp\":100,\"def\":"+defence+",\"magic\":"+magic+"},\"defensive\":{\"stab\":"+physicalBonus+",\"slash\":"+physicalBonus+",\"crush\":"+physicalBonus+",\"standard\":"+physicalBonus+",\"heavy\":"+physicalBonus+",\"light\":"+physicalBonus+",\"magic\":"+magicBonus+"},\"attributes\":"+(undead?"[\"undead\"]":"[]")+"}").getAsJsonObject());
    }
    static CombatCalculator.Inputs inputs(AccountContext account, ReferenceCatalog.Monster target, String style, String stance, String task, boolean onTask, boolean charged)
    { return new CombatCalculator.Inputs(account,target,new CombatCalculator.Mode(style,stance),"Fire Surge",task,TaskReferenceData.Location.UNKNOWN,TaskReferenceData.Exemption.UNKNOWN,onTask,charged,false,CombatCalculator.PrayerBoost.NONE); }
    static BankSnapshot snapshot() { return new BankSnapshot(System.currentTimeMillis(),Map.of(net.runelite.api.gameval.ItemID.AIRRUNE,1000L,net.runelite.api.gameval.ItemID.FIRERUNE,1000L,net.runelite.api.gameval.ItemID.WRATHRUNE,1000L),Map.of(),Map.of()); }
    static OwnedGearPlanner.Gear melee(int id,String name,boolean two,int attack,int strength,int stab,int slash,int crush)
    { return new OwnedGearPlanner.Gear(id,id,name,3,two,attack,0,0,strength,0,0,stab,slash,crush); }
    static OwnedGearPlanner.Gear armor(int id,String name,int slot,int ranged)
    { return new OwnedGearPlanner.Gear(id,id,name,slot,false,0,ranged,0,0,0,0); }
    private static void require(boolean condition,String reason) { if(!condition){throw new AssertionError(reason);} }
    private static void near(double actual,double expected,String reason) { if(Math.abs(actual-expected)>1e-10){throw new AssertionError(reason+": "+actual+" != "+expected);} }
}
