package solver;

import grid.regular.square.RegularSquareGrid;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;

/**
 * Class representing a landscape class that must be present in the generated landscape
 */
public class LandscapeClass {

    public String name;
    public int index;
    public RegularSquareGrid grid;

    // Bounds for number of patches
    public int minNbPatches;
    public int maxNbPatches;

    // Bounds for size of patches
    public int minPatchSize;
    public int maxPatchSize;

    // MESH bounds
    private int netProduct_lb;
    private int netProduct_ub;
    public double mesh_lb;
    public double mesh_ub;

    // Choco variables
    public Model model;
    public IntVar[] patchSizes;
    public IntVar[] squaredPatchSizes;
    public IntVar sum;
    public IntVar sumOfSquares;
    public IntVar nbPatches;

    public LandscapeClass(String name, int index, RegularSquareGrid grid, Model model, int minNbPatches, int maxNbPatches, int minPatchSize, int maxPatchSize) {
        this.name = name;
        this.index = index;
        this.model = model;
        this.grid = grid;
        this.minNbPatches = minNbPatches;
        this.maxNbPatches = maxNbPatches;
        this.minPatchSize = minPatchSize;
        this.maxPatchSize = maxPatchSize;
        this.mesh_lb = -1;
        this.mesh_ub = -1;
        // Init patch size choco variables
        this.patchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize);
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

    public void setMesh(double mesh_lb, double mesh_ub) {
        this.mesh_lb = mesh_lb;
        this.mesh_ub = mesh_ub;
        this.netProduct_lb = (int) (mesh_lb * grid.getNbCells());
        this.netProduct_ub = (int) (mesh_ub * grid.getNbCells());
        this.squaredPatchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize * maxPatchSize);
        for (int i = 0; i < maxNbPatches; i++) {
            model.times(patchSizes[i], patchSizes[i], squaredPatchSizes[i]).post();
        }
        this.sumOfSquares = model.intVar(netProduct_lb, netProduct_ub);
        model.sum(squaredPatchSizes, "=", sumOfSquares).post();
    }

    public void setTotalSize(int minTotal, int maxTotal) {
        model.arithm(sum, ">=", minTotal).post();
        model.arithm(sum, "<=", maxTotal).post();
    }

    public void setLandscapeProportion(int minProportion, int maxProportion) throws ValueException {
        if (minProportion < 0 || minProportion > 100 || maxProportion < 0 || maxProportion > 100) {
            throw new ValueException("Min and max class proportion must be between 0 and 100");
        }
        if (maxProportion < minProportion) {
            throw  new ValueException("Max proportion must be greater than or equal to min proportion");
        }
        int min = grid.getNbCells() * minProportion / 100;
        int max = grid.getNbCells() * maxProportion / 100;
        setTotalSize(min, max);
    }

    public void setAllPatchesDifferentSize() {
        model.allDifferentExcept0(patchSizes).post();
    }

    public int getNbPatches() {
        if (nbPatches.isInstantiated()) {
            return nbPatches.getValue();
        }
        return -1;
    }

    public int[] getPatchSizes() {
        return Arrays.stream(patchSizes)
                .mapToInt(v -> v.isInstantiated() ? v.getValue() : -1)
                .filter(i -> i > 0)
                .toArray();
    }

    public int getTotalSize() {
        if (sum.isInstantiated()) {
            return sum.getValue();
        }
        return -1;
    }
    public double getMesh() {
//        if (squaredPatchSizes != null) {
//            if (sumOfSquares.isInstantiated()) {
//                return sumOfSquares.getValue() / grid.getNbCells();
//            }
//            return -1;
//        } else {
            double sSum = 0;
            for (IntVar p : patchSizes) {
                if (!p.isInstantiated()) {
                    return -1;
                }
                sSum += p.getValue() * p.getValue();
            }
            return sSum / grid.getNbCells();
//        }
    }
}
