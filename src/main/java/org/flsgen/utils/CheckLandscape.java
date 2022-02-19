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

package org.flsgen.utils;

import org.chocosolver.util.graphOperations.connectivity.ConnectivityFinder;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.regular.square.RegularSquareGrid;
import org.flsgen.solver.LandscapeGenerator;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

public class CheckLandscape {

    public static final int checkNP(LandscapeGenerator generator, int classId, INeighborhood neighborhood) {
        UndirectedGraph g = getClassGraph(generator, classId, neighborhood);
        ConnectivityFinder cf = new ConnectivityFinder(g);
        cf.findAllCC();
        return cf.getNBCC();
    }

    public static final int[] checkAREA(LandscapeGenerator generator, int classId, INeighborhood neighborhood) {
        UndirectedGraph g = getClassGraph(generator, classId, neighborhood);
        ConnectivityFinder cf = new ConnectivityFinder(g);
        cf.findAllCC();
        int[] area = new int[cf.getNBCC()];
        for (int i = 0; i < area.length; i++) {
            area[i] = cf.getSizeCC()[i];
        }
        Arrays.sort(area);
        return area;
    }

    private static UndirectedGraph getClassGraph(LandscapeGenerator generator, int classId, INeighborhood neighborhood) {
        RegularSquareGrid grid = generator.getGrid();
        UndirectedGraph g = GraphFactory.makeUndirectedGraph(grid.getNbCells(), SetType.BIPARTITESET, SetType.BIPARTITESET);
        for (int i = 0; i < grid.getNbCells(); i++) {
            if (generator.getRasterGrid()[i] == classId) {
                g.addNode(i);
            }
        }
        for (int i : g.getNodes()) {
            for (int j : neighborhood.getNeighbors(grid, i)) {
                if (generator.getRasterGrid()[j] == classId) {
                    g.addEdge(i, j);
                }
            }
        }
        return g;
    }

    public static double getNodataValue(String rasterPath) throws IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        double noData = CoverageUtilities.getNoDataProperty(gridCov).getAsSingleValue();
        gridCov.dispose(true);
        reader.dispose();
        return noData;
    }

    public static int[] getDimensions(String rasterPath) throws IOException {
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nbRows = gridCov.getRenderedImage().getHeight();
        int nbCols = gridCov.getRenderedImage().getWidth();
        return new int[] {nbRows, nbCols};
    }

    public static int[] getNodataCells(String rasterPath) throws IOException {
        int noData = (int) getNodataValue(rasterPath);
        File file = new File(rasterPath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D gridCov = reader.read(null);
        int nbRows = gridCov.getRenderedImage().getHeight();
        int nbCols = gridCov.getRenderedImage().getWidth();
        int[] values = new int[nbRows * nbCols];
        gridCov.getRenderedImage().getData().getSamples(0, 0, nbCols, nbRows, 0, values);
        gridCov.dispose(true);
        reader.dispose();
        return IntStream.range(0, values.length).filter(i -> values[i] == noData).toArray();
    }
}
