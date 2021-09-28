package solver;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class PolyominoGenerator {

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

    public RegularSquareGrid grid;
    public INeighborhood neighborhood;
    public INeighborhood bufferNeighborhood;
    public boolean[] boolGrid;
    public boolean[] bufferGrid;
    public int[][] neighbors;
    public double[] dem;
    public int nbAvailableCells;
    public List<Integer> avalaibleCells;

    public PolyominoGenerator(RegularSquareGrid grid, INeighborhood neighborhood, INeighborhood bufferNeighborhood) {
        this.grid = grid;
        double[][] fullDem = diamondSquare(1000, 1.4);
        dem = IntStream.range(0, this.grid.getNbCells())
                .mapToDouble(i -> {
                    int[] c = grid.getCoordinatesFromIndex(i);
                    return fullDem[c[0]][c[1]];
                }).toArray();
        this.neighborhood = neighborhood;
        this.bufferNeighborhood = bufferNeighborhood;
        this.neighbors = new int[grid.getNbCells()][];
        for (int i = 0; i < grid.getNbCells(); i++) {
            neighbors[i] = neighborhood.getNeighbors(grid, i);
        }
        this.boolGrid = new boolean[grid.getNbCells()];
        this.bufferGrid = new boolean[grid.getNbCells()];
        this.avalaibleCells = new ArrayList<>();
        for (int i = 0; i < grid.getNbCells(); i++) {
            boolGrid[i] = false;
            bufferGrid[i] = false;
            avalaibleCells.add(i);
        }
        this.nbAvailableCells = grid.getNbCells();
    }

    public boolean generatePolyomino(int size, boolean noHole) {
        assert avalaibleCells.size() > size;
        int[] cells = new int[size];
        ArrayList<Integer> neigh = new ArrayList<>();
        cells[0] = getRandomCell(avalaibleCells);
        int current = cells[0];
        int n = 1;
        boolGrid[current] = true;
        nbAvailableCells--;
//        NeighborhoodSelectionStrategy strategy = STRATEGIES_ALL[ThreadLocalRandom.current().nextInt(0, STRATEGIES_ALL.length)];
        NeighborhoodSelectionStrategy strategy = NeighborhoodSelectionStrategy.FROM_ALL;
        while (n < size) {
            int next = findNext(n, cells, noHole, strategy);
            if (next == -1) {
                return false;
            }
            n++;
        }
        int sizeBuff = 0;
        for (int i : cells) {
            int idxI = avalaibleCells.indexOf(i);
            avalaibleCells.remove(idxI);
            for (int j : bufferNeighborhood.getNeighbors(grid, i)) {
                if (!boolGrid[j]) {
                    sizeBuff++;
                    if (!bufferGrid[j]) {
                        bufferGrid[j] = true;
                        int idxJ = avalaibleCells.indexOf(j);
                        avalaibleCells.remove(idxJ);
                    }
                }
            }
        }
        return true;
    }

    public void filterHoles(List<Integer> neigh) {
        ISet toRemove = SetFactory.makeBipartiteSet(0);
        for (int i = 0; i < neigh.size(); i++) {
            boolGrid[neigh.get(i)] = true;
            nbAvailableCells--;
            if (!assertNoHole()) {
                toRemove.add(neigh.get(i));
            }
            boolGrid[neigh.get(i)] = false;
            nbAvailableCells++;
        }
        for (int i : toRemove) {
            neigh.remove(new Integer(i));
        }
    }

    public int findNext(int n, int[] cells, boolean noHole, NeighborhoodSelectionStrategy strategy) {
        switch (strategy) {
            case FROM_ALL:
                return findNextFromAll(n, cells, noHole);
            case FROM_LAST_POSSIBLE:
                return findNextFromLastPossibleCell(n, cells, noHole);
            case RANDOM:
                int strat = ThreadLocalRandom.current().nextInt(0, STRATEGIES.length);
                return findNext(n, cells, noHole, STRATEGIES[strat]);
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
            if (!boolGrid[i]) {
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
                if (!boolGrid[i] && !visited[i]) {
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

    public int findNextFromAll(int n, int[] cells, boolean noHole) {
        List<Integer> neigh = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j : neighbors[cells[i]]) {
                if (!boolGrid[j] && !bufferGrid[j]) {
                    neigh.add(j);
                }
            }
        }
        if (neigh.size() == 0) {
            return -1;
        }
        neigh.sort((t1, t2) -> {
            if (dem[t1] == dem[t2]) {
                return 0;
            } else if (dem[t1] > dem[t2]) {
                return -1;
            }
            return 1;
        });
        int next = -1;
        if (noHole) {
            boolean ok = false;
            while (!ok && neigh.size() > 0) {
                next = neigh.get(neigh.size() - 1);
                boolGrid[next] = true;
                nbAvailableCells--;
                if (assertNoHole()) {
                    cells[n] = next;
                    ok = true;
                } else {
                    boolGrid[next] = false;
                    nbAvailableCells++;
                    neigh.remove(neigh.size() - 1);
                }
            }
        } else {
//            next = getRandomCell(neigh);
            next = neigh.get(neigh.size() - 1);
            cells[n] = next;
            boolGrid[next] = true;
            nbAvailableCells--;
        }
        return next;
    };

    public int findNextFromLastPossibleCell(int n, int[] cells, boolean noHole) {
        List<Integer> neigh = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            for (int j : neighbors[cells[i]]) {
                if (!boolGrid[j] && !bufferGrid[j]) {
                    neigh.add(j);
                }
            }
            if (noHole) {
                filterHoles(neigh);
            }
            if (neigh.size() > 0) {
                neigh.sort((t1, t2) -> {
                    if (dem[t1] == dem[t2]) {
                        return 0;
                    } else if (dem[t1] > dem[t2]) {
                        return -1;
                    }
                    return 1;
                });
//                int next = getRandomCell(neigh);
                int next = neigh.get(neigh.size() - 1);
                cells[n] = next;
                boolGrid[next] = true;
                nbAvailableCells--;
                return next;
            }
        }
        return -1;
    }

    public double[][] diamondSquare(double randomness, double roughnessFactor) {
        // Get the smallest power of 2 greater than of equal to the largest landscape dimension
        int h = Math.max(grid.getNbRows(), grid.getNbCols());
        double pos = Math.ceil(Math.log(h) / Math.log(2));
        h = (int) (Math.pow(2, pos) + 1);
        System.out.println("Dimension = " + h + " x " + h);
        // Init matrix
        double[][] dem = new double[h][h];
        // Init edges
        dem[0][0] = randomDouble(-randomness, randomness);
        dem[0][h - 1] = randomDouble(-randomness, randomness);
        dem[h - 1][0] = randomDouble(-randomness, randomness);
        dem[h - 1][h - 1] = randomDouble(-randomness, randomness);
        double r = randomness / roughnessFactor;
        // Fill matrix
        int i = h - 1;
        while (i > 1) {
            int id = i / 2;
            for (int x = id; x < h; x += i) { // Diamond
                for (int y = id; y < h; y += i) {
                    double mean = (dem[x - id][y - id] + dem[x - id][y + id] + dem[x + id][y + id] + dem[x + id][y - id]) / 4;
                    dem[x][y] = mean + randomDouble(-r, r);
                }
            }
            int offset = 0;
            for (int x = 0; x < h; x += id) { // Square
                if (offset == 0) {
                    offset = id;
                } else {
                    offset = 0;
                }
                for (int y = offset; y < h; y += i) {
                    double sum = 0;
                    int n = 0;
                    if (x >= id) {
                        sum += dem[x - id][y];
                        n++;
                    }
                    if (x + id < h) {
                        sum += dem[x + id][y];
                        n++;
                    }
                    if (y >= id) {
                        sum += dem[x][y - id];
                        n++;
                    }
                    if (y + id < h) {
                        sum += dem[x][y + id];
                        n++;
                    }
                    dem[x][y] = sum / n + randomDouble(-r, r);
                }
            }
            i = id;
            r /= roughnessFactor;
        }
        return dem;
    }

    public double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public int getRandomCell(List<Integer> cells) {
        return cells.get(ThreadLocalRandom.current().nextInt(0, cells.size()));
    }

    public void exportDem(int x, int y, double resolution, String epsg, String dest) throws IOException, FactoryException {
        GridCoverageFactory gcf = new GridCoverageFactory();
        CoordinateReferenceSystem crs = CRS.decode(epsg);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                x, x + (grid.getNbCols() * resolution),
                y, y + (grid.getNbRows() * resolution),
                crs
        );
        WritableRaster rast = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_DOUBLE,
                grid.getNbCols(), grid.getNbRows(),
                1, null
        );
        rast.setPixels(0, 0, grid.getNbCols(), grid.getNbRows(), dem);
        GridCoverage2D gc = gcf.create("generated_landscape", rast, referencedEnvelope);
        GeoTiffWriter writer = new GeoTiffWriter(new File(dest));
        writer.write(gc,null);
    }

    public void exportRaster(int x, int y, double resolution, String epsg, String dest) throws IOException, FactoryException {
        GridCoverageFactory gcf = new GridCoverageFactory();
        CoordinateReferenceSystem crs = CRS.decode(epsg);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                x, x + (grid.getNbCols() * resolution),
                y, y + (grid.getNbRows() * resolution),
                crs
        );
        int[] data = IntStream.range(0, this.grid.getNbCells()).map(i -> boolGrid[i] ? 1 : 0).toArray();
        WritableRaster rast = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_INT,
                grid.getNbCols(), grid.getNbRows(),
                1, null
        );
        rast.setPixels(0, 0, grid.getNbCols(), grid.getNbRows(), data);
        GridCoverage2D gc = gcf.create("generated_landscape", rast, referencedEnvelope);
        GeoTiffWriter writer = new GeoTiffWriter(new File(dest));
        writer.write(gc,null);
    }

    public static void main(String[] args) throws IOException, FactoryException {
        int nRow = 200;
        int nCol = 200;
        RegularSquareGrid grid = new RegularSquareGrid(nRow, nCol);
        INeighborhood neighborhood = Neighborhoods.FOUR_CONNECTED;
        INeighborhood bufferNeighborhood = Neighborhoods.TWO_WIDE_FOUR_CONNECTED;
        EquivalentPatchSizesDistributions patchSizes = new EquivalentPatchSizesDistributions(
                grid,
                20,
                30,
                250,
                250,
                10,
                1000
        );
        Solver solver = patchSizes.model.getSolver();
        solver.showStatistics();
        solver.setSearch(Search.domOverWDegRefSearch(patchSizes.patchSizes));
        for (int l = 0; l < 10; l++) {
            if (solver.solve()) {
                System.out.println("---------------------  Landscape " + (l + 1) + "  ----------------------------------------------");
                int nbPatches = patchSizes.nbPatches.getValue();
                int[] sizesNoFilter = Arrays.stream(patchSizes.patchSizes)
                        .mapToInt(v -> v.getValue())
                        .toArray();

                int[] sizes = Arrays.stream(patchSizes.patchSizes)
                        .mapToInt(v -> v.getValue())
                        .filter(i -> i > 0)
                        .toArray();
                System.out.println("Number of patches = " + nbPatches);
                System.out.println("Patch sizes = " + Arrays.toString(sizesNoFilter));
                PolyominoGenerator polyominoGenerator = null;
                boolean b = false;
                int maxTry = 100;
                int n = 0;
                while (!b && n < maxTry) {
                    n++;
                    b = true;
                    polyominoGenerator = new PolyominoGenerator(grid, neighborhood, bufferNeighborhood);
                    for (int s : sizes) {
                        System.out.println("Generating patch of size " + s);
                        b &= polyominoGenerator.generatePolyomino(s, false);
                        if (!b) {
                            break;
                        }
                    }
                }
                if (!b) {
                    System.out.println("FAIL");
                } else {
                    System.out.println("Feasible landscape found after " + n + " tries");
                    for (int r = 0; r < nRow; r++) {
                        System.out.printf("  |");
                        for (int c = 0; c < nCol; c++) {
                            if (polyominoGenerator.boolGrid[grid.getIndexFromCoordinates(r, c)]) {
                                System.out.printf(" # ");
                            } else if (polyominoGenerator.bufferGrid[grid.getIndexFromCoordinates(r, c)]) {
                                System.out.printf(" · ");
                            } else {
                                System.out.printf("   ");
                            }
                        }
                        System.out.printf("|\n");
                    }
                    polyominoGenerator.exportRaster(
                            0, 0, 0.0001, "EPSG:4326",
                            "/home/djusteau/Documents/testPolyomino/testb_" + l  +".tif"
                    );
                    polyominoGenerator.exportDem(
                            0, 0, 0.0001, "EPSG:4326",
                            "/home/djusteau/Documents/testPolyomino/testb_" + l  +"_DEM.tif"
                    );
                }
            } else {
                break;
            }
            System.out.println("-----------------------------------------------------------------------------------");
        }
    }
}
