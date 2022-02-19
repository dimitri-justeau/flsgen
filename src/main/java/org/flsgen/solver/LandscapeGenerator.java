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

import io.github.geniot.indexedtreemap.IndexedTreeSet;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.PartialRegularSquareGrid;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.utils.CheckLandscape;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.RasterFactory;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Stochastic landscape generator from predefined landscape structures. Rely on a terrain to guide the generation.
 * The terrain can be either specified as input of generated with the diamond-square algorithm.
 */
public class LandscapeGenerator {

    public static final int NODATA = -1;

    public enum NeighborhoodSelectionStrategy {
        FROM_ALL,
        FROM_LAST_POSSIBLE,
        RANDOM
    }

    NeighborhoodSelectionStrategy[] STRATEGIES = new NeighborhoodSelectionStrategy[] {
            NeighborhoodSelectionStrategy.FROM_ALL,
            NeighborhoodSelectionStrategy.FROM_LAST_POSSIBLE
    };

    NeighborhoodSelectionStrategy[] STRATEGIES_ALL = new NeighborhoodSelectionStrategy[] {
            NeighborhoodSelectionStrategy.FROM_ALL,
            NeighborhoodSelectionStrategy.FROM_LAST_POSSIBLE,
            NeighborhoodSelectionStrategy.RANDOM
    };

    protected LandscapeStructure structure;
    protected Terrain terrain;
    protected RegularSquareGrid grid;
    protected int nbClasses;
    protected INeighborhood neighborhood;
    protected INeighborhood bufferNeighborhood;
    protected int[] rasterGrid;
    protected boolean[][] bufferGrid;
    protected int[][] neighbors;
    protected int nbAvailableCells;
    protected ISet avalaibleCells[];
    protected int nbTry;

    public LandscapeGenerator(LandscapeStructure structure, int neighborhood, int bufferWidth, Terrain terrain) throws FlsgenException {
        this(
                structure,
                Neighborhoods.resolveNeighborhood(structure.grid, neighborhood, 1),
                Neighborhoods.resolveNeighborhood(structure.grid, neighborhood, bufferWidth),
                terrain
        );
    }

    public LandscapeGenerator(LandscapeStructure structure, int neighborhood, int minBufferWidth, int maxBufferWidth, Terrain terrain) throws FlsgenException {
        this(
                structure,
                Neighborhoods.resolveNeighborhood(structure.grid, neighborhood, 1),
                Neighborhoods.resolveNeighborhood(structure.grid, neighborhood, minBufferWidth, maxBufferWidth),
                terrain
        );
    }

