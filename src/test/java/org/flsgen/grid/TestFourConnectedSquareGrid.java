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

package org.flsgen.grid;

import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Test case for FourConnectedSquareGrid.
 * Tests are on a 5x5 grid :
 * -------------------
 * | 0 | 1 | 2 | 3 | 4|
 * -------------------
 * | 5 | 6 | 7 | 8 | 9|
 * -------------------
 * | 10| 11| 12| 13| 14|
 * -------------------
 * | 15| 16| 17| 18| 19|
 * -------------------
 * | 20| 21| 22| 23| 24|
 * -------------------
 */
public class TestFourConnectedSquareGrid {

    @Test
    public void testGetNbCells() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        Assert.assertEquals(grid.getNbCells(), 25);
    }

    @Test
    public void testCoordinatesConversions() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        Assert.assertEquals(5, grid.getIndexFromCoordinates(1, 0));
        Assert.assertTrue(Arrays.equals(new int[]{1, 0}, grid.getCoordinatesFromIndex(5)));
        Assert.assertEquals(18, grid.getIndexFromCoordinates(3, 3));
        Assert.assertTrue(Arrays.equals(new int[]{3, 3}, grid.getCoordinatesFromIndex(18)));
        Assert.assertEquals(11, grid.getIndexFromCoordinates(2, 1));
        Assert.assertTrue(Arrays.equals(new int[]{2, 1}, grid.getCoordinatesFromIndex(11)));
    }

    @Test
    public void testGetNeighbors() {
        RegularSquareGrid grid = new RegularSquareGrid(5, 5);
        INeighborhood neigh = Neighborhoods.FOUR_CONNECTED;
        // Cell in the middle: 12
        int[] neighbors = neigh.getNeighbors(grid, 12);
        Arrays.sort(neighbors);
        int[] expected = new int[]{7, 11, 13, 17};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 0
        neighbors = neigh.getNeighbors(grid, 0);
        Arrays.sort(neighbors);
        expected = new int[]{1, 5};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 15
        neighbors = neigh.getNeighbors(grid, 15);
        Arrays.sort(neighbors);
        expected = new int[]{10, 16, 20};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 9
        neighbors = neigh.getNeighbors(grid, 9);
        Arrays.sort(neighbors);
        expected = new int[]{4, 8, 14};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
        // Cell 24
        neighbors = neigh.getNeighbors(grid, 24);
        Arrays.sort(neighbors);
        expected = new int[]{19, 23};
        Assert.assertTrue(Arrays.equals(neighbors, expected));
    }
}
