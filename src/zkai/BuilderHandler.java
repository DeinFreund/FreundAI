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
    List<Unit> factories;
    List<Node> virtualRadars;
    List<Unit> underConstruction;
    List<Node> grid;

    float energyUnderConstruction = 0;
    static Resource metal, energy;

    TreeMap<Integer, Float> pylonrange;

    int lastOrderId = 0;

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
        if (Math.random() < 0.15){
            //pending.add(new Build(u.getPos(),parent.defender,-1,lastOrderId++));
        }
        if (Math.random() < 0.6 )pending.add(new Fight(u.getPos(),-1,lastOrderId++));
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
        for (Node m : virtualRadars) {
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
                    //parent.callback.getMap().getDrawer().addPoint(u.getPos(), "node established");
                    b.order.timeout = parent.frame + 500;
                    break;
                }

            }/**/;// <- do not delete
            for (Node n : virtualRadars) {
                if (n.buildOrder != null && n.buildOrder.equals(((Build) b.order))) {
                    n.unit = u;
                    n.buildOrder = null;
                    parent.callback.getMap().getDrawer().addPoint(u.getPos(), "radar node established");
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
            //parent.callback.getMap().getDrawer().addPoint(u.getPos(), "new mex");
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
        if (min == Double.MAX_VALUE) return 0;
        return min;
    }
    //HERE IT'S OVER

    public UnitDef getDefenseTower(AIFloat3 pos){
        if (parent.defense.getDefense(pos) > 3000)
            return parent.bertha;
        if (parent.defense.getDefense(pos) > 300)
            return parent.hlt;
        if (parent.defense.getValue(pos) / parent.defense.maxVal > 0.42 || parent.defense.getDefense(pos) > 200)
            return parent.llt;
        return parent.defender;
    }
        
    
    public boolean inRadarRange(AIFloat3 pos){
        parent.debug("requesting radar coverage");
        for (Node n : virtualRadars){
            if (zkai.dist(n.pos, pos) < n.range * n.range /1.5) return true;
        }
        return false;
    }
    
    public void update(int frame) {
        for (int j = 0; j < 5 && j < builders.size(); j++) {//active check for timed out builders
            int i = (int) (parent.rnd.nextDouble() * builders.size());
            builders.get(i).update(frame);
        }
        for (int j = 0; j < 2 && j < idlers.size(); j++) {
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

                        //parent.callback.getMap().getDrawer().addPoint(pos, "building here");
                        
                        if (!inRadarRange(pos)){
                            pending.add(new Build(pos,parent.radar,-1,lastOrderId++));
                            virtualRadars.add(new Node((Build)pending.get(pending.size()-1),pos,parent.radar.getRadarRadius()));
                        }

                }
                if (done || !done) {
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
        }
        energyUnderConstruction /= 1.002;
        energyUnderConstruction = Math.max(energyUnderConstruction,0);
        if (parent.frame % 100 == 0) {
            parent.debug("Energy under construction: " + energyUnderConstruction);

        }
        if (frame%42 == 0){
            AIFloat3 pos = new AIFloat3((float)Math.random()*parent.callback.getMap().getWidth()*8,0,(float)Math.random()*parent.callback.getMap().getHeight()*8);
            if ((parent.defense.getValue(pos,500)>100+2*parent.defense.getDefense(pos)) && parent.frame > 10000){
                boolean busy  = false;
                for (Order o : pending){
                    if (o.id == -parent.llt.getUnitDefId() || o.id == -parent.hlt.getUnitDefId() || o.id == -parent.defender.getUnitDefId()
                             || o.id == -parent.bertha.getUnitDefId()) busy = true;
                }
                if (!busy){
                    parent.debug("Building Lotus because " + parent.defense.getValue(pos)/parent.defense.maxVal  + " - 0.5 > " + 
                            parent.defense.getDefense(pos)/Math.max(0.0001,parent.defense.maxDef));
                    
                    pending.add(new Build(pos,getDefenseTower(pos),-1,lastOrderId++));
                }
            }
        }
        if (frame % 100 == 42){
            
            if (parent.callback.getEconomy().getIncome(energy) < parent.callback.getEconomy().getUsage(energy)
                    && parent.callback.getEconomy().getCurrent(energy) < 250 ) {
                parent.debug("upgrading energy");
                upgradeEnergy(parent.callback.getEconomy().getUsage(energy) - parent.callback.getEconomy().getIncome(energy) - energyUnderConstruction);

            }
            if (parent.callback.getEconomy().getCurrent(metal) > 300 && parent.callback.getEconomy().getCurrent(energy) > 300 && parent.frame > 1000){
                boolean buildingNano = false;
                for (Order o : pending) {
                    if (o.id == - parent.nano.getUnitDefId()) {
                        buildingNano = true;
                    }
                }
                if (!buildingNano){
                    pending.add(new Build(factories.get(0).getPos(),parent.nano,-1,lastOrderId++));
                }
            }else {
                parent.debug("upgrading metal");
                List<Order> pendingc = new ArrayList();
                pendingc.addAll(pending);
                for (Order o : pendingc) {
                    if (o.id == -parent.mex.getUnitDefId()) {
                        grid.remove(findNode((Build)o));
                        pending.remove(o);
                    }
                }
                List<AIFloat3> newMexes = new ArrayList();
                for (int i = 0; i < builders.size(); i++){
                    AIFloat3 next = parent.expansion.nextMex(newMexes);
                    if (next != null) {
                        Build b =new Build(next, parent.mex, -1, lastOrderId++);
                        pending.add(b);
                        grid.add(new Node(b,b.position,pylonrange.get(b.building.getUnitDefId())));
                        newMexes.add(next);
                    } 
                }
                
            }
        }
        if (frame % 100 == 0) {
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
            }
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
            }
        }
    }
    
    Node findNode(Build b){
        for (Node n : grid){
            if (n.buildOrder == null) continue;
            if (n.buildOrder.equals( b)) return n;
        }
        return null;
    }
    
    Node findNode(Unit u){
        for (Node n : grid){
            if (n.unit == null) continue;
            if (n.unit.equals(u)) return n;
        }
        return null;
    }

    List<Node> getConnectedBuildings(Node start) {
        List<Node> result = new ArrayList();
        //parent.callback.getMap().getDrawer().addPoint(start.pos, "starting bfs from here");
        //parent.callback.getMap().getDrawer().addPoint(start.unit.getPos(), "= here");
        if (start == null) return result;
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
                //parent.callback.getMap().getDrawer().addPoint(u.getPos(), msg);
                for (Node o : grid) {
                    if ( !result.contains(o) && !newadded.contains(o)
                            && u.range + o.range >= Math.sqrt(zkai.dist(u.pos, o.pos))) {
                        newadded.add(o);
                        parent.callback.getMap().getDrawer().addLine(u.pos, o.pos);
                        //parent.callback.getMap().getDrawer().addPoint(o.getPos(), "connected to mex");
                    }
                }

            }
            added = newadded;
        }
        return result;
    }

    boolean upgradeEnergy(float amt) {
        while (amt > 0) {
            //solar
            UnitDef unitdef = parent.solar;
            float minval = Float.MAX_VALUE;
            Unit best = null;
            for (Unit m : mexes) {
                List<Node> conn = getConnectedBuildings(findNode(m));
                float e = 0;
                for (Node u : conn) {
                    if (u.unit == null) e+=2;
                    else e += u.unit.getResourceMake(energy);
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
                    vec = parent.callback.getMap().findClosestBuildSite(unitdef, vec, 800, 6, 0);
                    Build b  =new Build(vec, unitdef, -1, lastOrderId++);
                    pending.add(b);
                    grid.add(new Node(b,b.position,pylonrange.get(b.building.getUnitDefId())));
                    //parent.callback.getMap().getDrawer().addPoint(vec, "possible solar");
                }
            } else {
                Build b = new Build(new AIFloat3(-1,-1,-1), unitdef, -1, lastOrderId++);
                pending.add(b);
                grid.add(new Node(b,b.position,pylonrange.get(b.building.getUnitDefId())));
            }
            parent.debug("scheduled solar");
            energyUnderConstruction += 2;
            amt -= 2;
        }
        return true;
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
            //parent.callback.getMap().getDrawer().addPoint(unit.getPos(), "idle");
        }

        public boolean update(int frame) {//returns if idle
            if (order != null && frame > order.timeout && unit.getCurrentCommands().isEmpty()) {
                parent.callback.getMap().getDrawer().addPoint(unit.getPos(), "order timed out");
                idle();
            }
            if (!(parent.callback.getEnemyUnitsIn(unit.getPos(), 600) == null || 
                    parent.callback.getEnemyUnitsIn(unit.getPos(), 600).isEmpty()) && (order == null ||
                    zkai.dist(unit.getPos(),order.getPosition())> 150*150)){
                
                if(order !=null && order.id < 0){
                    grid.remove(findNode((Build)order));
                }
                if (parent.callback.getFriendlyUnitsIn(unit.getPos(), 500).size() > 4){
                    if (parent.rnd.nextBoolean()) build(unit.getPos(),getDefenseTower(unit.getPos()),100,3,frame+500,lastOrderId++);
                    else {
                        AIFloat3 pos = new AIFloat3(unit.getPos());
                        pos.add(new AIFloat3(50,0,50));
                        fight(pos,frame+200,lastOrderId++);
                    }
                }else{
                    move(factories.get(0).getPos(),frame+100,lastOrderId++);
                }
            }
            return order == null;
        }

        public boolean build(AIFloat3 p, UnitDef b, float r, int spacing, int t, int orderid) {
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
            order = new Build(p, b, t, orderid);
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
        
        public boolean fight(AIFloat3 trg, int t, int orderid) {
            
            parent.debug("fighting to " + trg.toString() + " (" + parent.callback.getMap().getWidth()*8 +"|" +
                    parent.callback.getMap().getHeight()*8+") until " + t);
            unit.fight(trg, (short) 0, t);
            order = new Fight(trg, t, orderid);
            if (idlers.contains(this)) {
                idlers.remove(this);
            }
            return true;
        }
        public boolean move(AIFloat3 trg, int t, int orderid) {

            unit.moveTo(trg, (short) 0, t);
            order = new Fight(trg, t, orderid);
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
            if (oi > maxOI){
                //parent.callback.getMap().getDrawer().addPoint(pos, building.getHumanName() + " here");
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
            //parent.callback.getMap().getDrawer().addPoint(target.getPos(), "going to be repaired");
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
    
    class Fight extends Order {

        AIFloat3 target;

        public Fight(AIFloat3 target, int t, int oi) {
            //parent.callback.getMap().getDrawer().addPoint(target.getPos(), "going to be repaired");
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

}
