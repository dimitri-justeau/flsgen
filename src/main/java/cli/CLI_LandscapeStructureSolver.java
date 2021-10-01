package cli;

import picocli.CommandLine;
import solver.LandscapeStructureSolver;
import solver.Solution;

@CommandLine.Command(
        name = "structure",
        mixinStandardHelpOptions = true,
        description = "Generate a landscape structure satisfying a set of targets"
)
public class CLI_LandscapeStructureSolver implements Runnable {

    @CommandLine.Option(
            names = "-f",
            description = "JSON input file describing landscape targets",
            required = true
    )
    String jsonPath;

    @Override
    public void run() {
        try {
            LandscapeStructureSolver lSolver = LandscapeStructureSolver.readFromJSON(jsonPath);
            lSolver.build();
            Solution s = lSolver.findSolution();
            if (s != null) {
                System.out.println("Solution found in " + lSolver.model.getSolver().getTimeCount() + " s");
                System.out.println(s.toJSON());
            } else {
                System.out.println("No possible solution");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
