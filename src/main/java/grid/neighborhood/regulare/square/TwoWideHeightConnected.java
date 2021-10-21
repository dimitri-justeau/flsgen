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

package grid.neighborhood.regulare.square;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2-wide height-connected neighborhood in a regular square grid.
 */
public class TwoWideHeightConnected<T extends RegularSquareGrid> implements INeighborhood<T> {

    public int[] getNeighbors(T grid, int i) {
        HeightConnected height = Neighborhoods.HEIGHT_CONNECTED;
        int[] heightneigh = height.getNeighbors(grid, i);
        List<Integer> neighbors = new ArrayList<>();
        for (int neigh : heightneigh) {
            neighbors.add(neigh);
            for (int nneigh : height.getNeighbors(grid, neigh)) {
                neighbors.add(nneigh);
            }
        }
        return neighbors.stream().mapToInt(x -> x).toArray();
    }

}
