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

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.regular.square.RegularSquareGrid;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Landscape structure org.flsgen.solver - given a list of landscape classes along with user targets, use choco
 * to find landscape structures satisfying user targets.
 */
public class LandscapeStructureSolver {

    public static final String KEY_AREA = "AREA";
    public static final String KEY_CA = "CA";
    public static final String KEY_PLAND = "PLAND";
    public static final String KEY_NP = "NP";
    public static final String KEY_PD = "PD";
    public static final String KEY_SPI = "SPI";
    public static final String KEY_LPI = "LPI";
    public static final String KEY_MESH = "MESH";
    public static final String KEY_SPLI = "SPLI";
    public static final String KEY_NPRO = "NPRO";
    public static final String KEY_SDEN = "SDEN";
    public static final String KEY_COHE = "COHE";
    public static final String KEY_DIVI = "DIVI";

    protected RegularSquareGrid grid;
    protected int nbCells;
    protected Model model;
    protected List<LandscapeClass> landscapeClasses;
    protected IntVar totalSum;
    protected boolean isBuilt;
    protected IntVar[] decisionVariables;

    public LandscapeStructureSolver(RegularSquareGrid grid) {
        this.grid = grid;
        this.nbCells = grid.getNbCells();
        this.model = new Model();
        this.landscapeClasses = new ArrayList<>();
        this.isBuilt = false;
    }

    /**
     * Constructs a landscape class to include in the problem
     * @param name class name
     * @param minNbPatches lower bound for the number of patches
     * @param maxNbPatches upper bound for the number of patches
     * @param minPatchSize lower bound for patch size
     * @param maxPatchSize upper bound for patch size
     * @return a landscape class object that can be further constrained
     */
    public LandscapeClass landscapeClass(String name, int minNbPatches, int maxNbPatches, int minPatchSize, int maxPatchSize) throws FlsgenException {
        if (isBuilt) {
            throw new FlsgenException("Cannot add a landscape class to a landscape structure org.flsgen.solver which is already built");
        }
        LandscapeClass ls = new LandscapeClass(name, landscapeClasses.size(), grid, getModel(), minNbPatches, maxNbPatches, minPatchSize, maxPatchSize);
        landscapeClasses.add(ls);
        return ls;
    }

    /**
     * Build the model - Need to be call before "findSolution"
     */
    public void build() {
        this.totalSum = getModel().intVar(0, grid.getNbCells());
        IntVar[] sums = new IntVar[landscapeClasses.size()];
        for (int i = 0; i < landscapeClasses.size(); i++) {
            sums[i] = landscapeClasses.get(i).sum;
            getModel().arithm(sums[i], "<", getModel().intVar(nbCells), "-", landscapeClasses.get(i).nbPatches).post();
        }
        getModel().sum(sums, "=", totalSum).post();
        getModel().arithm(totalSum, "<=", nbCells).post();
        this.decisionVariables = getModel().retrieveIntVars(true);
        this.isBuilt = true;
    }

    public void setRandomSearch() {
        long seed = System.currentTimeMillis();
        getModel().getSolver().setSearch(Search.randomSearch(decisionVariables, seed));
        getModel().getSolver().setRestartOnSolutions();
        getModel().getSolver().setGeometricalRestart(200, 1.5, new FailCounter(getModel(), 1), 100);
    }

    public void setDomOverWDegSearch() {
        getModel().getSolver().setSearch(Search.domOverWDegSearch(decisionVariables));
        getModel().getSolver().setGeometricalRestart(200, 1.5, new FailCounter(getModel(), 1), 100);
    }

    public void setDomOverWDegRefSearch() {
        getModel().getSolver().setSearch(Search.domOverWDegRefSearch(decisionVariables));
        getModel().getSolver().setGeometricalRestart(200, 1.5, new FailCounter(getModel(), 1), 100);
    }

    public void setActivityBasedSearch() {
        getModel().getSolver().setSearch(Search.activityBasedSearch(decisionVariables));
        getModel().getSolver().setGeometricalRestart(200, 1.5, new FailCounter(getModel(), 1), 100);
    }

    public void setDefaultSearch() {
        getModel().getSolver().setSearch(Search.defaultSearch(getModel()));
    }

    public void setConflictHistorySearch() {
        getModel().getSolver().setSearch(Search.conflictHistorySearch(decisionVariables));
        getModel().getSolver().setGeometricalRestart(200, 1.5, new FailCounter(getModel(), 1), 100);
    }

    public void setMinDomUBSearch() {
        getModel().getSolver().setSearch(Search.minDomUBSearch(decisionVariables));
    }

    public void setMinDomLBSearch() {
        getModel().getSolver().setSearch(Search.minDomLBSearch(decisionVariables));
    }

    public LandscapeStructure findSolution() {
        return findSolution(0);
    }

    /**
     * Solves the model
     * @param limitInSeconds The time limit. If 0, no time limit is set.
     * @return A LandscapeStructure object (solution wrapper) if successful, null otherwise.
     */
    public LandscapeStructure findSolution(int limitInSeconds) {
        if (!isBuilt) {
            build();
        }
        if (limitInSeconds > 0) {
            getModel().getSolver().addStopCriterion(new TimeCounter(getModel(), (long) (limitInSeconds * 1e9)));
        }
        if (getModel().getSolver().solve()) {
            return new LandscapeStructure(this);
        }
        return null;
    }

