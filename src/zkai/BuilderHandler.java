    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import zkai.Area.Owner;

/**
 *
 * @author User
 */
public class BuilderHandler {

    final float MAX_PYLONRANGE = 500;

    zkai parent;

    List<Builder> builders;
    List<Builder> idlers;

    List<Order> pending;

    List<Unit> mexes;
    List<Unit> buildings;
    List<Unit> factories;
    List<Node> virtualRadars;
    List<Unit> underConstruction;
    List<Node> grid;

    float energyUnderConstruction = 0;
    float reserveEnergy = 2;
    static Resource metal, energy;

    float antiAirMult = 1;

    TreeMap<Integer, Float> pylonrange;

    int lastOrderId = 0;
    int antinukes = 0;

    public BuilderHandler(zkai parent) {
        this.parent = parent;
        builders = new ArrayList();
        idlers = new ArrayList();
        mexes = new ArrayList();
        underConstruction = new ArrayList();
        buildings = new ArrayList();
        pending = new ArrayList();
        factories = new ArrayList();
        metal = parent.callback.getResources().get(0);
        energy = parent.callback.getResources().get(1);
        grid = new ArrayList();
        virtualRadars = new ArrayList();

        //init hardcoded pylonranges
        pylonrange = new TreeMap();
        pylonrange.put(parent.mex.getUnitDefId(), 50f);
        pylonrange.put(parent.solar.getUnitDefId(), 100f);
        pylonrange.put(parent.wind.getUnitDefId(), 60f);
        pylonrange.put(parent.fusion.getUnitDefId(), 150f);

        parent.debug("DEBUG: " + pylonrange.get(parent.mex.getUnitDefId()));

        pending.add(new Build(new AIFloat3(-1, -1, -1), parent.cloaky, -1, lastOrderId++));
    }

    public void unitIdle(Unit u) {
        for (Builder b : builders) {
            if (b.unit.getUnitId() == u.getUnitId()) {
                b.idle();
            }
        }
    }

    public void unitKilled(Unit u) {
        if (Math.random() < 0.15) {
            //pending.add(new Build(u.getPos(),parent.defender,-1,lastOrderId++));
        }
        if (Math.random() < 0.6) {
            pending.add(new Fight(u.getPos(), -1, lastOrderId++));
        }
        for (Builder b : builders) {
            if (b.unit.getUnitId() == u.getUnitId()) {
                b.update(parent.frame);
                if (b.order != null) {
                    pending.add(b.order);
                }
                builders.remove(b);
                break;
            }
        }
        for (Unit m : mexes) {
            if (m.getUnitId() == u.getUnitId()) {
                mexes.remove(m);
                break;
            }
        }
        for (Node m : grid) {
            if (m.unit != null && m.unit.getUnitId() == u.getUnitId()) {
                grid.remove(m);
                break;
            }
        }
        for (Node m : virtualRadars) {
            if (m.unit != null && m.unit.getUnitId() == u.getUnitId()) {
                grid.remove(m);
                break;
            }
        }
        for (Unit m : buildings) {
            if (m.getUnitId() == u.getUnitId()) {
                buildings.remove(m);
                break;
            }
        }
        for (Unit m : factories) {
            if (m.getUnitId() == u.getUnitId()) {
                factories.remove(m);
                break;
            }
        }
    }

    public void unitCreated(Unit u, Unit builder) {
        Builder b = null;
        for (Builder bb : builders) {
            if (bb.unit.equals(builder)) {
                b = bb;
                underConstruction.add(u);
            }
        }
        if (b != null && b.order != null) {
            for (Node n : grid) {
                if (n.buildOrder != null && n.buildOrder.equals(((Build) b.order))) {
                    n.unit = u;
                    n.buildOrder = null;
                    n.pos = u.getPos();
                    //parent.label(u.getPos(), "node established");
                    b.order.timeout = parent.frame + 500;
                    break;
                }

            }/**/;// <- do not delete
            for (Node n : virtualRadars) {
                if (n.buildOrder != null && n.buildOrder.equals(((Build) b.order))) {
                    n.unit = u;
                    n.buildOrder = null;
                    parent.label(u.getPos(), "radar node established");
                    b.order.timeout += (int) (((Build) b.order).building.getBuildTime() * 30);
                    break;
                }

            }
        }
    }

    public void unitFinished(Unit u) {

        if (u.getDef().getName().contains("factory")) {
            factories.add(u);
        }
        if (underConstruction.contains(u)) {
            underConstruction.remove(u);
        }
        if (u.getDef().equals(parent.mex)) {
            mexes.add(u);
            buildings.add(u);
            //parent.label(u.getPos(), "new mex");
        }
        if (u.getDef().equals(parent.solar)) {
            buildings.add(u);
            energyUnderConstruction -= 2;
        }
        if (u.getDef().equals(parent.rector)) {
            builders.add(new Builder(u));
        }
        if (u.getDef().getName().contains("com")) {
            builders.add(new Builder(u));
        }
        if (u.getDef().equals(parent.defender)) {
            buildings.add(u);
        }
        if (u.getDef().equals(parent.llt)) {
            buildings.add(u);
        }
        if (u.getDef().equals(parent.hlt)) {
            buildings.add(u);
        }
        if (u.getDef().equals(parent.razor)) {
            buildings.add(u);
        }
        if (u.getDef().equals(parent.bertha)) {
            buildings.add(u);
        }
        if (u.getDef().getName().contains("factory")) {
            buildings.add(u);
        }

    }

