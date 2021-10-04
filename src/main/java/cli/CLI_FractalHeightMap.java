package cli;

import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import picocli.CommandLine;
import solver.LandscapeGenerator;

@CommandLine.Command(
        name = "heightmap",
        mixinStandardHelpOptions = true,
        description = "Generate a fractal heightmap using the Diamond-Square algorithm"
)
public class CLI_FractalHeightMap implements Runnable {

    @CommandLine.Option(
            names = {"-H", "--height"},
            description = "Height (in pixels) of the heightmap to generate",
            required = true
    )
    int nbRows;

    @CommandLine.Option(
            names = {"-W", "--width"},
            description = "Width (in pixels) of the heightmap to generate",
            required = true
    )
    int nbCols;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Path to the raster to generate as output",
            required = true
    )
    String output;

    @Override
    public void run() {
        try {
            RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);
            LandscapeGenerator landscapeGenerator = new LandscapeGenerator(grid, Neighborhoods.FOUR_CONNECTED, Neighborhoods.FOUR_CONNECTED);
            landscapeGenerator.exportDem(0, 0, 0.0001, "EPSG:4326", output);
            System.out.println("Fractal heightmap generated and exported at " + output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
