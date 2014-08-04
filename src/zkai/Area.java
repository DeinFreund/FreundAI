/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.UnitDef;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import zkai.ThreatHandler.Fighter;
import zkai.ThreatHandler.Squad;

/**
 *
 * @author User
 */
public class Area {

    static zkai parent;
    static JFrame frm;
    static MapGUI pnl;

    public Owner owner;
    int x, y;
    int lastAttack = -1000;
    float danger = 0;
    Squad fighters;
    int guaranteedUnits = 0;
    boolean newlyCapped = false;

    public Area(int x, int y) {
        owner = Owner.neutral;
        
        this.x = x;
        this.y = y;
        fighters = parent.threats.new Squad();
        //parent.label(getCoords(), x + "|" + y);
        
        parent.callback.getMap().getDrawer().addLine(getTopLeftCorner(), new AIFloat3(getTopLeftCorner().x, 0, getBottomRightCorner().z));
        parent.callback.getMap().getDrawer().addLine(getTopLeftCorner(), new AIFloat3(getBottomRightCorner().x, 0, getTopLeftCorner().z));
        parent.callback.getMap().getDrawer().addLine(getBottomRightCorner(), new AIFloat3(getTopLeftCorner().x, 0, getBottomRightCorner().z));
        parent.callback.getMap().getDrawer().addLine(getBottomRightCorner(), new AIFloat3(getBottomRightCorner().x, 0, getTopLeftCorner().z));
    }

    public Owner getOwner() {
        if (parent.threats.getValue(getCoords(), getRadius() * 1) > 0) {
            return Owner.contested;
        }
        if (parent.frame - lastAttack > 500) {
            return owner;
        }
        return Owner.contested;
    }

    public void setOwner(Owner o) {
        owner = o;

    }

    public List<Area> getNearbyAreas(int dist) {
        List<Area> res = new ArrayList();
        for (Area a : areas) {
            if (a == this) {
                continue;
            }
            if (Math.max(Math.abs(a.x - x), Math.abs(a.y - y)) <= dist) {
                res.add(a);
            }
        }
        return res;
    }

    public int getUnitcount() {
        int res = 0;
        for (Fighter f : fighters.fighters) {
            if ((zkai.dist(f.unit.getPos(), getCoords())) < 0.25 * (getHeight() * getHeight() + getWidth() * getWidth())) {
                res++;
            }
        }
        return res;
    }

    public float getRadius() {
        return 0.5f * (float) Math.sqrt(getHeight() * getHeight() + getWidth() * getWidth());
    }

    public float getUnitvalue() {
        float res = 0;
        for (Fighter f : fighters.fighters) {
            if ((zkai.dist(f.unit.getPos(), getCoords())) < 0.25 * (getHeight() * getHeight() + getWidth() * getWidth())) {
                res += f.unit.getDef().getCost(BuilderHandler.metal);
            }
        }
        //add defense value here
        return res;
    }

    static int totUnits;

    static public void update_init() {
        float totDanger = 0;
        totUnits = 0;
        int totAreas = 0;
        if (parent.frame % 2000 == 0){
            heightmap = parent.callback.getMap().getHeightMap();
        }
        for (Area a : areas) {
            if (a.owner != Owner.ally) {
                continue;
            }
            totDanger += (parent.threats.getValue(a.getCoords(), a.getRadius()) + 20) * (a.getOwner() == Owner.contested ? 2 : 1)* (a.isBorder() ? 4 : 1) * (a.getMexes().isEmpty() ? 1 : 1.5);
            totAreas++;
            totUnits += a.fighters.size();
        }
        if (parent.frame < 5000) {
            return;
        }
        int unitsUsed = 0;
        for (Area a : areas) {
            if (a.owner != Owner.ally) {
                a.guaranteedUnits = 0;
                continue;
            }
            a.guaranteedUnits = (int) (((20 + parent.threats.getValue(a.getCoords(), a.getRadius()))) * (a.getOwner() == Owner.contested ? 2 : 1)* (a.isBorder() ? 4 : 1)
                    * (a.getMexes().isEmpty() ? 1 : 1.5) / totDanger * totUnits);
            unitsUsed += a.guaranteedUnits;
            //a.guaranteedUnits += totUnits / 2 / totAreas;
        }
        int blub = 0;
        while (unitsUsed < totUnits && blub < 100) {
            blub++;
            for (int i = 0; unitsUsed < totUnits && i < getAllied().size(); i++) {
                if (getAllied().get(i).isBorder()) {
                    getAllied().get(i).guaranteedUnits++;
                    unitsUsed++;

                }
            }
        }
    }