    //HERE STARTS CODE FROM BENJAMIN
    /**
     * Finds the distance to the closest builder in O(n)
     *
     * @param bu the builder which is excluded from the lookup
     * @param trg the point to compare the distance to
     * @return the minimal distance
     * @author Benjamin Schmid
     * @license WTFPL
     */
    private double closestBuilder(Builder bu, AIFloat3 trg) {
        // initialize the minimum
        double min = Double.MAX_VALUE;
        // check if builders exist
        // but the try/catch was written by DeinFreund
        try {
            if (builders == null) {
                throw new IllegalArgumentException("fuck you");
            }
        } catch (Exception ex) {
            System.exit(~0);
        }
        // iterate through all the builders in O(n)
        for (Builder b : builders) {
            // check if it is equals to the given builder
            if (b.equals(bu)) {
                continue;
            }
            // set the new minimum
            AIFloat3 t;//t stores the expected future position of b
            if (b.order != null) {
                t = b.order.getPosition();
            } else {
                t = b.unit.getPos();
            }
            min = Math.min(min, zkai.dist(t, trg));
        }
        // return the minimum
        if (min == Double.MAX_VALUE) {
            return 0;
        }
        return min;
    }
    //HERE IT'S OVER
    int bertha = 0;

    public UnitDef getDefenseTower(AIFloat3 pos) {
        if (parent.defense.getDefense(pos) * antiAirMult > 200 && parent.rnd.nextInt(3) == 0) {
            return parent.razor;
        }
        if (parent.defense.getDefense(pos) * antiAirMult > 100 && parent.defense.getDefense(pos) < 300) {
            return parent.razor;
        }
        if (parent.defense.getDefense(pos) > 300) {
            return parent.hlt;
        }
        if (parent.defense.getValue(pos) / parent.defense.maxVal > 0.42 || parent.defense.getDefense(pos) > 200) {
            return parent.llt;
        }
        return parent.defender;
    }

    public boolean inRadarRange(AIFloat3 pos) {
        //parent.debug("requesting radar coverage");
        for (Node n : virtualRadars) {
            if (zkai.dist(n.pos, pos) < n.range * n.range / 2) {
                return true;
            }
        }
        return false;
    }

