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

package org.flsgen.solver;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.solver.choco.PropSumOfSquares;

import java.util.Arrays;

/**
 * Class representing a landscape class that must be present in the generated landscape
 */
public class LandscapeClass {

    protected String name;
    protected int index;
    protected RegularSquareGrid grid;
    protected int landscapeSize;

    // Bounds for number of patches
    protected int minNbPatches;
    protected int maxNbPatches;

    // Bounds for size of patches
    protected int minPatchSize;
    protected int maxPatchSize;

    // MESH bounds
    protected double mesh_lb;
    protected double mesh_ub;

    // Choco variables
    protected Model model;
    protected IntVar[] patchSizes;
//    protected IntVar[] squaredPatchSizes;
    protected IntVar sum;
//    private IntVar sumOfSquares;
    protected IntVar nbPatches;

    public LandscapeClass(String name, int index, RegularSquareGrid grid, Model model, int minNbPatches, int maxNbPatches, int minPatchSize, int maxPatchSize) throws FlsgenException {
        this.name = name;
        this.index = index;
        this.model = model;
        this.grid = grid;
        this.landscapeSize = grid.getNbCells();
        if (maxPatchSize < minPatchSize) {
            throw new FlsgenException("Max patch area must be greater than or equal to min patch area");
        }
        if (maxNbPatches < minNbPatches) {
            throw new FlsgenException("Max patch area must be greater than or equal to min patch area");
        }
        this.minNbPatches = minNbPatches;
        this.maxNbPatches = maxNbPatches;
        this.minPatchSize = minPatchSize;
        this.maxPatchSize = maxPatchSize;
        this.mesh_lb = -1;
        this.mesh_ub = -1;
        // Init patch size choco variables
        this.patchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize, false);
        IntVar limit = model.intVar(0, maxNbPatches - minNbPatches);
        model.count(0, patchSizes, limit).post();
        this.nbPatches = model.intVar(minNbPatches, maxNbPatches);
        model.arithm(nbPatches, "=", model.intVar(maxNbPatches), "-", limit).post();
        for (int i = 0; i < patchSizes.length - 1; i++) {
            model.ifThen(model.arithm(patchSizes[i], "!=", 0).reify(), model.arithm(patchSizes[i], ">=", minPatchSize));
            model.arithm(patchSizes[i], "<=", patchSizes[i + 1]).post();
        }
        this.sum = model.intVar(minPatchSize * minNbPatches, maxPatchSize * maxNbPatches);
        model.sum(patchSizes, "=", sum).post();
    }

