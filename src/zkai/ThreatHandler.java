/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
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
        if (unit.getAllyTeam() == parent.com.getAllyTeam()) {
            parent.debug("addded friendly unit as enemy");
            return;
        }
        parent.debug("adding " + unit.getDef().getHumanName());
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
            p.value -= amount;
            if (p.value < 0) {
                useless.add(p);
            }
        }
        List<Enemy> uselessE = new ArrayList();
        for (Enemy e : enemies) {
            if (e.unit.getAllyTeam() == parent.com.getAllyTeam()) {
                uselessE.add(e);
            }
        }
        points.removeAll(useless);
        enemies.removeAll(uselessE);
    }

    public void addPoint(AIFloat3 point, float value) {
        points.add(new Point(point, value));
    }

    public Unit getTarget(Unit warrior) {
        Random rnd = new Random(warrior.getUnitId());
        float maxv = -1;
        Enemy target = null;
        for (Enemy e : enemies) {
            float r = (float)rnd.nextDouble()/2f + 0.5f;
            if (r*e.value / zkai.dist(warrior.getPos(), e.unit.getPos()) > maxv) {
                maxv = r*e.value / zkai.dist(warrior.getPos(), e.unit.getPos());
                target = e;
            }
        }
        if (target == null) return null;
        return target.unit;
    }
    public AIFloat3 getDanger(Unit warrior) {
        Random rnd = new Random(warrior.getUnitId());
        float maxv = -1;
        Point target = null;
        for (Point p : points) {
            float r = (float)rnd.nextDouble()/2f + 0.5f;
            if ( r*p.value / zkai.dist(warrior.getPos(), p.point) > maxv) {
                maxv =r* p.value / zkai.dist(warrior.getPos(), p.point);
                target = p;
            }
        }
        if (target == null) return null;
        return target.point;
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