    public void update(int frame) {
        for (int j = 0; j < 5 && j < builders.size(); j++) {//active check for timed out builders
            int i = (int) (parent.rnd.nextDouble() * builders.size());
            builders.get(i).update(frame);
        }
        /*for (int j = 0; j < 2 && j < idlers.size(); j++) {
         int i = (int) (parent.rnd.nextDouble() * idlers.size());
         if (i >= idlers.size()) {
         parent.debug("Warning: weird idlers stuff");
         continue;
         }
         float maxscore = 0;
         Order best = null;
         for (Order o : pending) {
         float score;
         score = 5e-7f*(float) Math.sqrt(closestBuilder(idlers.get(i), o.getPosition()));
         float a,b;
         a = score;
         switch (o.id) {
         case 40:
         //parent.debug("found repair order");
         score += 1f /  Math.sqrt(zkai.dist(idlers.get(i).unit.getPos(), ((Repair) o).target.getPos()));
         score *= 1.5;
         break;
         case 16:
         //parent.debug("found repair order");
                        
         score += 1f /  Math.sqrt(zkai.dist(idlers.get(i).unit.getPos(), o.getPosition()));
         score *= 1;
         if (zkai.dist(idlers.get(i).unit.getPos(), factories.get(0).getPos()) < 500*500) score = 0;
         break;
         default:
         if (o.id >= 0) {
         parent.debug("Warning: invalid build order");
         }
         if (((Build) o).position == null) {
         parent.debug("position is null");
         }
         if (((Build) o).position.x < 0) {
         score += 0.001;
         } else {
         score += Math.min(1f, 1f /  Math.sqrt(zkai.dist(idlers.get(i).unit.getPos(), o.getPosition())));
         }
         b = score;
         if (( ((Build)o).building.equals(parent.solar))) 
         if (parent.callback.getEconomy().getCurrent(energy) > 250) score /= 50;
         else score*=50;
         parent.debug(((Build) o).building.getHumanName() + "(" + o.orderId + "): " + a  + " + " + b +" => "+ score);
         }
         if (score > maxscore) {
         maxscore = score;
         best = o;
         }
         }
         if (best != null) {
         parent.debug("best is " + best.orderId);
         boolean done;
         switch (best.id) {
         case 40:
         done = idlers.get(i).repair(((Repair) best).target, frame + 400, best.orderId);
         break;
         case 16:
         done = idlers.get(i).fight(best.getPosition(), frame + 700, best.orderId);
         break;
         default:
         float radius = 800;
         int space = 6;
         if (((Build) best).building.equals(parent.mex)) {
         radius = 0;
         space = 1;
         }
         AIFloat3 pos = ((Build) best).position;
         if (pos.x < 0) {
         pos = idlers.get(i).unit.getPos();
         }
         done = idlers.get(i).build(pos, ((Build) best).building, radius, space, frame + 500, best.orderId);

         //parent.label(pos, "building here");
                        
         if (!inRadarRange(pos)){
         pending.add(new Build(pos,parent.radar,-1,lastOrderId++));
         virtualRadars.add(new Node((Build)pending.get(pending.size()-1),pos,parent.radar.getRadarRadius()));
         }

         }
         if (done || !done) {
         if (!(best.id < 0 && best.getPosition().x  < 0))
         pending.remove(best);
         }
         if (!done) {
         if (best.id == 40) {
         if (((Repair) best).target != null &&((Repair) best).target.getDef()!=null ){
         parent.debug("Warning: Repairing " + ((Repair) best).target.getDef().getHumanName() + " failed");
         }else{
         parent.debug("Warning: Repairing failed because target is missing");
         }
         } else {
         parent.debug("Warning: Building " + ((Build) best).building.getHumanName() + " failed");
         }
         }

         } else {

         parent.debug("nothing to do");
         }
         }*/
        energyUnderConstruction /= 1.002;
        energyUnderConstruction = Math.max(energyUnderConstruction, 0);
        reserveEnergy *= 1.00008;
        if (parent.frame % 37000 == 0 && Area.getAllied().size() > 0.2 * Area.areas.size()) {

            antinukes++;
        }
        if (parent.frame % 30000 == 0 && Area.getAllied().size() > 0.2 * Area.areas.size()) {
            bertha++;
        } else if (parent.frame % 15000 == 0 && Area.getAllied().size() > 0.7 * Area.areas.size()) {
            bertha++;
        }
        if (parent.frame % 100 == 0) {
            parent.debug("Energy under construction: " + energyUnderConstruction);

        }
        if (frame % 42 == 0) {
            AIFloat3 pos = new AIFloat3((float) Math.random() * parent.callback.getMap().getWidth() * 8, 0, (float) Math.random() * parent.callback.getMap().getHeight() * 8);
            if ((parent.defense.getValue(pos, 500) > 100 + 2 * parent.defense.getDefense(pos)) && parent.frame > 10000) {
                boolean busy = false;
                for (Order o : pending) {
                    if (o.id == -parent.llt.getUnitDefId() || o.id == -parent.hlt.getUnitDefId() || o.id == -parent.defender.getUnitDefId()
                            || o.id == -parent.bertha.getUnitDefId()) {
                        busy = true;
                    }
                }
                if (!busy) {
                    parent.debug("Building Lotus because " + parent.defense.getValue(pos) / parent.defense.maxVal + " - 0.5 > "
                            + parent.defense.getDefense(pos) / Math.max(0.0001, parent.defense.maxDef));

                    pending.add(new Build(pos, getDefenseTower(pos), -1, lastOrderId++));
                }
            }
        }
        if (frame % 100 == 42) {
            /*
             if (parent.callback.getEconomy().getIncome(energy) < parent.callback.getEconomy().getUsage(energy)
             && parent.callback.getEconomy().getCurrent(energy) < 250) {
             parent.debug("upgrading energy");
             upgradeEnergy(parent.callback.getEconomy().getUsage(energy) - parent.callback.getEconomy().getIncome(energy) - energyUnderConstruction);

             }*/
            if (parent.callback.getEconomy().getCurrent(metal) > 300 && parent.callback.getEconomy().getCurrent(energy) > 300 && parent.frame > 1000) {
                boolean buildingNano = false;
                for (Order o : pending) {
                    if (o.id == -parent.nano.getUnitDefId()) {
                        buildingNano = true;
                    }
                }
                if (!buildingNano) {
                    pending.add(new Build(factories.get(0).getPos(), parent.nano, -1, lastOrderId++));
                }
            } else {
                /*parent.debug("upgrading metal");
                 List<Order> pendingc = new ArrayList();
                 pendingc.addAll(pending);
                 for (Order o : pendingc) {
                 if (o.id == -parent.mex.getUnitDefId()) {
                 grid.remove(findNode((Build) o));
                 pending.remove(o);
                 }
                 }
                 List<AIFloat3> newMexes = new ArrayList();
                 for (int i = 0; i < builders.size(); i++) {
                 AIFloat3 next = parent.expansion.nextMex(newMexes);
                 if (next != null) {
                 Build b = new Build(next, parent.mex, -1, lastOrderId++);
                 pending.add(b);
                 grid.add(new Node(b, b.position, pylonrange.get(b.building.getUnitDefId())));
                 newMexes.add(next);
                 }
                 }
                 */
            }
        }
        if (frame % 100 == 0) {
            List<Node> invalidNodes = new ArrayList();
            for (Node n : grid) {
                if (n.buildOrder != null) {
                    boolean valid = false;
                    for (Builder b : builders) {
                        if (b.order != null && b.order.equals(n.buildOrder)) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) {
                        invalidNodes.add(n);
                    }
                }
            }
            grid.removeAll(invalidNodes);

            for (Unit b : underConstruction) {
                boolean found = false;
                for (Order o : pending) {
                    if (o.id == 40 && ((Repair) o).target.getUnitId() == b.getUnitId()) {
                        found = true;

                    }
                }
                if (!found) {
                    pending.add(new Repair(b, -1, lastOrderId++));
                }
            }

            parent.debug(mexes.size() + " mexes available.");
            for (Unit m : mexes) {
                getConnectedBuildings(findNode(m));
                parent.debug("bfs on mex");
            }/*
             for (Order o : pending) {
             if (o.orderId < 0) {//build order
             Build b = (Build) o;
             if (b.building.equals(parent.solar) && b.getPosition().x < 0) {
             pending.remove(o);
             grid.remove(findNode(b));
             upgradeEnergy(2);
             energyUnderConstruction -= 2;
             }
             }
             }*/

        }
    }

    Node findNode(Build b) {
        for (Node n : grid) {
            if (n.buildOrder == null) {
                continue;
            }
            if (n.buildOrder.equals(b)) {
                return n;
            }
        }
        return null;
    }

    Node findNode(Unit u) {
        for (Node n : grid) {
            if (n.unit == null) {
                continue;
            }
            if (n.unit.equals(u)) {
                return n;
            }
        }
        return null;
    }

