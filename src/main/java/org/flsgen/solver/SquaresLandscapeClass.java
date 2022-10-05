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

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.regular.square.RegularSquareGrid;


/**
 * Class representing a landscape class that must be present in the generated landscape
 */
public class SquaresLandscapeClass extends LandscapeClass{

    protected IntVar[] patchWidth;

    public SquaresLandscapeClass(String name, int index, RegularSquareGrid grid, Model model, int minNbPatches, int maxNbPatches, int minPatchSize, int maxPatchSize) throws FlsgenException {
        super(name, index, grid, model, minNbPatches, maxNbPatches, minPatchSize, maxPatchSize);
        this.patchWidth = model.intVarArray(maxNbPatches, 0, (int) Math.floor(Math.sqrt(maxPatchSize)), false);
        for (int i = 0; i < this.patchWidth.length; i++) {
            model.square(this.patchSizes[i], this.patchWidth[i]).post();
        }
    }

}
