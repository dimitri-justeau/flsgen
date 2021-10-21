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
import grid.regular.square.RegularSquareGrid;

import java.util.stream.IntStream;

/**
 * The height-connected neighborhood in a regular square grid.
 */
public class HeightConnected<T extends RegularSquareGrid> implements INeighborhood<T> {

    public int[] getNeighbors(T grid, int i) {
        int nbCols = grid.getNbCols();
        int nbRows = grid.getNbRows();
        int left = i % nbCols != 0 ? i - 1 : -1;
        int right = (i + 1) % nbCols != 0 ? i + 1 : -1;
        int top = i >= nbCols ? i - nbCols : -1;
        int bottom = i < nbCols * (nbRows - 1) ? i + nbCols : -1;
        int leftTop = (i % nbCols != 0) && i >= nbCols ? i - nbCols - 1 : -1;
        int rightTop = ((i + 1) % nbCols != 0) && i >= nbCols ? i - nbCols + 1 : -1;
        int leftBottom = (i < nbCols * (nbRows - 1)) && (i % nbCols != 0) ? i + nbCols - 1 : -1;
        int rightBottom = (i < nbCols * (nbRows - 1)) && ((i + 1) % nbCols != 0) ? i + nbCols + 1 : -1;
        return IntStream.of(left, right, top, bottom, leftTop, rightTop, leftBottom, rightBottom)
                .filter(x -> x >=0).toArray();
    }

}
