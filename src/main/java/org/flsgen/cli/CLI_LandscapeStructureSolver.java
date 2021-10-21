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

import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;
import org.flsgen.solver.LandscapeStructureSolver;
import org.flsgen.solver.LandscapeStructure;

import java.io.*;

import static org.flsgen.cli.ANSIColors.*;

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
            description = "Search strategy to use in the Choco org.flsgen.solver (possible values: ${COMPLETION-CANDIDATES}).",
            defaultValue = "DEFAULT"
    )
    SearchStrategy search;

    @Override
    public void run() {
        if (nbSolutions <= 0) {
            System.err.println(ANSI_RED + "Number of solutions must be at least 1" + ANSI_RESET);
            return;
        }
        if (nbSolutions > 1 && outputPrefix.equals("-")) {
            System.err.println(ANSI_RED + "STDOUT solution output is only possible when nbSolutions = 1" + ANSI_RESET);
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
                    case ACTIVITY_BASED:
                        lSolver.setActivityBasedSearch();
                        break;
                    case CONFLICT_HISTORY:
                        lSolver.setConflictHistorySearch();
                        break;
                    default:
                        lSolver.setDefaultSearch();
                        break;
                }
                if (nbSolutions == 1) {
                    // One solution case
                    LandscapeStructure s = lSolver.findSolution();
                    if (s != null) {
                        System.err.println(ANSI_GREEN + "Solution found in " + lSolver.getModel().getSolver().getTimeCount() + " s" + ANSI_RESET);
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
                            System.err.println(ANSI_GREEN + "Solution " + (n + 1) + " found (total solving time " + lSolver.getModel().getSolver().getTimeCount() + "s)" + ANSI_RESET);
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