//    public void initNetProduct(int netProductLB, int netProductUB) {
//        this.squaredPatchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize * maxPatchSize);
//        for (int i = 0; i < maxNbPatches; i++) {
//            model.times(patchSizes[i], patchSizes[i], squaredPatchSizes[i]).post();
//        }
//        this.sumOfSquares = model.intVar(netProductLB, netProductUB);
//        model.sum(squaredPatchSizes, "=", sumOfSquares).post();
//        model.post(new Constraint("sumOfSquares", new PropSumOfSquares(patchSizes, netProductLB, netProductUB)));
//    }

    ///--- USER TARGETS ---///

     // CA - Total class area
    /**
     * Set a total class area (CA) target
     * @param minClassArea
     * @param maxClassArea
     */
    public void setClassArea(int minClassArea, int maxClassArea) throws FlsgenException {
        if (maxClassArea < minClassArea) {
            throw new FlsgenException("Max class area must be greater than or equal to min class area");
        }
        model.arithm(sum, ">=", minClassArea).post();
        model.arithm(sum, "<=", maxClassArea).post();
    }

    // PLAND - Proportion of landscape
    /**
     * Set a proportion of landscape (PLAND) target
     * @param minProportion
     * @param maxProportion
     */
    public void setLandscapeProportion(double minProportion, double maxProportion) throws FlsgenException {
        if (minProportion < 0 || minProportion > 100 || maxProportion < 0 || maxProportion > 100) {
            throw new FlsgenException("Min and max class proportion must be between 0 and 100");
        }
        if (maxProportion < minProportion) {
            throw new FlsgenException("Max proportion must be greater than or equal to min proportion");
        }
        int min = (int) (landscapeSize * minProportion / 100);
        int max = (int) (landscapeSize * maxProportion / 100);
        setClassArea(min, max);
    }

    // PD - Patch density
    /**
     * Set a patch density (PD) target
     * @param minDensity
     * @param maxDensity
     */
    public void setPatchDensity(double minDensity, double maxDensity) throws FlsgenException {
        if (maxDensity < minDensity) {
            throw new FlsgenException("Max patch density area must be greater than or equal to min patch density");
        }
        int minNbPatches = (int) (minDensity * landscapeSize);
        int maxNbPatches = (int) (maxDensity * landscapeSize);
        model.arithm(nbPatches, ">=", minNbPatches).post();
        model.arithm(nbPatches, "<=", maxNbPatches).post();
    }

    // SPI - Smallest patch index
    /**
     * Set a smallest patch index (SPI) target
     * @param minSize
     * @param maxSize
     */
    public void setSmallestPatchSize(int minSize, int maxSize) throws FlsgenException {
        if (maxSize < minSize) {
            throw new FlsgenException("Max SPI must be greater than or equal to min SPI");
        }
        model.min(model.intVar(minSize, maxSize), patchSizes).post();
    }

    // LPI - Largest patch index
    /**
     * Set a largest patch index (LPI) target
     * @param minSize
     * @param maxSize
     */
    public void setLargestPatchSize(int minSize, int maxSize) throws FlsgenException {
        if (maxSize < minSize) {
            throw new FlsgenException("Max LPI must be greater than or equal to min LPI");
        }
        model.arithm(patchSizes[patchSizes.length - 1], ">=", minSize).post();
        model.arithm(patchSizes[patchSizes.length - 1], "<=", maxSize).post();
    }

    // MESH - Effective mesh size
    /**
     * Set an effective mesh size (MESH) target.
     * @param mesh_lb
     * @param mesh_ub
     */
    public void setMesh(double mesh_lb, double mesh_ub) throws FlsgenException {
        if (mesh_ub < mesh_lb) {
            throw new FlsgenException("Max MESH must be greater than or equal to min MESH");
        }
        this.mesh_lb = mesh_lb;
        this.mesh_ub = mesh_ub;
        long netProduct_lb = (long) (mesh_lb * landscapeSize);
        long netProduct_ub = (long) (mesh_ub * landscapeSize);
        setNetProduct(netProduct_lb, netProduct_ub);
    }

    // NPRO - Net product
    /**
     * Set a net product (NPRO) target
     * @param minNetProduct
     * @param maxNetProduct
     */
    public void setNetProduct(long minNetProduct, long maxNetProduct) throws FlsgenException {
        if (maxNetProduct < minNetProduct) {
            throw new FlsgenException("Max NPRO must be greater than or equal to min NPRO");
        }
//        if (sumOfSquares == null) {
//            initNetProduct(minNetProduct, maxNetProduct);
//        }
        model.post(new Constraint("sumOfSquares", new PropSumOfSquares(patchSizes, minNetProduct, maxNetProduct)));
//        model.arithm(sumOfSquares, ">=", minNetProduct).post();
//        model.arithm(sumOfSquares, "<=", maxNetProduct).post();
    }

    // SPLI - Splitting index
    /**
     * Set a splitting index (SPLI) target
     * @param minSplittingIndex
     * @param maxSplittingIndex
     */
    public void setSplittingIndex(double minSplittingIndex, double maxSplittingIndex) throws FlsgenException {
        if (maxSplittingIndex < minSplittingIndex) {
            throw new FlsgenException("Max SPLI must be greater than or equal to min SPLI");
        }
        long netProductLB = (long) (landscapeSize * landscapeSize / maxSplittingIndex);
        long netProductUB = (long) (landscapeSize * landscapeSize / minSplittingIndex);
        setNetProduct(netProductLB, netProductUB);
    }

    // SDEN - Splitting density
    /**
     * Set a splitting density (SDEN) target
     * @param minSplittingDensity
     * @param maxSplittingDensity
     */
    public void setSplittingDensity(double minSplittingDensity, double maxSplittingDensity) throws FlsgenException {
        if (maxSplittingDensity < minSplittingDensity) {
            throw new FlsgenException("Max SDEN must be greater than or equal to min SDEN");
        }
        long netProductLB = (long) (landscapeSize / maxSplittingDensity);
        long netProductUB = (long) (landscapeSize / minSplittingDensity);
        setNetProduct(netProductLB, netProductUB);
    }

    // COHE - Degree of coherence
    /**
     * Set a degree of coherence (COHE) index
     * @param minCoherence
     * @param maxCoherence
     */
    public void setDegreeOfCoherence(double minCoherence, double maxCoherence) throws FlsgenException {
        if (minCoherence < 0 || minCoherence > 1 || maxCoherence < 0 || maxCoherence > 1) {
            throw new FlsgenException("Min and max coherence must be between 0 and 1");
        }
        if (maxCoherence < minCoherence) {
            throw new FlsgenException("Max COHE must be greater than or equal to min COHE");
        }
        long netProductLB = (long) (minCoherence * landscapeSize * landscapeSize);
        long netProductUB = (long) (maxCoherence * landscapeSize * landscapeSize);
        setNetProduct(netProductLB, netProductUB);
    }

    // DIVI - Degree of landscape division
    /**
     * Set a degree of division (DIVI) target
     * @param minDivision
     * @param maxDivision
     */
    public void setDegreeOfDivision(double minDivision, double maxDivision) throws FlsgenException {
        if (minDivision < 0 || minDivision > 1 || maxDivision < 0 || maxDivision > 1) {
            throw new FlsgenException("Min and max degree of division must be between 0 and 1");
        }
        if (maxDivision < minDivision) {
            throw new FlsgenException("Max DIVI must be greater than or equal to min DIVI");
        }
        setDegreeOfCoherence(1 - maxDivision, 1 -minDivision);
    }

    /**
     * Post a constraint ensuring that all patch will have different areas
     */
    public void setAllPatchesDifferentSize() {
        model.allDifferentExcept0(patchSizes).post();
    }

    /**
     * @return The number of patches if the corresponding variable is instantiated
     */
    public int getNbPatches() {
        if (nbPatches.isInstantiated()) {
            return nbPatches.getValue();
        }
        return -1;
    }

    /**
     * @return The patch sizes if the corresponding variables are instantiated
     */
    public int[] getPatchSizes() {
        return Arrays.stream(patchSizes)
                .mapToInt(v -> v.isInstantiated() ? v.getValue() : -1)
                .filter(i -> i > 0)
                .toArray();
    }

    /**
     * @return The total class area if the corresponding variable is instantiated
     */
    public int getTotalSize() {
        if (sum.isInstantiated()) {
            return sum.getValue();
        }
        return -1;
    }

    /**
     * @return The landscape proportion if the corresponding variable is instantiated
     */
    public double getLandscapeProportion() {
        return 100 * (1.0 * getTotalSize()) / (1.0 * landscapeSize);
    }

    /**
     * @return The patch density if the corresponding variable is instantiated
     */
    public double getPatchDensity() {
        return (1.0 * getNbPatches()) / (1.0 * landscapeSize);
    }

    /**
     * @return The the smallest patch index if the corresponding variable is instantiated
     */
    public int getSmallestPatchIndex() {
        return getPatchSizes()[0];
    }

    /**
     * @return The largest patch index if the corresponding variable is instantiated
     */
    public int getLargestPatchIndex() {
        return getPatchSizes()[getNbPatches() - 1];
    }

    /**
     * @return The net product if the corresponding variable is instantiated
     */
    public long getNetProduct() {
        long npro = 0;
        for (IntVar p : patchSizes) {
            if (!p.isInstantiated()) {
                return -1;
            }
            long v = new Long(p.getValue()).longValue();
            npro += v * v;
        }
        return npro;
    }

    /**
     * @return The effective mesh size if the corresponding variable is instantiated
     */
    public double getMesh() {
        return (1.0 * getNetProduct()) / (1.0 * landscapeSize);
    }

    /**
     * @return The splitting index if the corresponding variable is instantiated
     */
    public double getSplittingIndex() {
        return (1.0 * landscapeSize * landscapeSize) / (1.0 * getNetProduct());
    }

    /**
     * @return The splitting density if the corresponding variable is instantiated
     */
    public double getSplittingDensity() {
        return (1.0 * landscapeSize) / (1.0 * getNetProduct());
    }

    /**
     * @return The degree of coherence if the corresponding variable is instantiated
     */
    public double getDegreeOfCoherence() {
        return (1.0 * getNetProduct()) / (1.0 * landscapeSize * landscapeSize);
    }

    /**
     * @return The degree of division if the corresponding variable is instantiated
     */
    public double getDegreeOfDivision() {
        return 1 - getDegreeOfCoherence();
    }
}