    /**
     * @return A JSON representation of the landscape structure org.flsgen.solver (formatted as expected in readFromJSON)
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

    /**
     * Instantiate a org.flsgen.solver from JSON problem descrition
     * @param reader the reader to the JSON problem description
     * @return the created LandscapeStructureSolver
     * @throws IOException
     * @throws JsonException
     */
    public static LandscapeStructureSolver readFromJSON(Reader reader) throws IOException, JsonException, FlsgenException {
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
            int[] nbPatches = getIntInterval(cljson, KEY_NP, true, name);
            // Get min and max patch sizes
            int[] patchSize = getIntInterval(cljson, KEY_AREA, true, name);
            // Construct the landscape class
            LandscapeClass landscapeClass = lStructSolver.landscapeClass(name, nbPatches[0], nbPatches[1], patchSize[0], patchSize[1]);
            // Get landscape class constraints
            // CA
            int[] ca = getIntInterval(cljson, KEY_CA, false, name);
            if (ca != null) {
                landscapeClass.setClassArea(ca[0], ca[1]);
            }
            // PLAND
            double[] pland = getDoubleInterval(cljson, KEY_PLAND, false, name);
            if (pland != null) {
                landscapeClass.setLandscapeProportion(pland[0], pland[1]);
            }
            // PD
            double[] pd = getDoubleInterval(cljson, KEY_PD, false, name);
            if (pd != null) {
                landscapeClass.setPatchDensity(pd[0], pd[1]);
            }
            // SPI
            int[] spi = getIntInterval(cljson, KEY_SPI, false, name);
            if (spi != null) {
                landscapeClass.setSmallestPatchSize(spi[0], spi[1]);
            }
            // LPI
            int[] lpi = getIntInterval(cljson, KEY_LPI, false, name);
            if (lpi != null) {
                landscapeClass.setLargestPatchSize(lpi[0], lpi[1]);
            }
            // MESH
            double[] mesh = getDoubleInterval(cljson, KEY_MESH, false, name);
            if (mesh != null) {
                landscapeClass.setMesh(mesh[0], mesh[1]);
            }
            // SPLI
            double[] spli = getDoubleInterval(cljson, KEY_SPLI, false, name);
            if (spli != null) {
                landscapeClass.setSplittingIndex(spli[0], spli[1]);
            }
            // NPRO
            int[] npro = getIntInterval(cljson, KEY_NPRO, false, name);
            if (npro != null) {
                landscapeClass.setNetProduct(npro[0], npro[1]);
            }
            // SDEN
            double[] sden = getDoubleInterval(cljson, KEY_SPLI, false, name);
            if (sden != null) {
                landscapeClass.setSplittingDensity(sden[0], sden[1]);
            }
            // COHE
            double[] cohe = getDoubleInterval(cljson, KEY_COHE, false, name);
            if (cohe != null) {
                landscapeClass.setDegreeOfCoherence(cohe[0], cohe[1]);
            }
            // DIVI
            double[] divi = getDoubleInterval(cljson, KEY_DIVI, false, name);
            if (divi != null) {
                landscapeClass.setDegreeOfDivision(divi[0], divi[1]);
            }
            // All patch with different sizes ?
            if (cljson.containsKey("patchesAllDifferent")) {
                Boolean clAllDiff = (Boolean) cljson.get("patchesAllDifferent");
                if (clAllDiff.booleanValue()) {
                    landscapeClass.setAllPatchesDifferentSize();
                }
            }
        }
        return lStructSolver;
    }

    public static int[] getIntInterval(JsonObject object, String key, boolean mandatory, String className) throws IOException {
        if (mandatory) {
            if (!object.containsKey(key)) {
                throw new IOException(key + " is a mandatory parameter of classes but missing in class " + className);
            }
        } else {
            if (!object.containsKey(key)) {
                return null;
            }
        }
        JsonArray interval = (JsonArray) object.get(key);
        if (interval.size() != 2) {
            throw new IOException(key + " must be an interval of two integer values (in class " + className + ")");
        }
        return new int[] {Integer.parseInt(interval.get(0).toString()), Integer.parseInt(interval.get(1).toString())};
    }

    public static double[] getDoubleInterval(JsonObject object, String key, boolean mandatory, String className) throws IOException {
        if (mandatory) {
            if (!object.containsKey(key)) {
                throw new IOException(key + " is a mandatory parameter of classes but missing in class " + className);
            }
        } else {
            if (!object.containsKey(key)) {
                return null;
            }
        }
        JsonArray interval = (JsonArray) object.get(key);
        if (interval.size() != 2) {
            throw new IOException(key + " must be an interval of two double values (in class " + className + ")");
        }
        return new double[] {Double.parseDouble(interval.get(0).toString()), Double.parseDouble(interval.get(1).toString())};
    }

    public Model getModel() {
        return model;
    }
}
