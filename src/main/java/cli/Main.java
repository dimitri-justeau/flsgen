package cli;

import picocli.CommandLine;

/**
 * Executable class for CLI
 */

@CommandLine.Command(
        name = "FLSGen",
        mixinStandardHelpOptions = true,
        description = "A fragmented neutral landscape generator",
        subcommands = {
            CLI_LandscapeStructureSolver.class,
            CLI_LandscapeGenerator.class,
            CLI_FractalHeightMap.class
        }
)
public class Main implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public void run() {

    }
}
