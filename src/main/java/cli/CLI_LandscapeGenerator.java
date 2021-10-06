package cli;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import picocli.CommandLine;
import solver.LandscapeGenerator;
import solver.LandscapeStructure;
import solver.Terrain;

import java.io.*;

@CommandLine.Command(
        name = "generator",
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
                LandscapeStructure s = LandscapeStructure.fromJSON(reader);
                // Generate landscape
                Terrain terrain = new Terrain(new RegularSquareGrid(s.nbRows, s.nbCols));
                if (terrainInput.equals("")) {
                    terrain.generateDiamondSquare(roughnessFactor);
                } else {
                    terrain.loadFromRaster(terrainInput);
                }
                INeighborhood bufferNeighborhood;
                switch (minDistance) {
                    case 1:
                        bufferNeighborhood = Neighborhoods.FOUR_CONNECTED;
                        break;
                    case 2:
                        bufferNeighborhood = Neighborhoods.TWO_WIDE_FOUR_CONNECTED;
                        break;
                    default:
                        bufferNeighborhood = Neighborhoods.K_WIDE_FOUR_CONNECTED(minDistance);
                        break;
                }
                LandscapeGenerator landscapeGenerator = new LandscapeGenerator(
                        s, Neighborhoods.FOUR_CONNECTED, bufferNeighborhood, terrain
                );
                if (nbLandscapes == 1) { // One landscape case
                    boolean b = landscapeGenerator.generate(terrainDependency, maxTry, maxTryPatch);
                    if (!b) {
                        System.out.println("FAIL");
                    } else {
                        System.out.println("Feasible landscape found after " + landscapeGenerator.nbTry + " tries");
                        landscapeGenerator.exportRaster(x, y, resolution, srs, outputPrefix + "_" + structNames[i] + ".tif");
                    }
                } else { // Several landscapes case
                    int n = 0;
                    while (n < nbLandscapes) {
                        boolean b = landscapeGenerator.generate(terrainDependency, maxTry, maxTryPatch);
                        if (!b) {
                            System.out.println("Failed to generate landscape " + (n + 1));
                        } else {
                            System.out.println("Feasible landscape " + (n + 1) + " found after " + landscapeGenerator.nbTry + " tries");
                            landscapeGenerator.exportRaster(x, y, resolution, srs, outputPrefix + "_" + structNames[i] + "_" + (n +  1) + ".tif");
                        }
                        n++;
                        landscapeGenerator.init();
                    }
                }
                if (!terrainOutput.equals("")) {
                    landscapeGenerator.terrain.exportRaster(x, y, resolution, srs, terrainOutput);
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
    }
}
