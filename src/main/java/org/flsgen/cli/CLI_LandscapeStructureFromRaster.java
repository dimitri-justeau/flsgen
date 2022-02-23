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

import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.solver.LandscapeStructure;
import picocli.CommandLine;

import java.io.*;

@CommandLine.Command(
        name = "extract_structure",
        mixinStandardHelpOptions = true,
        description = "Extracts a landscape structure from an existing raster."
)
public class CLI_LandscapeStructureFromRaster implements Runnable {

    @CommandLine.Parameters(
            description = "JSON output file -- Use \"-\" to write to STDOUT ",
            index = "1"
    )
    String outputFile;

    @CommandLine.Parameters(
            description = "Raster (.tif) input file to extract the landscape structure from",
            index = "0"
    )
    String inputRaster;

    @CommandLine.Parameters(
            description = "Raster values of the focal classes",
            index = "2..*"
    )
    int[] focalClasses;

    @CommandLine.Option(
            names = {"-c", "--connectivity"},
            description = "Connectivity definition in the regular grid - '4' (4-connected)" +
                    " or '8' (8-connected) (default: 4).",
            defaultValue = "4"
    )
    int connectivity;

    @Override
    public void run() {
        if (connectivity != 4 && connectivity !=8) {
            System.err.println(ANSIColors.ANSI_RED + "The Connectivity definition must be either 4 or 8" + ANSIColors.ANSI_RESET);
        }
        try {
            INeighborhood neigh = connectivity == 4 ? Neighborhoods.FOUR_CONNECTED : Neighborhoods.HEIGHT_CONNECTED;
            LandscapeStructure s = LandscapeStructure.fromRaster(inputRaster, focalClasses, neigh);
            if (outputFile.equals("-")) {
                System.out.println(s.toJSON());
            } else {
                FileWriter writer = new FileWriter(outputFile);
                writer.write(s.toJSON());
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
