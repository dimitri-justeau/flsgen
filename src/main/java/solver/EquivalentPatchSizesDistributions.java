package solver;

import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.impl.SetVarImpl;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Set;
import java.util.stream.IntStream;

public class EquivalentPatchSizesDistributions {

    public RegularSquareGrid grid;
    public int nbCells;
    public Model model;
    public int minNbPatches;
    public int maxNbPatches;
    public IntVar[] patchSizes;
    public IntVar[] squaredPatchSizes;
//    public SetVar patchSizes;
    public IntVar sum;
    public IntVar nbPatches;
    public double mesh_lb;
    public double mesh_ub;
    public int cumSize_lb;
    public int cumSize_ub;
    public int minPatchSize;
    public int maxPatchSize;

    public EquivalentPatchSizesDistributions(RegularSquareGrid grid, int minNbPatches, int maxNbPatches,
                                             double mesh_lb, double mesh_ub,
                                             int minPatchSize, int maxPatchSize) {
        this.grid = grid;
        this.nbCells = grid.getNbCells();
        this.mesh_lb = mesh_lb;
        this.mesh_ub = mesh_ub;
        this.cumSize_lb = (int) (mesh_lb * nbCells);
        this.cumSize_ub = (int) (mesh_ub * nbCells);
        this.model = new Model();
        this.minNbPatches = minNbPatches;
        this.maxNbPatches = maxNbPatches;
        this.minPatchSize = minPatchSize;
        this.maxPatchSize = maxPatchSize;
//        this.patchSizes = new SetVarImpl(
//                "patchSizes",
//                new int[]{}, SetType.BIPARTITESET,
//                IntStream.range(1, nbCells)
//                    .filter(i -> i >= minPatchSize)
//                    .filter(i -> i <= maxPatchSize)
//                    .map(i -> i * i).toArray(), SetType.BIPARTITESET,
//                model
//        );
//        this.patchSizes = model.setVar(
//        this.sum = model.intVar(cumSize_lb, cumSize_ub);
//        this.nbPatches = patchSizes.getCard();
//        model.arithm(nbPatches, ">=", minNbPatches).post();
//        model.arithm(nbPatches, "<=", maxNbPatches).post();
//        model.sum(patchSizes, sum).post();
        this.patchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize);
        IntVar limit = model.intVar(0, maxNbPatches - minNbPatches);
        model.count(0, patchSizes, limit).post();
        this.squaredPatchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize * maxPatchSize);
        for (int i = 0; i < maxNbPatches; i++) {
            model.square(squaredPatchSizes[i], patchSizes[i]).post();
        }
        this.sum = model.intVar(cumSize_lb, cumSize_ub);
        model.sum(squaredPatchSizes, "=", sum).post();
        this.nbPatches = model.intVar(minNbPatches, maxNbPatches);
        model.arithm(nbPatches, "=", model.intVar(maxNbPatches), "-", limit).post();
        for (int i = 0; i < patchSizes.length - 1; i++) {
            model.ifThen(model.arithm(patchSizes[i], "!=", 0).reify(), model.arithm(patchSizes[i], ">=", minPatchSize));
            model.arithm(patchSizes[i], "<=", patchSizes[i + 1]).post();
        }
//        model.allDifferentExcept0(patchSizes).post();
    }
}
