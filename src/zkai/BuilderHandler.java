/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkai;

import com.springrts.ai.oo.clb.Unit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 *
 * @author User
 */
public class BuilderHandler {

    zkai parent;

    List<Builder> builders;

    public BuilderHandler(zkai parent) {
        this.parent = parent;
        builders = new ArrayList();
    }

    class Builder {

        Unit unit;
        Deque<Integer> orders;

        public Builder(Unit u) {
            unit = u;
            orders = new ArrayDeque();  
        }

    }

}
