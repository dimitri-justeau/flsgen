package cli;

import grid.regular.square.RegularSquareGrid;
import picocli.CommandLine;
import solver.Terrain;

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
            required = true,
            defaultValue = "0.5"
    )
    double roughnessFactor;

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
            RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);
            Terrain terrain = new Terrain(grid);
            terrain.generateDiamondSquare(roughnessFactor);
            terrain.exportRaster(0, 0, 0.0001, "EPSG:4326", output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
