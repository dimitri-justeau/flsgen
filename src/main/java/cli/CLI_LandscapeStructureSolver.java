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
            description = "JSON output file (or prefix for multiple structure generation) for solution -- Use \"-\" to write to STDOUT " +
                    "(only possible for single-solution generation)"
    )
    String output;

    @CommandLine.Option(
            names = {"-n", "--nb-solutions"},
            description = "Number of solutions to generate, if greater than one, use a prefix for JSON output file (default: 1).",
            defaultValue = "1"
    )
    int nbSolutions;

    @Override
    public void run() {
        if (nbSolutions <= 0) {
            System.err.println(ANSIColors.ANSI_RED + "Number of solutions must be at least 1" + ANSIColors.ANSI_RESET);
            return;
        }
        if (nbSolutions > 1 && output.equals("-")) {
            System.err.println(ANSIColors.ANSI_RED + "STDOUT solution output is only possible when nbSolutions = 1" + ANSIColors.ANSI_RESET);
            return;
        }
        try {
            Reader reader;
            if (jsonPath.equals("-")) {
                reader = new BufferedReader(new InputStreamReader(System.in));
            } else {
                reader = new FileReader(jsonPath);
            }
            LandscapeStructureSolver lSolver = LandscapeStructureSolver.readFromJSON(reader);
            lSolver.build();
            if (nbSolutions == 1) {
                // One solution case
                LandscapeStructure s = lSolver.findSolution();
                if (s != null) {
                    System.err.println(ANSI_GREEN + "Solution found in " + lSolver.model.getSolver().getTimeCount() + " s" + ANSI_RESET);
                    if (output.equals("-")) {
                        System.out.println(s.toJSON());
                    } else {
                        FileWriter writer = new FileWriter(output);
                        writer.write(s.toJSON());
                        writer.close();
                    }
                } else {
                    System.err.println(ANSI_RED + "No possible solution" + ANSI_RESET);
                }
            } else { // Several solutions case
                int n = 0;
                while (n < nbSolutions) {
                    LandscapeStructure s = lSolver.findSolution();
                    if (s != null) {
                        System.err.println(ANSI_GREEN + "Solution " + (n + 1) + " found (total solving time " + lSolver.model.getSolver().getTimeCount() + " s)" + ANSI_RESET);
                        FileWriter writer = new FileWriter(output + (n + 1) + ".json");
                        writer.write(s.toJSON());
                        writer.close();
                        n++;
                    } else {
                        if (n == 0) {
                            System.err.println(ANSI_RED + "No possible solution" + ANSI_RESET);
                        } else {
                            System.err.println(ANSI_RED + "No more possible solutions" + ANSI_RESET);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
