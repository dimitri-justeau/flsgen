package cli;

import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import picocli.CommandLine;
import solver.LandscapeStructureSolver;
import solver.PolyominoGenerator;
import solver.Solution;

@CommandLine.Command(
        name = "generator",
        mixinStandardHelpOptions = true,
        description = "Generate a landscape from a given structure"
)
public class CLI_LandscapeGenerator implements Runnable {

    @CommandLine.Option(
            names = "-f",
            description = "JSON input file describing landscape structure",
            required = true
    )
    String jsonPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Output path for generated landscape",
            required = true
    )
    String output;

    @Override
    public void run() {
        try {
            Solution s = Solution.fromJSON(jsonPath);
            RegularSquareGrid grid = new RegularSquareGrid(s.nbRows, s.nbCols);
            PolyominoGenerator polyominoGenerator = new PolyominoGenerator(grid, Neighborhoods.FOUR_CONNECTED, Neighborhoods.FOUR_CONNECTED);
            polyominoGenerator.generateFromJSONStructure(jsonPath, output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
