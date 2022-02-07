/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2022, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.flsgen.solver.choco;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.RealEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.VariableUtils;

/**
 * V0 * V1 = V2
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 26/01/11
 */
public class PropTimesNaiveMixed extends Propagator<Variable> {

    protected static final int MAX = Integer.MAX_VALUE - 1, MIN = Integer.MIN_VALUE + 1;

    private final IntVar v0;
    private final RealVar v1;
    private final RealVar v2;

    public PropTimesNaiveMixed(IntVar v1, RealVar v2, RealVar result) {
        super(new Variable[]{v1, v2, result}, PropagatorPriority.TERNARY, false);
        this.v0 = v1;
        this.v1 = v2;
        this.v2 = result;
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if (VariableUtils.isReal(vars[vIdx])) {
            return RealEventType.BOUND.getMask();
        } else {
            return IntEventType.boundAndInst();
        }
    }

    @Override
    public final void propagate(int evtmask) throws ContradictionException {
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = div(v0, v2.getLB(), v2.getUB(), v1.getLB(), v1.getUB());
            hasChanged |= div(v1, v2.getLB(), v2.getUB(), v0.getLB(), v0.getUB());
            hasChanged |= mul(v2, v0.getLB(), v0.getUB(), v1.getLB(), v1.getUB());
        }
//        if (v2.isInstantiatedTo(0) && (v0.isInstantiatedTo(0) || v1.isInstantiatedTo(0))) {
//            setPassive();
//        }
    }

    @Override
    public final ESat isEntailed() {
        if (isCompletelyInstantiated()) {
            return ESat.eval(v0.getValue() * v1.getLB() - v2.getLB() <= Math.min(v1.getPrecision(), v2.getPrecision()));
        }
        return ESat.UNDEFINED;
    }

    private boolean div(IntVar var, double a, double b, double c, double d) throws ContradictionException {
        int min, max;

        if (a <= 0 && b >= 0 && c <= 0 && d >= 0) { // case 1
            min = MIN;
            max = MAX;
            return var.updateLowerBound(min, this) | var.updateUpperBound(max, this);
        } else if (c == 0 && d == 0 && (a > 0 || b < 0)) // case 2
            fails(); // TODO: could be more precise, for explanation purpose
        else if (c < 0 && d > 0 && (a > 0 || b < 0)) { // case 3
            max = (int) Math.floor(Math.max(Math.abs(a), Math.abs(b)));
            min = (int) Math.ceil(-max);
            return var.updateLowerBound(min, this) | var.updateUpperBound(max, this);
        } else if (c == 0 && d != 0 && (a > 0 || b < 0)) // case 4 a
            return div(var, a, b, 1, d);
        else if (c != 0 && d == 0 && (a > 0 || b < 0)) // case 4 b
            return div(var, a, b, c, -1);
        else { // if (c > 0 || d < 0) { // case 5
            double ac = a / c, ad = a / d,
                    bc = b / c, bd = b / d;
            double low = Math.min(Math.min(ac, ad), Math.min(bc, bd));
            double high = Math.max(Math.max(ac, ad), Math.max(bc, bd));
            min = (int) Math.round(Math.ceil(low));
            max = (int) Math.round(Math.floor(high));
            if (min > max) this.fails(); // TODO: could be more precise, for explanation purpose
            return var.updateLowerBound(min, this) | var.updateUpperBound(max, this);
        }
        return false;
    }

    private boolean div(RealVar var, double a, double b, double c, double d) throws ContradictionException {
        double min, max;

        if (a <= 0 && b >= 0 && c <= 0 && d >= 0) { // case 1
            min = MIN;
            max = MAX;
            return var.updateLowerBound(min, this) | var.updateUpperBound(max, this);
        } else if (c == 0 && d == 0 && (a > 0 || b < 0)) // case 2
            fails(); // TODO: could be more precise, for explanation purpose
        else if (c < 0 && d > 0 && (a > 0 || b < 0)) { // case 3
            max = Math.max(Math.abs(a), Math.abs(b));
            min = -max;
            return var.updateLowerBound(min, this) | var.updateUpperBound(max, this);
        } else if (c == 0 && d != 0 && (a > 0 || b < 0)) // case 4 a
            return div(var, a, b, 1, d);
        else if (c != 0 && d == 0 && (a > 0 || b < 0)) // case 4 b
            return div(var, a, b, c, -1);
        else { // if (c > 0 || d < 0) { // case 5
            double ac = a / c, ad = a / d,
                    bc = b / c, bd = b / d;
            double low = Math.min(Math.min(ac, ad), Math.min(bc, bd));
            double high = Math.max(Math.max(ac, ad), Math.max(bc, bd));
            if (low > high) this.fails(); // TODO: could be more precise, for explanation purpose
            return var.updateLowerBound(low, this) | var.updateUpperBound(high, this);
        }
        return false;
    }

    private boolean mul(RealVar var, double a, double b, double c, double d) throws ContradictionException {
        double min = Math.min(Math.min(a * c, a * d), Math.min(b * c, b * d));
        double max = Math.max(Math.max(a * c, a * d), Math.max(b * c, b * d));
        return var.updateLowerBound(min, this) | var.updateUpperBound(max, this);
    }


}
