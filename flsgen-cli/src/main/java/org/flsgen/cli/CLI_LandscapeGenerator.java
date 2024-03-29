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

package org.flsgen.cli;
import org.apache.commons.io.FilenameUtils;

import org.apache.commons.io.IOUtils;
import org.flsgen.RasterUtils;
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.solver.LandscapeGenerator;
import org.flsgen.solver.LandscapeStructure;
import org.flsgen.solver.LandscapeStructureFactory;
import org.flsgen.solver.Terrain;
import org.flsgen.utils.ANSIColors;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import picocli.CommandLine;

import java.io.*;

@CommandLine.Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generate landscapes from given structures. To produce more realistic landscape, " +
                "the algorithm relies on a terrain either given as input or automatically generated as" +
                " a fractal terrain."
)
public class CLI_LandscapeGenerator implements Runnable {

    @CommandLine.Parameters(
            description = "Output raster prefix path for generated landscape(s)",
            index = "0"
    )
    String outputPrefix;

    @CommandLine.Parameters(
            description = "JSON input file describing landscape structure -- Use \"-\" to read from STDIN " +
                    "(only possible with one structure as input) -- " +
                    "Use multiple space-separated paths to generate landscapes with different structures.",
            index = "1..*"
    )
    String[] jsonPaths;

    @CommandLine.Option(
            names = {"-e", "-et", "--export-terrain"},
            description = "Set an output raster path to export the terrain used to generate the landscape",
            defaultValue = ""
    )
    String terrainOutput;

    @CommandLine.Option(
            names = {"-l", "-lt", "--load-terrain"},
            description = "Load the terrain used by the algorithm from a raster instead of generating it",
            defaultValue = ""
    )
    String terrainInput;

    @CommandLine.Option(
            names = {"-R", "--roughness"},
            description = "Roughness parameter (also called H), between 0 and 1 for fractal terrain generation." +
                    " Lower values produce rougher terrain (default: 0.5)",
            defaultValue = "0.5"
    )
    double roughnessFactor;

    @CommandLine.Option(
            names = {"-T", "--terrain-dependency"},
            description = "Terrain dependency of the patch generation algorithm, between 0 and 1." +
                    " 0 means no dependency to the terrain, and 1 mean that patch generation is entirely guided by" +
                    " the terrain (default: 0.5)",
            defaultValue = "0.5"
    )
    double terrainDependency;

    @CommandLine.Option(
            names = {"-D", "--distance-between-patches"},
            description = "Minimum distance (in number of cells) between patches from a same class (default: 2).",
            defaultValue = "2"
    )
    int minDistance;

    @CommandLine.Option(
            names = {"-M", "--max-min-dist"},
            description = "If set, the minimum distance between patches is variable between `minDistance` and `maxMinDistance`.",
            defaultValue = "-1"
    )
    int maxMinDistance;

    @CommandLine.Option(
            names = {"-x"},
            description = "Top left x coordinate of the output raster (default: 0)",
            defaultValue = "0"
    )
    double x;

    @CommandLine.Option(
            names = {"-y"},
            description = "Top left y coordinate of the output raster (default: 0)",
            defaultValue = "0"
    )
    double y;