    public boolean isBorder() {
        for (Area a : areas) {
            if (a == this) {
                continue;
            }
            if ((Math.abs(a.x - x) + Math.abs(a.y - y)) == 1 && a.owner != Owner.ally) {
                return true;
            }
        }
        return false;
    }

    public static List<Area> getAllied() {
        List<Area> res = new ArrayList();
        for (Area a : areas) {
            if (a.owner == Owner.ally) {
                res.add(a);
            }
        }
        return res;
    }
    
    public AIFloat3 getDefensePos(UnitDef unitdef){
        int w = 20;
        int h = 20;
        float best = -100;
        AIFloat3 res = getCoords();
        for (int x = 0; x < w; x++){
            for (int y = 0; y < h; y ++){
                AIFloat3 pos = new AIFloat3(getTopLeftCorner().x+x*getWidth()/w,0,getTopLeftCorner().z+y*getHeight()/h);
                if (parent.callback.getMap().isPossibleToBuildAt(unitdef, pos, 0) && getHeight(pos) > best){
                    best = getHeight(pos);
                    res = pos;
                }
            }
        }
        return res;
    }

    public void update() {
        if (owner == Owner.enemy){
            for (Area a : getNearbyAreas(1)) {
                if (newlyCapped) {
                    break;
                }
                if (a.getOwner() == Owner.neutral && parent.frame % 400 == 0 && parent.frame > 4000) {
                    boolean dangerous = false;
                    for (Area n : a.getNearbyAreas(1)) {
                        if (n.owner == Owner.ally) {
                            dangerous = true;
                        }
                    }
                    if (!dangerous) {
                        a.setOwner(Owner.enemy);
                        a.newlyCapped = true;
                        parent.debug("enemy expanding to " + a.x + "|" + a.y);
                        break;
                    }
                }
            }
        }
        if (owner == Owner.ally) {

            for (Area a : getNearbyAreas(1)) {
                if (newlyCapped) {
                    break;
                }
                if (a.getOwner() == Owner.neutral && parent.frame % 300 == 0 && parent.frame > 3000 &&!(parent.builder.factories.size() > 0
                        && Area.getArea(parent.builder.factories.get(0).getPos()) == this && parent.frame < 4000 && getUnitcount() < 10)) {
                    boolean dangerous = false;
                    for (Area n : a.getNearbyAreas(1)) {
                        if (n.owner == Owner.enemy) {
                            dangerous = true;
                        }
                    }
                    if (!dangerous) {
                        a.setOwner(Owner.ally);
                        a.newlyCapped = true;
                        parent.debug("expanding to " + a.x + "|" + a.y);
                        break;
                    }
                }
            }
            
            int radius = 0;
            while (fighters.size() < guaranteedUnits) {
                radius++;
                for (Area a : getNearbyAreas(radius)) {
                    while (a.fighters.size() > a.guaranteedUnits && fighters.size() < guaranteedUnits) {
                        Fighter f = a.fighters.fighters.get(parent.rnd.nextInt(a.fighters.size()));
                        a.fighters.remove(f);
                        fighters.add(f);
                    }
                }
            }
            if (parent.frame % 90 == 0) {
                AIFloat3 threat = new AIFloat3(-1000, 0, -1000);
                if (parent.threats.getTarget(getCoords()) != null) {
                    threat = parent.threats.getTarget(getCoords()).getPos();
                }
                for (Fighter f : fighters.fighters) {
                    AIFloat3 pt = new AIFloat3(parent.rnd.nextFloat() * getWidth() + getWidth() * x, 0, parent.rnd.nextFloat() * getHeight() + getHeight() * y);
                    if (threat.x > 0 && Math.sqrt(zkai.dist(getCoords(), threat)) < getRadius() * 5 && (Area.getArea(threat).owner == Owner.ally ||
                            parent.threats.getDanger(threat) / parent.threats.maxval < 0.3)) {
                        f.unit.fight(threat, (short) 0, parent.frame + 500);
                    } else if ((zkai.dist(f.unit.getPos(), getCoords())) < 0.25 * (getHeight() * getHeight() + getWidth() * getWidth())) {
                        f.unit.patrolTo(pt, (short) 0, parent.frame + 500);
                    } else {
                        f.unit.fight(pt, (short) 0, parent.frame + 500);
                    }
                }
            }
        }
        int allied = 0;
        for (Area a : areas) {
            if (a.owner == Owner.ally) {
                allied++;
            }
        }

        /*if (owner == Owner.ally &&  fighters.size() == 0 && allied > 1 && !newlyCapped) {
         setOwner(Owner.neutral);

         parent.debug("retreating from " + x + "|" + y + " because no defending units left");
         }
         */
        if (owner == Owner.ally && parent.threats.getValue(getCoords(), getRadius()) > getUnitvalue() * 0.5 + 0.5 * fighters.getValue() && !newlyCapped) {
            //retreat
            setOwner(Owner.neutral);
            parent.debug("retreating from " + x + "|" + y + " because no chance of defending");

        }
        if (owner == Owner.enemy
                && ThreatHandler.getValue(parent.callback.getFriendlyUnitsIn(getCoords(), getRadius())) > parent.threats.getValue(getCoords(), getRadius())) {
            owner = Owner.neutral;
        }
        if (parent.threats.getStaticValue(getCoords(), getRadius()) > 0) {
            owner = Owner.enemy;
            
            //parent.debug("enemy owns " + x + "|" + y + " because statics");
        }
        if (owner != Owner.ally && fighters.size() > 0) {
            float best = Float.MAX_VALUE;
            Area besta = null;
            for (Area a : areas) {
                if (a.owner != Owner.ally) {
                    continue;
                }
                float dist = zkai.dist(getCoords(), a.getCoords());
                if (dist < best) {
                    best = dist;
                    besta = a;
                }
            }
            if (besta != null) {
                besta.fighters.fighters.addAll(fighters.fighters);
                fighters.clear();
            }
        }
        newlyCapped = false;
    }

