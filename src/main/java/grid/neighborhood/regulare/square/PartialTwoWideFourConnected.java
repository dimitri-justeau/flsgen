/*
 * Copyright (c) 2018, Dimitri Justeau-Allaire
 *
 * CIRAD, UMR AMAP, F-34398 Montpellier, France
 * Institut Agronomique neo-Caledonien (IAC), 98800 Noumea, New Caledonia
 * AMAP, Univ Montpellier, CIRAD, CNRS, INRA, IRD, Montpellier, France
 *
 * This file is part of Choco-reserve.
 *
 * Choco-reserve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Choco-reserve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Choco-reserve.  If not, see <https://www.gnu.org/licenses/>.
 */

package grid.neighborhood.regulare.square;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.PartialRegularSquareGrid;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

/**
 * The 2-wide four-connected neighborhood in a regular square grid.
 */
public class PartialTwoWideFourConnected<T extends PartialRegularSquareGrid> implements INeighborhood<T> {

    public ISet getNeighbors(T grid, int i) {
        PartialFourConnected four = Neighborhoods.PARTIAL_FOUR_CONNECTED;
        ISet neighbors = SetFactory.makeBitSet(0);
        ISet fourneigh = four.getNeighbors(grid, i);
        for (int neigh : fourneigh) {
//            neighbors.add(neigh);
            for (int nneigh : four.getNeighbors(grid, neigh)) {
                if (nneigh != i) {
                    neighbors.add(nneigh);
                }
            }
        }
        return neighbors;
    }

}
