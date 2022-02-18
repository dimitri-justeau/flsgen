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

import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.flsgen.utils.CheckLandscape.checkNP;
import static org.flsgen.utils.CheckLandscape.checkAREA;

public class CheckTest {

    @Test
    public void testNP_AND_AREA() throws FlsgenException {
        RegularSquareGrid grid = new RegularSquareGrid(50, 50);
        LandscapeStructureSolver ls = new LandscapeStructureSolver(grid);
        ls.landscapeClass("cls0", 2, 10, 60, 100);
        ls.landscapeClass("cls1", 2, 10, 60, 100);
        ls.landscapeClass("cls2", 2, 10, 60, 100);
        ls.build();
        ls.setRandomSearch();
        Terrain t = new Terrain(grid);
        t.generateDiamondSquare(0.5);
        for (int i = 0; i < 10; i++) {
            LandscapeStructure struct = ls.findSolution();
            LandscapeGenerator gen = new LandscapeGenerator(
                    struct,
                    Neighborhoods.FOUR_CONNECTED,
                    Neighborhoods.TWO_WIDE_FOUR_CONNECTED,
                    t
            );
            if (gen.generate(0.5, 10, 10, false)) {
                int np0 = checkNP(gen, 0, Neighborhoods.FOUR_CONNECTED);
                int np1 = checkNP(gen, 1, Neighborhoods.FOUR_CONNECTED);
                int np2 = checkNP(gen, 2, Neighborhoods.FOUR_CONNECTED);
                int[] area0 = checkAREA(gen, 0, Neighborhoods.FOUR_CONNECTED);
                int[] area1 = checkAREA(gen, 1, Neighborhoods.FOUR_CONNECTED);
                int[] area2 = checkAREA(gen, 2, Neighborhoods.FOUR_CONNECTED);
                Assert.assertEquals(np0, struct.getNbPatches(0));
                Assert.assertEquals(np1, struct.getNbPatches(1));
                Assert.assertEquals(np2, struct.getNbPatches(2));
                Assert.assertTrue(Arrays.equals(area0, struct.getPatchSizes(0)));
                Assert.assertTrue(Arrays.equals(area1, struct.getPatchSizes(1)));
                Assert.assertTrue(Arrays.equals(area2, struct.getPatchSizes(2)));
            }
        }
    }

    @Test
    public void testStructureFromRaster() throws IOException {
        String path = getClass().getClassLoader().getResource("test_raster.tif").getPath();
        LandscapeStructure struct = LandscapeStructure.fromRaster(path, new int[] {0, 1, 2}, Neighborhoods.FOUR_CONNECTED);
        Assert.assertEquals(struct.getNbPatches(0), 40);
        Assert.assertEquals(struct.getNbPatches(1), 30);
        Assert.assertEquals(struct.getNbPatches(2), 20);
        Assert.assertEquals(struct.getLandscapeProportion(0), 20);
        Assert.assertEquals(struct.getLandscapeProportion(1), 10);
        Assert.assertEquals(struct.getLandscapeProportion(2), 10);
        RegularSquareGrid grid = new RegularSquareGrid(struct.getNbRows(), struct.getNbCols());
        Terrain terrain = new Terrain(grid);
        terrain.generateDiamondSquare(0.4);
        LandscapeGenerator generator = new LandscapeGenerator(
                struct,
                Neighborhoods.FOUR_CONNECTED,
                Neighborhoods.FOUR_CONNECTED,
                terrain
        );
        Assert.assertTrue(generator.generate(0.5, 5, 10, false));
    }
}
