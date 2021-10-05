package cli;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import picocli.CommandLine;
import solver.LandscapeGenerator;
import solver.LandscapeStructure;
import solver.Terrain;

import java.io.*;

@CommandLine.Command(
        name = "generator",
        mixinStandardHelpOptions = true,
        description = "Generate a landscape from a given structure. To produce more realistic landscape, " +
                "the algorithm relies on a terrain either given as input or automatically generated as" +
                " a fractal terrain."
)
public class CLI_LandscapeGenerator implements Runnable {

    @CommandLine.Parameters(
            description = "JSON input file describing landscape structure -- Use \"-\" to read from STDIN"
    )
    String jsonPath;

    @CommandLine.Parameters(
            description = "Output raster path for generated landscape"
    )
    String output;

    @CommandLine.Option(
            names = {"-et", "--export-terrain"},
            description = "Set an output raster path to export the terrain used to generate the landscape",
            defaultValue = ""
    )
    String terrainOutput;

    @CommandLine.Option(
            names = {"-lt", "--load-terrain"},
            description = "Load the terrain used by the algorithm from a raster instead of generating it",
            defaultValue = ""
    )
    String terrainInput;

    @CommandLine.Option(
            names = {"-R", "--roughness"},
            description = "Roughness parameter (also called H), between 0 and 1 for fractal terrain generation." +
                    " Lower values produce rougher terrain (0.5 by default)",
            defaultValue = "0.5"
    )
    double roughnessFactor;

    @CommandLine.Option(
            names = {"-T", "--terrain-dependency"},
            description = "Terrain dependency of the patch generation algorithm, between 0 and 1." +
                    " 0 means no dependency to the terrain, and 1 mean that patch generation is entirely guided by" +
                    " the terrain (default 0.5)",
            defaultValue = "0.5"
    )
    double terrainDependency;

    @CommandLine.Option(
            names = {"-D", "--distance-between-patches"},
            description = "Minimum distance (in number of cells) between patches from a same class (default 2).",
            defaultValue = "2"
    )
    int minDistance;

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
            // Read input structure
            Reader reader;
            if (jsonPath.equals("-")) {
                reader = new BufferedReader(new InputStreamReader(System.in));
            } else {
                reader = new FileReader(jsonPath);
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
            landscapeGenerator.generate(output, terrainDependency);
            if (!terrainOutput.equals("")) {
                landscapeGenerator.terrain.exportRaster(0, 0, 0.0001, "EPSG:4326", terrainOutput);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
