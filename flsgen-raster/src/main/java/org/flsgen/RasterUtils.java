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

package org.flsgen;

import org.chocosolver.solver.constraints.nary.nvalue.amnv.rules.R;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.utils.CheckLandscape;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.util.CoverageUtilities;
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
import java.util.stream.IntStream;

public class RasterUtils {

    public static double[] loadDoubleDataFromRaster(String rasterPath, RegularSquareGrid grid) throws FlsgenException, IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nRow = gridCov.getRenderedImage().getHeight();
        int nCol = gridCov.getRenderedImage().getWidth();
        if (nRow != grid.getNbRows() || nCol != grid.getNbCols()) {
            throw new FlsgenException("Input terrain raster must have the same dimensions as the landscape to generate");
        }
        DataBuffer buff = gridCov.getRenderedImage().getData().getDataBuffer();
        double[] data = IntStream.range(0, grid.getNbCells())
                .mapToDouble(i -> buff.getElemDouble(i))
                .toArray();
        gridCov.dispose(true);
        reader.dispose();
        return data;
    }

    public static int[] loadIntDataFromRaster(String rasterPath) throws IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nbRows = gridCov.getRenderedImage().getHeight();
        int nbCols = gridCov.getRenderedImage().getWidth();
        int[] values = new int[nbRows * nbCols];
        gridCov.getRenderedImage().getData().getSamples(0, 0, nbCols, nbRows, 0, values);
        gridCov.dispose(true);
        reader.dispose();
        return values;
    }

    public static void exportDoubleRaster(double[] data, RegularSquareGrid grid, double x, double y,
                                          double resolution_x, double resolution_y, String epsg,
                                          String dest) throws IOException, FactoryException {
        GridCoverageFactory gcf = new GridCoverageFactory();
        CoordinateReferenceSystem crs = CRS.decode(epsg);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                x, x + (grid.getNbCols() * resolution_x),
                y - + (grid.getNbRows() * resolution_y), y,
                crs
        );
        WritableRaster rast = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_DOUBLE,
                grid.getNbCols(), grid.getNbRows(),
                1, null
        );
        rast.setPixels(0, 0, grid.getNbCols(), grid.getNbRows(), data);
        GridCoverage2D gc = gcf.create("generated_landscape", rast, referencedEnvelope);
        GeoTiffWriter writer = new GeoTiffWriter(new File(dest));
        writer.write(gc,null);
        System.out.println("Fractal terrain raster exported at " + dest);
        gc.dispose(true);
        writer.dispose();
    }

    public static void exportDoubleRaster(double[] data, RegularSquareGrid grid, double x, double y, double resolution,
                                          String epsg, String dest) throws IOException, FactoryException {
        exportDoubleRaster(data, grid, x, y, resolution, resolution, epsg, dest);
    }

    /**
     * Export the generated landscape to a raster file
     * @param x X position (geographical coordinates) of the top-left output raster pixel
     * @param y Y position (geographical coordinates) of the top-left output raster pixel
     * @param resolution_x x-spatial resolution (geographical units) of the output raster (i.e. pixel width)
     * @param resolution_y y-spatial resolution (geographical units) of the output raster (i.e. pixel height)
     * @param epsg EPSG identifier of the output projection
     * @param dest path of output raster
     * @throws IOException
     * @throws FactoryException
     */
    public static void exportIntRaster(int[] data, RegularSquareGrid grid, double x, double y, double resolution_x,
                                double resolution_y, String epsg, String dest) throws IOException, FactoryException {
        GridCoverageFactory gcf = new GridCoverageFactory();
        CoordinateReferenceSystem crs = CRS.decode(epsg);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                x, x + (grid.getNbCols() * resolution_x),
                y - (grid.getNbRows() * resolution_y), y,
                crs
        );
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
     * Export the generated landscape to a raster file
     * @param x X position (geographical coordinates) of the top-left output raster pixel
     * @param y Y position (geographical coordinates) of the top-left output raster pixel
     * @param resolution spatial resolution (geographical units) of the output raster (i.e. pixel width)
     * @param epsg EPSG identifier of the output projection
     * @param dest path of output raster
     * @throws IOException
     * @throws FactoryException
     */
    public static void exportIntRaster(int[] data, RegularSquareGrid grid, double x, double y, double resolution, String epsg, String dest) throws IOException, FactoryException {
        exportIntRaster(data, grid, x, y, resolution, resolution, epsg, dest);
    }

    public static double getNodataValue(String rasterPath) throws IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        double noData = CoverageUtilities.getNoDataProperty(gridCov).getAsSingleValue();
        gridCov.dispose(true);
        reader.dispose();
        return noData;
    }

    public static int[] getDimensions(String rasterPath) throws IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nbRows = gridCov.getRenderedImage().getHeight();
        int nbCols = gridCov.getRenderedImage().getWidth();
        return new int[] {nbRows, nbCols};
    }

    public static int[] getNodataCells(String rasterPath) throws IOException {
        int noData = (int) getNodataValue(rasterPath);
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nbRows = gridCov.getRenderedImage().getHeight();
        int nbCols = gridCov.getRenderedImage().getWidth();
        int[] values = new int[nbRows * nbCols];
        gridCov.getRenderedImage().getData().getSamples(0, 0, nbCols, nbRows, 0, values);
        gridCov.dispose(true);
        reader.dispose();
        return IntStream.range(0, values.length).filter(i -> values[i] == noData).toArray();
    }

    public static String getSrs(String input) throws IOException {
        File file = new File(input);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        String srs = gridCov.getEnvelope2D().getCoordinateReferenceSystem().getIdentifiers().iterator().next().toString();
        gridCov.dispose(true);
        reader.dispose();
        return srs;
    }

    public static double[] getXYRes(String input) throws IOException {
        File file = new File(input);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        double x = gridCov.getEnvelope2D().getMinX();
        double y = gridCov.getEnvelope2D().getMinY();
        double resolution = gridCov.getEnvelope2D().getHeight() / gridCov.getRenderedImage().getHeight();
        gridCov.dispose(true);
        reader.dispose();
        return new double[] {x, y, resolution};
    }
}
