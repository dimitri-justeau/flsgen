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
        if (args.length == 0) {
            new CommandLine(new Main()).usage(System.out);
            return;
        }
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public void run() {
    }
}
