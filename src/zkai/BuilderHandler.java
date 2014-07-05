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
    List<Node> grid;

    float energyUnderConstruction = 0;
    Resource metal, energy;

    TreeMap<Integer, Float> pylonrange;
    
    int lastOrderId = 0;

    public BuilderHandler(zkai parent) {
        this.parent = parent;
        builders = new ArrayList();
        idlers = new ArrayList();
        mexes = new ArrayList();
        buildings = new ArrayList();
        pending = new ArrayList();
        metal = parent.callback.getResources().get(0);
        energy = parent.callback.getResources().get(1);
        grid = new ArrayList();

        //init hardcoded pylonranges
        pylonrange = new TreeMap();
        pylonrange.put(parent.mex.getUnitDefId(), 50f);
        pylonrange.put(parent.solar.getUnitDefId(), 100f);
        pylonrange.put(parent.wind.getUnitDefId(), 60f);

        parent.debug("DEBUG: " + pylonrange.get(parent.mex.getUnitDefId()));
    }

    public void unitIdle(Unit u) {
        for (Builder b : builders) {
            if (b.unit.getUnitId() == u.getUnitId()) {
                b.idle();
            }
        }
    }

    public void unitKilled(Unit u) {
        for (Builder b : builders) {
            if (b.unit.getUnitId() == u.getUnitId()) {
                b.update(parent.frame);
                if (b.order != null) {
                    pending.add(b.order);
                }
                builders.remove(b);
                return;
            }
        }
        for (Unit m : mexes) {
            if (m.getUnitId() == u.getUnitId()) {
                mexes.remove(m);
                return;
            }
        }
        for (Node m : grid) {
            if (m.unit != null && m.unit.getUnitId() == u.getUnitId()) {
                grid.remove(m);
                return;
            }
        }
        for (Unit m : buildings) {
            if (m.getUnitId() == u.getUnitId()) {
                buildings.remove(m);
                return;
            }
        }
    }
    
    public void unitCreated(Unit u, Unit builder){
        Builder b = null;
        for (Builder bb: builders){
            if (bb.unit.equals(builder)){
                b = bb;
            }
        }
        if (b != null && b.order != null){
            for (Node n : grid){
                if (n.buildOrder != null && n.buildOrder.equals(((Build)b.order))){
                    
                }
                    
            }
        }
    }

    public void unitFinished(Unit u) {

        if (u.getDef().equals(parent.mex)) {
            mexes.add(u);
            buildings.add(u);
            parent.callback.getMap().getDrawer().addPoint(u.getPos(), "new mex");
        }
        if (u.getDef().equals(parent.solar)) {
            buildings.add(u);
        }
        if (u.getDef().equals(parent.rector)) {
            builders.add(new Builder(u));
        }
    }

    public void update(int frame) {
        for (int j = 0; j < 5 && j < builders.size(); j++) {//active check for timed out builders
            int i = (int) (parent.rnd.nextDouble() * builders.size());
            builders.get(i).update(frame);
        }
        for (int j = 0; j < 5 && j < idlers.size(); j++) {
            int i = (int) (parent.rnd.nextDouble() * idlers.size());
            float maxscore = 0;
            Order best = null;
            for (Order o : pending) {
                float score;
                switch (o.id) {
                    case 40:
                        score = 1f / zkai.dist(idlers.get(i).unit.getPos(), ((Repair) o).target.getPos());
                        score *= 1.5;
                        break;
                    default:
                        score = 1f / zkai.dist(idlers.get(i).unit.getPos(), ((Build) o).position);
                }
                if (score > maxscore) {
                    maxscore = score;
                    best = o;
                }
            }
            if (best != null) {
                boolean done;
                switch (best.id) {
                    case 40:
                        done = idlers.get(i).repair(((Repair) best).target, frame + 200);
                        break;
                    default:
                        float radius = 800;
                        int space = 3;
                        if (((Build) best).building.equals(parent.mex)) {
                            radius = 0;
                            space = 1;
                        }
                        AIFloat3 pos = ((Build) best).position;
                        if (pos.x < 0) {
                            pos = idlers.get(i).unit.getPos();
                        }
                        done = idlers.get(i).build(pos, ((Build) best).building, radius, space, frame + 600);
                        
                }
                if (done || !done) {
                    pending.remove(best);
                }

            }
        }
        if (parent.callback.getEconomy().getIncome(energy) < parent.callback.getEconomy().getUsage(energy)
                && parent.callback.getEconomy().getCurrent(energy) < 250) {
            upgradeEnergy(parent.callback.getEconomy().getUsage(energy) - parent.callback.getEconomy().getIncome(energy) - energyUnderConstruction);
        }
        if (frame % 100 == 0) {
            parent.debug(mexes.size() + " mexes available.");
            for (Unit m : mexes) {
                getConnectedBuildings(m);
                parent.debug("bfs on mex");
            }
        }
    }

    void expand() {

    }

    List<Unit> getConnectedBuildings(Unit start) {
        List<Unit> result = new ArrayList();
        List<Unit> added = new ArrayList();
        added.add(start);
        while (!added.isEmpty()) {
            result.addAll(added);
            List<Unit> newadded = new ArrayList();
            for (Unit u : added) {
                //parent.debug("starting new bfs from " + u.getDef().getHumanName());
                //parent.debug("pylonrange: " + pylonrange.get(u.getDef().getUnitDefId()));
                //String msg = "range: " + (pylonrange.get(u.getDef().getUnitDefId()) + MAX_PYLONRANGE) + " -> "
                //       + parent.callback.getFriendlyUnitsIn(u.getPos(), pylonrange.get(u.getDef().getUnitDefId())+ MAX_PYLONRANGE).size() + " units";
                //parent.debug(msg);
                //parent.callback.getMap().getDrawer().addPoint(u.getPos(), msg);
                for (Unit o : parent.callback.getFriendlyUnitsIn(u.getPos(), pylonrange.get(u.getDef().getUnitDefId()) + MAX_PYLONRANGE)) {
                    if (pylonrange.containsKey(o.getDef().getUnitDefId()) && !result.contains(o) && !newadded.contains(o)
                            && pylonrange.get(u.getDef().getUnitDefId()) + pylonrange.get(o.getDef().getUnitDefId()) >= Math.sqrt(zkai.dist(u.getPos(), o.getPos()))) {
                        newadded.add(o);
                        parent.callback.getMap().getDrawer().addLine(u.getPos(), o.getPos());
                        //parent.callback.getMap().getDrawer().addPoint(o.getPos(), "connected to mex");
                    }
                }

            }
            added = newadded;
        }
        return result;
    }

    void upgradeEnergy(float amt) {
        while (amt > 0) {
            //solar
            UnitDef unitdef = parent.solar;
            float minval = Float.MAX_VALUE;
            Unit best = null;
            for (Unit m : mexes) {
                List<Unit> conn = getConnectedBuildings(m);
                float e = 0;
                for (Unit u : conn) {
                    e += u.getResourceMake(energy);
                }
                if (e < minval) {
                    best = m;
                }
            }
            if (best != null) {
                List<Unit> conn = getConnectedBuildings(best);
                float mindist = Float.MAX_VALUE;
                Unit u1 = null, u2 = null;
                for (Unit u : conn) {
                    for (Unit b : buildings) {
                        if (!pylonrange.containsKey(b.getDef().getUnitDefId())) {
                            continue;
                        }
                        if (conn.contains(b)) {
                            continue;
                        }
                        if (zkai.dist(u.getPos(), b.getPos()) < mindist) {
                            mindist = zkai.dist(u.getPos(), b.getPos());
                            u1 = u;
                            u2 = b;
                        }
                    }
                }
                if (u1 != null && u2 != null) {
                    AIFloat3 vec = u1.getPos();
                    vec.interpolate(u2.getPos(), (float) (pylonrange.get(unitdef.getUnitDefId()) * 0.75 / Math.sqrt(zkai.dist(u1.getPos(), u2.getPos()))));
                    vec = parent.callback.getMap().findClosestBuildSite(unitdef, vec, 800, 3, 0);
                    pending.add(new Build(vec, unitdef, -1));
                    parent.callback.getMap().getDrawer().addPoint(vec, "possible solar");
                }
            } else {
                pending.add(new Build(new AIFloat3(-1f, -1f, -1f), unitdef, -1));
            }
            energyUnderConstruction += 2;
            amt -= 2;
        }
    }

    final class Builder {

        Unit unit;
        Order order;

        public Builder(Unit u) {
            unit = u;
            idle();
        }

        public void idle() {
            order = null;
            idlers.add(this);
        }

        public boolean update(int frame) {//returns if idle
            if (order != null && frame > order.timeout) {
                idle();
            }
            return order == null;
        }

        public boolean build(AIFloat3 p, UnitDef b, float r, int spacing, int t) {
            if (r > 0) {
                p = parent.callback.getMap().findClosestBuildSite(b, p, r, spacing, 0);
            }
            if (p.x < 0) {
                return false;
            }
            if (!parent.callback.getMap().isPossibleToBuildAt(b, p, 0)) {
                return false;
            }
            unit.build(b, p, 0, (short) 0, t);
            order = new Build(p, b, t);
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }

        public boolean repair(Unit trg, int t) {

            if (trg.getAllyTeam() != parent.team) {
                return false;
            }
            unit.repair(trg, (short) 0, t);
            order = new Repair(trg, t);
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
    }

    class Move extends Order {

        AIFloat3 position;

        public Move(AIFloat3 p, int t) {
            id = 10;
            position = p;
            timeout = t;
            orderId = lastOrderId++;
        }
    }

    class Build extends Order {

        UnitDef building;
        AIFloat3 position;

        public Build(AIFloat3 pos, UnitDef building, int t) {
            timeout = t;
            id = building.getUnitDefId() * -1;
            position = pos;
            this.building = building;
            orderId = lastOrderId++;
        }
    }

    class Repair extends Order {

        Unit target;

        public Repair(Unit target, int t) {
            timeout = t;
            id = 40;
            this.target = target;
            orderId = lastOrderId++;
        }
    }

    abstract class Order {

        public boolean equals(Order o){
            return orderId == o.orderId;
        }
        
        int orderId;
        int id;
        int timeout;
    }

}