    public LandscapeGenerator(LandscapeStructure structure, INeighborhood neighborhood, INeighborhood bufferNeighborhood, Terrain terrain) {
        this.structure = structure;
        this.grid = structure.grid;
        this.nbClasses = structure.names.length;
        this.terrain = terrain;
        this.neighborhood = neighborhood;
        this.bufferNeighborhood = bufferNeighborhood;
        this.neighbors = new int[grid.getNbCells()][];
        for (int i = 0; i < grid.getNbCells(); i++) {
            neighbors[i] = neighborhood.getNeighbors(grid, i);
        }
        init();
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public int getNbTry() {
        return nbTry;
    }

    public RegularSquareGrid getGrid() {
        return grid;
    }

    public int[] getRasterGrid() {
        return rasterGrid;
    }

    /**
     * Initialize data structures
     */
    public void init() {
        this.rasterGrid = new int[grid.getNbCells()];
        this.bufferGrid = new boolean[nbClasses][];
        this.avalaibleCells = new ISet[nbClasses];
        for (int i = 0; i < nbClasses; i++) {
            avalaibleCells[i] = SetFactory.makeBipartiteSet(0);
            bufferGrid[i] = new boolean[grid.getNbCells()];
        }
        for (int i = 0; i < grid.getNbCells(); i++) {
            rasterGrid[i] = NODATA;
            for (int j = 0; j < nbClasses; j++) {
                avalaibleCells[j].add(i);
                bufferGrid[j][i] = false;
            }
        }
        this.nbAvailableCells = grid.getNbCells();
    }

    /**
     * Generates a patch in the landscape
     * @param classId the class of the patch to generate
     * @param size the size of the patch to generate
     * @param terrainDependency the terrain dependency, between 0 (no terrain dependency) and 1 (only guided by terrain)
     * @param noHole if true ensure that the patch contains no hole
     * @return true if patch generation was successful, false otherwise
     */
    public boolean generatePatch(int classId, int size, double terrainDependency, boolean noHole) {
        if (avalaibleCells[classId].size() < size) {
            return false;
        }
        int[] cells = new int[size];
        cells[0] = getRandomCell(avalaibleCells[classId]);
        int current = cells[0];
        int n = 1;
        rasterGrid[current] = classId;
        nbAvailableCells--;
        boolean success = true;
        NeighborhoodSelectionStrategy strategy = NeighborhoodSelectionStrategy.FROM_ALL;
        IndexedTreeSet<Integer> neigh;
        if (grid instanceof PartialRegularSquareGrid) {
            neigh = new IndexedTreeSet<>((t1, t2) -> {
                int tt1 = ((PartialRegularSquareGrid) grid).getCompleteIndex(t1);
                int tt2 = ((PartialRegularSquareGrid) grid).getCompleteIndex(t2);
                if (terrain.dem[tt1] == terrain.dem[tt2]) {
                    return 0;
                } else if (terrain.dem[tt1] < terrain.dem[tt2]) {
                    return -1;
                }
                return 1;
            });
        } else {
            neigh = new IndexedTreeSet<>((t1, t2) -> {
                if (terrain.dem[t1] == terrain.dem[t2]) {
                    return 0;
                } else if (terrain.dem[t1] < terrain.dem[t2]) {
                    return -1;
                }
                return 1;
            });
        }
        while (n < size) {
            int next = findNext(classId, n, cells, terrainDependency, noHole, strategy, neigh);
            if (next == -1) {
                success = false;
                break;
            }
            n++;
        }
        if (success) { // Patch generation was successful, construct buffer.
            for (int i : cells) {
                for (int k = 0; k < avalaibleCells.length; k++) {
                    avalaibleCells[k].remove(i);
                }
                boolean border = false;
                for (int j : neighbors[i]) {
                    if (rasterGrid[j] == NODATA) {
                        border = true;
                        break;
                    }
                }
                if (border) {
                    for (int j : bufferNeighborhood.getNeighbors(grid, i)) {
                        if (rasterGrid[j] == NODATA && !bufferGrid[classId][j]) {
                            bufferGrid[classId][j] = true;
                            avalaibleCells[classId].remove(j);
                        }
                    }
                }
            }
        } else { // Patch generation failed, backtrack org.flsgen.grid to the previous state.
            for (int i = 0; i < n; i++) {
                rasterGrid[cells[i]] = NODATA;
                nbAvailableCells++;
            }
        }
        return success;
    }

    /**
     * Filters potential cells that would create a hole
     * @param classId class of the concerned patch
     * @param neigh available cells
     */
    public void filterHoles(int classId, List<Integer> neigh) {
        ISet toRemove = SetFactory.makeBipartiteSet(0);
        for (int i = 0; i < neigh.size(); i++) {
            rasterGrid[neigh.get(i)] = classId;
            nbAvailableCells--;
            if (!assertNoHole()) {
                toRemove.add(neigh.get(i));
            }
            rasterGrid[neigh.get(i)] = NODATA;
            nbAvailableCells++;
        }
        for (int i : toRemove) {
            neigh.remove(new Integer(i));
        }
    }

    /**
     * Find the next cell to add in patch
     * @param classId patch class
     * @param n current patch size
     * @param cells patch cells
     * @param terrainDependency the terrain dependency, between 0 (no terrain dependency) and 1 (only guided by terrain)
     * @param noHole if true ensure that the patch contains no hole
     * @param strategy neighborhood selection strategy
     * @return the next cell to add in patch
     */
    public int findNext(int classId, int n, int[] cells, double terrainDependency, boolean noHole, NeighborhoodSelectionStrategy strategy, IndexedTreeSet<Integer> neigh) {
        switch (strategy) {
            case FROM_ALL:
                return findNextFromAll(classId, n, cells, terrainDependency, noHole, neigh);
            case FROM_LAST_POSSIBLE:
                return findNextFromLastPossibleCell(classId, n, cells, noHole);
            case RANDOM:
                int strat = ThreadLocalRandom.current().nextInt(0, STRATEGIES.length);
                return findNext(classId, n, cells, terrainDependency, noHole, STRATEGIES[strat], neigh);
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * @return true if there is no hole in patches.
     */
    public boolean assertNoHole() {
        if (nbAvailableCells == 0) {
            return true;
        }
        int nbCells = grid.getNbCells();
        boolean[] visited = new boolean[nbCells];
        int[] queue = new int[nbCells];
        int front = 0;
        int rear = 0;
        int nbVisited = 0;
        int current = -1;
        for (int i = 0; i < nbCells; i++) {
            if (rasterGrid[i] == NODATA) {
                current = i;
                break;
            }
        }
        int nbOut = nbAvailableCells;
        visited[current] = true;
        queue[front] = current;
        rear++;
        nbVisited++;
        while (front != rear) {
            current = queue[front++];
            for (int i : neighbors[current]) {
                if (rasterGrid[i] == NODATA && !visited[i]) {
                    queue[rear++] = i;
                    visited[i] = true;
                    nbVisited++;
                }
            }
        }
        return nbVisited == nbOut;
    }

    /**
     * Find the next cell to add in the patch from all neighboring cells
     * @param classId patch class
     * @param n current patch size
     * @param cells patch cells
     * @param terrainDependency the terrain dependency, between 0 (no terrain dependency) and 1 (only guided by terrain)
     * @param noHole if true ensure that the patch contains no hole
     * @return the next cell to include in patch
     */
    public int findNextFromAll(int classId, int n, int[] cells, double terrainDependency, boolean noHole, IndexedTreeSet<Integer> neigh) {
        for (int j : neighbors[cells[n - 1]]) {
            if (rasterGrid[j] == NODATA && !bufferGrid[classId][j]) {
                neigh.add(j);
            }
        }
        if (neigh.size() == 0) {
            return -1;
        }
        int next = -1;
        if (noHole) {
            boolean ok = false;
            while (!ok && neigh.size() > 0) {
                int minIdx = 0;
                int maxIdx = (int) Math.round(neigh.size() * (1 - terrainDependency));
                maxIdx = maxIdx > 0 ? maxIdx : 1;
                int idx = randomInt(minIdx, maxIdx);
                next = neigh.exact(idx);
                rasterGrid[next] = classId;
                nbAvailableCells--;
                if (assertNoHole()) {
                    cells[n] = next;
                    ok = true;
                } else {
                    rasterGrid[next] = NODATA;
                    nbAvailableCells++;
                    neigh.remove(idx);
                    next = -1;
                }
            }
        } else {
            // terrain dependency
            int minIdx = 0;
            int maxIdx = (int) Math.round(neigh.size() * (1 - terrainDependency));
            maxIdx = maxIdx > 0 ? maxIdx : 1;
            int idx = minIdx == maxIdx ? minIdx : randomInt(minIdx, maxIdx);
            next = neigh.exact(idx);
            cells[n] = next;
            rasterGrid[next] = classId;
            nbAvailableCells--;
        }
        neigh.remove(next);
        return next;
    };

    /**
     * Find the next cell to add in patch from the neighbors of the previously added cell
     * @param classId patch class
     * @param n current patch size
     * @param cells patch cells
     * @param noHole if true ensure that the patch contains no hole
     * @return the next cell to include in patch
     */
    public int findNextFromLastPossibleCell(int classId, int n, int[] cells, boolean noHole) {
        List<Integer> neigh = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            for (int j : neighbors[cells[i]]) {
                if (rasterGrid[j] == NODATA && !bufferGrid[classId][j]) {
                    neigh.add(j);
                }
            }
            if (noHole) {
                filterHoles(classId, neigh);
            }
            if (neigh.size() > 0) {
                neigh.sort((t1, t2) -> {
                    if (terrain.dem[t1] == terrain.dem[t2]) {
                        return 0;
                    } else if (terrain.dem[t1] > terrain.dem[t2]) {
                        return -1;
                    }
                    return 1;
                });
                int next = neigh.get(neigh.size() - 1);
                cells[n] = next;
                rasterGrid[next] = classId;
                nbAvailableCells--;
                return next;
            }
        }
        return -1;
    }

    public int randomInt(int min, int max) {
        return new SecureRandom().nextInt(max - min) + min;
    }

    public int getRandomCell(ISet cells) {
        return cells.toArray()[(new SecureRandom().nextInt(cells.size()))];
    }

    /**
     * Export the generated landscape to a raster file
     * @param x X position (geographical coordinates) of the top-left output raster pixel
     * @param y Y position (geographical coordinates) of the top-left output raster pixel
     * @param resolution Spatial resolution (geographical units) of the output raster (i.e. pixel dimension)
     * @param epsg EPSG identifier of the output projection
     * @param dest path of output raster
     * @throws IOException
     * @throws FactoryException
     */
    public void exportRaster(double x, double y, double resolution, String epsg, String dest) throws IOException, FactoryException {
        GridCoverageFactory gcf = new GridCoverageFactory();
        CoordinateReferenceSystem crs = CRS.decode(epsg);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                x, x + (grid.getNbCols() * resolution),
                y, y + (grid.getNbRows() * resolution),
                crs
        );
        int[] data;
        if (grid instanceof PartialRegularSquareGrid) {
            int noDataValue = (int) CheckLandscape.getNodataValue(structure.maskRasterPath);
            data = new int[grid.getNbCols() * grid.getNbRows()];
            for (int i = 0; i < data.length; i++) {
                if (!((PartialRegularSquareGrid) grid).getDiscardSet().contains(i)) {
                    data[i] = rasterGrid[((PartialRegularSquareGrid) grid).getPartialIndex(i)];
                } else {
                    data[i] = noDataValue;
                }
            }
        } else {
            data = IntStream.range(0, this.grid.getNbCells()).map(i -> rasterGrid[i]).toArray();
        }
        WritableRaster rast = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_INT,
                grid.getNbCols(), grid.getNbRows(),
                1, null
        );
        rast.setPixels(0, 0, grid.getNbCols(), grid.getNbRows(), data);
        GridCoverage2D gc = gcf.create("generated_landscape", rast, referencedEnvelope);
        GeoTiffWriter writer = new GeoTiffWriter(new File(dest));
        writer.write(gc,null);
        System.out.println("Landscape raster exported at " + dest);
        gc.dispose(true);
        writer.dispose();
    }

