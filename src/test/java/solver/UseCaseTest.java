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

package solver;

import com.github.cliftonlabs.json_simple.JsonException;
import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.loop.monitors.ISearchMonitorFactory;
import org.opengis.referencing.FactoryException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UseCaseTest {

    @Test
    public void useCase1() throws IOException, JsonException {
        String path = getClass().getClassLoader().getResource("targets_1.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        LandscapeStructure s = ls.findSolution();
        // Check values for class 0
        int NP1 = s.getNbPatches(0);
        int CA1 = s.getTotalSize(0);
        double MESH1 = s.getMesh(0);
        int SPI1 = s.getLargestPatchIndex(0);
        int LPI1 = s.getLargestPatchIndex(0);
        Assert.assertTrue(NP1 >= 1 && NP1 <= 10);
        Assert.assertTrue(CA1 >= 1000 && CA1 <= 6000);
        Assert.assertTrue(MESH1 >= 55 && MESH1 <= 500);
        Assert.assertTrue(SPI1 >= 300);
        Assert.assertTrue(LPI1 == 4000);
        // Check values for class 1
        int NP2 = s.getNbPatches(1);
        double PLAND2 = s.getLandscapeProportion(1);
        int SPI2 = s.getSmallestPatchIndex(1);
        int LPI2 = s.getLargestPatchIndex(1);
        Assert.assertTrue(NP2 >= 1 && NP2 <= 15);
        Assert.assertTrue(SPI2 >= 200);
        Assert.assertTrue(LPI2 <= 12000);
        Assert.assertTrue(PLAND2 >= 50.5 && PLAND2 <= 60);
    }

    @Test
    public void useCase2() throws IOException, JsonException {
        String path = getClass().getClassLoader().getResource("targets_2.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        LandscapeStructure s = ls.findSolution();
        Assert.assertEquals(s.getMesh(0), 225);
        Assert.assertEquals(s.getLandscapeProportion(1), 40);
    }

    @Test
    public void useCase3() throws IOException, JsonException {
        String path = getClass().getClassLoader().getResource("targets_3.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        LandscapeStructure s = ls.findSolution();
    }

    @Test
    public void useCase4() throws IOException, JsonException {
        String path = getClass().getClassLoader().getResource("targets_4.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        ls.setDomOverWDegRefSearch();
        LandscapeStructure s = ls.findSolution();
    }

    @Test public void useCaseFromScratch() throws FactoryException, IOException {
        RegularSquareGrid grid = new RegularSquareGrid(200, 200);
        double[] mesh = new double[] {1000, 2000, 3000, 4000, 5000};
        for (double m : mesh) {
            LandscapeStructureSolver ls = new LandscapeStructureSolver(grid);
            LandscapeClass c = ls.landscapeClass("habitat", 1, 10, 100, 100*100);
            c.setMesh(0.999 * m, 1.001 * m);
            ls.build();
            LandscapeStructure s = ls.findSolution();
            System.out.println("MESH = " + s.getMesh(0));
            System.out.println("PLAND = " + s.getLandscapeProportion(0));
            Terrain t = new Terrain(grid);
            t.generateDiamondSquare(0.5);
            INeighborhood n = Neighborhoods.FOUR_CONNECTED;
            INeighborhood d = Neighborhoods.TWO_WIDE_FOUR_CONNECTED;
            LandscapeGenerator generator = new LandscapeGenerator(s, n, d, t);
            generator.generate(0.5, 10, 10);
//            generator.exportRaster(0, 0, 1, "EPSG:3163", "/home/djusteau/Documents/testPolyomino/solutions2/MESH__" + m + ".tif");
        }
    }
}
