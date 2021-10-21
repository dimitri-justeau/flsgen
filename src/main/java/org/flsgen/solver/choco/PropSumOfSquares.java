/*
 * Copyright (c) 2021, Dimitri Justeau-Allaire
 *
 * Institut Agronomique neo-Caledonien (IAC), 98800 Noumea, New Caledonia
 * AMAP, Univ Montpellier, CIRAD, CNRS, INRA, IRD, Montpellier, France
 *
 * This file is part of flsgen.
 *
 * flsgen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * flsgen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with flsgen.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.flsgen.solver.choco;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;


/**
 * A propagator for SUM(x_i) o b
 * <br/>
 * Based on "Bounds Consistency Techniques for Long Linear Constraint" </br>
 * W. Harvey and J. Schimpf
 * <p>
 *
 * @author Charles Prud'homme
 * @since 18/03/11
 */
public class PropSumOfSquares extends Propagator<IntVar> {

    /**
     * Number of variables
     */
    protected final int l;

    /**
     * Variability of each variable (ie domain amplitude)
     */
    protected final int[] I;

    /**
     * Stores the maximal variability
     */
    protected int maxI;

    /**
     * SUm of lower bounds
     */
    protected long sumLB;

    /**
     * Sum of upper bounds
     */
    protected long sumUB;

    protected long LB;
    protected long UB;

    protected long b;

    /**
     * Creates a sum propagator: SUM(x_i) o b
     * Coefficients are induced by <code>pos</code>:
     * those before <code>pos</code> (included) are equal to 1,
     * the other ones are equal to -1.
     *
     * @param variables list of integer variables
     */
    public PropSumOfSquares(IntVar[] variables, long LB, long UB) {
        this(variables, LB, UB, computePriority(variables.length), false);
    }


    PropSumOfSquares(IntVar[] variables, long LB, long UB, PropagatorPriority priority, boolean reactOnFineEvent){
        super(variables, priority, reactOnFineEvent);
        l = variables.length;
        I = new int[l];
        maxI = 0;
        b = 0;
        this.LB = LB;
        this.UB = UB;
    }

