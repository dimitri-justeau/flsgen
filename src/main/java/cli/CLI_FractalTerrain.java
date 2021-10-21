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

package cli;

import grid.regular.square.RegularSquareGrid;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import picocli.CommandLine;
import solver.Terrain;

import java.io.File;
import java.io.IOException;

@CommandLine.Command(
        name = "terrain",
        mixinStandardHelpOptions = true,
        description = "Generate a fractal terrain using the Diamond-Square algorithm"
)
public class CLI_FractalTerrain implements Runnable {

    @CommandLine.Option(
            names = {"-H", "--height"},
            description = "Height (in pixels) of the terrain to generate",
            required = true
    )
    int nbRows;

    @CommandLine.Option(
            names = {"-W", "--width"},
            description = "Width (in pixels) of the terrain to generate",
            required = true
    )
    int nbCols;

    @CommandLine.Option(
            names = {"-R", "--roughness"},
            description = "Roughness parameter (also called H), between 0 and 1. Lower values produce rougher terrain (0.5 by default)",
            defaultValue = "0.5"
    )
    double roughnessFactor;

    @CommandLine.Option(
            names = {"-x"},
            description = "Top left x coordinate of the output raster (default 0)",
            defaultValue = "0"
    )
    double x;

    @CommandLine.Option(
            names = {"-y"},
            description = "Top left y coordinate of the output raster (default 0)",
            defaultValue = "0"
    )
    double y;

    @CommandLine.Option(
            names = {"-r", "--resolution"},
            description = "Spatial resolution of the output raster (in CRS unit, default 0.0001)",
            defaultValue = "0.0001"
    )
    double resolution;

    @CommandLine.Option(
            names = {"-s", "-srs", "--spatial-reference-system"},
            description = "Spatial reference system of the output raster (default: EPSG:4326)",
            defaultValue = "EPSG:4326"
    )
    String srs;

    @CommandLine.Option(
            names = {"-t", "-ot", "--output-template"},
            description = "Raster template to use for output raster metadata",
            defaultValue = ""
    )
    String template;

    @CommandLine.Parameters(
            description = "Path to the raster to generate as output"
    )
    String output;

    @Override
    public void run() {
        try {
            if (roughnessFactor < 0 || roughnessFactor > 1) {
                System.err.println(ANSIColors.ANSI_RED + "Roughness factor must be in [0, 1]" + ANSIColors.ANSI_RESET);
                return;
            }
            if (!template.equals("")) {
                initRasterMetadataFromTemplate(template);
            }
            RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);
            Terrain terrain = new Terrain(grid);
            terrain.generateDiamondSquare(roughnessFactor);
            terrain.exportRaster(x, y, resolution, srs, output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initRasterMetadataFromTemplate(String input) throws IOException {
        File file = new File(input);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        resolution = gridCov.getEnvelope2D().getHeight() / gridCov.getRenderedImage().getHeight();
        srs = gridCov.getEnvelope2D().getCoordinateReferenceSystem().getIdentifiers().iterator().next().toString();
        x = gridCov.getEnvelope2D().getMinX();
        y = gridCov.getEnvelope2D().getMinY();
    }
}