    /**
     * Landscape generation main algorithm
     * @param terrainDependency the terrain dependency, between 0 (no terrain dependency) and 1 (only guided by terrain)
     * @param maxTry Maximum number of trials for landscape generation
     * @param maxTryPatch Maximum number of trials for patch generation
     * @return true if landscape generation was successful, otherwise false
     */
    public boolean generate(double terrainDependency, int maxTry, int maxTryPatch) {
        return generate(terrainDependency, maxTry, maxTryPatch, true);
    }

    /**
     * Landscape generation main algorithm
     * @param terrainDependency the terrain dependency, between 0 (no terrain dependency) and 1 (only guided by terrain)
     * @param maxTry Maximum number of trials for landscape generation
     * @param maxTryPatch Maximum number of trials for patch generation
     * @param verbose If true print progress
     * @return true if landscape generation was successful, otherwise false
     */
    public boolean generate(double terrainDependency, int maxTry, int maxTryPatch, boolean verbose) {
        nbTry = 0;
        boolean b = false;
        while (!b && nbTry < maxTry) {
            nbTry++;
            b = true;
            for (int i = 0; i < structure.names.length; i++) {
                if (verbose) {
                    System.out.println("---------------------  Generating patches for class " + structure.names[i] + "  ---------------------");
                }
                int nbPatches = structure.nbPatches[i];
                int[] sizes = structure.patchSizes[i];
                if (verbose) {
                    System.out.println("Number of patches = " + nbPatches);
                    System.out.println("Patch sizes = " + Arrays.toString(sizes));
                }
                for (int j = sizes.length - 1; j >= 0; j--) {
                    int k = sizes[j];
                    if (verbose) {
                        System.out.println("Generating patch of size " + k);
                    }
                    boolean patchGenerated = false;
                    for (int p = 0; p < maxTryPatch; p++) {
                        patchGenerated = generatePatch(i, k, terrainDependency, false);
                        if (patchGenerated) {
                            break;
                        }
                    }
                    b &= patchGenerated;
                    if (!b) {
                        break;
                    }
                }
                if (!b) {
                    init();
                    break;
                }
            }
        }
        return b;
    }

