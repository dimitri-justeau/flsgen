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

package flsgen.solver;

import org.flsgen.RasterUtils;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.solver.Terrain;
import org.opengis.referencing.FactoryException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FractalTerrainTest {

    @Test
    public void generateTerrain() throws IOException, FactoryException {
        RegularSquareGrid grid = new RegularSquareGrid(200, 200);
        Terrain t = new Terrain(grid);
        t.generateDiamondSquare(0.4);
        Path temp = Files.createTempFile("terrain", ".tif");
        RasterUtils.exportDoubleRaster(t.getData(), t.getGrid(), 0, 0, 0.01, "EPSG:4326", temp.toString());
        Files.delete(temp);
    }
}
