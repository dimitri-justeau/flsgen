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
import grid.regular.square.PartialRegularSquareGrid;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

import java.util.stream.IntStream;

/**
 * The four-connected neighborhood in a partial regular square grid.
 */
public class PartialFourConnected<T extends PartialRegularSquareGrid> implements INeighborhood<T> {

    public int[] getNeighbors(T grid, int partialIdx) {
        int idx = grid.getCompleteIndex(partialIdx);
        int nbCols = grid.getNbCols();
        int nbRows = grid.getNbRows();
        int left = idx % nbCols != 0 ? grid.getPartialIndex(idx - 1) : -1;
        int right = (idx + 1) % nbCols != 0 ? grid.getPartialIndex(idx + 1) : -1;
        int top = idx >= nbCols ? grid.getPartialIndex(idx - nbCols) : -1;
        int bottom = idx < nbCols * (nbRows - 1) ? grid.getPartialIndex(idx + nbCols) : -1;
        return IntStream.of(left, right, top, bottom).filter(x -> x >= 0).toArray();
    }

}
