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

package grid;

/**
 * Abstract base class for grids.
 */
public abstract class Grid {

    /**
     * @return The number of cells of the grid.
     */
    public abstract int getNbCells();

    /**
     * @return The cartesian coordinates of the sites (center or centroid).
     */
    public abstract double[][] getCartesianCoordinates();

    /**
     * @return The cartesian coordinates of a site (center or centroid).
     */
    public abstract double[] getCartesianCoordinates(int site);
}