    List<Node> getConnectedBuildings(Node start) {
        List<Node> result = new ArrayList();
        //parent.label(start.pos, "starting bfs from here");
        //parent.label(start.unit.getPos(), "= here");
        if (start == null) {
            return result;
        }
        List<Node> added = new ArrayList();
        added.add(start);
        while (!added.isEmpty()) {
            result.addAll(added);
            List<Node> newadded = new ArrayList();
            for (Node u : added) {
                //parent.debug("starting new bfs from " + u.getDef().getHumanName());
                //parent.debug("pylonrange: " + pylonrange.get(u.getDef().getUnitDefId()));
                //String msg = "range: " + (pylonrange.get(u.getDef().getUnitDefId()) + MAX_PYLONRANGE) + " -> "
                //       + parent.callback.getFriendlyUnitsIn(u.getPos(), pylonrange.get(u.getDef().getUnitDefId())+ MAX_PYLONRANGE).size() + " units";
                //parent.debug(msg);
                //parent.label(u.getPos(), msg);
                for (Node o : grid) {
                    if (!result.contains(o) && !newadded.contains(o)
                            && u.range + o.range >= Math.sqrt(zkai.dist(u.pos, o.pos))) {
                        newadded.add(o);
                        parent.callback.getMap().getDrawer().addLine(u.pos, o.pos);
                        //parent.label(o.getPos(), "connected to mex");
                    }
                }

            }
            added = newadded;
        }
        return result;
    }

    Build upgradeEnergy(Area a, UnitDef unitdef) {
        //solar
        //UnitDef unitdef = parent.solar;
        float minval = Float.MAX_VALUE;
        Unit best = null;
        for (Unit m : mexes) {
            if (Area.getArea(m.getPos()) != a) {
                continue;
            }
            List<Node> conn = getConnectedBuildings(findNode(m));
            float e = 0;
            for (Node u : conn) {
                if (u.unit == null) {
                    e += 2;
                } else {
                    e += u.unit.getResourceMake(energy);
                    e -= u.unit.getResourceUse(energy);
                }
            }
            if (e < minval) {
                best = m;
            }
        }
        if (best != null) {
            List<Node> conn = getConnectedBuildings(findNode(best));
            float mindist = Float.MAX_VALUE;
            Node u1 = null, u2 = null;
            for (Node u : conn) {
                for (Node b : grid) {
                    if (conn.contains(b)) {
                        continue;
                    }
                    if (zkai.dist(u.pos, b.pos) < mindist) {
                        mindist = zkai.dist(u.pos, b.pos);
                        u1 = u;
                        u2 = b;
                    }
                }
            }
            if (u1 != null && u2 != null) {
                AIFloat3 vec = new AIFloat3(u1.pos);
                vec.interpolate(u2.pos, (float) (pylonrange.get(unitdef.getUnitDefId()) * 0.75 / Math.sqrt(zkai.dist(u1.pos, u2.pos))));
                Build b = new Build(vec, unitdef, -1, lastOrderId++);
                return b;
                //parent.label(vec, "possible solar");
            }
        }
        Build b = new Build(new AIFloat3(-1, -1, -1), unitdef, -1, lastOrderId++);
        return b;

    }

    AIFloat3 findClosestBuildPos(AIFloat3 pos, UnitDef unit) {
        pos = new AIFloat3(pos);
        if (unit.equals(parent.mex)) {
            return pos;
        }
        pos = parent.callback.getMap().findClosestBuildSite(unit, pos, 800, 6, 0);
        int mult = 1;
        if (unit.equals(parent.cloaky)) mult = 4;
        while (zkai.dist(pos, parent.closestMetalSpot(pos)) < 70 * 70 * mult) {
            pos.x +=( parent.rnd.nextInt(40) - 20)*mult;
            pos.z += (parent.rnd.nextInt(40) - 20)*mult;
            pos = parent.callback.getMap().findClosestBuildSite(unit, pos, 800, 6, 0);
        }
        return pos;

    }

    List<Area> berthaAreas = new ArrayList();

    Area getBerthaArea(int index) {
        while (index > berthaAreas.size()) {
            if (index == 1) {
                List<Area> as = Area.getArea(factories.get(0).getPos()).getNearbyAreas(1);
                as.add(Area.getArea(factories.get(0).getPos()));
                float maxh = -100;
                Area best = null;
                for (Area a : as) {
                    if (Area.getHeight(a.getDefensePos(parent.bertha)) > maxh) {
                        maxh = Area.getHeight(a.getDefensePos(parent.bertha));
                        best = a;
                    }
                }
                berthaAreas.add(best);
            } else {
                List<Area> as = new ArrayList();
                for (Area a : Area.areas) {
                    if (a.closestOfOwner(Owner.enemy).gridDistance(a) >= 3 && a.closestOfOwner(Owner.enemy).gridDistance(a) < 6) {
                        as.add(a);
                    }
                }
                float maxh = -100;
                Area best = null;
                for (Area a : as) {
                    if (Area.getHeight(a.getDefensePos(parent.bertha)) > maxh) {
                        maxh = Area.getHeight(a.getDefensePos(parent.bertha));
                        best = a;
                    }
                }
                berthaAreas.add(best);
            }
        }
        return berthaAreas.get(index - 1);
    }

    List<Area> antinukeAreas = new ArrayList();

    Area getAntinukeArea(int index) {
        if (index > antinukeAreas.size()) {
            if (parent.threats.antinukes.isEmpty()) {
                antinukeAreas.add(Area.getArea(factories.get(0).getPos()));

            } else {

                antinukeAreas.add(Area.getAllied().get(parent.rnd.nextInt(Area.getAllied().size())));
            }
        }
        return antinukeAreas.get(index - 1);
    }

    final class Builder {

        Unit unit;
        Order order;
        Action action;

        public Builder(Unit u) {
            unit = u;
            idle();
        }

