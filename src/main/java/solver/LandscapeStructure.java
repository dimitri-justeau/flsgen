package solver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Reader;

public class LandscapeStructure {

    public int nbRows;
    public int nbCols;
    public String[] names;
    public int[] totalSize;
    public int[] nbPatches;
    public int[][] patchSizes;
    public double[] mesh;

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
        JSONObject json = new JSONObject();
        json.put("nbRows", nbRows);
        json.put("nbCols", nbCols);
        JSONArray classes = new JSONArray();
        for (int i = 0; i < names.length; i++) {
            JSONObject cl = new JSONObject();
            cl.put("name", names[i]);
            cl.put("totalSize", totalSize[i]);
            cl.put("nbPatches", nbPatches[i]);
            JSONArray sizes = new JSONArray();
            for (int s : patchSizes[i]) {
                sizes.add(s);
            }
            cl.put("patchSizes", sizes);
            classes.add(cl);
            cl.put("mesh", mesh[i]);
        }
        json.put("classes", classes);
        return json.toJSONString();
    }

    public static LandscapeStructure fromJSON(Reader reader) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
//        FileReader reader = new FileReader(jsonPath);
        JSONObject structure = (JSONObject) jsonParser.parse(reader);
        int nbRows = Integer.parseInt(structure.get("nbRows").toString());
        int nbCols = Integer.parseInt(structure.get("nbCols").toString());
        JSONArray classes = (JSONArray) structure.get("classes");
        String[] names = new String[classes.size()];
        int[] totalSize = new int[classes.size()];
        int[] nbPatches = new int[classes.size()];
        int[][] patchSizes = new int[classes.size()][];
        double[] mesh = new double[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            JSONObject c = (JSONObject) classes.get(i);
            names[i] = (String) c.get("name");
            nbPatches[i] = Integer.parseInt(c.get("nbPatches").toString());
            mesh[i] = Double.parseDouble(c.get("mesh").toString());
            JSONArray sizes = (JSONArray) c.get("patchSizes");
            patchSizes[i] = new int[sizes.size()];
            for (int j = 0; j < sizes.size(); j++) {
                patchSizes[i][j] = Integer.parseInt(sizes.get(j).toString());
            }
        }
        return new LandscapeStructure(nbRows, nbCols, names, totalSize, nbPatches, patchSizes, mesh);
    }
}
