/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zkai.Area.Owner;
import zkai.BuilderHandler.Order;

/**
 *
 * @author User
 */
public class zkai extends com.springrts.ai.oo.AbstractOOAI {
    
    public static final boolean DEFENSES = true;
    
    List<Unit> builders = new ArrayList();
    List<Unit> fighters = new ArrayList();
    List<Unit> radars = new ArrayList();
    List<Unit> units = new ArrayList();
    List<Unit> buildings = new ArrayList();
    BuilderHandler builder;
    ExpansionHandler expansion;
    DefenseMap defense;
    OOAICallback callback;
    public Unit com;
    int team = 0;
    Unit fac = null;
    UnitDef solar, mex, wind, cloaky, rector, glaive, rocko, warrior, radar, nano, defender, llt, hlt, bertha, hammer, zeus, sniper, fusion, razor, antinuke, gremlin;
    List<AIFloat3> availablemetalspots = new ArrayList();
    ThreatHandler threats;
    int frame = 0;
    int nextRadar = 0, nextNano = 0;
    
    public void checkForMetal(String luamsg) {
        try {
            debug(luamsg.substring(12, luamsg.length()));
            for (String spotDesc : luamsg.substring(13, luamsg.length() - 2).split("},\\{")) {
                debug(spotDesc);
                float x, y, z;
                y = Float.parseFloat(spotDesc.split(",")[0].split(":")[1]);
                x = Float.parseFloat(spotDesc.split(",")[1].split(":")[1]);
                z = Float.parseFloat(spotDesc.split(",")[3].split(":")[1]);
                availablemetalspots.add(new AIFloat3(x, y, z));
            }
            
            if (availablemetalspots.isEmpty()) {
                debug("This is a map with no metal spots");
            } else {
                debug("This is a map with metal spots. Listing Values...");
                for (AIFloat3 metalspot : availablemetalspots) {
                    debug("Metal Spot at X: " + metalspot.x + ", Y: " + metalspot.y + ", Z: " + metalspot.z);
                    //callback.getMap().getDrawer().addPoint(metalspot, "Metal Spot");
                }
            }
        } catch (Exception ex) {
            debug("printing exception at checkmetal");
            printException(ex);
        }
    }
    
    @Override
    public int luaMessage(String inData) {
        try {
            debug("got lua message: " + inData);
            if (inData.length() > 11 && inData.substring(0, 11).equalsIgnoreCase("METAL_SPOTS")) {
                checkForMetal(inData);
            } else {
                debug("no metal message - " + inData.substring(0, 12));
            }
        } catch (Exception ex) {
            
            debug("printing exception at luamsg");
            printException(ex);
        }
        return 0;
    }
    