    /**
     * Landscape generation main algorithm
     * @param terrainDependency the terrain dependency, between 0 (no terrain dependency) and 1 (only guided by terrain)
     * @param maxTry Maximum number of trials for landscape generation
     * @param maxTryPatch Maximum number of trials for patch generation
     * @return true if landscape generation was successful, otherwise false
     */
    public boolean generateAlt(double terrainDependency, int maxTry, int maxTryPatch) {
        nbTry = 0;
        boolean b = false;
        while (!b && nbTry < maxTry) {
            nbTry++;
            b = true;

            Map<Integer, List<Integer>> patches = new HashMap();
            for (int i = 0; i < structure.names.length; i++) {
                List<Integer> cls = new ArrayList();
                for (int j : structure.patchSizes[i]) {
                    cls.add(j);
                }
                Collections.shuffle(cls);
                patches.put(i, cls);
            }
            int c = 0;
            while (!patches.isEmpty()) {
                Random rand = new Random(System.currentTimeMillis());
                int i = patches.keySet().stream().mapToInt(v -> v).toArray()[c];
                boolean patchGenerated = false;
                int k = patches.get(i).remove(patches.get(i).size() - 1);
                System.out.println("Generate patch of class " + i + " of size " + k);
                for (int p = 0; p < maxTryPatch; p++) {
                    patchGenerated = generatePatch(i, k, terrainDependency, false);
                    if (patchGenerated) {
                        break;
                    }
                }
                b &= patchGenerated;
                if (!b) {
                    init();
                    break;
                }
                if (patches.get(i).size() == 0) {
                    patches.remove(i);
                }
                if (c >= patches.size() - 1) {
                    c = 0;
                } else {
                    c++;
                }
            }
        }
        return b;
    }
}