        public void idle() {
            order = null;

            //idlers.add(this);
            //parent.label(unit.getPos(), "idle");
            if (action == null) {
                if (factories.isEmpty()) {
                    boolean possible = true;
                    for (Builder b : builders) {
                        if (b.action != null && b.action.getAction() == Actions.buildFactory) {
                            possible = false;
                        }
                    }
                    if (possible) {
                        boolean done = build(findClosestBuildPos(unit.getPos(), parent.cloaky), parent.cloaky, parent.frame + 2500, lastOrderId++);
                        if (done) {
                            action = new BuildFactory(Area.getArea(unit.getPos()));
                        }
                    }
                }
                boolean possible = true;
                for (Builder b : builders) {
                    if (b.action != null && b.action.getAction() == Actions.buildFactory) {
                        possible = false;
                    }
                }
                if (action == null && parent.frame > 3000 && (parent.callback.getEconomy().getCurrent(energy)
                        > 380) && parent.callback.getEconomy().getCurrent(metal) > 150 && factories.size() > 0
                        && possible) {

                    if (zkai.dist(unit.getPos(), factories.get(0).getPos()) < 500 * 500) {
                        action = new BuildFactory(Area.getArea(factories.get(0).getPos()));
                    } else {
                        parent.requestConstructor();
                    }
                }
                if (action == null) {
                    int porcers = 0;
                    for (Builder b : builders) {
                        if (b.action != null && (b.action.getAction() == Actions.porc || b.action.getAction() == Actions.bertha)) {
                            porcers++;
                        }
                    }
                    if (antinukes > parent.threats.antinukes.size() && porcers < builders.size() / 2 && zkai.DEFENSES) {
                        parent.debug("planning on building antinuke");
                        action = new Antinuke(getAntinukeArea(antinukes), antinukes);

                    } else if (bertha > parent.threats.berthas.size() && porcers < builders.size() / 2 && zkai.DEFENSES) {
                        parent.debug("planning on building bertha");
                        action = new Bertha(getBerthaArea(bertha), bertha);

                    } else if (porcers < builders.size() / 5f && zkai.DEFENSES && parent.frame > 32000 / antiAirMult) {
                        float best = -1;
                        Area besta = null;
                        for (Area a : Area.areas) {
                            if (a.owner != Owner.ally) {
                                continue;
                            }
                            float val = parent.threats.getDanger(a.getCoords()) * parent.rnd.nextFloat();
                            if (val > best && a.isBorder() && parent.defense.getDefense(a.getCoords()) * 3 < Math.sqrt(a.danger) + parent.threats.getValue(a.getCoords(), 5 * a.getRadius())) {
                                best = val;
                                besta = a;
                            }
                        }
                        if (besta != null) {
                            parent.debug((parent.defense.getDefense(besta.getCoords()) * 3) + " < " + (besta.danger / 2 + parent.threats.getValue(besta.getCoords(), 5 * besta.getRadius())));
                            action = new Porc(besta);
                        }
                    }
                }
                //parent.debug("would upgrade energy: " + String.valueOf(parent.callback.getEconomy().getIncome(energy) + energyUnderConstruction < parent.callback.getEconomy().getUsage(energy) + reserveEnergy));

                int ebuilders = 0;
                for (Builder b : builders) {
                    if (b.action != null && b.action.getAction() == Actions.buildEnergy) {
                        ebuilders++;
                    }
                }
                if (action == null && (!(parent.frame < 5000) || ebuilders * 5 < builders.size()) && parent.frame > 500
                        && ((parent.callback.getEconomy().getIncome(energy) + energyUnderConstruction < parent.callback.getEconomy().getUsage(energy)
                        + reserveEnergy && parent.callback.getEconomy().getCurrent(energy) < 100) || parent.callback.getEconomy().getIncome(energy) < reserveEnergy)) {
                    float best = Float.MAX_VALUE;
                    Area besta = null;
                    Area simplea = null;
                    float bestsimple = Float.MAX_VALUE;
                    for (Area a : Area.areas) {
                        if (a.getOwner() != Owner.ally) {
                            continue;
                        }
                        Build b = upgradeEnergy(a, parent.solar);
                        float dist = zkai.dist(a.getCoords(), unit.getPos());
                        if (b.getPosition().x < 0 && dist < bestsimple) {
                            bestsimple = dist;
                            simplea = a;
                        }
                        if (b.getPosition().x >= 0 && dist < best) {
                            best = dist;
                            besta = a;
                        }
                    }
                    if (besta != null) {
                        float amt = parent.callback.getEconomy().getUsage(energy) + reserveEnergy
                                - (parent.callback.getEconomy().getIncome(energy) + energyUnderConstruction);
                        amt /= 1.5;
                        energyUnderConstruction += amt;
                        action = new BuildEnergy(besta, amt);
                        parent.label(besta.getCoords(), "building " + amt + " e");
                    } else if (simplea != null) {
                        action = new BuildEnergy(simplea, 2f);
                        parent.label(simplea.getCoords(), "building 2 e");
                    }
                }

                if (action == null) {

                    float best = Float.MAX_VALUE;
                    Area besta = null;
                    for (Area a : Area.areas) {
                        if (a.getOwner() != Owner.ally) {
                            continue;
                        }
                        if (a.getFreeMexes().size() > 0 || !inRadarRange(a.getCoords())) {
                            boolean occupied = false;
                            for (Builder b : builders) {
                                if (b.action != null && b.action.getAction() == Actions.buildMexes && b.action.getArea() == a) {
                                    occupied = true;
                                }
                            }
                            if (occupied) {

                                //parent.label(a.getCoords(), "occupied here");
                                continue;
                            }
                            float dist = (float) (Math.sqrt(zkai.dist(a.getCoords(), unit.getPos())) / Math.sqrt(Math.max(a.getFreeMexes().size(), 1))
                                    * (parent.threats.getDanger(a.getCoords()) / parent.threats.maxval));
                            //parent.label(a.getCoords(), dist+ " pts");
                            if (dist < best) {
                                best = dist;
                                besta = a;
                            }
                        } else {
                            //parent.label(a.getCoords(), "No free mexes here");
                        }
                    }
                    if (besta != null) {
                        besta.owner = Owner.ally;
                        action = new BuildMexes(besta);
                    } else {
                        parent.label(unit.getPos(), "no mexing possibilities");
                    }
                }
                if (action == null) {
                    if (Math.random() < 0.5) {
                        action = new ReclaimAction(Area.getAllied().get(parent.rnd.nextInt(Area.getAllied().size())));
                        unit.reclaimInArea(action.area.getCoords(), action.area.getRadius(), (short) 0, parent.frame + 2000);
                        order = new Move(new AIFloat3(), parent.frame + 2000, lastOrderId++);
                    } else {

                        action = new RepairAction(Area.getAllied().get(parent.rnd.nextInt(Area.getAllied().size())));
                    }
                }

            } else {
                if (action.area.owner != Owner.ally) {
                    action = null;
                } else {
                    switch (action.getAction()) {
                        case buildFactory:
                            if (parent.callback.getEconomy().getCurrent(metal) < 150 || parent.callback.getEconomy().getCurrent(energy) < 350 || parent.frame < 1000) {
                                action = null;
                            } else {
                                build(new Build(factories.get(0).getPos(), parent.nano, -1, lastOrderId++));
                            }
                            break;
                        case buildMexes:
                            parent.label(unit.getPos(), "mex");

                            if (!inRadarRange(action.getArea().getCoords())) {
                                AIFloat3 pos = findClosestBuildPos(action.getArea().getDefensePos(parent.radar), parent.radar);
                                build(pos, parent.radar, parent.frame + 700, lastOrderId++);
                                virtualRadars.add(new Node((Build) order, parent.radar.getRadarRadius()));
                            } else {
                                float best = Float.MAX_VALUE;
                                AIFloat3 mex = null;
                                for (AIFloat3 m : action.area.getFreeMexes()) {
                                    float dist = zkai.dist(m, unit.getPos());
                                    if (dist < best) {
                                        best = dist;
                                        mex = m;
                                    }
                                }
                                if (mex == null) {
                                    action = null;//all mexes capped
                                    //parent.label(unit.getPos(), "no more mex");
                                } else {
                                    //parent.label(unit.getPos(), "building mex");
                                    build(mex, parent.mex, parent.frame + 500, lastOrderId++);//parent.callback.getMap().findClosestBuildSite(parent.mex, mex, 10, 0, 0)
                                }
                            }
                            break;
                        case buildEnergy:
                            parent.label(unit.getPos(), "e");
                            if ((parent.callback.getEconomy().getIncome(energy) + energyUnderConstruction - ((BuildEnergy) action).amount
                                    > parent.callback.getEconomy().getUsage(energy) + reserveEnergy && parent.callback.getEconomy().getIncome(energy)
                                    > reserveEnergy) || ((BuildEnergy) action).amount <= 0) {
                                energyUnderConstruction -= ((BuildEnergy) action).amount;
                                energyUnderConstruction = Math.max(energyUnderConstruction, 0);
                                parent.debug("aborted building energy");
                                action = null;
                            } else {
                                Unit rep = null;
                                for (Unit u : parent.callback.getFriendlyUnitsIn(unit.getPos(), 500)) {
                                    if (u.isBeingBuilt() && pylonrange.containsKey(u.getDef().getUnitDefId())) {
                                        rep = u;
                                    }
                                }
                                Builder assist = null;
                                for (Builder b : builders) {
                                    if (b.equals(this)) {
                                        continue;
                                    }
                                    if (b.action != null && b.action.getAction() == Actions.buildEnergy && b.action.getArea() == action.getArea()
                                            && b.order != null && b.order.id < 0) {
                                        assist = b;
                                    }
                                }
                                if (assist != null) {
                                    ((BuildEnergy) assist.action).amount += ((BuildEnergy) action).amount;

                                    ((BuildEnergy) action).amount = 0;
                                    guard(assist.unit, 300 * (int) ((BuildEnergy) assist.action).amount, lastOrderId++);
                                } else if (rep == null) {
                                    UnitDef def;
                                    if (parent.callback.getEconomy().getIncome(energy) > 100 && parent.rnd.nextInt(7) == 0) {
                                        def = parent.fusion;
                                        ((BuildEnergy) action).amount -= 35;
                                    } else {
                                        def = parent.solar;
                                        ((BuildEnergy) action).amount -= 2;
                                    }
                                    build(upgradeEnergy(action.getArea(), def));
                                } else {
                                    repair(rep, parent.frame + 800, lastOrderId++);
                                }
                            }
                            break;
                        case porc:
                            parent.label(unit.getPos(), "porc");
                            parent.debug("porcing" + zkai.DEFENSES);
                            if (parent.defense.getDefense(action.area.getCoords()) * 3 >= Math.sqrt(action.area.danger) + parent.threats.getValue(action.area.getCoords(), 5 * action.area.getRadius())) {
                                parent.debug(parent.defense.getDefense(action.area.getCoords()) * 3 + " > " + (action.area.danger / 2 + parent.threats.getValue(action.area.getCoords(), 5 * action.area.getRadius())));
                                action = null;

                            } else {
                                Builder assist = null;
                                for (Builder b : builders) {
                                    if (b.action != null && b.action.getAction() == Actions.porc && b.action.area == action.area && b.order != null
                                            && b.order.id < 0) {
                                        assist = b;
                                    }
                                }
                                if (assist == null) {
                                    UnitDef def = getDefenseTower(action.area.getCoords());
                                    build(new Build(action.area.getDefensePos(def), def, parent.frame + 2000, lastOrderId++));
                                } else {
                                    guard(assist.unit, parent.frame + 500, lastOrderId++);
                                }
                            }
                            break;
                        case reclaim:
                            parent.label(unit.getPos(), "reclaim");
                            action = null;
                            break;
                        case repair:
                            parent.label(unit.getPos(), "repair");
                            boolean busy = false;
                            for (Unit u : parent.callback.getFriendlyUnitsIn(action.area.getCoords(), action.area.getRadius())) {
                                if (u.getUnitId() != unit.getUnitId() && u.getHealth() < u.getMaxHealth() && !(u.isBeingBuilt() && zkai.dist(u.getPos(), factories.get(0).getPos()) < 100 * 100)) {
                                    repair(u, parent.frame + 500, lastOrderId++);
                                    busy = true;
                                    break;
                                }
                            }
                            if (!busy) {
                                action = null;
                            }
                            break;
                        case bertha:
                            parent.debug("building bertha");
                            Bertha bb = (Bertha) action;
                            if (bb.index <= parent.threats.berthas.size()) {
                                action = null;
                                break;
                            }
                            if (zkai.dist3d(unit.getPos(), action.area.getDefensePos(parent.bertha)) > 120 * 120) {
                                if (zkai.dist3d(unit.getPos(), action.area.getDefensePos(parent.bertha)) > 500 * 500){
                                    move(parent.threats.randomize(action.area.getDefensePos(parent.bertha),300), parent.frame + 200, lastOrderId++);
                                }else{
                                    
                                    move(action.area.getDefensePos(parent.bertha), parent.frame + 200, lastOrderId++);
                                }
                            } else {

                                Builder assist = null;
                                for (Builder b : builders) {
                                    if (b.action != null && b.action.getAction() == Actions.bertha && ((Bertha) b.action).index == bb.index
                                            && b.order != null && b.order.id < 0) {
                                        assist = b;
                                    }
                                }
                                if (assist != null) {
                                    guard(assist.unit, parent.frame + 500, lastOrderId++);
                                } else {
                                    build(new Build(action.area.getDefensePos(parent.bertha), parent.bertha, parent.frame + 2000, lastOrderId++));
                                }
                            }
                            break;
                        case antinuke:
                            parent.debug("building antinuke");
                            if (antinukes <= parent.threats.antinukes.size()) {
                                action = null;
                                break;
                            }
                            Antinuke an = (Antinuke) action;
                            Builder ass = null;
                            for (Builder b : builders) {
                                if (b.action != null && b.action.getAction() == Actions.antinuke && ((Antinuke) b.action).index == an.index
                                        && b.order != null && b.order.id < 0) {
                                    ass = b;
                                }
                            }
                            if (ass != null) {
                                guard(ass.unit, parent.frame + 500, lastOrderId++);
                            } else {
                                build(new Build(action.area.getCoords(), parent.antinuke, parent.frame + 2000, lastOrderId++));
                            }
                            break;
                        default:

                            parent.label(unit.getPos(), "unknown action");
                    }
                }
                if (action == null) {
                    idle();
                }
            }
            if (action == null) {

                //parent.label(unit.getPos(), "idle");
            } else {

                //parent.label(unit.getPos(), "action: " + action.getAction().name());
            }
        }