    @CommandLine.Option(
            names = {"-r", "--resolution"},
            description = "Spatial resolution of the output raster (in CRS unit, default: 0.0001)",
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

    @CommandLine.Option(
            names = {"-m", "-mt", "--max-try"},
            description = "Maximum number or trials to generate the whole landscape (default: 100).",
            defaultValue = "100"
    )
    int maxTry;

    @CommandLine.Option(
            names = {"-p", "-mtp", "--max-try-patch"},
            description = "Maximum number of trials to generate a patch (default: 100).",
            defaultValue = "100"
    )
    int maxTryPatch;

    @CommandLine.Option(
            names = {"-n", "--nb-landscapes"},
            description = "Number of landscapes to generate (default: 1).",
            defaultValue = "1"
    )
    int nbLandscapes;

    @CommandLine.Option(
            names = {"-c", "--connectivity"},
            description = "Connectivity definition in the regular square grid - '4' (4-connected)" +
                    " or '8' (8-connected) (default: 4).",
            defaultValue = "4"
    )
    int connectivity;

    @Override
    public void run() {
        try {
            // Check parameters
            if (roughnessFactor < 0 || roughnessFactor > 1) {
                System.err.println(ANSIColors.ANSI_RED + "Roughness factor must be in [0, 1]" + ANSIColors.ANSI_RESET);
                return;
            }
            if (terrainDependency < 0 || terrainDependency > 1) {
                System.err.println(ANSIColors.ANSI_RED + "Terrain dependency must be in [0, 1]" + ANSIColors.ANSI_RESET);
                return;
            }
            if (minDistance <= 0) {
                System.err.println(ANSIColors.ANSI_RED + "Minimum distance between patches must be at least 1" + ANSIColors.ANSI_RESET);
                return;
            }
            if (maxTry <= 0) {
                System.err.println(ANSIColors.ANSI_RED + "Maximum trials must be at least 1" + ANSIColors.ANSI_RESET);
                return;
            }
            if (maxTryPatch <= 0) {
                System.err.println(ANSIColors.ANSI_RED + "Maximum patch trials must be at least 1" + ANSIColors.ANSI_RESET);
                return;
            }
            if (!template.equals("")) {
                initRasterMetadataFromTemplate(template);
            }
            if (connectivity != 4 && connectivity !=8) {
                System.err.println(ANSIColors.ANSI_RED + "The Connectivity definition must be either 4 or 8" + ANSIColors.ANSI_RESET);
            }
            // Read input structure
            String[] structNames = new String[jsonPaths.length];
            if (jsonPaths.length == 1 && jsonPaths[0].equals("-")) {
                structNames[0] = "STDIN";
            } else {
                for (int i = 0; i < jsonPaths.length; i++) {
                    structNames[i] = FilenameUtils.removeExtension(new File(jsonPaths[i]).getName());
                }
            }
            for (int i = 0; i < jsonPaths.length; i++) {
                Reader reader;
                if (jsonPaths.length == 1 && jsonPaths[0].equals("-")) {
                   reader = new BufferedReader(new InputStreamReader(System.in));
                } else {
                    reader = new FileReader(jsonPaths[i]);
                }
                LandscapeStructure s = LandscapeStructureFactory.readFromJSON(IOUtils.toString(reader));
                reader.close();
                // Generate landscape
                Terrain terrain = new Terrain(new RegularSquareGrid(s.getNbRows(), s.getNbCols()));
                if (terrainInput.equals("")) {
                    terrain.generateDiamondSquare(roughnessFactor);
                } else {
                    double[] rasterData = RasterUtils.loadDoubleDataFromRaster(terrainInput, terrain.getGrid());
                    terrain.loadFromData(rasterData);
                }
                INeighborhood bufferNeighborhood;
                if (maxMinDistance > 1) {
                    bufferNeighborhood = connectivity == 4 ?
                            Neighborhoods.VARIABLE_WIDTH_FOUR_CONNECTED(minDistance, maxMinDistance) :
                            Neighborhoods.VARIABLE_WIDTH_HEIGHT_CONNECTED(minDistance, maxMinDistance);
                } else {
                    switch (minDistance) {
                        case 1:
                            bufferNeighborhood = connectivity == 4 ? Neighborhoods.FOUR_CONNECTED : Neighborhoods.HEIGHT_CONNECTED;
                            break;
                        case 2:
                            bufferNeighborhood = connectivity == 4 ? Neighborhoods.TWO_WIDE_FOUR_CONNECTED : Neighborhoods.TWO_WIDE_HEIGHT_CONNECTED;
                            break;
                        default:
                            bufferNeighborhood = connectivity == 4 ? Neighborhoods.K_WIDE_FOUR_CONNECTED(minDistance) : Neighborhoods.K_WIDE_HEIGHT_CONNECTED(minDistance);
                            break;
                    }
                }
                INeighborhood c = connectivity == 4 ? Neighborhoods.FOUR_CONNECTED : Neighborhoods.HEIGHT_CONNECTED;
                LandscapeGenerator landscapeGenerator = new LandscapeGenerator(
                        s, c, bufferNeighborhood, terrain
                );
                if (nbLandscapes == 1) { // One landscape case
                    boolean b = landscapeGenerator.generate(terrainDependency, maxTry, maxTryPatch);
                    if (!b) {
                        System.out.println("FAIL");
                    } else {
                        System.out.println("Feasible landscape found after " + landscapeGenerator.getNbTry() + " tries");
                        int noDataValue = -1;
                        if (s.getMaskRasterPath() != null) {
                            noDataValue = (int) RasterUtils.getNodataValue(s.getMaskRasterPath());
                        }
                        int[] rasterData = landscapeGenerator.getRasterData(noDataValue);
                        RasterUtils.exportIntRaster(rasterData, landscapeGenerator.getGrid(), x, y, resolution, srs, outputPrefix + "_" + structNames[i] + ".tif");
                    }
                } else { // Several landscapes case
                    int n = 0;
                    while (n < nbLandscapes) {
                        boolean b = landscapeGenerator.generate(terrainDependency, maxTry, maxTryPatch);
                        if (!b) {
                            System.out.println("Failed to generate landscape " + (n + 1));
                        } else {
                            System.out.println("Feasible landscape " + (n + 1) + " found after " + landscapeGenerator.getNbTry() + " tries");
                            int noDataValue = -1;
                            if (s.getMaskRasterPath() != null) {
                                noDataValue = (int) RasterUtils.getNodataValue(s.getMaskRasterPath());
                            }
                            int[] rasterData = landscapeGenerator.getRasterData(noDataValue);
                            RasterUtils.exportIntRaster(rasterData, landscapeGenerator.getGrid(), x, y, resolution, srs, outputPrefix + "_" + structNames[i] + "_" + (n +  1) + ".tif");
                        }
                        n++;
                        landscapeGenerator.init();
                    }
                }
                if (!terrainOutput.equals("")) {
                    int[] rasterData = landscapeGenerator.getRasterData((int) RasterUtils.getNodataValue(s.getMaskRasterPath()));
                    RasterUtils.exportIntRaster(rasterData, landscapeGenerator.getGrid(), x, y, resolution, srs, terrainOutput);
                }
            }
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
        gridCov.dispose(true);
        reader.dispose();
    }
}
