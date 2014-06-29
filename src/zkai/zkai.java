/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.CommandDescription;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author User
 */
public class zkai extends com.springrts.ai.oo.AbstractOOAI {

    List<Unit> builders = new ArrayList();
    List<Unit> fighters = new ArrayList();
    List<Unit> radars = new ArrayList();
    List<Unit> units = new ArrayList();
    OOAICallback callback;
    public Unit com;
    Unit fac = null;
    UnitDef solar, mex, wind, cloaky, rector, glaive, rocko, warrior,radar;
    List<AIFloat3> availablemetalspots = new ArrayList();
    ThreatHandler threats = new ThreatHandler(this);
    int frame = 0;

    public void checkForMetal() {
        Resource metal = callback.getResources().get(0);
        availablemetalspots = callback.getMap().getResourceMapSpotsPositions(metal);
        if (availablemetalspots.isEmpty()) {
            debug("This is a map with no metal spots");
        } else {
            debug("This is a map with metal spots. Listing Values...");
            for (AIFloat3 metalspot : availablemetalspots) {
                metalspot.x += 20;
                metalspot.z += 50;
                debug("Metal Spot at X: " + metalspot.x + ", Y: " + metalspot.y + ", Z: " + metalspot.z);
            }
        }
    }

    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
        if (attacker == null || attacker.getUnitId() < 0) {

            threats.addPoint(unit.getPos(), damage);
        } else {
            if (attacker.getAllyTeam() == com.getAllyTeam()) {
                debug("damage added friend");
            }
            threats.addUnit(attacker, damage);
        }
        debug("was damaged " + damage);
        return 0;
    }

    public int enemyEnterLOS(Unit enemy) {
        if (enemy.getAllyTeam() == com.getAllyTeam()) {
            debug("enterlos added friend");
        }
        threats.addUnit(enemy, enemy.getHealth());
        return 0;
    }

    public int enemyLeaveLOS(Unit enemy) {
        threats.removeUnit(enemy);
        debug(enemy.getDef().getHumanName() + " left LOS");
        return 0;
    }

    public int enemyEnterRadar(Unit enemy) {
        if (enemy.getAllyTeam() == com.getAllyTeam()) {
            debug("enter radar added friend");
        }
        threats.addUnit(enemy, enemy.getHealth());
        return 0;
    }

    public int enemyLeaveRadar(Unit enemy) {
        threats.removeUnit(enemy);
        debug(enemy.getDef().getHumanName() + " left radar");
        return 0;
    }

    public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
        return 0;
    }

    public int enemyDestroyed(Unit enemy, Unit attacker) {
        threats.removeUnit(enemy);
        return 0;
    }

    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
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

        return 0;
    }

    @Override
    public int init(int teamId, OOAICallback callback) {
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
            if (def.getName().equals("armrectr")) {
                rector = def;
                debug("found rector");
            }
            if (def.getName().equals("armpw")) {
                glaive = def;
                debug("found glaive");
            }
            if (def.getName().equals("armsolar")) {
                solar = def;
                debug("found solar");

            }
            if (def.getName().equals("cormex")) {
                mex = def;
                debug("found mex");

            }
        }
        if (solar == null) {
            debug("didnt find wind nor solar");
        }
        callback.getGame().sendTextMessage("gl hf", 0);
        checkForMetal();
        return 0;
    }

    public static float dist(AIFloat3 a, AIFloat3 b) {
        return (float) (Math.pow(a.x - b.x, 2) + Math.pow(a.z - b.z, 2)); //+ Math.pow(a.y - b.y, 2)
    }

    public Unit closestUnitToRepair(AIFloat3 pos) {
        debug("getting closest unit to repair");
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        for (Unit u : units) {
            if (((u.isBeingBuilt() && callback.getEconomy().getCurrent(callback.getResources().get(1)) > 100) || 
                    u.getHealth() < u.getMaxHealth()) && dist(pos, u.getPos()) < mindis) {
                mindis = dist(pos, u.getPos());
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

    public Unit closestUnit(AIFloat3 pos) {
        float mindis = Float.MAX_VALUE;
        Unit res = null;
        try {
            debug("looking for mex");
            for (Unit u : units) {
                if (u.getDef().getName().equals("cormex")) {
                    debug("units contains mex");
                }
                if (dist(pos, u.getPos()) < mindis) {
                    mindis = dist(pos, u.getPos());
                    res = u;

                }
            }
        } catch (Exception ex) {
            debug("Exception during closestUnit: " + ex);
        }
        debug("closest unit is: " + res.getDef().getHumanName() + " at a distance of " + mindis + " - "
                + pos.x + "|" + pos.z + " -> " + res.getPos().x + "|" + res.getPos().z);
        return res;
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

    @Override
    public int unitIdle(Unit unit) {

        debug("i1");
        if (builders.contains(unit)) {
            if (fac == null) {
                if (unit.equals(com)) {
                    debug("plopping fac");
                    com.build(cloaky, callback.getMap().findClosestBuildSite(cloaky, com.getPos(), 400f, 0, 0), 0, (short) 0, Integer.MAX_VALUE);
                }
            } else {
                debug("i2");
                if (closestRadar(unit.getPos()) == null ||dist(closestRadar(unit.getPos()).getPos(),unit.getPos()) > 2000*2000){
                    debug("building radar");
                    unit.build(radar, callback.getMap().findClosestBuildSite(radar, unit.getPos(), 400f, 3, 0), 0, (short) 0, Integer.MAX_VALUE);
                }
                else if ((callback.getEconomy().getCurrent(callback.getResources().get(0)) > 200 || rnd.nextBoolean())
                        && closestUnitToRepair(unit.getPos()) != null) { //metal excess

                    debug("i2b");
                    unit.repair(closestUnitToRepair(unit.getPos()), (short) 0, Integer.MAX_VALUE);

                } else if (callback.getEconomy().getCurrent(callback.getResources().get(1)) < 320) {

                    debug("i2a");
                    unit.build(solar, callback.getMap().findClosestBuildSite(solar, unit.getPos(), 400f, 3, 0), 0, (short) 0, Integer.MAX_VALUE);
                    callback.getGame().sendTextMessage(unit.getDef().getHumanName() + " building solar", 0);
                    //unit.build(mex, closestMetalSpot(unit.getPos()),0, (short) 0, Integer.MAX_VALUE);
                } else {
                    debug("i2c");
                    debug("building mex at " + closestMetalSpot(unit.getPos()).x + "|" + closestMetalSpot(unit.getPos()).z);

                    unit.build(mex, closestMetalSpot(unit.getPos()), 0, (short) 0, Integer.MAX_VALUE);
                }
                debug("energy: " + callback.getEconomy().getCurrent(callback.getResources().get(1)));
            }
        }
        debug("i3");
        if (fighters.contains(unit)) {

            if (threats.getTarget(unit) != null) {
                unit.attack(threats.getTarget(unit), (short) 0, frame + 200);
            } else if (threats.getDanger(unit) != null) {
                unit.moveTo(threats.getDanger(unit), (short) 0, frame + 1000);
            } else {
                unit.moveTo(units.get(rnd.nextInt(units.size())).getPos(), (short) 0, frame + 200);
            }
        }
        return 0;
    }

    Random rnd = new Random();

    @Override
    public int update(int frame) {
        try {
            //debug("update");
            this.frame = frame;

            for (int unitsChecked = 0; unitsChecked < 10; unitsChecked++) {
                Unit u = units.get(rnd.nextInt(units.size()));//check a random unit each frame
                if (u.getCurrentCommands().isEmpty()) {
                    debug("1a");
                    boolean building = true;
                    for (CommandDescription cmd : u.getSupportedCommands()) {
                        if (cmd.getId() == 10) {
                            building = false;
                        }
                    }
                    if (building || u.getDef().getName().contains("fac")) {

                    } else {

                        //u.moveTo(u.getPos(), (short) 32, Integer.MAX_VALUE);
                        debug("2a");
                        unitIdle(u);
                        debug("found idling " + u.getDef().getHumanName());
                    }
                } else if (u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getTimeOut() < frame) {//attack
                    debug("1b");
                    debug("timeout " + u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getTimeOut() + " it is: " + frame);
                    if (u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getTimeOut() < frame) {
                        debug("timed out");
                        unitIdle(u);
                    }
                } else if (u.getCurrentCommands().get(u.getCurrentCommands().size() - 1).getId() == 10 && builders.contains(u)) {//move
                    debug("stopped constructor");
                    u.stop((short) 0, frame + 20);
                }
            }

            if (frame % 50 == 0) {
                debug("2");
                threats.decay(5f);
                if (fac != null && fac.getCurrentCommands().isEmpty()) {
                    if (callback.getEconomy().getCurrent(callback.getResources().get(0)) < 320 || (Math.round(Math.sqrt(builders.size()) / 3 * rnd.nextDouble()) > 0)) {
                        int r = rnd.nextInt(7);
                        if (r < 4) {
                            fac.build(glaive, fac.getPos(), 0, (short) 0, Integer.MAX_VALUE);
                        } else if (r < 6) {
                            fac.build(rocko, fac.getPos(), 0, (short) 0, Integer.MAX_VALUE);
                        } else if (r < 7) {
                            fac.build(warrior, fac.getPos(), 0, (short) 0, Integer.MAX_VALUE);
                        }
                        debug(callback.getEconomy().getCurrent(callback.getResources().get(0)) + " metal -> fighters");
                    } else {
                        fac.build(rector, fac.getPos(), 0, (short) 0, Integer.MAX_VALUE);
                        debug(callback.getEconomy().getCurrent(callback.getResources().get(0)) + " metal -> rector");
                    }
                }
            }
        } catch (Exception ex) {
            debug("Exception: " + ex.getMessage());
        }
        return 0;
    }

    public void debug(String s) {
        //callback.getGame().sendTextMessage(s, 0);
    }

    @Override
    public int unitCreated(Unit unit, Unit builder) {

        if (!units.contains(unit)) {
            units.add(unit);
        }
        if (unit.getDef().getName().equals("cormex")) {

            callback.getGame().sendTextMessage("created mex at " + unit.getPos().x + "|" + unit.getPos().z, 0);
            //removeClosestMetalSpot(unit.getPos());
        }
        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
        if (!units.contains(unit)) {
            units.add(unit);
        }
        if (unit.getDef().getName().contains("factory")) {
            fac = unit;
            debug("fac finished");
            AIFloat3 f3 = unit.getPos();
            f3.z += 200;
            f3.y += 80;
            unit.moveTo(f3, (short) 0, Integer.MAX_VALUE); //units move out of fac
        }
        if (unit.getDef().getName().contains("com")) {
            com = unit;
            callback.getGame().sendTextMessage("Found com", 0);
            builders.add(unit);
            unit.moveTo(unit.getPos(), (short) 0, Integer.MAX_VALUE); //forces idle call
            //com.build(cloaky, callback.getMap().findClosestBuildSite(cloaky, com.getPos(), 50f, 0, 0), 0, (short)0, Integer.MAX_VALUE);
        }
        if (unit.getDef().getName().equals("armpw")) {

            callback.getGame().sendTextMessage("Found glaive", 0);
            fighters.add(unit);
            //unit.moveTo(unit.getPos(), (short) 0, Integer.MAX_VALUE); //forces idle call
        }
        if (unit.getDef().equals(rocko)) {

            callback.getGame().sendTextMessage("Found rocko", 0);
            fighters.add(unit);
            //unit.moveTo(unit.getPos(), (short) 0, Integer.MAX_VALUE); //forces idle call
        }
        if (unit.getDef().equals(radar)) {

            callback.getGame().sendTextMessage("Found radar", 0);
            radars.add(unit);
            //unit.moveTo(unit.getPos(), (short) 0, Integer.MAX_VALUE); //forces idle call
        }
        if (unit.getDef().equals(warrior)) {

            callback.getGame().sendTextMessage("Found warrior", 0);
            fighters.add(unit);
            //unit.moveTo(unit.getPos(), (short) 0, Integer.MAX_VALUE); //forces idle call
        }
        if (unit.getDef().getName().equals("armrectr")) {

            callback.getGame().sendTextMessage("Found rector", 0);
            builders.add(unit);
            //unit.moveTo(unit.getPos(), (short) 0, Integer.MAX_VALUE); //forces idle call
        }

        return 0;
    }

}