        public boolean update(int frame) {//returns if idle
            if (parent.frame % 50 == 0) {
                if (order == null) {
                    parent.label(unit.getPos(), "null");
                } else {

                    //parent.label(unit.getPos(), "cmd: " + order.id);
                }
            }
            if (order != null && frame > order.timeout && (!(order.id < 0) || unit.getCurrentCommands().isEmpty())) {
                parent.label(unit.getPos(), "order timed out");
                idle();
            }
            if (!(parent.threats.getValue(unit.getPos(), 600) == 0) && (order == null
                    || zkai.dist(unit.getPos(), order.getPosition()) > 150 * 150)) {

                if (order != null && order.id < 0) {
                    grid.remove(findNode((Build) order));
                }
                if (parent.callback.getFriendlyUnitsIn(unit.getPos(), 500).size() > 4) {
                    if (parent.rnd.nextBoolean() || (factories.size() > 0 && zkai.dist(unit.getPos(), factories.get(0).getPos()) < 300)) {
                        //build(findClosestBuildPos(unit.getPos(), getDefenseTower(unit.getPos())), getDefenseTower(unit.getPos()), frame + 500, lastOrderId++);

                        AIFloat3 pos = new AIFloat3(unit.getPos());
                        pos.add(new AIFloat3(50, 0, 50));
                        fight(pos, frame + 600, lastOrderId++);
                        unit.reclaimInArea(unit.getPos(), 300f, (short) 0, parent.frame + 600);
                    } else {
                        AIFloat3 pos = new AIFloat3(unit.getPos());
                        pos.add(new AIFloat3(50, 0, 50));
                        fight(pos, frame + 600, lastOrderId++);
                    }
                } else {
                    //move(factories.get(0).getPos(), frame + 400, lastOrderId++);
                }
            }

            if (order == null || action == null) {
                idle();
            }
            return order == null;
        }

