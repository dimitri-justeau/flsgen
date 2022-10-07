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
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.regular.square.PartialRegularSquareGrid;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.utils.RasterConnectivityFinder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Class representing a landscape structure instantiation - for storing and retrieving solutions.
 */
public class LandscapeStructure {

    protected int nbRows;
    protected int nbCols;
    protected String[] names;
    protected int[] totalSize; // CA
    protected int[] nbPatches; // NP
    protected int[][] patchSizes; // AREA
    protected long[] npro; // NPRO
    protected boolean[] isSquare;
    protected LandscapeStructureSolver s;
    protected RegularSquareGrid grid;

    protected String maskRasterPath;

    public LandscapeStructure(int nbRows, int nbCols, String maskRasterPath, int[] noDataCells, String[] names,
                              int[] totalSize, int[] nbPatches, int[][] patchSizes, long[] npro, boolean[] isSquare) {
        this.nbRows = nbRows;
        this.nbCols = nbCols;
        this.maskRasterPath = maskRasterPath;
        this.names = names;
        this.totalSize = totalSize;
        this.nbPatches = nbPatches;
        this.patchSizes = patchSizes;
        this.isSquare = isSquare;
        this.npro = npro;
        if (noDataCells.length > 0) {
            this.grid = new PartialRegularSquareGrid(nbRows, nbCols, noDataCells);
        } else {
            this.grid = new RegularSquareGrid(nbRows, nbCols);
        }
    }

    public LandscapeStructure(int nbRows, int nbCols, String[] names, int[] totalSize, int[] nbPatches, int[][] patchSizes,
                              long[] npro, boolean[] isSquare) {
        this(nbRows, nbCols, null, new int[] {}, names, totalSize, nbPatches, patchSizes, npro, isSquare);
    }

    public LandscapeStructure(LandscapeStructureSolver s) {
        this.s = s;
        this.grid = s.getGrid();
        this.nbRows = s.getGrid().getNbRows();
        this.nbCols = s.getGrid().getNbCols();
        if (s.maskRasterPath != null) {
            this.maskRasterPath = s.maskRasterPath;
        }
        this.names = new String[s.landscapeClasses.size()];
        this.totalSize = new int[s.landscapeClasses.size()];
        this.nbPatches = new int[s.landscapeClasses.size()];
        this.patchSizes = new int[s.landscapeClasses.size()][];
        this.npro = new long[s.landscapeClasses.size()];
        this.isSquare = new boolean[s.landscapeClasses.size()];
        for (int i = 0; i < s.landscapeClasses.size(); i++) {
            names[i] = s.landscapeClasses.get(i).name;
            nbPatches[i] = s.landscapeClasses.get(i).getNbPatches();
            patchSizes[i] = s.landscapeClasses.get(i).getPatchSizes();
            npro[i] = s.landscapeClasses.get(i).getNetProduct();
            totalSize[i] = s.landscapeClasses.get(i).getTotalSize();
            isSquare[i] = s.landscapeClasses.get(i) instanceof SquaresLandscapeClass;
        }
    }

