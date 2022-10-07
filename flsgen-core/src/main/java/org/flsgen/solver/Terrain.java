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
import org.flsgen.grid.regular.square.RegularSquareGrid;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

public class Terrain {

    protected RegularSquareGrid grid;
    protected double[] dem;

    public Terrain(RegularSquareGrid grid) {
        this.grid = grid;
    }

    public void loadFromData(double[] data) throws IOException, FlsgenException {
        if (grid.getNbRows() * grid.getNbCols() != data.length) {
            throw new FlsgenException("Input terrain raster must have the same dimensions as the landscape to generate");
        }
        dem = data;
    }

    public void generateDiamondSquare(double roughnessFactor) {
        // Get the smallest power of 2 greater than of equal to the largest landscape dimension
        int h = Math.max(grid.getNbRows(), grid.getNbCols());
        double pos = Math.ceil(Math.log(h) / Math.log(2));
        h = (int) (Math.pow(2, pos) + 1);
        double[][] terrain = new double[h][h];
        // Init edges
        terrain[0][0] = randomDouble(-h, h);
        terrain[0][h - 1] = randomDouble(-h, h);
        terrain[h - 1][0] = randomDouble(-h, h);
        terrain[h - 1][h - 1] = randomDouble(-h, h);
        double r = h * Math.pow(2, - 2 * roughnessFactor);
        // Fill matrix
        int i = h - 1;
        while (i > 1) {
            int id = i / 2;
            for (int x = id; x < h; x += i) { // Diamond
                for (int y = id; y < h; y += i) {
                    double mean = (terrain[x - id][y - id] + terrain[x - id][y + id] + terrain[x + id][y + id] + terrain[x + id][y - id]) / 4;
                    terrain[x][y] = mean + randomDouble(-r, r);
                }
            }
            int offset = 0;
            for (int x = 0; x < h; x += id) { // Square
                if (offset == 0) {
                    offset = id;
                } else {
                    offset = 0;
                }
                for (int y = offset; y < h; y += i) {
                    double sum = 0;
                    int n = 0;
                    if (x >= id) {
                        sum += terrain[x - id][y];
                        n++;
                    }
                    if (x + id < h) {
                        sum += terrain[x + id][y];
                        n++;
                    }
                    if (y >= id) {
                        sum += terrain[x][y - id];
                        n++;
                    }
                    if (y + id < h) {
                        sum += terrain[x][y + id];
                        n++;
                    }
                    terrain[x][y] = sum / n + randomDouble(-r, r);
                }
            }
            i = id;
            r *= Math.pow(2, - 2 * roughnessFactor);
        }
        dem = IntStream.range(0, grid.getNbCells())
                .mapToDouble(v -> {
                    int[] c = grid.getCoordinatesFromIndex(v);
                    return terrain[c[0]][c[1]];
                }).toArray();
    }

    public static double randomDouble(double min, double max) {
        return new Random().nextDouble() * (max - min) + min;
    }

    public double[] getData() {
        return dem;
    }

    public RegularSquareGrid getGrid() {
        return grid;
    }
}