    public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
        unitFinished(unit);
        return 0;
    }
    
    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
        try {
            Area.getArea(unit.getPos()).lastAttack = frame;
            Area.getArea(unit.getPos()).danger += damage;
            if (attacker == null || attacker.getUnitId() < 0) {
                
                threats.addPoint(unit.getPos(), damage);
            } else {
                if (attacker.getAllyTeam() == team) {
                    debug("damage added friend");
                }
                threats.addUnit(attacker, damage);
            }
        } catch (Exception ex) {
            printException(ex);
        }
        //debug("was damaged " + damage);
        return 0;
    }
    
    public int enemyEnterLOS(Unit enemy) {
        try {
            if (enemy.getAllyTeam() == team) {
                debug("enterlos added friend");
            }
            
            if (enemy.getMaxSpeed() < 0.5 || enemy.getDef().getMaxAcceleration() == 0) {
                //found building
                Area.getArea(enemy.getPos()).setOwner(Owner.enemy);
            }
            threats.addUnit(enemy, -1);
        } catch (Exception ex) {
            printException(ex);
        }
        return 0;
    }
    
    public int enemyLeaveLOS(Unit enemy) {
        //threats.removeUnit(enemy, false);
        //debug(enemy.getDef().getHumanName() + " left LOS");
        return 0;
    }
    
    public int enemyEnterRadar(Unit enemy) {
        try {
            if (enemy.getAllyTeam() == team) {
                debug("enter radar added friend");
            }
            threats.addUnit(enemy, -1);
        } catch (Exception ex) {
            printException(ex);
        }
        return 0;
    }
    
    public void printException(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        debug("exception(" + sw.toString().replace("\n", " ") + ") " + ex);
    }
    
    public int enemyLeaveRadar(Unit enemy) {
        try {
            threats.removeUnit(enemy, false);
        } catch (Exception e) {
            printException(e);
        }
        //debug(enemy.getDef().getHumanName() + " left radar");
        return 0;
    }
    
    public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
        return 0;
    }
    
    public int enemyDestroyed(Unit enemy, Unit attacker) {
        threats.removeUnit(enemy, true);
        return 0;
    }
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
        Area.getArea(unit.getPos()).danger += unit.getDef().getCost(BuilderHandler.metal);
        builder.unitKilled(unit);
        threats.unitDestroyed(unit);
        units.remove(unit);
        if (builders.contains(unit)) {
            builders.remove(unit);
        }
        if (fighters.contains(unit)) {
            fighters.remove(unit);
        }
        if (radars.contains(unit)) {
            radars.remove(unit);
        }
        if (unit.equals(fac)) {
            fac = null;
        }
        
        return 0;
    }
    
    @Override
    public int init(int teamId, OOAICallback callback) {
        
        try {
            this.callback = callback;
            List<UnitDef> unitDefs = this.callback.getUnitDefs();
            for (UnitDef def : unitDefs) {
                if (def.getName().equals("armwin")) {
                    wind = def;
                    debug("found wind");
                }
                if (def.getName().equals("factorycloak")) {
                    cloaky = def;
                    debug("found cloaky");
                }
                if (def.getName().equals("armrock")) {
                    rocko = def;
                    debug("found rocko");
                }
                if (def.getName().equals("corrad")) {
                    radar = def;
                    debug("found radar");
                }
                if (def.getName().equals("armwar")) {
                    warrior = def;
                    debug("found warrior");
                }
                if (def.getName().equals("armfus")) {
                    fusion = def;
                    debug("found fusion");
                }
                if (def.getName().equals("corrazor")) {
                    razor = def;
                    debug("found razor");
                }
                if (def.getName().equals("armham")) {
                    hammer = def;
                    debug("found hammer");
                }
                if (def.getName().equals("armrectr")) {
                    rector = def;
                    debug("found rector");
                }
                if (def.getName().equals("armsnipe")) {
                    sniper = def;
                    debug("found sniper");
                }
                if (def.getName().equals("armpw")) {
                    glaive = def;
                    debug("found glaive");
                }
                if (def.getName().equals("armjeth")) {
                    gremlin = def;
                    debug("found gremlin");
                }
                if (def.getName().equals("armzeus")) {
                    zeus = def;
                    debug("found zeus");
                }
                if (def.getName().equals("corllt")) {
                    llt = def;
                    debug("found llt");
                }
                if (def.getName().equals("armamd")) {
                    antinuke = def;
                    debug("found antinuke");
                    
                }
                if (def.getName().equals("corhlt")) {
                    hlt = def;
                    debug("found hlt");
                    
                }
                if (def.getName().equals("armsolar")) {
                    solar = def;
                    debug("found solar");
                    
                }
                if (def.getName().equals("corrl")) {
                    defender = def;
                    debug("found defender");
                    
                }
                if (bertha == null && def.getName().equals("armbrtha")) {
                    bertha = def;
                    debug("found bertha");
                    
                }
                //if (def.getName().equals("corsilo")) {
                if (def.getName().equals("armbrtha")) {
                    bertha = def;
                    debug("found bertha");
                    
                }
                if (def.getName().equals("cormex")) {
                    mex = def;
                    debug("found mex");
                    
                }
                if (def.getName().equals("armnanotc")) {
                    nano = def;
                    debug("found nano");
                    
                }
            }
            if (solar == null) {
                debug("didnt find wind nor solar");
            }
            builder = new BuilderHandler(this);
            threats = new ThreatHandler(this);
            defense = new DefenseMap(this);
            Area.init(this);
            expansion = new ExpansionHandler(this);
            team = callback.getGame().getMyAllyTeam();
            AIFloat3 startpos = parseStartScript();
            callback.getGame().sendStartPosition(true, startpos);
            debug("i'm team " + team);
            callback.getGame().sendTextMessage("gl hf", 0);
            callback.getGame().sendTextMessage("porc: " + DEFENSES, 0);
            debug("There are " + callback.getGroups().size() + " groups.");
        } catch (Exception ex) {
            printException(ex);
        }
        return 0;
    }
    
    public static float dist(AIFloat3 a, AIFloat3 b) {
        if (a == null || b == null) {
            return Float.MAX_VALUE;
        }
        return (float) (Math.pow(a.x - b.x, 2) + Math.pow(a.z - b.z, 2)); //+ Math.pow(a.y - b.y, 2)
    }
    
    public static float dist3d(AIFloat3 a, AIFloat3 b) {
        if (a == null || b == null) {
            return Float.MAX_VALUE;
        }
        return (float) (Math.pow(a.x - b.x, 2) + Math.pow(a.z - b.z, 2) + Math.pow(a.y - b.y, 2)); //+ Math.pow(a.y - b.y, 2)
    }
    
    public Unit closestUnitToRepair(Unit unit) {
        debug("getting closest unit to repair");
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        for (Unit u : units) {
            if (((u.isBeingBuilt() && !u.getDef().equals(glaive) && !u.getDef().equals(rocko) && !u.getDef().equals(rector))
                    || (!u.isBeingBuilt() && u.getHealth() < u.getMaxHealth())) && dist(unit.getPos(), u.getPos()) < mindis && !u.equals(unit)) {
                mindis = dist(unit.getPos(), u.getPos());
                res = u;
            }
        }
        return res;
    }
    
    public Unit closestRadar(AIFloat3 pos) {
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        try {
            for (Unit u : units) {
                if (u.getDef() == radar && dist(pos, u.getPos()) < mindis) {
                    mindis = dist(pos, u.getPos());
                    res = u;
                    
                }
            }
        } catch (Exception ex) {
            debug("Exception during closestRadar: " + ex);
        }
        //debug("closest unit is: " + res.getDef().getHumanName() + " at a distance of " + mindis + " - "
        //       + pos.x + "|" + pos.z + " -> " + res.getPos().x + "|" + res.getPos().z);
        return res;
    }
    
    public Unit closestUnitByUnitDef(AIFloat3 pos, UnitDef def, boolean enemies) {
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        try {
            for (Unit u : units) {
                if ((def == null || u.getDef().equals(def)) && dist(pos, u.getPos()) < mindis) {
                    mindis = dist(pos, u.getPos());
                    res = u;
                    
                }
            }
            for (Unit u : callback.getEnemyUnitsIn(pos, mindis)) {
                if (!enemies) {
                    break;
                }
                if ((def == null || u.getDef().equals(def)) && dist(pos, u.getPos()) < mindis) {
                    mindis = dist(pos, u.getPos());
                    res = u;
                    
                }
            }
        } catch (Exception ex) {
            debug("Exception during closestX: " + ex);
        }
        //debug("closest unit is: " + res.getDef().getHumanName() + " at a distance of " + mindis + " - "
        //       + pos.x + "|" + pos.z + " -> " + res.getPos().x + "|" + res.getPos().z);
        return res;
    }
    
    public Unit closestUnit(AIFloat3 pos) {
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        try {
            //debug("looking for mex");

            for (Unit u : units) {
                if (u.getDef().getName().equals("cormex")) {
                    //   debug("units contains mex");
                }
                if (dist(pos, u.getPos()) < mindis) {
                    mindis = dist(pos, u.getPos());
                    res = u;
                    
                }
            }
        } catch (Exception ex) {
            debug("Exception during closestUnit: " + ex);
        }
        //debug("closest unit is: " + res.getDef().getHumanName() + " at a distance of " + mindis + " - "
        //       + pos.x + "|" + pos.z + " -> " + res.getPos().x + "|" + res.getPos().z);
        return res;
    }
    
    public Unit closestBuilder(AIFloat3 pos) {
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        try {
            //debug("looking for mex");

            for (Unit u : builders) {
                if (u.getDef().getName().equals("cormex")) {
                    //   debug("units contains mex");
                }
                if (dist(pos, u.getPos()) < mindis) {
                    mindis = dist(pos, u.getPos());
                    res = u;
                    
                }
            }
        } catch (Exception ex) {
            debug("Exception during closestUnit: " + ex);
        }
        //debug("closest unit is: " + res.getDef().getHumanName() + " at a distance of " + mindis + " - "
        //       + pos.x + "|" + pos.z + " -> " + res.getPos().x + "|" + res.getPos().z);
        return res;
    }
    
    public List<AIFloat3> getAvailableMetalSpots() {
        List<AIFloat3> list = new ArrayList();
        try {
            if (availablemetalspots == null) {
                availablemetalspots = new ArrayList();
            }
            for (AIFloat3 metalspot : availablemetalspots) {
                if ((closestUnitByUnitDef(metalspot, mex, true) == null || dist(metalspot, closestUnitByUnitDef(metalspot, null, true).getPos()) > 1000f)
                        && dist(metalspot, callback.getMap().findClosestBuildSite(mex, metalspot, 1000f, 0, 0)) < 10f) {
                    list.add(metalspot);
                }
            }
            if (list.isEmpty()) {
                debug("WARNING: didnt find any metalspots");
            }
        } catch (Exception ex) {
            debug("Exception during getAvailableMetalSpot: " + ex);
            printException(ex);
        }
        //availablemetalspots.remove(closestspot);
        return list;
        
    }
    
    public AIFloat3 closestMetalSpot(AIFloat3 unitposition) {
        AIFloat3 closestspot = null;
        try {
            for (AIFloat3 metalspot : availablemetalspots) {
                if (closestspot == null) {
                    closestspot = metalspot;
                } else if (dist(metalspot, unitposition) <= dist(closestspot, unitposition)
                        && dist(metalspot, closestUnit(metalspot).getPos()) > 1000f) {
                    closestspot = metalspot;
                }
            }
            if (closestspot == null) {
                debug("WARNING: didnt find metalspot");
            }
        } catch (Exception ex) {
            debug("Exception during closestMetalSpot: " + ex);
        }
        //availablemetalspots.remove(closestspot);
        return closestspot;
        
    }
    
    public AIFloat3 removeClosestMetalSpot(AIFloat3 unitposition) {
        AIFloat3 closestspot = null;
        for (AIFloat3 metalspot : availablemetalspots) {
            if (closestspot == null) {
                closestspot = metalspot;
            } else if (dist(metalspot, unitposition) < dist(closestspot, unitposition) && metalspot.hashCode() != unitposition.hashCode()) {
                closestspot = metalspot;
            }
        }
        availablemetalspots.remove(closestspot);
        return closestspot;
        
    }
    
    int nextBuildOrder = 0;
    
    @Override
    public int unitMoveFailed(Unit unit) {
        return unitIdle(unit);
    }
    
    @Override
    public int unitIdle(Unit unit) {
        try {
            builder.unitIdle(unit);
            //debug("i1");
            if (!unit.getCurrentCommands().isEmpty()) {
                return 0;
            }
            if (builders.contains(unit) && frame > nextBuildOrder) {
                nextBuildOrder = frame + 30;
                if (fac == null) {
                    if (unit.equals(com)) {
                        debug("plopping fac");
                        com.build(cloaky, callback.getMap().findClosestBuildSite(cloaky, com.getPos(), 600f, 0, 0), 0, (short) 4, Integer.MAX_VALUE);
                    }
                } else if (false) {
                    debug("i2");
                    if ((closestRadar(unit.getPos()) == null || dist(closestRadar(unit.getPos()).getPos(), unit.getPos()) > 1500 * 1500) && frame > nextRadar) {
                        debug("building radar");
                        AIFloat3 pos = callback.getMap().findClosestBuildSite(radar, unit.getPos(), 600f, 3, 0);
                        unit.build(radar, callback.getMap().findClosestBuildSite(radar, unit.getPos(), 600f, 3, 0), 0, (short) 4, Integer.MAX_VALUE);
                        callback.getMap().getDrawer().addPoint(unit.getPos(), "building radar(" + pos.x + "|" + pos.z + ") " + unit.getCurrentCommands().size());
                        nextRadar = frame + 200;
                    } else if (closestUnitToRepair(unit) != null
                            && dist(unit.getPos(), closestUnitToRepair(unit).getPos()) < 300 * 300 + rnd.nextDouble() * 300 * 300) {

                        //debug("i2b");
                        unit.repair(closestUnitToRepair(unit), (short) 4, frame + 500);
                        callback.getMap().getDrawer().addPoint(unit.getPos(), "repairing " + closestUnitToRepair(unit).getDef().getHumanName());
                    } else if (rnd.nextDouble() > 0.94) {
                        double maxd = -1;
                        Unit best = null;
                        for (Unit b : buildings) {
                            double r = rnd.nextDouble();
                            if (threats.getDanger(b.getPos()) + r > maxd) {
                                best = b;
                                maxd = threats.getDanger(b.getPos()) + r;
                            }
                        }
                        if (best != null && callback.getMap().findClosestBuildSite(defender, best.getPos(), 600f, 3, 0).x > 0) {
                            unit.build(defender, callback.getMap().findClosestBuildSite(defender, best.getPos(), 600f, 3, 0), 0, (short) 4, frame + 1000);
                            callback.getMap().getDrawer().addPoint(unit.getPos(), "building defender");
                        }
                    } else if (callback.getEconomy().getCurrent(callback.getResources().get(0)) > 200
                            && callback.getEconomy().getIncome(callback.getResources().get(0)) > callback.getEconomy().getUsage(callback.getResources().get(0))
                            && callback.getEconomy().getCurrent(callback.getResources().get(1)) > 350) {
                        //metal excess + enough energy
                        boolean helping = false;
                        for (Unit u : units) {
                            if (u.getDef().equals(nano) && u.isBeingBuilt()) {
                                unit.repair(u, (short) 4, Integer.MAX_VALUE);
                                helping = true;
                                callback.getMap().getDrawer().addPoint(unit.getPos(), "helping with nano");
                                break;
                            }
                        }
                        if (!helping && frame > nextNano) {
                            unit.build(nano, callback.getMap().findClosestBuildSite(nano, fac.getPos(), 600f, 3, 0), 0, (short) 4, frame + 1000);
                            nextNano = frame + 200;
                            callback.getMap().getDrawer().addPoint(unit.getPos(), "building nano");
                        }
                    } else if (callback.getEconomy().getCurrent(callback.getResources().get(1)) < 250) {

                        //debug("i2a");
                        unit.build(solar, callback.getMap().findClosestBuildSite(solar, unit.getPos(), 600f, 3, 0), 0, (short) 4, frame + 1000);
                        callback.getGame().sendTextMessage(unit.getDef().getHumanName() + " building solar", 0);
                        callback.getMap().getDrawer().addPoint(unit.getPos(), "building solar");
                        //unit.build(mex, closestMetalSpot(unit.getPos()),0, (short) 4, Integer.MAX_VALUE);
                    } else {
                        //debug("i2c");
                        debug("building mex at " + closestMetalSpot(unit.getPos()).x + "|" + closestMetalSpot(unit.getPos()).z);
                        
                        unit.build(mex, closestMetalSpot(unit.getPos()), 0, (short) 4, Integer.MAX_VALUE);
                        callback.getMap().getDrawer().addPoint(unit.getPos(), "building mex");
                    }
                    //debug("energy: " + callback.getEconomy().getCurrent(callback.getResources().get(1)));
                }
            }
            //debug("i3");
            /*if (fighters.contains(unit)) {
             if (closestRadar(unit.getPos()) != null && dist(unit.getPos(), closestRadar(unit.getPos()).getPos()) > 2000 * 2000 || unit.getHealth() < 0.3 * unit.getMaxHealth()) {
             if (builders.size() > 0) {
             unit.moveTo(builders.get(rnd.nextInt(builders.size())).getPos(), (short) 4, frame + 200);
             } else {
             unit.moveTo(fac.getPos(), (short) 4, frame + 200);
             }
             } else if (threats.getTarget(unit) != null) {
             unit.attack(threats.getTarget(unit), (short) 4, frame + 200);
             } /*else if (threats.getDanger(unit) != null) {
             unit.moveTo(threats.getDanger(unit), (short) 4, frame + 1000);
             }*/
            /* else {
             unit.moveTo(units.get(rnd.nextInt(units.size())).getPos(), (short) 4, frame + 200);
             }
             }*/
            if (unit.getDef().equals(nano)) {
                //unit.guard(fac, (short) 4, Integer.MAX_VALUE);
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            callback.getMap().getDrawer().addPoint(unit.getPos(), "exception at unitIdle(" + sw.toString().replace("\n", " ") + ") " + ex);
        }
        return 0;
    }
    
    Random rnd = new Random();
    
    @Override
    public int update(int frame) {
        try {

            //debug("update");
            this.frame = frame;
            builder.update(frame);
            threats.update();
            /*for (int unitsChecked = 0; unitsChecked < 1; unitsChecked++) {
             Unit u = units.get(rnd.nextInt(units.size()));//check a random unit each frame
             if (u.getPos().x < 0) {
             units.remove(u);
             continue;
             }
             if (u.getDef().equals(nano)) {
             u.guard(fac, (short) 4, Integer.MAX_VALUE);
             }
             if (fighters.contains(u) && !radars.isEmpty() && dist(u.getPos(), closestRadar(u.getPos()).getPos()) > 2000 * 2000 || u.getHealth() < 0.3 * u.getMaxHealth()) {
             u.moveTo(closestBuilder(u.getPos()).getPos(), (short) 4, frame + 200);
             }
             if (fac != null && dist(u.getPos(), fac.getPos()) < 30 * 30) {
             AIFloat3 f3 = fac.getPos();
             f3.z += 200;
             u.moveTo(f3, (short) 4, frame + 200);
             }
             if (u.getCurrentCommands().isEmpty()) {
             //debug("1a");
             boolean building = true;
             if (u.getSupportedCommands() != null) {
             for (CommandDescription cmd : u.getSupportedCommands()) {
             if (cmd.getId() == 10) {
             building = false;
             }
             }
             if (building || u.getDef().getName().contains("fac")) {
                            
             } else {

             //u.moveTo(u.getPos(), (short) 32, Integer.MAX_VALUE);
             //debug("2a");
             if (!u.isActivated()) {
             unitIdle(u);
             }

             //debug("found idling " + u.getDef().getHumanName());
             }
             }
             } else if (u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getTimeOut() < frame) {//attack
             //debug("1b");
             //debug("timeout " + u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getTimeOut() + " it is: " + frame);
             if (u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getTimeOut() < frame) {
             //debug("timed out");
             unitIdle(u);
             }
             } /*else if (u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getId() == 10 && builders.contains(u)) {//move
             debug("stopped constructor");
             u.stop((short) 4, frame + 20);
             }*/

            //}
            if (frame % 12 == 0) {
                //debug("2");
                threats.decay(1.01f);
                defense.pnl.updateUI();
                defense.pnl2.updateUI();
            }
            if (frame % 55 == 0 || facidle) {
                if (fac != null && fac.getCurrentCommands().isEmpty()) {
                    boolean pendingNano = false;
                    for (Order o : builder.pending) {
                        if (o.id == -nano.getUnitDefId()) {
                            pendingNano = true;
                        }
                    }//fighters.size() < 10 ||
                    if (((frame < 10000 && builder.builders.size() > 1)
                            || builder.builders.size() > (int) Math.sqrt(callback.getEconomy().getIncome(BuilderHandler.metal) * 0.8)) && !needCon) {
                        
                        fac.build(threats.getNeededUnit(), fac.getPos(), 0, (short) 4, Integer.MAX_VALUE);
                        
                        debug(callback.getEconomy().getCurrent(callback.getResources().get(0)) + " metal -> fighters");
                    } else {
                        needCon = false;
                        fac.build(rector, fac.getPos(), 0, (short) 4, Integer.MAX_VALUE);
                        debug(callback.getEconomy().getCurrent(callback.getResources().get(0)) + " metal -> rector");
                    }
                    facidle = false;
                }
            }
        } catch (Exception ex) {
            debug("Exception at update: " + ex.getMessage());
            printException(ex);
        }
        return 0;
    }
    
    boolean needCon = false;
    
    public void requestConstructor() {
        needCon = true;
    }
    
    public void debug(String s) {
        if (s.contains("Exception") || frame < 1000) {
            callback.getGame().sendTextMessage(s, callback.getGame().getMyTeam());
        }
    }
    
    public void label(AIFloat3 pos, String s) {
        debug(s);
        //callback.getMap().getDrawer().addPoint(pos,s);
    }
    
    @Override
    public int unitCreated(Unit unit, Unit builder) {
        
        this.builder.unitCreated(unit, builder);
        if (!units.contains(unit)) {
            units.add(unit);
        }
        if (unit.getDef().getName().equals("cormex")) {
            
            callback.getGame().sendTextMessage("created mex at " + unit.getPos().x + "|" + unit.getPos().z, 0);
            //removeClosestMetalSpot(unit.getPos());
        }
        if (unit.getDef().equals(radar)) {
            
            callback.getGame().sendTextMessage("created radar", 0);
            radars.add(unit);
            //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
        }
        return 0;
    }
    
    boolean facidle = false;
    
    @Override
    public int unitFinished(Unit unit) {
        try {
            builder.unitFinished(unit);
            threats.unitFinished(unit);
            
            if (!units.contains(unit)) {
                units.add(unit);
            }
            if (unit.getDef().getName().contains("factory")) {
                fac = unit;
                debug("fac finished");
                AIFloat3 f3 = unit.getPos();
                f3.z += 200;
                f3.y += 80;
                unit.moveTo(f3, (short) 4, Integer.MAX_VALUE); //units move out of fac
            }
            if (unit.getDef().getName().contains("com")) {
                com = unit;
                callback.getGame().sendTextMessage("Found com", 0);
                builders.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
                for (Area a : Area.getArea(com.getPos()).getNearbyAreas(1)) {
                    a.setOwner(Owner.ally);
                }
                Area.getArea(com.getPos()).setOwner(Owner.ally);

                //com.build(cloaky, callback.getMap().findClosestBuildSite(cloaky, com.getPos(), 50f, 0, 0), 0, (short)0, Integer.MAX_VALUE);
            } else if (unit.getDef().isAbleToMove()) {
                facidle = true;
                if (builder.factories.size() > 0) {
                    builder.factories.get(0).stop((short) 0, frame + 10);
                }
            }
            if (unit.getDef().getName().equals("armpw")) {
                
                callback.getGame().sendTextMessage("Found glaive", 0);
                fighters.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().equals(rocko)) {
                
                callback.getGame().sendTextMessage("Found rocko", 0);
                fighters.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().equals(solar)) {
                
                callback.getGame().sendTextMessage("Found solar", 0);
                buildings.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().equals(mex)) {
                
                callback.getGame().sendTextMessage("Found mex", 0);
                buildings.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().equals(radar)) {
                
                callback.getGame().sendTextMessage("Found radar", 0);
                if (!radars.contains(unit)) {
                    radars.add(unit);
                }
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().equals(warrior)) {
                
                callback.getGame().sendTextMessage("Found warrior", 0);
                fighters.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().equals(nano)) {
                
                callback.getGame().sendTextMessage("Found nano", 0);
                unit.patrolTo(fac.getPos(), (short) 0, Integer.MAX_VALUE);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
            if (unit.getDef().getName().equals("armrectr")) {
                
                callback.getGame().sendTextMessage("Found rector", 0);
                builders.add(unit);
                //unit.moveTo(unit.getPos(), (short) 4, Integer.MAX_VALUE); //forces idle call
            }
        } catch (Exception ex) {
            debug("Exception at unitfinished");
            printException(ex);
        }
        return 0;
    }

    //code by Anarchid: https://github.com/Anarchid/zkgbai/blob/master/src/zkgbai/ZKGraphBasedAI.java
    private AIFloat3 parseStartScript() {
        String script = callback.getGame().getSetupScript();
        Pattern p = Pattern.compile("\\[allyteam(\\d)\\]\\s*\\{([^\\}]*)\\}");
        Matcher m = p.matcher(script);
        AIFloat3 retval = new AIFloat3();
        while (m.find()) {
            int allyTeamId = Integer.parseInt(m.group(1));
            String teamDefBody = m.group(2);
            Pattern sbp = Pattern.compile("startrect\\w+=(\\d+(\\.\\d+)?);");
            Matcher mb = sbp.matcher(teamDefBody);
            
            float[] startbox = new float[4];
            int i = 0;

            // 0 -> bottom
            // 1 -> left
            // 2 -> right
            // 3 -> top
            while (mb.find()) {
                startbox[i] = Float.parseFloat(mb.group(1));
                i++;
            }
            
            int mapWidth = 8 * callback.getMap().getWidth();
            int mapHeight = 8 * callback.getMap().getHeight();
            
            startbox[0] *= mapHeight;
            startbox[1] *= mapWidth;
            startbox[2] *= mapWidth;
            startbox[3] *= mapHeight;
            
            if (allyTeamId == team) {
                retval = new AIFloat3(0.5f * startbox[1] + 0.5f * startbox[2], 0, 0.5f * startbox[0] + 0.5f * startbox[3]);
            }
            for (Area a : Area.getAreasInRectangle(new AIFloat3(startbox[1], 0, startbox[3]), new AIFloat3(startbox[2], 0, startbox[0]))) {
                a.setOwner((allyTeamId == team) ? Owner.neutral : Owner.enemy);
            }
        }
        return retval;
    }
    
}
