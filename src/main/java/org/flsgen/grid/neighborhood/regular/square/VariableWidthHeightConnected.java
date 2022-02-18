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

package org.flsgen.grid.neighborhood.regular.square;

import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.neighborhood.Neighborhoods;
import org.flsgen.grid.regular.square.RegularSquareGrid;

import java.util.Random;

/**
 * variable-wide four-connected neighborhood in a regular square org.flsgen.grid.
 */
public class VariableWidthHeightConnected<T extends RegularSquareGrid> implements INeighborhood<T> {

    private int minWidth;
    private int maxWidth;

    public VariableWidthHeightConnected(int minWidth, int maxWidth) throws FlsgenException {
        if (maxWidth < minWidth) {
            throw new FlsgenException("Min width must be lower than max width");
        }
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
    }

    public int[] getNeighbors(T grid, int i) {
        HeightConnected height = Neighborhoods.HEIGHT_CONNECTED;
        int[] heightNeigh = height.getNeighbors(grid, i);
        ISet neighbors = SetFactory.makeRangeSet();
        ISet next = SetFactory.makeRangeSet();
        for (int n : heightNeigh) {
            neighbors.add(n);
            next.add(n);
        }
        Random rd = new Random(System.currentTimeMillis());
        int v = rd.nextInt(maxWidth - minWidth + 1);
        for (int j = 1; j < minWidth + v; j++) {
            int[] nextA = next.toArray();
            next.clear();
            for (int n : nextA) {
                if (j < minWidth || rd.nextBoolean()) {
                    for (int neigh : height.getNeighbors(grid, n)) {
                        neighbors.add(neigh);
                        next.add(neigh);
                    }
                }
            }
        }
        return neighbors.toArray();
    }

}
