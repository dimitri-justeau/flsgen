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

import org.flsgen.grid.neighborhood.INeighborhood;
import org.flsgen.grid.regular.square.RegularSquareGrid;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Class containing algorithms to find all connected components by performing one dfs
 * it uses Tarjan algorithm in a non recursive way and can be performed in O(M+N) time c.f. Gondrand Minoux
 * <p>
 *
 * // ADAPTED FROM choco-graph by Dimitri Justeau-Allaire, Jean-Guillaume Fages is the initial author.
 * @author Jean-Guillaume Fages
 * @author Dimitri Justeau.
 */
public class RasterConnectivityFinder {

    //***********************************************************************************
    // CONNECTED COMPONENTS ONLY
    //***********************************************************************************

    private int n;
    private int[] values;
    private int[][] neighs;
    private long npro;
    private int[] CCFirstNode, CCNextNode, nodeCC, parent, fifo, sizeCC;
    private int nbCC, sizeMinCC, sizeMaxCC;
    Map<Integer, Integer> graphIdxToRasterIdx;
    Map<Integer, Integer> rasterIdxToGraphIdx;
    private int classValue;

    /**
     * Create an object that can compute Connected Components (CC) of a graph g
     * Can also quickly tell whether g is biconnected or not (only for undirected graph)
     */
    public RasterConnectivityFinder(int nbRows, int nbCols, int[] values, int classValue, INeighborhood neighborhood) {
        this.n = 0;
        this.values = values;
        this.classValue = classValue;
        RegularSquareGrid grid = new RegularSquareGrid(nbRows, nbCols);
        int currentIdx = 0;
        graphIdxToRasterIdx = new HashMap<>();
        rasterIdxToGraphIdx = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == classValue) {
                n++;
                rasterIdxToGraphIdx.put(i, currentIdx);
                graphIdxToRasterIdx.put(currentIdx, i);
                currentIdx++;
            }
        }
        this.neighs = new int[n][];
        currentIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == classValue) {
                neighs[currentIdx] = IntStream.of(neighborhood.getNeighbors(grid, i))
                        .filter(v -> values[v] == classValue)
                        .map(v -> rasterIdxToGraphIdx.get(v)).toArray();
                currentIdx++;
            }
        }
        parent = new int[n];
        fifo = new int[n];
    }

    public int getNbNodes() {
        return n;
    }

    /**
     * get the number of CC in g
     * Beware you should call method findAllCC() first
     *
     * @return nbCC the number of CC in g
     */
    public int getNBCC() {
        return nbCC;
    }

    /**
     * Get the size (number of nodes) of the smallest CC in g.
     * Beware you should call method findAllCC() first.
     *
     * @return sizeMinCC the size of the smallest CC in g.
     */
    public int getSizeMinCC() {
        return sizeMinCC;
    }

    /**
     * Get the size (number of nodes) of the largest CC in g.
     * Beware you should call method findAllCC() first.
     *
     * @return sizeMaxCC the size of the largest CC in g.
     */
    public int getSizeMaxCC() {
        return sizeMaxCC;
    }

    /**
     * @return The size of the CCs as an int array.
     */
    public int[] getSizeCC() {
        return sizeCC;
    }

    public int[] getCCFirstNode() {
        return CCFirstNode;
    }

    public int[] getCCNextNode() {
        return CCNextNode;
    }

    public int[] getNodeCC() {
        return nodeCC;
    }

    /**
     * Find all connected components of graph by performing one dfs
     * Complexity : O(M+N) light and fast in practice
     */
    public void findAllCC() {
        if (nodeCC == null) {
            CCFirstNode = new int[n];
            CCNextNode = new int[n];
            nodeCC = new int[n];
            sizeCC = new int[n];
        }
        sizeMinCC = 0;
        sizeMaxCC = 0;
        npro = 0;
        for (int i = 0; i < n; i++) {
            parent[i] = -1;
        }
        for (int i = 0; i < CCFirstNode.length; i++) {
            CCFirstNode[i] = -1;
            sizeCC[i] = -1;
        }
        int cc = 0;
        for (int i = 0; i < n; i++) {
            if (parent[i] == -1) {
                findCC(i, cc);
                if (sizeMinCC == 0 || sizeMinCC > sizeCC[cc]) {
                    sizeMinCC = sizeCC[cc];
                }
                if (sizeMaxCC < sizeCC[cc]) {
                    sizeMaxCC = sizeCC[cc];
                }
                npro += Long.valueOf(sizeCC[cc]) * Long.valueOf(sizeCC[cc]);
                cc++;
            }
        }
        nbCC = cc;
    }

    private void findCC(int start, int cc) {
        int first = 0;
        int last = 0;
        int size = 1;
        fifo[last++] = start;
        parent[start] = start;
        add(start, cc);
        while (first < last) {
            int i = fifo[first++];
            for (int j : neighs[i]) {
                if (parent[j] == -1) {
                    parent[j] = i;
                    add(j, cc);
                    size++;
                    fifo[last++] = j;
                }
            }
        }
        sizeCC[cc] = size;
    }

    private void add(int node, int cc) {
        nodeCC[node] = cc;
        CCNextNode[node] = CCFirstNode[cc];
        CCFirstNode[cc] = node;
    }

    public int[] getCC(int ccIndex) {
        int[] cc = new int[sizeCC[ccIndex]];
        int j = 0;
        int i = getCCFirstNode()[ccIndex];
        while (i != -1) {
            cc[j++] = graphIdxToRasterIdx.get(i);
            i = getCCNextNode()[i];
        }
        return cc;
    }

    public long getNpro() {
        return npro;
    }
}
