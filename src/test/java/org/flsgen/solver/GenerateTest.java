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
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.opengis.referencing.FactoryException;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GenerateTest {

    @Test
    public void generateLandscape() throws IOException, JsonException, FlsgenException, FactoryException {
        String path = getClass().getClassLoader().getResource("struct_target.json").getPath();
        String rasterPath = getClass().getClassLoader().getResource("terrain.tif").getPath();
        LandscapeStructure structure = LandscapeStructure.fromJSON(new FileReader(path));
        RegularSquareGrid grid = new RegularSquareGrid(200, 200);
        Terrain terrain = new Terrain(grid);
        terrain.loadFromRaster(rasterPath);
        INeighborhood neighborhood = Neighborhoods.FOUR_CONNECTED;
        INeighborhood distance = Neighborhoods.TWO_WIDE_FOUR_CONNECTED;
        LandscapeGenerator generator = new LandscapeGenerator(structure, neighborhood, distance, terrain);
        generator.generate(0.5, 10,10);
        Path temp = Files.createTempFile("landscape_struct_target", ".tif");
        generator.exportRaster(0, 0, 0.001, "EPSG:4326", temp.toString());
        Files.delete(temp);
    }
}
