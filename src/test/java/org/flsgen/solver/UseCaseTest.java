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
import org.flsgen.grid.Grid;
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.opengis.referencing.FactoryException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class UseCaseTest {

    @Test
    public void useCase1() throws IOException, JsonException, FlsgenException {
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
    public void useCase2() throws IOException, JsonException, FlsgenException {
        String path = getClass().getClassLoader().getResource("targets_2.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        LandscapeStructure s = ls.findSolution();
        Assert.assertEquals(s.getMesh(0), 225);
        Assert.assertEquals(s.getLandscapeProportion(1), 40);
    }

    @Test
    public void useCase3() throws IOException, JsonException, FlsgenException {
        String path = getClass().getClassLoader().getResource("targets_3.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        ls.findSolution();
    }

    @Test
    public void useCase4() throws IOException, JsonException, FlsgenException {
        String path = getClass().getClassLoader().getResource("targets_4.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        ls.setDomOverWDegRefSearch();
        ls.findSolution();
    }

    @Test
    public void useCase5() throws IOException, JsonException, FlsgenException {
        String path = getClass().getClassLoader().getResource("targets_5.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        for (int i = 0; i < 100; i++) {
            LandscapeStructure struct = ls.findSolution(2);
            Assert.assertTrue(struct != null);
            Assert.assertTrue(struct.getMeanPatchArea(0) >= 1200);
            Assert.assertTrue(struct.getMeanPatchArea(0) <= 1300);
        }
    }

    @Test
    public void useCase6_LargeLandscape() throws IOException, JsonException, FlsgenException, FactoryException {
        String path = getClass().getClassLoader().getResource("targets_6.json").getPath();
        LandscapeStructureSolver ls = LandscapeStructureSolver.readFromJSON(new FileReader(path));
        ls.build();
        ls.setRandomSearch();
        LandscapeStructure struct = ls.findSolution();
        Terrain terrain = new Terrain(ls.grid);
        terrain.generateDiamondSquare(0.4);
        LandscapeGenerator gen = new LandscapeGenerator(struct, Neighborhoods.FOUR_CONNECTED, Neighborhoods.VARIABLE_WIDTH_FOUR_CONNECTED(4, 8), terrain);
        Assert.assertTrue(gen.generateAlt(0.8, 1, 5));

    }

    @Test
    public void useCaseNonFocalPland() throws FlsgenException {
        RegularSquareGrid grid = new RegularSquareGrid(200, 200);
        LandscapeStructureSolver ls = new LandscapeStructureSolver(grid);
        ls.landscapeClass("Class 1", 1, 10, 0, 10000);
        ls.landscapeClass("Class 2", 1, 10, 0, 10000);
        ls.landscapeClass("Class 3", 1, 10, 0, 10000);
        ls.build();
        ls.setNonFocalLandscapeProportion(20, 25);
        for (int i = 0; i < 100; i++) {
            LandscapeStructure struct = ls.findSolution(2);
            Assert.assertTrue(struct != null);
            Assert.assertTrue(struct.getNonFocalLandscapeProportion() >= 20);
            Assert.assertTrue(struct.getNonFocalLandscapeProportion() <= 25);
        }
    }

    @Test public void useCaseFromScratch() throws FlsgenException {
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
            generator.generate(0.5, 40, 40);
        }
    }

    @Test public void useCaseFromScratchVariableBuffer() throws FlsgenException {
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
            INeighborhood d = Neighborhoods.VARIABLE_WIDTH_FOUR_CONNECTED(1, 10);
            LandscapeGenerator generator = new LandscapeGenerator(s, n, d, t);
            generator.generate(0.5, 40, 40);
        }
    }
}
