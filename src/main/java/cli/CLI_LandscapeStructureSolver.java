package cli;

import org.apache.commons.io.FilenameUtils;
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

    public enum SearchStrategy {
        DEFAULT,
        RANDOM,
        DOM_OVER_W_DEG,
        DOM_OVER_W_DEG_REF,
        ACTIVITY_BASED,
        CONFLICT_HISTORY,
        MIN_DOM_UB,
        MIN_DOM_LB,
        MIN_DOM_RANDOM
    }

    @CommandLine.Parameters(
            description = "JSON output file (or prefix for multiple structure generation) for solution -- Use \"-\" to write to STDOUT " +
                    "(only possible with one structure as input and single-solution generation)",
            index = "0"
    )
    String outputPrefix;

    @CommandLine.Parameters(
            description = "JSON input file(s) describing landscape targets -- " +
                    "Use \"-\" to read from STDIN (only possible with one structure as input) -- " +
                    "Use multiple space-separated paths to generate landscapes with different structures.",
            index = "1..*"
    )
    String[] jsonPaths;

    @CommandLine.Option(
            names = {"-n", "--nb-solutions"},
            description = "Number of solutions to generate, if greater than one, use a prefix for JSON output file (default: 1).",
            defaultValue = "1"
    )
    int nbSolutions;

    @CommandLine.Option(
            names = {"-s", "--search-strategy"},
            description = "Search strategy to use in the Choco solver (possible values: ${COMPLETION-CANDIDATES}).",
            defaultValue = "DEFAULT"
    )
    SearchStrategy search;

    @Override
    public void run() {
        if (nbSolutions <= 0) {
            System.err.println(ANSIColors.ANSI_RED + "Number of solutions must be at least 1" + ANSIColors.ANSI_RESET);
            return;
        }
        if (nbSolutions > 1 && outputPrefix.equals("-")) {
            System.err.println(ANSIColors.ANSI_RED + "STDOUT solution output is only possible when nbSolutions = 1" + ANSIColors.ANSI_RESET);
            return;
        }
        try {
            String[] targetNames = new String[jsonPaths.length];
            if (jsonPaths.length == 1 && jsonPaths[0].equals("-")) {
                targetNames[0] = "STDIN";
            } else {
                for (int i = 0; i < jsonPaths.length; i++) {
                    targetNames[i] = FilenameUtils.removeExtension(new File(jsonPaths[i]).getName());
                }
            }

            for (int i = 0; i < jsonPaths.length; i++) {
                Reader reader;
                if (jsonPaths.length == 1 && jsonPaths[0].equals("-")) {
                    reader = new BufferedReader(new InputStreamReader(System.in));
                } else {
                    reader = new FileReader(jsonPaths[i]);
                }
                LandscapeStructureSolver lSolver = LandscapeStructureSolver.readFromJSON(reader);
                lSolver.build();
                lSolver.model.getSolver().showShortStatistics();
                switch (search) {
                    case DEFAULT:
                        lSolver.setDefaultSearch();
                        break;
                    case RANDOM:
                        lSolver.setRandomSearch();
                        break;
                    case DOM_OVER_W_DEG:
                        lSolver.setDomOverWDegSearch();
                        break;
                    case DOM_OVER_W_DEG_REF:
                        lSolver.setDomOverWDegRefSearch();
                        break;
                    case MIN_DOM_LB:
                        lSolver.setMinDomLBSearch();
                        break;
                    case MIN_DOM_UB:
                        lSolver.setMinDomUBSearch();
                        break;
                    case MIN_DOM_RANDOM:
                        lSolver.setMinDomRandomSearch();
                        break;
                    case ACTIVITY_BASED:
                        lSolver.setActivityBasedSearch();
                        break;
                    case CONFLICT_HISTORY:
                        lSolver.setConflictHistorySearch();
                        break;
                }
                if (nbSolutions == 1) {
                    // One solution case
                    LandscapeStructure s = lSolver.findSolution();
                    if (s != null) {
                        System.err.println(ANSI_GREEN + "Solution found in " + lSolver.model.getSolver().getTimeCount() + " s" + ANSI_RESET);
                        if (outputPrefix.equals("-")) {
                            System.out.println(s.toJSON());
                        } else {
                            FileWriter writer = new FileWriter(outputPrefix + "_" + targetNames[i] + ".json");
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
                            System.err.println(ANSI_GREEN + "Solution " + (n + 1) + " found (total solving time " + lSolver.model.getSolver().getTimeCount() + "s)" + ANSI_RESET);
                            FileWriter writer = new FileWriter(outputPrefix + "_" + targetNames[i] + "_" + (n + 1) + ".json");
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
