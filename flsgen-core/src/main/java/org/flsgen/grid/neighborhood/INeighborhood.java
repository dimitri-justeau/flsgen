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

import org.chocosolver.solver.Model;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.flsgen.grid.Grid;

/**
 * Interface specifying a neighborhood definition in a org.flsgen.grid.
 */
public interface INeighborhood<T extends Grid> {

    /**
     * @param grid A org.flsgen.grid.
     * @param i    The index of a cell.
     * @return The neighbors of i in the org.flsgen.grid.
     */
    int[] getNeighbors(T grid, int i);

    /**
     * @param grid    A org.flsgen.grid.
     * @param model   The GraphModel to be associated with the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The full spatial graph associated to the org.flsgen.grid. Full means that there will be one node for each cell.
     */
    default UndirectedGraph getFullGraph(T grid, Model model, SetType setType) {
        int nbCells = grid.getNbCells();
        UndirectedGraph g = new UndirectedGraph(model, nbCells, setType, false);
        for (int i = 0; i < nbCells; i++) {
            g.addNode(i);
            int[] neighbors = getNeighbors(grid, i);
            for (int ii : neighbors) {
                g.addEdge(i, ii);
            }
        }
        return g;
    }

    /**
     * @param grid    A org.flsgen.grid.
     * @param model   The GraphModel to be associated with the graph.
     * @param cells   The cells to be included in the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The partial graph associated to a subset of cells of the org.flsgen.grid.
     */
    default UndirectedGraph getPartialGraph(T grid, Model model, int[] cells, SetType setType) {
        int nbCells = grid.getNbCells();
//        UndirectedGraphIncrementalCC partialGraph = new UndirectedGraphIncrementalCC(model, nbCells, setType, false);
        UndirectedGraph partialGraph = GraphFactory.makeStoredUndirectedGraph(model, nbCells, setType, setType);
        for (int i : cells) {
            partialGraph.addNode(i);
        }
        for (int i : cells) {
            int[] neighbors = getNeighbors(grid, i);
            for (int ii : neighbors) {
                if (partialGraph.getNodes().contains(ii)) {
                    partialGraph.addEdge(i, ii);
                }
            }
        }
        return partialGraph;
    }

    default UndirectedGraph getPartialGraphUB(T grid, Model model, int[] cells, SetType setType) {
        return getPartialGraphUB(grid, model, cells, setType, false);
    }


    /**
     * @param grid    A org.flsgen.grid.
     * @param model   The GraphModel to be associated with the graph.
     * @param cells   The cells to be included in the graph.
     * @param setType The SetType to use for encoding the graph.
     * @return The partial graph associated to a subset of cells of the org.flsgen.grid.
     */
    default UndirectedGraph getPartialGraphUB(T grid, Model model, int[] cells, SetType setType, boolean decr) {
        int nbCells = grid.getNbCells();
        UndirectedGraph partialGraph;
//        if (decr) {
////            partialGraph = new UndirectedGraphDecrementalCC(model, nbCells, setType, false);
//
//        } else {
            partialGraph = new UndirectedGraph(model, nbCells, setType, false);
//            partialGraph = new UndirectedGraph()
//        }
        for (int i : cells) {
            partialGraph.addNode(i);
        }
        for (int i : cells) {
            int[] neighbors = getNeighbors(grid, i);
            for (int ii : neighbors) {
                if (partialGraph.getNodes().contains(ii)) {
                    partialGraph.addEdge(i, ii);
                }
            }
        }
//        if (decr) {
//            ((UndirectedGraphDecrementalCC) partialGraph).init();
//        }
        return partialGraph;
    }
//
//    default UndirectedGraph getPartialGraphUBFromLB(T org.flsgen.grid, Model model, int[] cells, SetType setType, UndirectedGraphIncrementalCC GLB) {
//        int nbCells = org.flsgen.grid.getNbCells();
//        UndirectedGraph partialGraph;
//        partialGraph = new UndirectedGraphDecrementalFromSubgraph(model, nbCells, setType, GLB, false);
//        for (int i : cells) {
//            partialGraph.addNode(i);
//        }
//        for (int i : cells) {
//            ISet neighbors = getNeighbors(org.flsgen.grid, i);
//            for (int ii : neighbors) {
//                if (partialGraph.getNodes().contains(ii)) {
//                    partialGraph.addEdge(i, ii);
//                }
//            }
//        }
//        return partialGraph;
//    }
}