    public AIFloat3 getCoords() {
        float xx = parent.callback.getMap().getWidth() * 8 * (x + 0.5f) / map.length;
        float yy = parent.callback.getMap().getHeight() * 8 * (y + 0.5f) / map[x].length;
        return new AIFloat3(xx, getHeight(new AIFloat3(xx,0,yy)), yy);
    }

    public float getWidth() {
        return parent.callback.getMap().getWidth() * 8 / map.length;
    }

    public float getHeight() {
        return parent.callback.getMap().getHeight() * 8 / map[0].length;
    }

    public AIFloat3 getTopLeftCorner() {
        float xx = parent.callback.getMap().getWidth() * 8 * (x) / map.length;
        float yy = parent.callback.getMap().getHeight() * 8 * (y) / map[x].length;
        return new AIFloat3(xx, 0, yy);
    }

    public AIFloat3 getBottomRightCorner() {
        float xx = parent.callback.getMap().getWidth() * 8 * (x + 1f) / map.length;
        float yy = parent.callback.getMap().getHeight() * 8 * (y + 1f) / map[x].length;
        return new AIFloat3(xx, 0, yy);
    }

    public List<AIFloat3> getMexes() {
        AIFloat3 c1 = getTopLeftCorner();
        AIFloat3 c2 = getBottomRightCorner();
        List<AIFloat3> res = new ArrayList();
        for (AIFloat3 m : parent.availablemetalspots) {
            if (m.x >= c1.x && m.z >= c1.z && m.x < c2.x && m.z < c2.z) {
                res.add(m);
            }
        }
        return res;
    }

    public int gridDistance(Area other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }
    
    public Area farthestOfOwner(Owner o,AIFloat3 pt, float maxdist) {
        float best = 0;
        maxdist *= maxdist;
        Area besta = null;
        for (Area a : Area.areas) {
            float dist = a.gridDistance(this) + parent.rnd.nextFloat();
            if (dist > best && a.owner == o && zkai.dist(a.getCoords(), pt) < maxdist) {
                best = dist;
                besta = a;
            }
        }
        return besta;

    }
    public Area closestOfOwner(Owner o) {
        int best = Integer.MAX_VALUE;
        Area besta = null;
        for (Area a : Area.areas) {
            if (a.gridDistance(this) < best && a.owner == o) {
                best = a.gridDistance(this);
                besta = a;
            }
        }
        return besta;

    }