    /**
     * @return A JSON representation of the solution
     */
    public String toJSON() {
        JsonObject json = new JsonObject();
        json.put("nbRows", getNbRows());
        json.put("nbCols", getNbCols());
        if (maskRasterPath != null) {
            json.put("maskRasterPath", maskRasterPath);
        }
        json.put(LandscapeStructureSolver.KEY_NON_FOCAL_PLAND, getNonFocalLandscapeProportion());
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
            cl.put(LandscapeStructureSolver.KEY_AREA_MN, getMeanPatchArea(i));
            cl.put(LandscapeStructureSolver.KEY_NPRO, npro[i]);
            cl.put(LandscapeStructureSolver.KEY_MESH, getMesh(i));
            cl.put(LandscapeStructureSolver.KEY_SPLI, getSplittingIndex(i));
            cl.put(LandscapeStructureSolver.KEY_SDEN, getSplittingDensity(i));
            cl.put(LandscapeStructureSolver.KEY_COHE, getDegreeOfCoherence(i));
            cl.put(LandscapeStructureSolver.KEY_DIVI, getDegreeOfDivision(i));
            cl.put(LandscapeStructureSolver.KEY_PLAND, getLandscapeProportion(i));
            cl.put(LandscapeStructureSolver.KEY_PD, getPatchDensity(i));
            cl.put(LandscapeStructureSolver.KEY_SPI, getSmallestPatchIndex(i));
            cl.put(LandscapeStructureSolver.KEY_LPI, getLargestPatchIndex(i));
            cl.put(LandscapeStructureSolver.IS_SQUARE, this.isSquare[i]);
            classes.add(cl);
        }
        json.put("classes", classes);
        return Jsoner.prettyPrint(json.toJson());
    }

    public static LandscapeStructure fromJSON(String json, int nbRows, int nbCols, int[] noDataCells) throws JsonException, IOException {
        JsonObject structure = (JsonObject) Jsoner.deserialize(new StringReader(json));
        JsonArray classes = (JsonArray) structure.get("classes");
        String[] names = new String[classes.size()];
        int[] totalSize = new int[classes.size()];
        int[] nbPatches = new int[classes.size()];
        int[][] patchSizes = new int[classes.size()][];
        long[] npro = new long[classes.size()];
        boolean[] isSquare = new boolean[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            JsonObject c = (JsonObject) classes.get(i);
            names[i] = (String) c.get("name");
            JsonArray sizes = (JsonArray) c.get(LandscapeStructureSolver.KEY_AREA);
            patchSizes[i] = new int[sizes.size()];
            for (int j = 0; j < sizes.size(); j++) {
                patchSizes[i][j] = Integer.parseInt(sizes.get(j).toString());
            }
            nbPatches[i] = patchSizes[i].length;
            if (c.containsKey(LandscapeStructureSolver.KEY_NPRO)) {
                npro[i] = Long.parseLong(c.get(LandscapeStructureSolver.KEY_NPRO).toString());
            } else {
                long netProduct = 0;
                for (int p : patchSizes[i]) {
                    netProduct += Long.valueOf(p) * Long.valueOf(p);
                }
                npro[i] = netProduct;
            }
            if (c.containsKey(LandscapeStructureSolver.IS_SQUARE)) {
                isSquare[i] = Boolean.parseBoolean(c.get(LandscapeStructureSolver.IS_SQUARE).toString());
            } else {
                isSquare[i] = false;
            }
        }
        if (structure.containsKey("maskRasterPath")) {
            String maskRasterPath = structure.get("maskRasterPath").toString();
            return new LandscapeStructure(
                    nbRows, nbCols, maskRasterPath,
                    noDataCells,
                    names, totalSize, nbPatches, patchSizes, npro, isSquare
            );
        }
        nbRows = Integer.parseInt(structure.get("nbRows").toString());
        nbCols = Integer.parseInt(structure.get("nbCols").toString());
        return new LandscapeStructure(nbRows, nbCols, names, totalSize, nbPatches, patchSizes, npro, isSquare);
    }

    public static LandscapeStructure fromRasterData(int[] values, int nbRows, int nbCols, int noDataValue, int[] focalClasses, INeighborhood neighborhood, String rasterPath) throws IOException {
        return fromRasterData(values, nbRows, nbCols, noDataValue, focalClasses, neighborhood, true, rasterPath);
    }

    public static LandscapeStructure fromRasterData(int[] values, int nbRows, int nbCols, int noDataValue,
                                                    int[] focalClasses, INeighborhood neighborhood,
                                                    boolean discardNoData, String rasterPath) throws IOException {
        String[] names = IntStream.of(focalClasses).mapToObj(i -> "" + i).toArray(String[]::new);
        int[] nbPatches = new int[focalClasses.length];
        int[] totalSize = new int[focalClasses.length];
        int[][] patchSizes = new int[focalClasses.length][];
        long[] npro = new long[focalClasses.length];
        boolean[] isSquare = new boolean[focalClasses.length];
        for (int k = 0; k < focalClasses.length; k++) {
            int classId = focalClasses[k];
            RasterConnectivityFinder cf = new RasterConnectivityFinder(nbRows, nbCols, values, classId, neighborhood);
            totalSize[k] = cf.getNbNodes();
            cf.findAllCC();
            int nbCC = cf.getNBCC();
            nbPatches[k] = nbCC;
            patchSizes[k] = new int[nbCC];
            for (int i = 0; i < nbCC; i++) {
                patchSizes[k][i] = cf.getSizeCC()[i];
            }
            npro[k] = cf.getNpro();
            Arrays.sort(patchSizes[k]);
            isSquare[k] = false;
        }
        if (discardNoData) {
            int[] noDataCells = IntStream.range(0, values.length).filter(i -> values[i] == noDataValue).toArray();
            return new LandscapeStructure(nbRows, nbCols, rasterPath, noDataCells, names, totalSize, nbPatches, patchSizes, npro, isSquare);
        }
        return new LandscapeStructure(nbRows, nbCols, names, totalSize, nbPatches, patchSizes, npro, isSquare);
    }

    public int getLandscapeSize() {
        return grid.getNbCells();
    }

    public double getNonFocalLandscapeProportion() {
        return 100 * (1.0 * (getLandscapeSize() - Arrays.stream(totalSize).sum()) / (1.0 * getLandscapeSize()));
    }

    public int getNbPatches(int classId) {
        return nbPatches[classId];
    }

    public int[] getPatchSizes(int classId) {
        return patchSizes[classId];
    }

    public double getMeanPatchArea(int classId) {
        return (1.0 * totalSize[classId]) / (1.0 * nbPatches[classId]);
    }

    public int getTotalSize(int classId) {
        return totalSize[classId];
    }

    public double getLandscapeProportion(int classId) {
        return 100 * (1.0 * getTotalSize(classId)) / (1.0 * getLandscapeSize());
    }

    public double getPatchDensity(int classId) {
        return (1.0 * getNbPatches(classId)) / (1.0 * getLandscapeSize());
    }

    public int getSmallestPatchIndex(int classId) {
        if (getNbPatches(classId) > 0) {
            return getPatchSizes(classId)[0];
        }
        return -1;
    }

    public int getLargestPatchIndex(int classId) {
        if (getNbPatches(classId) > 0) {
            return getPatchSizes(classId)[getNbPatches(classId) - 1];
        }
        return -1;
    }

    public long getNetProduct(int classId) {
        return npro[classId];
    }

    public double getMesh(int classId) {
        return (1.0 * getNetProduct(classId)) / (1.0 * getLandscapeSize());
    }

    public double getSplittingIndex(int classId) {
        return (1.0 * getLandscapeSize() * getLandscapeSize()) / (1.0 * getNetProduct(classId));
    }

    public double getSplittingDensity(int classId) {
        return (1.0 * getLandscapeSize()) / (1.0 * getNetProduct(classId));
    }

    public double getDegreeOfCoherence(int classId) {
        return (1.0 * getNetProduct(classId)) / (1.0 * getLandscapeSize() * getLandscapeSize());
    }

    public double getDegreeOfDivision(int classId) {
        return 1 - getDegreeOfCoherence(classId);
    }

    public int getNbRows() {
        return nbRows;
    }

    public int getNbCols() {
        return nbCols;
    }

    public String getMaskRasterPath() {
        return maskRasterPath;
    }
}
