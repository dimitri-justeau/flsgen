package cli;

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
            names = {"-R", "--roughness"},
            description = "Roughness parameter (also called H), between 0 and 1 for fractal terrain generation." +
                    " Lower values produce rougher terrain (0.5 by default)",
            required = true,
            defaultValue = "0.5"
    )
    double roughnessFactor;

    @Override
    public void run() {
        try {
            // Check parameters
            if (roughnessFactor < 0 || roughnessFactor > 1) {
                System.err.println(ANSIColors.ANSI_RED + "Roughness factor must be in [0, 1]" + ANSIColors.ANSI_RESET);
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
            terrain.generateDiamondSquare(roughnessFactor);
            LandscapeGenerator landscapeGenerator = new LandscapeGenerator(
                    s, Neighborhoods.FOUR_CONNECTED, Neighborhoods.FOUR_CONNECTED, terrain
            );
            landscapeGenerator.generate(output);
            if (!terrainOutput.equals("")) {
                landscapeGenerator.terrain.exportRaster(0, 0, 0.0001, "EPSG:4326", terrainOutput);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
