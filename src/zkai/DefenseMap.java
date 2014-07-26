/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author User
 */
public class DefenseMap {

    List<Unit> buildings;
    zkai parent;
    JFrame frm, frm2;
    DefensePanel pnl, pnl2;
    double maxVal=1,maxDef=1;

    public DefenseMap(zkai parent) {
        this.buildings = parent.builder.buildings;//has to be reference
        this.parent = parent;
        frm = new JFrame("DefenseCapabilityMap");
        frm.setVisible(true);
        frm.setSize(200, 200);
        frm2 = new JFrame("DefenseValueMap");
        frm2.setVisible(true);
        frm2.setSize(200, 200);
        pnl = new DefensePanel(parent.callback, this, true);
        pnl2 = new DefensePanel(parent.callback, this, false);
        frm.add(pnl);
        frm2.add(pnl2);
    }

    public float getDefense(AIFloat3 pos) {
        float res = 0;
        List<Unit> bc = new ArrayList();
        bc.addAll(buildings);
        for (Unit b : bc) {
            if (b == null) {
                parent.debug("null building");
                buildings.remove(b);
                continue;
            }
            if (b.getDef() == null) {
                parent.debug(" building without def");
                buildings.remove(b);
                continue;
            }
            /*if (b.getDef().getMaxWeaponRange() > 0) {
                parent.debug(b.getDef().getHumanName() + "'s range: " + b.getDef().getMaxWeaponRange());
            }*/
            if ( b.getDef().getMaxWeaponRange() > 100 && b.getDef().getMaxWeaponRange() >= Math.sqrt(zkai.dist(pos, b.getPos()))) {
                res += b.getDef().getCost(BuilderHandler.metal);
            }
        }
        return res;
    }

    public float getValue(AIFloat3 pos) {
        float res = 0;
        for (Unit b : buildings) {
            float dist = Math.max((float) Math.sqrt(zkai.dist(pos, b.getPos())),50);
            if (b.getResourceMake(BuilderHandler.metal) < 0.1 && b.getDef().equals(parent.mex)) {
                res += 10f / dist;
            }
            res += Math.abs(b.getResourceMake(BuilderHandler.metal) * 5 / dist);
            res += Math.abs(b.getResourceMake(BuilderHandler.energy) / dist);
            res += Math.abs(b.getDef().getCost(BuilderHandler.metal) / 100f / dist);
        }
        return res;
    }
    public float getValue(AIFloat3 pos, float radius) {
        float res = 0;
        for (Unit b : buildings) {
            float dist = Math.max((float) Math.sqrt(zkai.dist(pos, b.getPos())),50);
            if (dist < radius) res += Math.abs(b.getDef().getCost(BuilderHandler.metal));
                
        }
        return res;
    }
}

class DefensePanel extends JPanel {

    OOAICallback callback;
    DefenseMap threatmap;
    boolean mode;

    public DefensePanel(OOAICallback callback, DefenseMap map, boolean mode) {
        super();
        this.mode = mode;
        threatmap = map;
        this.callback = callback;
        map.parent.debug("ThreatPanel initialized");

    }

    int lastRedraw = 0;

    @Override
    protected void paintComponent(Graphics g) {

        if (threatmap.parent.frame - lastRedraw < 10) {
            return;
        }
        lastRedraw = threatmap.parent.frame;
        float[][] val = new float[getWidth()][getHeight()];
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                AIFloat3 pos = new AIFloat3(8 * x / (float) getWidth() * callback.getMap().getWidth(), -1f, 8 * y / (float) getHeight() * callback.getMap().getHeight());
                if (mode) {
                    val[x][y] = threatmap.getDefense(pos);
                } else {
                    val[x][y] = threatmap.getValue(pos);
                }
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
        //threatmap.parent.debug("DefensePanel(" + mode + ") painted. min = " + min + " max = " + max);
        if (!mode) threatmap.maxVal = max;
        else threatmap.maxDef = max;
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
