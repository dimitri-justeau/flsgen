package solver;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.io.Reader;

public class LandscapeStructure {

    public int nbRows;
    public int nbCols;
    public String[] names;
    public int[] totalSize;
    public int[] nbPatches;
    public int[][] patchSizes;
    public double[] mesh;
    LandscapeStructureSolver s;

    public LandscapeStructure(int nbRows, int nbCols, String[] names, int[] totalSize, int[] nbPatches, int[][] patchSizes, double[] mesh) {
        this.nbRows = nbRows;
        this.nbCols = nbCols;
        this.names = names;
        this.totalSize = totalSize;
        this.nbPatches = nbPatches;
        this.patchSizes = patchSizes;
        this.mesh = mesh;
    }

    public LandscapeStructure(LandscapeStructureSolver s) {
        this.s = s;
        this.nbRows = s.grid.getNbRows();
        this.nbCols = s.grid.getNbCols();
        this.names = new String[s.landscapeClasses.size()];
        this.totalSize = new int[s.landscapeClasses.size()];
        this.nbPatches = new int[s.landscapeClasses.size()];
        this.patchSizes = new int[s.landscapeClasses.size()][];
        this.mesh = new double[s.landscapeClasses.size()];
        for (int i = 0; i < s.landscapeClasses.size(); i++) {
            names[i] = s.landscapeClasses.get(i).name;
            nbPatches[i] = s.landscapeClasses.get(i).getNbPatches();
            patchSizes[i] = s.landscapeClasses.get(i).getPatchSizes();
            mesh[i] = s.landscapeClasses.get(i).getMesh();
            totalSize[i] = s.landscapeClasses.get(i).getTotalSize();
        }
    }

    /**
     * @return A JSON representation of the solution
     */
    public String toJSON() {
        JsonObject json = new JsonObject();
        json.put("nbRows", nbRows);
        json.put("nbCols", nbCols);
        JsonArray classes = new JsonArray();
        for (int i = 0; i < names.length; i++) {
            JsonObject cl = new JsonObject();
            cl.put("name", names[i]);
            cl.put(LandscapeStructureSolver.KEY_CA, totalSize[i]);
            cl.put(LandscapeStructureSolver.KEY_NP, nbPatches[i]);
            JsonArray sizes = new JsonArray();
            for (int s : patchSizes[i]) {
                sizes.add(s);
            }
            cl.put(LandscapeStructureSolver.KEY_AREA, sizes);
            cl.put(LandscapeStructureSolver.KEY_MESH, mesh[i]);
            if (s != null) {
                cl.put(LandscapeStructureSolver.KEY_NPRO, s.landscapeClasses.get(i).getNetProduct());
                cl.put(LandscapeStructureSolver.KEY_SPLI, s.landscapeClasses.get(i).getSplittingIndex());
                cl.put(LandscapeStructureSolver.KEY_SDEN, s.landscapeClasses.get(i).getSplittingDensity());
                cl.put(LandscapeStructureSolver.KEY_COHE, s.landscapeClasses.get(i).getDegreeOfCoherence());
                cl.put(LandscapeStructureSolver.KEY_DIVI, s.landscapeClasses.get(i).getDegreeOfDivision());
                cl.put(LandscapeStructureSolver.KEY_PLAND, s.landscapeClasses.get(i).getLandscapeProportion());
                cl.put(LandscapeStructureSolver.KEY_PD, s.landscapeClasses.get(i).getPatchDensity());
                cl.put(LandscapeStructureSolver.KEY_SPI, s.landscapeClasses.get(i).getSmallestPatchIndex());
                cl.put(LandscapeStructureSolver.KEY_LPI, s.landscapeClasses.get(i).getLargestPatchIndex());
            }
            classes.add(cl);
        }
        json.put("classes", classes);
        return Jsoner.prettyPrint(json.toJson());
    }

    public static LandscapeStructure fromJSON(Reader reader) throws JsonException {
        JsonObject structure = (JsonObject) Jsoner.deserialize(reader);
        int nbRows = Integer.parseInt(structure.get("nbRows").toString());
        int nbCols = Integer.parseInt(structure.get("nbCols").toString());
        JsonArray classes = (JsonArray) structure.get("classes");
        String[] names = new String[classes.size()];
        int[] totalSize = new int[classes.size()];
        int[] nbPatches = new int[classes.size()];
        int[][] patchSizes = new int[classes.size()][];
        double[] mesh = new double[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            JsonObject c = (JsonObject) classes.get(i);
            names[i] = (String) c.get("name");
            nbPatches[i] = Integer.parseInt(c.get(LandscapeStructureSolver.KEY_NP).toString());
            mesh[i] = Double.parseDouble(c.get(LandscapeStructureSolver.KEY_MESH).toString());
            JsonArray sizes = (JsonArray) c.get(LandscapeStructureSolver.KEY_AREA);
            patchSizes[i] = new int[sizes.size()];
            for (int j = 0; j < sizes.size(); j++) {
                patchSizes[i][j] = Integer.parseInt(sizes.get(j).toString());
            }
        }
        return new LandscapeStructure(nbRows, nbCols, names, totalSize, nbPatches, patchSizes, mesh);
    }
}
