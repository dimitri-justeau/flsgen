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

package org.flsgen.grid.neighborhood;

import org.flsgen.exception.FlsgenException;
import org.flsgen.grid.neighborhood.regular.square.*;

/**
 * Utility class for accessing neighborhoods.
 */
public class Neighborhoods {

    public final static FourConnected FOUR_CONNECTED = new FourConnected();
    public final static HeightConnected HEIGHT_CONNECTED = new HeightConnected();
    public final static PartialFourConnected PARTIAL_FOUR_CONNECTED = new PartialFourConnected();
    public final static PartialHeightConnected PARTIAL_HEIGHT_CONNECTED = new PartialHeightConnected();
    public final static TwoWideHeightConnected TWO_WIDE_HEIGHT_CONNECTED = new TwoWideHeightConnected();
    public final static TwoWideFourConnected TWO_WIDE_FOUR_CONNECTED = new TwoWideFourConnected();
    public final static PartialTwoWideFourConnected PARTIAL_TWO_WIDE_FOUR_CONNECTED = new PartialTwoWideFourConnected();
    public final static PartialTwoWideHeightConnected PARTIAL_TWO_WIDE_HEIGHT_CONNECTED = new PartialTwoWideHeightConnected();

    public final static KWideFourConnected K_WIDE_FOUR_CONNECTED(int k) {
        return new KWideFourConnected(k);
    }

    public final static PartialKWideFourConnected PARTIAL_K_WIDE_FOUR_CONNECTED(int k) {
        return new PartialKWideFourConnected(k);
    }

    public final static VariableWidthFourConnected VARIABLE_WIDTH_FOUR_CONNECTED(int minWidth, int maxWidth) throws FlsgenException {
        return new VariableWidthFourConnected(minWidth, maxWidth);
    }

    public final static PartialVariableWidthFourConnected PARTIAL_VARIABLE_WIDTH_FOUR_CONNECTED(int minWidth, int maxWidth) throws FlsgenException {
        return new PartialVariableWidthFourConnected(minWidth, maxWidth);
    }

    public final static KWideHeightConnected K_WIDE_HEIGHT_CONNECTED(int k) {
        return new KWideHeightConnected(k);
    }

    public final static PartialKWideHeightConnected PARTIAL_K_WIDE_HEIGHT_CONNECTED(int k) {
        return new PartialKWideHeightConnected(k);
    }

    public final static VariableWidthHeightConnected VARIABLE_WIDTH_HEIGHT_CONNECTED(int minWidth, int maxWidth) throws FlsgenException {
        return new VariableWidthHeightConnected(minWidth, maxWidth);
    }

    public final static PartialVariableWidthHeightConnected PARTIAL_VARIABLE_WIDTH_HEIGHT_CONNECTED(int minWidth, int maxWidth) throws FlsgenException {
        return new PartialVariableWidthHeightConnected(minWidth, maxWidth);
    }
}
