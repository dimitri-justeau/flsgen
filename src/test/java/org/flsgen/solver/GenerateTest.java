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
import org.opengis.referencing.FactoryException;
import org.testng.Assert;
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
        LandscapeGenerator generator = new LandscapeGenerator(structure, 4, 2, terrain);
        generator.generate(0.5, 10,10);
        Path temp = Files.createTempFile("landscape_struct_target", ".tif");
        generator.exportRaster(0, 0, 0.001, "EPSG:4326", temp.toString());
        Files.delete(temp);
    }

    @Test
    public void generateLandscapeWithMask() throws IOException, FactoryException, FlsgenException {
        String path = getClass().getClassLoader().getResource("mask_raster.tif").getPath();
        LandscapeStructure struct = LandscapeStructure.fromRaster(path, new int[] {1}, Neighborhoods.FOUR_CONNECTED);
        RegularSquareGrid grid = new RegularSquareGrid(struct.getNbRows(), struct.getNbCols());
        Terrain terrain = new Terrain(grid);
        terrain.generateDiamondSquare(0.01);
        LandscapeGenerator generator = new LandscapeGenerator(struct, 4, 1, 2, terrain);
        boolean b = generator.generate(0.95, 5, 10);
        Assert.assertTrue(b);
    }

    @Test
    public void testGenerateWithUniformTerrain() throws IOException, FlsgenException {
        String path = getClass().getClassLoader().getResource("mask_raster.tif").getPath();
        String terrain_path = getClass().getClassLoader().getResource("uniform_mask.tif").getPath();
        LandscapeStructure struct = LandscapeStructure.fromRaster(path, new int[] {1}, Neighborhoods.FOUR_CONNECTED);
        Terrain t = new Terrain(new RegularSquareGrid(struct.getNbRows(), struct.getNbCols()));
        t.loadFromRaster(terrain_path);
        LandscapeGenerator gen = new LandscapeGenerator(struct, 4, 1, 2, t);
        boolean b = gen.generate(0, 1, 5, false);
        Assert.assertTrue(b);
    }
}
