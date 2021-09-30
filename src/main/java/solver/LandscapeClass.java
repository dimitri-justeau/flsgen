package solver;

import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

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
    public IntVar squaredSum;
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
            model.square(squaredPatchSizes[i], patchSizes[i]).post();
        }
        this.squaredSum = model.intVar(netProduct_lb, netProduct_ub);
        model.square(squaredSum, sum).post();
    }

    public void setTotalSize(int minTotal, int maxTotal) {
        model.arithm(sum, ">=", minTotal).post();
        model.arithm(sum, "<=", maxTotal).post();
    }

    public void setAllPatchesDifferentSize() {
        model.allDifferentExcept0(patchSizes).post();
    }
}
