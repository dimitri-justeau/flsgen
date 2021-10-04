package cli;

import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import picocli.CommandLine;
import solver.LandscapeGenerator;
import solver.LandscapeStructure;

import java.io.*;

@CommandLine.Command(
        name = "generator",
        mixinStandardHelpOptions = true,
        description = "Generate a landscape from a given structure"
)
public class CLI_LandscapeGenerator implements Runnable {

    @CommandLine.Parameters(
            description = "JSON input file describing landscape structure -- Use \"-\" to read from STDIN"
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
            Reader reader;
            if (jsonPath.equals("-")) {
                reader = new BufferedReader(new InputStreamReader(System.in));
            } else {
                reader = new FileReader(jsonPath);
            }
            LandscapeStructure s = LandscapeStructure.fromJSON(reader);
            RegularSquareGrid grid = new RegularSquareGrid(s.nbRows, s.nbCols);
            LandscapeGenerator landscapeGenerator = new LandscapeGenerator(s, Neighborhoods.FOUR_CONNECTED, Neighborhoods.FOUR_CONNECTED);
            landscapeGenerator.generate(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