    public List<AIFloat3> getFreeMexes() {
        AIFloat3 c1 = getTopLeftCorner();
        AIFloat3 c2 = getBottomRightCorner();
        List<AIFloat3> res = new ArrayList();
        for (AIFloat3 m : parent.availablemetalspots) {
            if (m.x >= c1.x && m.z >= c1.z && m.x < c2.x && m.z < c2.z
                    && (parent.callback.getMap().isPossibleToBuildAt(parent.mex, m, 0))) {
                res.add(m);
            }
        }
        //parent.callback.getMap().getDrawer().deletePointsAndLines(getCoords());
        //parent.label(getCoords(),  res.size() + " free mexes");
        return res;
    }

    public float getCenterHeight() {
        return getHeight(getCoords());
    }

    static List<Float> heightmap ;
    
    static public float getHeight(AIFloat3 pos) {
        return heightmap.get(Math.round(pos.z / 8) * parent.callback.getMap().getWidth() + Math.round(pos.x / 8));
    }

    static public void init(zkai p) {
        parent = p;
        heightmap = parent.callback.getMap().getHeightMap();
        map = new Area[8][8];
        areas = new ArrayList();
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {
                map[x][y] = new Area(x, y);
                areas.add(map[x][y]);
            }
        }
        frm = new JFrame("Map possession");
        frm.setVisible(true);
        frm.setSize(200, 200);
        pnl = new MapGUI();
        frm.add(pnl);
    }

    static Area getArea(AIFloat3 coords) {
        return map[(int) (coords.x * map.length / 8 / parent.callback.getMap().getWidth())][(int) (coords.z * map[0].length / 8 / parent.callback.getMap().getHeight())];
    }

    static List<Area> getAreasInRectangle(AIFloat3 coords, AIFloat3 coords2) {
        int x1 = (int) (coords.x * map.length / 8 / parent.callback.getMap().getWidth());
        int y1 = (int) (coords.z * map[0].length / 8 / parent.callback.getMap().getHeight());
        int x2 = (int) (coords2.x * map.length / 8 / parent.callback.getMap().getWidth());
        int y2 = (int) (coords2.z * map[0].length / 8 / parent.callback.getMap().getHeight());
        List<Area> ret = new ArrayList();
        for (int x = x1; x <= Math.min(x2, map.length - 1); x++) {
            for (int y = y1; y <= Math.min(y2, map[x].length - 1); y++) {
                ret.add(map[x][y]);
            }
        }
        return ret;
    }

    static Area[][] map;
    static List<Area> areas;

    public enum Owner {

        ally, enemy, contested, neutral;
    }

}

class MapGUI extends JPanel {

    public MapGUI() {
        super();
        Area.parent.debug("MapGUI initialized");

    }

    @Override
    protected void paintComponent(Graphics g) {

        float[][] val = new float[getWidth()][getHeight()];
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        int w = getWidth() / Area.map.length;
        int h = getHeight() / Area.map[0].length;
        for (int x = 0; x < Area.map.length; x++) {
            for (int y = 0; y < Area.map[x].length; y++) {
                Color stringcol = Color.black;
                switch (Area.map[x][y].getOwner()) {
                    case ally:
                        g.setColor(Color.blue);
                        //g.setColor(new Color(Area.map[x][y].guaranteedUnits*255/Area.totUnits,Area.map[x][y].guaranteedUnits*255/Area.totUnits , 255-Area.map[x][y].guaranteedUnits*255/Area.totUnits));
                        stringcol = Color.white;
                        break;
                    case enemy:
                        g.setColor(new Color(255, 0, 0));
                        stringcol = Color.white;
                        break;
                    case contested:
                        g.setColor(new Color(255, 255, 50));
                        stringcol = Color.black;
                        break;
                    case neutral:
                        g.setColor(new Color(200, 200, 200));
                        stringcol = Color.white;
                        break;
                }
                g.fillRect(x * w, y * h, w, h);
                g.setColor(Color.darkGray);
                g.drawRect(x * w, y * h, w, h);
                g.setColor(stringcol);
                g.drawString(String.valueOf(Area.map[x][y].guaranteedUnits), x * w + w / 2 - 2, y * h + h / 2 + 4);
            }
        }

    }

}
