/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.AIFloat3;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author User
 */
public class ExpansionHandler {

    zkai parent;
    
    
    
    public ExpansionHandler(zkai parent) {
        this.parent = parent;
    }

    AIFloat3 nextMex(List<AIFloat3> used) {
        float max = 0;
        AIFloat3 maxv = null;
        float safest = Float.MAX_VALUE;
        List<AIFloat3> avail = parent.getAvailableMetalSpots();
        avail.removeAll(used);
        for (AIFloat3 m : avail) {
            if (parent.threats.getDanger(m) < safest) {
                safest = parent.threats.getDanger(m);
            }
            if (parent.defense.getDefense(m) > max) {
                max = parent.defense.getDefense(m);
                maxv = m;
            }
        }
        if (maxv != null) {
            //parent.label(maxv, "recommended mex");
            return maxv;
        }
        maxv = null;

        List<AIFloat3> safeMetalSpots = new ArrayList();
        for (AIFloat3 m : avail) {
            if (parent.threats.getDanger(m) <= safest * 2 + 1) {
                safeMetalSpots.add(m);
            }
        }
        float maxDefense = -1;
        for (AIFloat3 m : safeMetalSpots) {
            if (parent.defense.getValue(m) > maxDefense) {
                maxDefense = parent.defense.getValue(m);
                maxv = m;
            }
        }
        if (maxv != null) {
            //parent.label(maxv, "recommended mex");
            return maxv;
        }
        parent.debug("WARNING: no metal spot found for expansion");
        return null;

    }
    
}