    /**
     * Compute the priority of the propagator wrt the number of involved variables
     * @param nbvars number of variables
     * @return the priority
     */
    protected static PropagatorPriority computePriority(int nbvars) {
        if (nbvars == 1) {
            return PropagatorPriority.UNARY;
        } else if (nbvars == 2) {
            return PropagatorPriority.BINARY;
        } else if (nbvars == 3) {
            return PropagatorPriority.TERNARY;
        } else {
            return PropagatorPriority.LINEAR;
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.boundAndInst();
    }


    /**
     * Prepare the propagation: compute sumLB, sumUB and I
     */
    protected void prepare() {
        sumLB = sumUB = 0;
        int lb;
        int ub;
        maxI = 0;
        for (int i = 0; i < vars.length; i++) {
            lb = vars[i].getLB();
            ub = vars[i].getUB();
            long longLB = Long.valueOf(lb);
            long longUB = Long.valueOf(ub);
            sumLB += longLB * longLB;
            sumUB += longUB * longUB;
            I[i] = (ub - lb);
            if(maxI < I[i]) {
                maxI = I[i];
            }
        }
    }


    @Override
    public void propagate(int evtmask) throws ContradictionException {
        filter();
    }

    /**
     * Execute filtering wrt the operator
     * @throws ContradictionException if contradiction is detected
     */
    protected void filter() throws ContradictionException {
        prepare();
        if (LB == UB) {
            filterOnEq();
        } else {
            filterOnLeq();
            filterOnGeq();
        }
    }

    /**
     * Apply filtering when operator is EQ
     * @throws ContradictionException if contradiction is detected
     */
    protected void filterOnEq() throws ContradictionException {
        boolean anychange;
        long F = LB - sumLB;
        long E = sumUB - LB;
        do {
            anychange = false;
            // When explanations are on, no global failure allowed
            if (model.getSolver().isLearnOff() && (F < 0 || E < 0)) {
                fails();
            }
            if (maxI > F || maxI > E) {
                int lb;
                int ub;
                int i = 0;
                maxI = 0;
                // positive coefficients first
                while (i < vars.length) {
                    long longI = Long.valueOf(I[i]);
                    if ((longI * longI) - F > 0) {
                        lb = vars[i].getLB();
                        ub = lb + I[i];
                        if (vars[i].updateUpperBound((int) (Math.sqrt(F) + lb), this)) {
                            int nub = vars[i].getUB();
                            long longNub = Long.valueOf(nub);
                            long longUb = Long.valueOf(ub);
                            E += (longNub * longNub) - (longUb * longUb);
                            I[i] = nub - lb;
                            anychange = true;
                        }
                    }
                    if ((longI * longI) - E > 0) {
                        ub = vars[i].getUB();
                        lb = ub - I[i];
                        if (vars[i].updateLowerBound((int) (ub - Math.sqrt(E)), this)) {
                            int nlb = vars[i].getLB();
                            long longNlb = Long.valueOf(nlb);
                            long longLb = Long.valueOf(lb);
                            F -= (longNlb * longNlb) - (longLb * longLb);
                            I[i] = ub - nlb;
                            anychange = true;
                        }
                    }
                    if(maxI < I[i])maxI = I[i];
                    i++;
                }
            }
            // useless since true when all variables are instantiated
            if (F <= 0 && E <= 0) {
                this.setPassive();
                return;
            }
        } while (anychange) ;
    }

    /**
     * Apply filtering when operator is LE
     * @throws ContradictionException if contradiction is detected
     */
    protected void filterOnLeq() throws ContradictionException {
        long F = UB - sumLB;
        long E = sumUB - LB;
        // When explanations are on, no global failure allowed
        if (model.getSolver().isLearnOff() && F < 0) {
            fails();
        }
        if (maxI > F) {
            maxI = 0;
            int lb;
            int ub;
            int i = 0;
            // positive coefficients first
            while (i < vars.length) {
                long longI = Long.valueOf(I[i]);
                if ((longI * longI) - F > 0) {
                    lb = vars[i].getLB();
                    ub = lb + I[i];
                    if (vars[i].updateUpperBound((int) (Math.sqrt(F) + lb), this)) {
                        int nub = vars[i].getUB();
                        long longNub = Long.valueOf(nub);
                        long longUB = Long.valueOf(ub);
                        E += (longNub * longNub) - (longUB * longUB);
                        I[i] = nub - lb;
                    }
                }
                if(maxI < I[i])maxI = I[i];
                i++;
            }
        }
        if (E <= 0) {
            this.setPassive();
        }
    }

    /**
     * Apply filtering when operator is GE
     * @throws ContradictionException if contradiction is detected
     */
    protected void filterOnGeq() throws ContradictionException {
        long F = UB - sumLB;
        long E = sumUB - LB;
        // When explanations are on, no global failure allowed
        if (model.getSolver().isLearnOff() && E < 0) {
            fails();
        }
        if(maxI > E) {
            maxI = 0;
            int lb;
            int ub;
            int i = 0;
            // positive coefficients first
            while (i < vars.length) {
                long longI = Long.valueOf(I[i]);
                if ((longI * longI) - E > 0) {
                    ub = vars[i].getUB();
                    lb = ub - I[i];
                    if (vars[i].updateLowerBound((int) (ub - Math.sqrt(E)), this)) {
                        int nlb = vars[i].getLB();
                        long longNlb = Long.valueOf(nlb);
                        long longLB = Long.valueOf(lb);
                        F -= (longNlb * longNlb) - (longLB * longLB);
                        I[i] = ub - nlb;
                    }
                }
                if(maxI < I[i])maxI = I[i];
                i++;
            }
        }
        if (F <= 0) {
            this.setPassive();
        }
    }

    @Override
    public ESat isEntailed() {
        long sumUB = 0;
        long sumLB = 0;
        for (int i =0; i < vars.length; i++) { // first the positive coefficients
            long longLB = Long.valueOf(vars[i].getLB());
            long longUB = Long.valueOf(vars[i].getUB());
            sumLB += longLB * longLB;
            sumUB += longUB * longUB;
        }
        return check(sumLB, sumUB);
    }

    /**
     * Whether the current state of the scalar product is entailed
     * @param sumLB sum of lower bounds
     * @param sumUB sum of upper bounds
     * @return the entailment check
     */
    public ESat check(long sumLB, long sumUB){
        if (sumLB > UB) {
            return ESat.FALSE;
        }
        if (sumUB < LB) {
            return ESat.FALSE;
        }
        if (sumLB >= LB && sumUB <= UB) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}