package cli;

import picocli.CommandLine;
import solver.LandscapeStructureSolver;
import solver.LandscapeStructure;

import java.io.*;

import static cli.ANSIColors.*;

@CommandLine.Command(
        name = "structure",
        mixinStandardHelpOptions = true,
        description = "Generate a landscape structure satisfying a set of targets"
)
public class CLI_LandscapeStructureSolver implements Runnable {

    @CommandLine.Parameters(
            description = "JSON input file describing landscape targets -- Use \"-\" to read from STDIN"
    )
    String jsonPath;

    @CommandLine.Parameters(
            description = "JSON output file for solution -- Use \"-\" to write to STDOUT"
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
            LandscapeStructureSolver lSolver = LandscapeStructureSolver.readFromJSON(reader);
            lSolver.build();
            LandscapeStructure s = lSolver.findSolution();
            if (s != null) {
                System.err.println(ANSI_GREEN + "Solution found in " + lSolver.model.getSolver().getTimeCount() + " s" + ANSI_RESET);
                if (output.equals("-")) {
                    System.out.println(s.toJSON());
                } else {
                    new FileWriter(output).write(s.toJSON());
                }
            } else {
                System.err.println(ANSI_RED + "No possible solution" + ANSI_RESET);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