        public boolean build(Build order) {
            if (order.timeout < 0) {
                order.timeout = parent.frame + 1000;
            }
            if (order.position.x < 0) {
                order.position = unit.getPos();
            }
            return build(findClosestBuildPos(order.position, order.building), order.building, order.timeout, order.orderId);
        }

        public boolean build(AIFloat3 p, UnitDef b, int t, int orderid) {

            if (p.x < 0) {
                return false;
            }
            if (!parent.callback.getMap().isPossibleToBuildAt(b, p, 0)) {
                return false;
            }
            unit.build(b, p, 0, (short) 0, t);
            order = new Build(p, b, t, orderid);
            if (pylonrange.containsKey(b.getUnitDefId())) {
                grid.add(new Node((Build) order, pylonrange.get(b.getUnitDefId())));
            }
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }

        public boolean repair(Unit trg, int t, int orderid) {

            if (trg.getAllyTeam() != parent.team) {
                return false;
            }
            unit.repair(trg, (short) 0, t);
            order = new Repair(trg, t, orderid);
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }

        public boolean guard(Unit trg, int t, int orderid) {

            if (trg.getAllyTeam() != parent.team) {
                return false;
            }
            unit.guard(trg, (short) 0, t);
            order = new Guard(trg, t, orderid);
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }

        public boolean fight(AIFloat3 trg, int t, int orderid) {

            parent.debug("fighting to " + trg.toString() + " (" + parent.callback.getMap().getWidth() * 8 + "|"
                    + parent.callback.getMap().getHeight() * 8 + ") until " + t);
            unit.fight(trg, (short) 0, t);
            order = new Fight(trg, t, orderid);
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }

        public boolean move(AIFloat3 trg, int t, int orderid) {

            unit.moveTo(trg, (short) 0, t);
            order = new Move(trg, t, orderid);
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }

    }

    class Node {

        AIFloat3 pos;
        float range;
        Build buildOrder;
        Unit unit;

        public Node(Unit u, AIFloat3 p, float r) {
            unit = u;
            pos = p;
            range = r;
            buildOrder = null;
        }

        public Node(Build b, AIFloat3 p, float r) {
            buildOrder = b;
            pos = p;
            range = r;
            unit = null;
        }

        public Node(Build b, float r) {
            buildOrder = b;
            pos = b.getPosition();
            range = r;
            unit = null;
        }
    }

    class Move extends Order {

        AIFloat3 position;

        public Move(AIFloat3 p, int t, int oi) {
            id = 10;
            position = p;
            timeout = t;
            orderId = oi;
        }

        @Override
        public AIFloat3 getPosition() {
            return position;
        }
    }

    int maxOI = -1;

    class Build extends Order {

        UnitDef building;
        AIFloat3 position;

        public Build(AIFloat3 pos, UnitDef building, int t, int oi) {
            timeout = t;
            id = building.getUnitDefId() * -1;
            position = pos;
            this.building = building;
            orderId = oi;
            if (oi > maxOI) {
                //parent.label(pos, building.getHumanName() + " here");
                maxOI = oi;
            }

        }

        @Override
        public AIFloat3 getPosition() {
            return position;
        }
    }

    class Repair extends Order {

        Unit target;

        public Repair(Unit target, int t, int oi) {
            //parent.label(target.getPos(), "going to be repaired");
            timeout = t;
            id = 40;
            this.target = target;
            orderId = oi;
        }

        @Override
        public AIFloat3 getPosition() {
            return target.getPos();
        }
    }

    class Guard extends Order {

        Unit target;

        public Guard(Unit target, int t, int oi) {
            //parent.label(target.getPos(), "going to be repaired");
            timeout = t;
            id = 25;
            this.target = target;
            orderId = oi;
        }

        @Override
        public AIFloat3 getPosition() {
            return target.getPos();
        }
    }

    class Fight extends Order {

        AIFloat3 target;

        public Fight(AIFloat3 target, int t, int oi) {
            //parent.label(target.getPos(), "going to be repaired");
            timeout = t;
            id = 16;
            this.target = target;
            orderId = oi;
        }

        @Override
        public AIFloat3 getPosition() {
            return target;
        }
    }

    abstract class Order {

        public boolean equals(Order o) {
            return orderId == o.orderId;
        }

        abstract public AIFloat3 getPosition();

        int orderId;
        int id;
        int timeout;
    }

    class ReclaimAction extends Action {

        public ReclaimAction(Area a) {
            area = a;
        }

        @Override
        public Actions getAction() {
            return Actions.reclaim;
        }
    }

    class RepairAction extends Action {

        public RepairAction(Area a) {
            area = a;
        }

        @Override
        public Actions getAction() {
            return Actions.repair;
        }
    }

    class BuildFactory extends Action {

        public BuildFactory(Area a) {
            area = a;
        }

        @Override
        public Actions getAction() {
            return Actions.buildFactory;
        }
    }

    class Bertha extends Action {

        int index;

        public Bertha(Area a, int index) {
            area = a;
            this.index = index;
        }

        @Override
        public Actions getAction() {
            return Actions.bertha;
        }
    }

    class BuildMexes extends Action {

        public BuildMexes(Area a) {
            area = a;
        }

        @Override
        public Actions getAction() {
            return Actions.buildMexes;
        }
    }

    class BuildEnergy extends Action {

        float amount;

        public BuildEnergy(Area a, float amt) {
            area = a;
            amount = amt;
        }

        @Override
        public Actions getAction() {
            return Actions.buildEnergy;
        }
    }

    class Porc extends Action {

        public Porc(Area a) {
            area = a;
        }

        @Override
        public Actions getAction() {
            return Actions.porc;
        }
    }

    class Antinuke extends Action {

        int index;

        public Antinuke(Area a, int index) {
            area = a;
            this.index = index;
        }

        @Override
        public Actions getAction() {
            return Actions.antinuke;
        }
    }

    abstract class Action {

        public boolean equals(Action o) {
            return super.equals(o);//didnt feel like implementing this
        }

        Area area;

        public Area getArea() {
            return area;
        }

        abstract public Actions getAction();

    }

    enum Actions {

        reclaim, repair, buildFactory, buildMexes, buildEnergy, porc, bertha, antinuke;
    }
}
