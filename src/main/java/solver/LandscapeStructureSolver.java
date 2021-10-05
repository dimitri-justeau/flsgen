package solver;

import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.differences.D;
import org.chocosolver.solver.variables.IntVar;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

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
        for (int i = 0; i < landscapeClasses.size(); i++) {
            sums[i] = landscapeClasses.get(i).sum;
        }
        model.sum(sums, "=", totalSum).post();
        model.arithm(totalSum, "<=", nbCells).post();
        this.isBuilt = true;
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
        JSONObject json = new JSONObject();
        json.put("nbRows", grid.getNbRows());
        json.put("nbCols", grid.getNbCols());
        JSONArray classes = new JSONArray();
        for (LandscapeClass l : landscapeClasses) {
            JSONObject cl = new JSONObject();
            cl.put("name", l.name);
            JSONArray nbPatches = new JSONArray();
            nbPatches.add(l.minNbPatches);
            nbPatches.add(l.maxNbPatches);
            cl.put("nbPatches", nbPatches);
            JSONArray patchSize = new JSONArray();
            patchSize.add(l.minPatchSize);
            patchSize.add(l.maxPatchSize);
            cl.put("patchSize", patchSize);
            classes.add(cl);

        }
        json.put("classes", classes);
        return json.toJSONString();
    }

    public static LandscapeStructureSolver readFromJSON(Reader reader) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject targets = (JSONObject) jsonParser.parse(reader);
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
        JSONArray classes = (JSONArray) targets.get("classes");
        for (Object cl : classes) {
            JSONObject cljson = (JSONObject) cl;
            // Get name
            if (!cljson.containsKey("name")) {
                throw new IOException("'name' is a mandatory parameter of classes but missing in input JSON file");
            }
            String name = cljson.get("name").toString();
            // Get min and max nb patches
            if (!cljson.containsKey("nbPatches")) {
                throw new IOException("'nbPatches' is a mandatory parameter of classes but missing in class " + name);
            }
            JSONArray nbPatches = (JSONArray) cljson.get("nbPatches");
            if (nbPatches.size() != 2) {
                throw new IOException("'nbPatches' must be an interval of two integer values (in class " + name + ")");
            }
            int minNbPatches = Integer.parseInt(nbPatches.get(0).toString());
            int maxNbPatches = Integer.parseInt(nbPatches.get(1).toString());
            // Get min and max patch sizes
            if (!cljson.containsKey("patchSize")) {
                throw new IOException("'patchSize' is a mandatory parameter of classes but missing in class " + name);
            }
            JSONArray patchSize = (JSONArray) cljson.get("patchSize");
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
                JSONArray clTotalSize = (JSONArray) cljson.get("totalSize");
                if (clTotalSize.size() != 2) {
                    throw new IOException("'totalSize' must be an interval of two integer values (in class " + name + ")");
                }
                int minTotalSize = Integer.parseInt(clTotalSize.get(0).toString());
                int maxTotalSize = Integer.parseInt(clTotalSize.get(1).toString());
                landscapeClass.setTotalSize(minTotalSize, maxTotalSize);
            }
            if (landscapeProportion) {
                JSONArray clProportion = (JSONArray) cljson.get("landscapeProportion");
                if (clProportion.size() != 2) {
                    throw new IOException("'landscapeProportion' must be an interval of two integer values (in class " + name + ")");
                }
                int minProportion = Integer.parseInt(clProportion.get(0).toString());
                int maxProportion = Integer.parseInt(clProportion.get(1).toString());
                landscapeClass.setLandscapeProportion(minProportion, maxProportion);
            }
            // 2. MESH
            if (cljson.containsKey("mesh")) {
                JSONArray clMesh = (JSONArray) cljson.get("mesh");
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
