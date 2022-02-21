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

package org.flsgen.solver;

import com.github.cliftonlabs.json_simple.JsonException;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StructureTest {

    @Test
    public void generateStructure() throws IOException, JsonException, FlsgenException {
        String path = getClass().getClassLoader().getResource("targets_4.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        LandscapeStructure struct = ls.findSolution();
        Path temp = Files.createTempFile("struct", ".json");
        FileWriter writer = new FileWriter(temp.toString());
        writer.write(struct.toJSON());
        writer.close();
        Files.delete(temp);
    }
}
