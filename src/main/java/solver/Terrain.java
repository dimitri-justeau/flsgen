package solver;

import grid.regular.square.RegularSquareGrid;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
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
import java.util.stream.IntStream;

public class Terrain {

    public RegularSquareGrid grid;
    public double[] dem;

    public Terrain(RegularSquareGrid grid) {
        this.grid = grid;
    }

    public void loadFromRaster(String rasterPath) throws IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nRow = gridCov.getRenderedImage().getHeight();
        int nCol = gridCov.getRenderedImage().getWidth();
        if (nRow != grid.getNbRows() || nCol != grid.getNbCols()) {
            throw new ValueException("Input terrain raster must have the same dimensions as the landscape to generate");
        }
        DataBuffer buff = gridCov.getRenderedImage().getData().getDataBuffer();
        dem = IntStream.range(0, grid.getNbCells())
                .mapToDouble(i -> buff.getElemDouble(i))
                .toArray();
    }

    public void generateDiamondSquare(double roughnessFactor) {
        // Get the smallest power of 2 greater than of equal to the largest landscape dimension
        int h = Math.max(grid.getNbRows(), grid.getNbCols());
        double pos = Math.ceil(Math.log(h) / Math.log(2));
        h = (int) (Math.pow(2, pos) + 1);
        double[][] terrain = new double[h][h];
        // Init edges
        terrain[0][0] = randomDouble(-h, h);
        terrain[0][h - 1] = randomDouble(-h, h);
        terrain[h - 1][0] = randomDouble(-h, h);
        terrain[h - 1][h - 1] = randomDouble(-h, h);
        double r = h * Math.pow(2, - 2 * roughnessFactor);
        // Fill matrix
        int i = h - 1;
        while (i > 1) {
            int id = i / 2;
            for (int x = id; x < h; x += i) { // Diamond
                for (int y = id; y < h; y += i) {
                    double mean = (terrain[x - id][y - id] + terrain[x - id][y + id] + terrain[x + id][y + id] + terrain[x + id][y - id]) / 4;
                    terrain[x][y] = mean + randomDouble(-r, r);
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
                        sum += terrain[x - id][y];
                        n++;
                    }
                    if (x + id < h) {
                        sum += terrain[x + id][y];
                        n++;
                    }
                    if (y >= id) {
                        sum += terrain[x][y - id];
                        n++;
                    }
                    if (y + id < h) {
                        sum += terrain[x][y + id];
                        n++;
                    }
                    terrain[x][y] = sum / n + randomDouble(-r, r);
                }
            }
            i = id;
            r *= Math.pow(2, - 2 * roughnessFactor);
        }
        dem = IntStream.range(0, grid.getNbCells())
                .mapToDouble(v -> {
                    int[] c = grid.getCoordinatesFromIndex(v);
                    return terrain[c[0]][c[1]];
                }).toArray();
    }

    public static double randomDouble(double min, double max) {
        return new SecureRandom().nextDouble() * (max - min) + min;
    }

    public void exportRaster(int x, int y, double resolution, String epsg, String dest) throws IOException, FactoryException {
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
        System.out.println("Fractal terrain raster exported at " + dest);
    }
}
