package solver;

import grid.neighborhood.INeighborhood;
import grid.regular.square.RegularSquareGrid;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.RasterFactory;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

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

    public LandscapeStructure structure;
    public Terrain terrain;
    public RegularSquareGrid grid;
    public int nbClasses;
    public INeighborhood neighborhood;
    public INeighborhood bufferNeighborhood;
    public int[] rasterGrid;
    public boolean[][] bufferGrid;
    public int[][] neighbors;
    public int nbAvailableCells;
    public List<Integer> avalaibleCells[];

    public LandscapeGenerator(LandscapeStructure structure, INeighborhood neighborhood, INeighborhood bufferNeighborhood, Terrain terrain) {
        this.structure = structure;
        this.grid = new RegularSquareGrid(structure.nbRows, structure.nbCols);
        this.nbClasses = structure.names.length;
        this.terrain = terrain;
        this.neighborhood = neighborhood;
        this.bufferNeighborhood = bufferNeighborhood;
        this.neighbors = new int[grid.getNbCells()][];
        for (int i = 0; i < grid.getNbCells(); i++) {
            neighbors[i] = neighborhood.getNeighbors(grid, i);
        }
        this.rasterGrid = new int[grid.getNbCells()];
        this.bufferGrid = new boolean[nbClasses][];
        this.avalaibleCells = new ArrayList[nbClasses];
        for (int i = 0; i < nbClasses; i++) {
            avalaibleCells[i] = new ArrayList<>();
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

    public boolean generatePatch(int classId, int size, double terrainDependency, boolean noHole) {
        assert avalaibleCells[classId].size() >= size;
        int[] cells = new int[size];
        cells[0] = getRandomCell(avalaibleCells[classId]);
        int current = cells[0];
        int n = 1;
        rasterGrid[current] = classId;
        nbAvailableCells--;
        boolean success = true;
//        NeighborhoodSelectionStrategy strategy = STRATEGIES_ALL[ThreadLocalRandom.current().nextInt(0, STRATEGIES_ALL.length)];
        NeighborhoodSelectionStrategy strategy = NeighborhoodSelectionStrategy.FROM_ALL;
        while (n < size) {
            int next = findNext(classId, n, cells, terrainDependency, noHole, strategy);
            if (next == -1) {
                success = false;
                break;
            }
            n++;
        }
        if (success) { // Patch generation was successful, construct buffer.
            for (int i : cells) {
                int idxI = avalaibleCells[classId].indexOf(i);
                avalaibleCells[classId].remove(idxI);
                for (int j : bufferNeighborhood.getNeighbors(grid, i)) {
                    if (rasterGrid[j] == NODATA && !bufferGrid[classId][j]) {
                        bufferGrid[classId][j] = true;
                        int idxJ = avalaibleCells[classId].indexOf(j);
                        avalaibleCells[classId].remove(idxJ);
                    }
                }
            }
        } else { // Patch generation failed, backtrack grid to the previous state.
            for (int i = 0; i < n; i++) {
                rasterGrid[cells[i]] = NODATA;
                nbAvailableCells++;
            }
        }
        return success;
    }

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

    public int findNext(int classId, int n, int[] cells, double terrainDependency, boolean noHole, NeighborhoodSelectionStrategy strategy) {
        switch (strategy) {
            case FROM_ALL:
                return findNextFromAll(classId, n, cells, terrainDependency, noHole);
            case FROM_LAST_POSSIBLE:
                return findNextFromLastPossibleCell(classId, n, cells, noHole);
            case RANDOM:
                int strat = ThreadLocalRandom.current().nextInt(0, STRATEGIES.length);
                return findNext(classId, n, cells, terrainDependency, noHole, STRATEGIES[strat]);
            default:
                throw new UnsupportedOperationException();
        }
    }

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
        int nbOut = nbAvailableCells;;
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
        if (nbVisited == nbOut) {
            return true;
        }
        return false;
    }

    public int findNextFromAll(int classId, int n, int[] cells, double terrainDependency, boolean noHole) {
        List<Integer> neigh = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j : neighbors[cells[i]]) {
                if (rasterGrid[j] == NODATA && !bufferGrid[classId][j]) {
                    neigh.add(j);
                }
            }
        }
        if (neigh.size() == 0) {
            return -1;
        }
        neigh.sort((t1, t2) -> {
            if (terrain.dem[t1] == terrain.dem[t2]) {
                return 0;
            } else if (terrain.dem[t1] < terrain.dem[t2]) {
                return -1;
            }
            return 1;
        });
        int next = -1;
        if (noHole) {
            boolean ok = false;
            while (!ok && neigh.size() > 0) {
                int minIdx = 0;
                int maxIdx = (int) Math.round(neigh.size() * (1 - terrainDependency));
                maxIdx = maxIdx > 0 ? maxIdx : 1;
                int idx = randomInt(minIdx, maxIdx);
                //
                next = neigh.get(idx);
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
            // 90% chances of choosing the lowest elevation neighbor - 10% of choosing any neighbor
            int minIdx = 0;
            int maxIdx = (int) Math.round(neigh.size() * (1 - terrainDependency));
            maxIdx = maxIdx > 0 ? maxIdx : 1;
            int idx = minIdx == maxIdx ? minIdx : randomInt(minIdx, maxIdx);
            //
            next = neigh.get(idx);
            cells[n] = next;
            rasterGrid[next] = classId;
            nbAvailableCells--;
        }
        return next;
    };

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
//                int next = getRandomCell(neigh);
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

    public int getRandomCell(List<Integer> cells) {
        return cells.get(new SecureRandom().nextInt(cells.size()));
    }

    public void exportRaster(double x, double y, double resolution, String epsg, String dest) throws IOException, FactoryException {
        GridCoverageFactory gcf = new GridCoverageFactory();
        CoordinateReferenceSystem crs = CRS.decode(epsg);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                x, x + (grid.getNbCols() * resolution),
                y, y + (grid.getNbRows() * resolution),
                crs
        );
        int[] data = IntStream.range(0, this.grid.getNbCells()).map(i -> rasterGrid[i]).toArray();
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
    }

    public void generate(String dest, double terrainDependency, double x, double y, double resolution, String srs) throws IOException, FactoryException {
        LandscapeGenerator landscapeGenerator = null;
        boolean b = false;
        int maxTry = 100;
        int maxTryPatch = 100;
        int n = 0;

        while (!b && n < maxTry) {
            n++;
            b = true;
            landscapeGenerator = new LandscapeGenerator(structure, neighborhood, bufferNeighborhood, terrain);
            for (int i = 0; i < structure.names.length; i++) {
                System.out.println("---------------------  Generating patches for class " + structure.names[i] + "  ----------------------------------------------");
                int nbPatches = structure.nbPatches[i];
                int[] sizes = structure.patchSizes[i];
                System.out.println("Number of patches = " + nbPatches);
                System.out.println("Patch sizes = " + Arrays.toString(sizes));
                for (int k : sizes) {
                    System.out.println("Generating patch of size " + k);
                    boolean patchGenerated = false;
                    for (int p = 0; p < maxTryPatch; p++) {
                        patchGenerated = landscapeGenerator.generatePatch(i, k, terrainDependency, false);
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
                    break;
                }
            }
            if (!b) {
                System.out.println("FAIL");
            } else {
                System.out.println("Feasible landscape found after " + n + " tries");
                landscapeGenerator.exportRaster(
                        x, y, resolution, srs,
                        dest
                );
            }
        }
    }
}
