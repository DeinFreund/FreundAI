/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author User
 */
public class ThreatHandler {

    zkai parent;
    public ThreatHandler(zkai parent) {
        this.parent = parent;
    }

    List<Enemy> enemies = new ArrayList();
    List<Point> points = new ArrayList();

    public void removeUnit(Unit unit) {
        parent.debug("removing " + unit.getDef().getHumanName());
        for (Enemy e : enemies) {
            if (e.unit.equals(unit)) {
                enemies.remove(e);
                parent.debug(".. sucessful");
                break;
            }
        }
    }

    public void addUnit(Unit unit, float value) {
        if (value < 0) value = 100;
        if (unit.getAllyTeam() == parent.team) {
            parent.debug("addded friendly unit as enemy");
            return;
        }
        if (unit.getDef() != null){
            
        if (value > 50)parent.callback.getMap().getDrawer().addPoint(unit.getPos(), "enemy " +unit.getDef().getTooltip()+": " + value);
        parent.debug("adding " + unit.getDef().getHumanName());
        }else{
            
            parent.callback.getMap().getDrawer().addPoint(unit.getPos(), "enemy unknown: " + value);
        }
        for (Enemy e : enemies) {
            if (e.unit.equals(unit)) {
                e.value += value;
                return;
            }
        }
        enemies.add(new Enemy(unit, value));
    }

    public void decay(float amount) {
        List<Point> useless = new ArrayList();
        for (Point p : points) {
        
            p.value /= amount;
            if (p.value < 1) {
                useless.add(p);
            }
        }
        List<Enemy> uselessE = new ArrayList();
        for (Enemy e : enemies) {
            if (e.unit.getAllyTeam() == parent.team ||e.unit.getPos().x < 0) {
                uselessE.add(e);
            }
        }
        points.removeAll(useless);
        enemies.removeAll(uselessE);
    }

    public void addPoint(AIFloat3 point, float value) {
        parent.callback.getMap().getDrawer().addPoint(point, "danger: " + value);
        points.add(new Point(point, value));
    }

    public Unit getTarget(Unit warrior) {
        Random rnd = new Random(warrior.getUnitId());
        float maxv = -1;
        Enemy target = null;
        for (Enemy e : enemies) {
            float r = (float)rnd.nextDouble()/2f + 0.5f;
            int mult = 1;
            if (isRaidable(e.unit) ^ isRaidable(warrior)) mult *= 100;
            if (1f/(r*e.value *mult)/ zkai.dist(warrior.getPos(), e.unit.getPos()) > maxv) {
                maxv = 1f/(r*e.value *mult)/ zkai.dist(warrior.getPos(), e.unit.getPos());
                target = e;
            }
        }
        if (target == null || target.unit.getAllyTeam()==parent.team) return null;
        return target.unit;
    }
    public AIFloat3 getDanger(Unit warrior) {
        Random rnd = new Random(warrior.getUnitId());
        float maxv = -1;
        Point target = null;
        for (Point p : points) {
            float r = (float)rnd.nextDouble()/2f + 0.5f;
            if ( 1f/(r*p.value) / zkai.dist(warrior.getPos(), p.point) > maxv) {
                maxv =1f/(r* p.value) / zkai.dist(warrior.getPos(), p.point);
                target = p;
            }
        }
        if (target == null) return null;
        return target.point;
    }

    private boolean isRaidable(Unit unit){
        if (unit.getDef() == null) return true;
        return unit.getDef().getTooltip().contains("Skirmisher") || unit.getDef().getTooltip().contains("Attack") ||
                unit.getDef().getTooltip().contains("Raider")|| unit.getDef().getTooltip().contains("Fast")|| unit.getDef().getTooltip().contains("Blockade");
    }
    
    public UnitDef getNeededUnit(){
        int glaive =1;
        int rocko = 1;
        for (Enemy e : enemies){
            if (isRaidable(e.unit) ){
                glaive ++;
            }else{
                rocko++;
            }
        }
        int owng=1, ownr=1;
        for (Unit u : parent.fighters){
            if (u.getDef().equals(parent.glaive)){
                owng++;
            }else{
                ownr++;
            }
        }
        if (glaive/(float)rocko > owng /(float)ownr){
            return parent.glaive;
        }
        return parent.rocko;
    }

    public float getDanger(AIFloat3 point) {
        float total = 0;
        for (Point p : points) {
            total += p.value / zkai.dist(point, p.point);
        }
        for (Enemy e : enemies) {
            total += e.value / zkai.dist(point, e.unit.getPos());
        }
        return total;
    }

    class Enemy {

        Unit unit;
        float value;

        public Enemy(Unit u, float v) {
            unit = u;
            value = v;
        }
    }

    class Point {

        AIFloat3 point;
        float value;

        public Point(AIFloat3 p, float v) {
            point = p;
            value = v;
        }
    }
}
