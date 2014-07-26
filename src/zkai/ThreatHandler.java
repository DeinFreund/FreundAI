/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.CallbackAIException;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import zkai.BuilderHandler.Builder;

/**
 *
 * @author User
 */
public class ThreatHandler {

    zkai parent;
    ThreatPanel pnl;
    JFrame frm;
    float maxval;

    public ThreatHandler(zkai parent) {
        this.parent = parent;
        attackers= new ArrayList();
        defenders= new ArrayList();
        move = new LinkedList();
        patrol = new LinkedList();
        attack = new LinkedList();
        for (int i = 0; i < 3; i++)
            defenders.add(new Squad());
        attackers.add(new Squad());
        parent.debug("initialized ThreatHandler");

    }

    List<Enemy> enemies = new ArrayList();
    List<Point> points = new ArrayList();

    public void removeUnit(Unit unit) {
        //parent.debug("removing " + unit.getDef().getHumanName());
        for (Enemy e : enemies) {
            if (e.unit.equals(unit)) {
                addPoint(unit.getPos(), e.value);
                enemies.remove(e);
                parent.debug(".. sucessful");
                break;
            }
        }
    }

    public void addUnit(Unit unit, float value) {
        if (value < 0) {
            value = 100;
        }
        if (unit.getAllyTeam() == parent.team) {
            parent.debug("added friendly unit as enemy");
            return;
        }
        if (unit.getDef() != null) {

            if (value > 50) {
                //parent.callback.getMap().getDrawer().addPoint(unit.getPos(), "enemy " + unit.getDef().getTooltip() + ": " + value);
            }
            parent.debug("adding " + unit.getDef().getHumanName());
        } else {

            //parent.callback.getMap().getDrawer().addPoint(unit.getPos(), "enemy unknown: " + value);
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
        if (pnl == null || frm == null) {
            frm = new JFrame("Threatmap");
            frm.setVisible(true);
            frm.setSize(200, 200);
            pnl = new ThreatPanel(parent.callback, this);
            frm.add(pnl);
        }
        pnl.updateUI();
        List<Point> useless = new ArrayList();
        for (Point p : points) {

            p.value /= amount;
            if (p.value < 1) {
                useless.add(p);
            }
        }
        List<Enemy> uselessE = new ArrayList();
        for (Enemy e : enemies) {
            if (e.unit.getAllyTeam() == parent.team || e.unit.getPos().x < 0 || !parent.callback.getEnemyUnitsInRadarAndLos().contains(e.unit)) {
                uselessE.add(e);
            }
        }
        points.removeAll(useless);
        enemies.removeAll(uselessE);
    }

    public void addPoint(AIFloat3 point, float value) {
        //parent.callback.getMap().getDrawer().addPoint(point, "danger: " + value);
        points.add(new Point(point, value));
    }

    public Unit getTarget(Unit warrior) {
        Random rnd = new Random(warrior.getUnitId());
        float maxv = -1;
        Enemy target = null;
        for (Enemy e : enemies) {
            float r = (float) rnd.nextDouble() / 2f + 0.5f;
            int mult = 1;
            if (isRaidable(e.unit) ^ isRaidable(warrior)) {
                mult *= 100;
            }
            if (1f / (r * e.value * mult) / zkai.dist(warrior.getPos(), e.unit.getPos()) > maxv) {
                maxv = 1f / (r * e.value * mult) / zkai.dist(warrior.getPos(), e.unit.getPos());
                target = e;
            }
        }
        if (target == null || target.unit.getAllyTeam() == parent.team) {
            return null;
        }
        return target.unit;
    }
    public Unit getTarget(AIFloat3 pos) {
        float maxv = -1;
        Enemy target = null;
        for (Enemy e : enemies) {
            int mult = 1;
            if (1f / (e.value * mult) / zkai.dist(pos, e.unit.getPos()) > maxv) {
                maxv = 1f / (e.value * mult) / zkai.dist(pos, e.unit.getPos());
                target = e;
            }
        }
        if (target == null || target.unit.getAllyTeam() == parent.team) {
            return null;
        }
        return target.unit;
    }

    public AIFloat3 getDanger(Unit warrior) {
        Random rnd = new Random(warrior.getUnitId());
        float maxv = -1;
        Point target = null;
        for (Point p : points) {
            float r = (float) rnd.nextDouble() / 2f + 0.5f;
            if (1f / (r * p.value) / zkai.dist(warrior.getPos(), p.point) > maxv) {
                maxv = 1f / (r * p.value) / zkai.dist(warrior.getPos(), p.point);
                target = p;
            }
        }
        if (target == null) {
            return null;
        }
        return target.point;
    }

    private boolean isRaidable(Unit unit) {
        if (unit.getDef() == null) {
            return true;
        }
        return unit.getDef().getTooltip().contains("Skirmisher") || unit.getDef().getTooltip().contains("Attack")
                || unit.getDef().getTooltip().contains("Raider") || unit.getDef().getTooltip().contains("Fast") || unit.getDef().getTooltip().contains("Blockade");
    }

    public UnitDef getNeededUnit() {
        int glaive = 1;
        int rocko = 1;
        for (Enemy e : enemies) {
            if (isRaidable(e.unit)) {
                glaive++;
            } else {
                rocko++;
            }
        }
        int owng = 1, ownr = 1;
        for (Fighter u : fighters) {
            if (u.kind == Kind.raider) {
                owng++;
            } else if (u.kind == Kind.skirmish){
                ownr++;
            }
        }
        if (glaive / (float) rocko > owng / (float) ownr) {
            return parent.glaive;
        }
        if (Math.random() < 0.25) return parent.warrior;
        return parent.rocko;
    }

    public float getDanger(AIFloat3 point) {
        float total = 0;
        for (Point p : points) {
            total += p.value / Math.max(50,Math.sqrt(zkai.dist(point, p.point)));
        }
        for (Enemy e : enemies) {
            total += e.value / Math.max(50,Math.sqrt(zkai.dist(point, e.unit.getPos())));
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

    
    //fighter stuff
    
    List<Fighter> fighters = new ArrayList();
    List<Squad> attackers;
    List<Squad> defenders;
    List<Fighter> guards = new ArrayList();
    
    public void unitDestroyed(Unit u){
        for (Fighter f : fighters){
            if (f.unit.equals(u)){
                for(Squad q: defenders)
                    if (q.fighters.contains(f)) q.fighters.remove(f);
                for(Squad q: attackers)
                    if (q.fighters.contains(f)) q.fighters.remove(f);
                fighters.remove(f);
                guards.remove(f);
                break;
            }
        }
        for (Fighter g : guards){
            if (g.vip.equals(u)){
                g.vip = getVIP();
            }
        }
    }
    
    public Unit getVIP(){
        Map<Unit,Integer> amt = new TreeMap();
        
        for (Fighter g : guards){
            if (g.vip == null) continue;
            if (!amt.containsKey(g.vip)) amt.put(g.vip, 0);
            amt.put(g.vip, amt.get(g.vip)+1);
        }
        int min = Integer.MAX_VALUE;
        Unit best = null;
        for (Builder u : parent.builder.builders){
            if (!amt.containsKey(u.unit)) amt.put(u.unit, 0);
            if (amt.get(u.unit) < min){
                min = amt.get(u.unit);
                best = u.unit;
            }
        }
        if (best==null){
            parent.debug("Warning: No VIP found");
        }
        return best;
        //will give nullpointers when all builders are dead
    }
    
    public void unitFinished(Unit u){
        if ( u.getDef().equals(parent.rocko)||u.getDef().equals(parent.glaive)||u.getDef().equals(parent.warrior)){
            if (u.getDef().equals(parent.rocko)) {
                fighters.add(new Fighter(Kind.skirmish, u));
            } else if (u.getDef().equals(parent.glaive)) {
                fighters.add(new Fighter(Kind.raider, u));
            }else if (u.getDef().equals(parent.warrior)) {
                fighters.add(new Fighter(Kind.riot, u));
            }
            switch (parent.rnd.nextInt(4)) {
                case 0:
                    attackers.get(parent.rnd.nextInt(attackers.size())).add(fighters.get(fighters.size() - 1));
                    break;
                case 1:
                    defenders.get(parent.rnd.nextInt(defenders.size())).add(fighters.get(fighters.size() - 1));
                    break;
                case 2:
                case 3:
                    fighters.get(fighters.size()-1).vip = getVIP();
                    guards.add(fighters.get(fighters.size()-1));
                    break;
            }
        }
    }
    
    public AIFloat3 randomize(AIFloat3 pos, float radius){
        AIFloat3 ret = new AIFloat3(pos);
        ret.add(new AIFloat3((parent.rnd.nextFloat()-0.5f)*2*radius,0,(parent.rnd.nextFloat()-0.5f)*2*radius));
        return ret;
    }
    public AIFloat3 randomizeCircle(AIFloat3 pos, int seed, float radius, float moveSpeed){
        AIFloat3 ret = new AIFloat3(pos);
        float ang = (float)((parent.frame/1.7 * moveSpeed / parent.rocko.getSpeed())/180f*Math.PI + new Random(seed).nextFloat()*2*Math.PI);//not actually random -- not anymore
        ret.add(new AIFloat3((float)Math.cos(ang)*radius,0,(float)Math.sin(ang)*radius));
        return ret;
    }
    
    class Pair1{Unit unit; AIFloat3 pos;public Pair1(Unit u, AIFloat3 p){unit=u;pos=p;}}
    class Pair2{Unit unit; Unit target;public Pair2(Unit u, Unit p){unit=u;target=p;}}
    Queue<Pair1 > move,patrol;
    Queue<Pair2> attack;
    
    public void update(){
        try{
            for (int i = 0; i < 1; i++){
                if (!move.isEmpty()){
                    Pair1 p = move.poll();
                    p.unit.moveTo(p.pos, (short)0, 1000);
                }
                if (!patrol.isEmpty()){
                    Pair1 p = patrol.poll();
                    p.unit.fight(p.pos, (short)0, 1000);
                }
                if (!attack.isEmpty()){
                    Pair2 p = attack.poll();
                    p.unit.attack(p.target, (short)0, 1000);
                }
            }
        }catch(CallbackAIException ex){}
        for (int j = 0; j < 5 && j < fighters.size(); j++) {
            int i = (int) (parent.rnd.nextDouble() * fighters.size());
            if (fighters.get(i).vip != null && j==0){
                float range = 750;
                Unit trg = getTarget(fighters.get(i).unit.getPos());

                boolean attacking = false;
                if (trg != null){
                    float dist = (float)Math.sqrt(zkai.dist(fighters.get(i).unit.getPos(), trg.getPos()));
                    if (dist < range) {
                        attacking = true;
                        if (fighters.get(i).kind == Kind.skirmish){
                        fighters.get(i).unit.fight(trg.getPos(), (short)0, 300);
                        }else
                        fighters.get(i).unit.attack(trg, (short)0, 300);
                    }
                }
                if (!attacking){
                    fighters.get(i).unit.fight(randomizeCircle(fighters.get(i).vip.getPos(),fighters.get(i).unit.getUnitId(),120,
                            fighters.get(i).unit.getDef().getSpeed()), (short)0, 1000);
                }
                
            }
            /*if (fighters.get(i).kind == Kind.skirmish){
                float range = fighters.get(i).unit.getMaxRange();
                //parent.debug("rocko range: " + range);
                List<Unit> nearby = parent.callback.getEnemyUnitsIn(fighters.get(i).unit.getPos(), range);
                if (!nearby.isEmpty()){
                    parent.debug("rocko in combat");
                    float min = Float.MAX_VALUE;
                    Unit close = null;
                    for (Unit u : nearby){
                        if (zkai.dist(u.getPos(), fighters.get(i).unit.getPos()) < min){
                            min = zkai.dist(u.getPos(), fighters.get(i).unit.getPos());
                            close = u;
                        }
                    }
                    float movedt = range - (float)Math.sqrt(min);
                    AIFloat3 pos = new AIFloat3(fighters.get(i).unit.getPos());
                    pos.sub(close.getPos());
                    pos.scale(1f/pos.length());
                    pos.scale(movedt);
                    pos.add(fighters.get(i).unit.getPos());
                    //parent.callback.getMap().getDrawer().addPoint(pos,"rocko going here");
                    fighters.get(i).unit.moveTo(pos, (short)0, 100);
                }/*else{
                    Unit trg = getTarget(fighters.get(i).unit);
                    if (trg != null){
                        float dist = (float)Math.sqrt(zkai.dist(fighters.get(i).unit.getPos(), trg.getPos()));
                        if (dist < range*3) fighters.get(i).unit.attack(trg, (short)0, 300);
                    }
                }
               
                
            }else*/ if (fighters.get(i).kind == Kind.raider){
                
                List<Unit> nearby = parent.callback.getEnemyUnitsIn(fighters.get(i).unit.getPos(), 500);
                if (!nearby.isEmpty()){
                    parent.debug("glaive in combat");
                    float min = Float.MAX_VALUE;
                    Unit close = null;
                    for (Unit u : nearby){
                        if (zkai.dist(u.getPos(), fighters.get(i).unit.getPos()) < min && u.getDef().getTooltip().contains("Riot")){
                            min = zkai.dist(u.getPos(), fighters.get(i).unit.getPos());
                            close = u;
                        }
                    }
                    if (close != null){
                        float range = close.getMaxRange();
                        float movedt =2* range - (float)Math.sqrt(min);
                        AIFloat3 pos = new AIFloat3(fighters.get(i).unit.getPos());
                        pos.sub(close.getPos());
                        pos.scale(1f/pos.length());
                        pos.scale(movedt);
                        pos.add(fighters.get(i).unit.getPos());
                        //parent.callback.getMap().getDrawer().addPoint(pos,"rocko going here");
                        fighters.get(i).unit.moveTo(pos, (short)0, 100);
                    }
                }else{
                    /*
                    float range = fighters.get(i).unit.getMaxRange();
                    Unit trg = getTarget(fighters.get(i).unit);
                    
                    if (trg != null){
                        float dist = (float)Math.sqrt(zkai.dist(fighters.get(i).unit.getPos(), trg.getPos()));
                        if (dist < range*3) fighters.get(i).unit.attack(trg, (short)0, 300);
                    }*/
                }
            }
        }
        if (parent.frame%200==66){
            for(Squad q: defenders){
                Unit trg = getTarget(q.getPos());
                if (trg!= null && zkai.dist(q.getPos(), trg.getPos()) < 2000*2000 && parent.defense.getValue(trg.getPos()) / parent.defense.maxVal > 0.05){
                    q.patrol(trg.getPos());
                }else
                    q.patrol(randomize(q.target,500));
                
            }
            for(Squad q: attackers)
            {
                Unit trg = getTarget(q.getPos());
                if (trg!= null && zkai.dist(q.getPos(), trg.getPos()) < 2000*2000){
                    q.patrol(trg.getPos());
                }else
                q.patrol(randomize(q.target,500));
            }
            
        }
        if (parent.frame%1000==80){
            for(Squad q: defenders){
                AIFloat3 p = new AIFloat3();
                int k = 0;
                if (Math.random() < 0.5){
                    p = parent.builder.builders.get(parent.rnd.nextInt(parent.builder.builders.size())).order.getPosition();
                }else{
                    while(parent.defense.getValue(p)/parent.defense.maxVal < 0.05 && k++ < 600) p = new AIFloat3((float)parent.rnd.nextFloat()*parent.callback.getMap().getWidth()*8
                            ,0f,(float)parent.rnd.nextFloat()*parent.callback.getMap().getHeight()*8);
                    if (k >590) continue;
                }
                q.target = p;
                
                //parent.callback.getMap().getDrawer().addPoint(p, "defending");
            }
            for(Squad q: attackers){
                AIFloat3 p;
                float maxv = 0;
                AIFloat3 pos = new AIFloat3();
                for (int i = 0; i < 100; i++){
                    p = new AIFloat3((float)(parent.rnd.nextFloat()-0.5)*parent.callback.getMap().getWidth()*3,0f,
                            (float)(parent.rnd.nextFloat()-0.5)*parent.callback.getMap().getHeight()*3);
                    p.add(q.target);
                    if (getDanger(p) > maxv){
                        maxv = getDanger(p);
                        pos = p;
                    }
                }
                
                if (q.fighters.size() < 10 || q.fighters.size() < defenders.get(0).fighters.size()) pos = parent.builder.factories.get(0).getPos();
                parent.callback.getMap().getDrawer().addPoint(pos, "attacking");
                q.target = pos;
            }
        }
    }
    
    final class Squad {

        List<Fighter> fighters;
        float value;
        AIFloat3 target = new AIFloat3();

        public Squad() {
            clear();
        }
        
        public AIFloat3 getPos(){
            AIFloat3 ret = new AIFloat3();
            for (Fighter f : fighters){
                ret.add(f.unit.getPos());
            }
            ret.scale(1f/fighters.size());
            return ret;
        }

        public void add(Fighter f) {
            fighters.add(f);
            value += f.unit.getDef().getCost(BuilderHandler.metal);
        }

        public void remove(Fighter f) {
            value -= f.unit.getDef().getCost(BuilderHandler.metal);
            fighters.remove(f);
        }

        public void clear() {
            value = 0;
            fighters = new ArrayList();
        }

        public Squad merge(Squad other) {
            Squad ret = new Squad();
            for (Fighter f : other.fighters) {
                ret.add(f);
            }
            for (Fighter f : this.fighters) {
                ret.add(f);
            }
            this.clear();
            other.clear();
            return ret;
        }
        
        public void patrol(AIFloat3 target){
            for (Fighter f : fighters){
                patrol.add(new Pair1(f.unit,target));
            }
        }
        public void move(AIFloat3 target){
            for (Fighter f : fighters){
                move.add(new Pair1(f.unit,target));
            }
        }
        public void attack(Unit target){
            for (Fighter f : fighters){
                attack.add(new Pair2(f.unit,target));
            }
        }
    }

    class Fighter {

        Kind kind;
        Unit unit;
        Unit vip;

        public Fighter(Kind kind, Unit u) {
            this.kind = kind;
            unit = u;
        }
    }

    enum Kind {

        riot, skirmish, raider;
    }
}

class ThreatPanel extends JPanel {

    OOAICallback callback;
    ThreatHandler threatmap;

    public ThreatPanel(OOAICallback callback, ThreatHandler map) {
        super();
        threatmap = map;
        this.callback = callback;
        map.parent.debug("ThreatPanel initialized");

    }

    @Override
    protected void paintComponent(Graphics g) {

        float[][] val = new float[getWidth()][getHeight()];
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                AIFloat3 pos = new AIFloat3(8 * x / (float) getWidth() * callback.getMap().getWidth(), -1f, 8 * y / (float) getHeight() * callback.getMap().getHeight());
                val[x][y] = threatmap.getDanger(pos);
                //if ((x == 0 && y==0) || (x == 0 && y == getHeight()-1) || (x == getWidth()-1 && y == 0) ||
                //        (x == getWidth()-1 && y == getHeight()-1 ))callback.getMap().getDrawer().addPoint(pos,"dangerCheck: " + val[x][y]);
                if (val[x][y] > max) {
                    max = val[x][y];
                }
                if (val[x][y] < min) {
                    min = val[x][y];
                }
            }
        }
        //threatmap.parent.debug("ThreatPanel painted. min = " + min + " max = " + max);
        threatmap.maxval = max;
        max -= min;
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                val[x][y] -= min;
                img.setRGB(x, y, new Color(Math.min(1f, 2 * val[x][y] / max), (float) Math.sqrt(val[x][y] / max), (float) Math.sqrt(val[x][y] / max)).getRGB());
            }
        }
        g.drawImage(img, 0, 0, null);

    }

}
