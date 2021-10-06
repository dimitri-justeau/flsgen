package solver;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandomBound;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.ConflictHistorySearch;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class LandscapeStructureSolver {

    public RegularSquareGrid grid;
    public int nbCells;
    public Model model;
    public List<LandscapeClass> landscapeClasses;
    public IntVar totalSum;
    public boolean isBuilt;
    public IntVar[] decisionVariables;

    public LandscapeStructureSolver(RegularSquareGrid grid) {
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
        int nbMaxTotalPatches = 0;
        for (int i = 0; i < landscapeClasses.size(); i++) {
            sums[i] = landscapeClasses.get(i).sum;
            nbMaxTotalPatches += landscapeClasses.get(i).patchSizes.length;
        }
        model.sum(sums, "=", totalSum).post();
        model.arithm(totalSum, "<=", nbCells).post();
//        this.decisionVariables = new IntVar[nbMaxTotalPatches + landscapeClasses.size()];
//        int n = 0;
//        for (int i = 0; i < landscapeClasses.size(); i++) {
//            for (IntVar var : landscapeClasses.get(i).patchSizes) {
//                this.decisionVariables[n] = var;
//                n++;
//            }
//            decisionVariables[nbMaxTotalPatches + i] = landscapeClasses.get(i).nbPatches;
//        }
        this.decisionVariables = model.retrieveIntVars(true);
        this.isBuilt = true;
    }

    public void setRandomSearch() {
        long seed = System.currentTimeMillis();
        model.getSolver().setSearch(Search.randomSearch(decisionVariables, seed));
        model.getSolver().setRestartOnSolutions();
        model.getSolver().setGeometricalRestart(100, 1.5, new FailCounter(model, 1), 100);
    }

    public void setDomOverWDegRandomSearch() {
        long seed = System.currentTimeMillis();
        IntValueSelector value = new IntDomainRandom(seed);
        IntValueSelector bound = new IntDomainRandomBound(seed);
        IntValueSelector selector = var -> {
            if (var.hasEnumeratedDomain()) {
                return value.selectValue(var);
            } else {
                return bound.selectValue(var);
            }
        };
        model.getSolver().setSearch(Search.intVarSearch(
                new ConflictHistorySearch(decisionVariables, seed),
                selector,
                decisionVariables
        ));
        model.getSolver().setRestartOnSolutions();
        model.getSolver().setGeometricalRestart(100, 2, new FailCounter(model, 1), 100);
    }

    public void setMinDomRandomSearch() {
        long seed = System.currentTimeMillis();
        IntValueSelector value = new IntDomainRandom(seed);
        IntValueSelector bound = new IntDomainRandomBound(seed);
        IntValueSelector selector = var -> {
            if (var.hasEnumeratedDomain()) {
                return value.selectValue(var);
            } else {
                return bound.selectValue(var);
            }
        };
        model.getSolver().setSearch(Search.intVarSearch(
                new FirstFail(model),
                selector,
                decisionVariables
        ));
        model.getSolver().setRestartOnSolutions();
        model.getSolver().setGeometricalRestart(100, 1.5, new FailCounter(model, 1), 100);
    }

    public void setDomOverWDegSearch() {
//        model.getSolver().setSearch(Search.domOverWDegSearch(decisionVariables));
        model.getSolver().setSearch(Search.intVarSearch(decisionVariables));
        model.getSolver().setRestartOnSolutions();
//        model.getSolver().setGeometricalRestart(100, 2, new FailCounter(model, 1), 100);
    }

    public void setDomOverWDegRefSearch() {
        model.getSolver().setSearch(Search.domOverWDegRefSearch(decisionVariables));
        model.getSolver().setRestartOnSolutions();
        model.getSolver().setGeometricalRestart(100, 2, new FailCounter(model, 1), 100);
    }

    public void setActivityBasedSearch() {
        model.getSolver().setSearch(Search.activityBasedSearch(decisionVariables));
        model.getSolver().setRestartOnSolutions();
        model.getSolver().setGeometricalRestart(100, 2, new FailCounter(model, 1), 100);
    }

    public void setDefaultSearch() {
        model.getSolver().setSearch(Search.defaultSearch(model));
    }

    public void setConflictHistorySearch() {
        model.getSolver().setSearch(Search.conflictHistorySearch(decisionVariables));
        model.getSolver().setRestartOnSolutions();
        model.getSolver().setGeometricalRestart(100, 2, new FailCounter(model, 1), 100);
    }

    public void setMinDomUBSearch() {
        model.getSolver().setSearch(Search.minDomUBSearch(decisionVariables));
    }

    public void setMinDomLBSearch() {
        model.getSolver().setSearch(Search.minDomLBSearch(decisionVariables));
    }

    public LandscapeStructure findSolution() {
        if (model.getSolver().solve()) {
            return new LandscapeStructure(this);
        }
        return null;
    }

    /**
     * @return A JSON representation of the landscape structure solver (formatted as expected in readFromJSON)
     */
    public String toJSON() {
        JsonObject json = new JsonObject();
        json.put("nbRows", grid.getNbRows());
        json.put("nbCols", grid.getNbCols());
        JsonArray classes = new JsonArray();
        for (LandscapeClass l : landscapeClasses) {
            JsonObject cl = new JsonObject();
            cl.put("name", l.name);
            JsonArray nbPatches = new JsonArray();
            nbPatches.add(l.minNbPatches);
            nbPatches.add(l.maxNbPatches);
            cl.put("nbPatches", nbPatches);
            JsonArray patchSize = new JsonArray();
            patchSize.add(l.minPatchSize);
            patchSize.add(l.maxPatchSize);
            cl.put("patchSize", patchSize);
            classes.add(cl);

        }
        json.put("classes", classes);
        return Jsoner.prettyPrint(json.toJson());
    }

    public static LandscapeStructureSolver readFromJSON(Reader reader) throws IOException, JsonException {
        JsonObject targets = (JsonObject) Jsoner.deserialize(reader);
        // Get map dimensions
        if (!targets.containsKey("nbRows") || !targets.containsKey("nbCols")) {
            throw new IOException("'nbRows' and 'nbCols' are mandatory parameters but missing in input JSON file");
        }
        int nbRows = Integer.parseInt(targets.get("nbRows").toString());
        int nbCols = Integer.parseInt(targets.get("nbCols").toString());
        RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);
        LandscapeStructureSolver lStructSolver = new LandscapeStructureSolver(grid);
        // Get classes
        if (!targets.containsKey("classes")) {
            throw new IOException("'classes' is a mandatory parameter but missing in input JSON file");
        }
        JsonArray classes = (JsonArray) targets.get("classes");
        for (Object cl : classes) {
            JsonObject cljson = (JsonObject) cl;
            // Get name
            if (!cljson.containsKey("name")) {
                throw new IOException("'name' is a mandatory parameter of classes but missing in input JSON file");
            }
            String name = cljson.get("name").toString();
            // Get min and max nb patches
            if (!cljson.containsKey("nbPatches")) {
                throw new IOException("'nbPatches' is a mandatory parameter of classes but missing in class " + name);
            }
            JsonArray nbPatches = (JsonArray) cljson.get("nbPatches");
            if (nbPatches.size() != 2) {
                throw new IOException("'nbPatches' must be an interval of two integer values (in class " + name + ")");
            }
            int minNbPatches = Integer.parseInt(nbPatches.get(0).toString());
            int maxNbPatches = Integer.parseInt(nbPatches.get(1).toString());
            // Get min and max patch sizes
            if (!cljson.containsKey("patchSize")) {
                throw new IOException("'patchSize' is a mandatory parameter of classes but missing in class " + name);
            }
            JsonArray patchSize = (JsonArray) cljson.get("patchSize");
            if (nbPatches.size() != 2) {
                throw new IOException("'patchSize' must be an interval of two integer values (in class " + name + ")");
            }
            int minPatchSize = Integer.parseInt(patchSize.get(0).toString());
            int maxPatchsize = Integer.parseInt(patchSize.get(1).toString());
            // Construct the landscape class
            LandscapeClass landscapeClass = lStructSolver.landscapeClass(name, minNbPatches, maxNbPatches, minPatchSize, maxPatchsize);
            // Get landscape class constraints
            // 1. Total size or proportion
            boolean totalSize = cljson.containsKey("totalSize");
            boolean landscapeProportion = cljson.containsKey("landscapeProportion");
            if (totalSize && landscapeProportion) {
                throw new IOException("'totalSize' and 'landscapeProportion' cannot be used simultaneously (in class " + name + ")");
            }
            if (totalSize) {
                JsonArray clTotalSize = (JsonArray) cljson.get("totalSize");
                if (clTotalSize.size() != 2) {
                    throw new IOException("'totalSize' must be an interval of two integer values (in class " + name + ")");
                }
                int minTotalSize = Integer.parseInt(clTotalSize.get(0).toString());
                int maxTotalSize = Integer.parseInt(clTotalSize.get(1).toString());
                landscapeClass.setTotalSize(minTotalSize, maxTotalSize);
            }
            if (landscapeProportion) {
                JsonArray clProportion = (JsonArray) cljson.get("landscapeProportion");
                if (clProportion.size() != 2) {
                    throw new IOException("'landscapeProportion' must be an interval of two integer values (in class " + name + ")");
                }
                int minProportion = Integer.parseInt(clProportion.get(0).toString());
                int maxProportion = Integer.parseInt(clProportion.get(1).toString());
                landscapeClass.setLandscapeProportion(minProportion, maxProportion);
            }
            // 2. MESH
            if (cljson.containsKey("mesh")) {
                JsonArray clMesh = (JsonArray) cljson.get("mesh");
                if (clMesh.size() != 2) {
                    throw new IOException("'mesh' must be an interval of two integer values (in class " + name + ")");
                }
                double minMesh = Double.parseDouble(clMesh.get(0).toString());
                double maxMesh = Double.parseDouble(clMesh.get(1).toString());
                landscapeClass.setMesh(minMesh, maxMesh);
            }
            // 3. All patch with different sizes
            if (cljson.containsKey("patchesAllDifferent")) {
                Boolean clAllDiff = (Boolean) cljson.get("patchesAllDifferent");
                if (clAllDiff.booleanValue()) {
                    landscapeClass.setAllPatchesDifferentSize();
                }
            }
        }
        return lStructSolver;
    }
}
