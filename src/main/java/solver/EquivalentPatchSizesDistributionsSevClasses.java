package solver;

import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class EquivalentPatchSizesDistributionsSevClasses {

    public RegularSquareGrid grid;
    public int nbCells;
    public Model model;
    public List<LandscapeClass> landscapeClasses;
    public IntVar totalSum;
    public boolean isBuilt;

    public EquivalentPatchSizesDistributionsSevClasses(RegularSquareGrid grid) {
        this.grid = grid;
        this.nbCells = grid.getNbCells();
        this.model = new Model();
        this.landscapeClasses = new ArrayList<>();
        this.isBuilt = false;
    }

    public LandscapeClass landscapeClass(String name, int minNbPatches, int maxNbPatches, int minPatchSize, int maxPatchSize) {
        LandscapeClass ls = new LandscapeClass(name, landscapeClasses.size(), grid, model, minNbPatches, maxNbPatches, minPatchSize, maxPatchSize);
        landscapeClasses.add(ls);
        return ls;
    }

    public void build() {
        this.totalSum = model.intVar(0, grid.getNbCells());
        IntVar[] sums = new IntVar[landscapeClasses.size()];
        for (int i = 0; i < landscapeClasses.size(); i++) {
            sums[i] = landscapeClasses.get(i).sum;
        }
        model.sum(sums, "=", totalSum).post();
        model.arithm(totalSum, "<=", nbCells).post();
        this.isBuilt = true;
    }
}
