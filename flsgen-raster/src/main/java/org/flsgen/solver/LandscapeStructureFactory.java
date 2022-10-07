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
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.flsgen.RasterUtils;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.neighborhood.INeighborhood;

import java.io.*;

public class LandscapeStructureFactory {

    public static LandscapeStructure fromRaster(String rasterPath, int[] focalClasses,
                                                INeighborhood neighborhood) throws IOException {
        int[] values = RasterUtils.loadIntDataFromRaster(rasterPath);
        int[] dimensions = RasterUtils.getDimensions(rasterPath);
        int noDataValue = (int) RasterUtils.getNodataValue(rasterPath);
        return LandscapeStructure.fromRasterData(
                values, dimensions[0], dimensions[1], noDataValue,
                focalClasses, neighborhood, rasterPath
        );
    }

    public static LandscapeStructure readFromJSON(String json) throws JsonException, IOException, FlsgenException {
        JsonObject targets = (JsonObject) Jsoner.deserialize(new StringReader(json));
        // Get map dimensions
        if (targets.containsKey("maskRasterPath")) {
            String maskRasterPath = targets.get("maskRasterPath").toString();
            int[] dimensions = RasterUtils.getDimensions(maskRasterPath);
            int[] noDataCells = RasterUtils.getNodataCells(maskRasterPath);
            return LandscapeStructure.fromJSON(json, dimensions[0], dimensions[1], noDataCells);
        } else {
            return LandscapeStructure.fromJSON(json, 0, 0, new int[] {});
        }
    }
}
