package cli;

import picocli.CommandLine;
import solver.LandscapeStructureSolver;
import solver.LandscapeStructure;

import java.io.*;

@CommandLine.Command(
        name = "structure",
        mixinStandardHelpOptions = true,
        description = "Generate a landscape structure satisfying a set of targets"
)
public class CLI_LandscapeStructureSolver implements Runnable {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

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
