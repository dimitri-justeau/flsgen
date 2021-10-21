/*
 * Copyright (c) 2021, Dimitri Justeau-Allaire
 *
 * Institut Agronomique neo-Caledonien (IAC), 98800 Noumea, New Caledonia
 * AMAP, Univ Montpellier, CIRAD, CNRS, INRA, IRD, Montpellier, France
 *
 * This file is part of flsgen.
 *
 * flsgen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * flsgen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with flsgen.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.flsgen.cli;

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
