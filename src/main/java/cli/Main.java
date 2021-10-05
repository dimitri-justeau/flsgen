package cli;

import org.geotools.util.logging.Logging;
import picocli.CommandLine;

import java.util.logging.Level;
import java.util.logging.Logger;

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
            CLI_FractalTerrain.class
        }
)
public class Main implements Runnable {


    public static void main(String[] args) {
        // Turn off hsqldb info logging
        Logger LOGGER = Logging.getLogger("hsqldb.db");
        LOGGER.setLevel(Level.WARNING);
        System.setProperty("hsqldb.reconfig_logging", "false");
        //
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
