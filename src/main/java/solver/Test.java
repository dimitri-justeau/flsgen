package solver;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.stream.IntStream;

public class Test {

    public static void main(String[] args) {
        Model model = new Model();
        SetVar s1 = model.setVar(new int[] {}, IntStream.range(1, 10).map(i -> i * i).toArray());
        SetVar s2 = model.setVar(new int[] {}, IntStream.range(1, 10).map(i -> i * i).toArray());
        IntVar sum1 = model.intVar(0, IntStream.range(1, 10).map(i -> i * i).sum());
        IntVar sum2 = model.intVar(0, IntStream.range(1, 10).map(i -> i * i).sum());
        model.sum(s1, sum1).post();
        model.sum(s2, sum2).post();
        model.arithm(sum1, "=", sum2).post();
        model.arithm(sum1, ">", 0).post();
        model.arithm(sum2, ">", 0).post();
        model.allDifferent(s1, s2).post();
        Solver solver = model.getSolver();
//        solver.showStatistics();
//        while (solver.solve()) {
//            System.out.println("SUM 1 = " + sum1.getValue() + " - " + s1.getValue());
//            System.out.println("SUM 2 = " + sum2.getValue() + " - " + s2.getValue());
//        }

        // SUM 1 = 122 - {9, 49, 64}
        // SUM 2 = 122 - {16, 25, 81}

        Model model2 = new Model();
        int n = 50;
        RegularSquareGrid grid = new RegularSquareGrid(n, n);
        INeighborhood neigh = Neighborhoods.FOUR_CONNECTED;

        UndirectedGraphVar g1 = model2.nodeInducedGraphVar("g1",
                GraphFactory.makeStoredUndirectedGraph(model2, n * n, SetType.BIPARTITESET, SetType.BIPARTITESET),
                neigh.getFullGraph(grid, model2, SetType.BIPARTITESET)
        );
        UndirectedGraphVar g2 = model2.nodeInducedGraphVar("g1",
                GraphFactory.makeStoredUndirectedGraph(model2, n * n, SetType.BIPARTITESET, SetType.BIPARTITESET),
                neigh.getFullGraph(grid, model2, SetType.BIPARTITESET)
        );
        UndirectedGraphVar g3 = model2.nodeInducedGraphVar("g1",
                GraphFactory.makeStoredUndirectedGraph(model2, n * n, SetType.BIPARTITESET, SetType.BIPARTITESET),
                neigh.getFullGraph(grid, model2, SetType.BIPARTITESET)
        );

        model2.nbConnectedComponents(g1, model2.intVar(1)).post();
        model2.nbConnectedComponents(g2, model2.intVar(1)).post();
        model2.nbConnectedComponents(g3, model2.intVar(1)).post();

        model2.nbNodes(g1, model2.intVar(9)).post();
        model2.nbNodes(g2, model2.intVar(49)).post();
        model2.nbNodes(g3, model2.intVar(64)).post();

        SetVar ns1 = model2.graphNodeSetView(g1);
        SetVar ns2 = model2.graphNodeSetView(g2);
        SetVar ns3 = model2.graphNodeSetView(g3);

        model2.allDisjoint(ns1, ns2, ns3).post();

        Solver solver2 = model2.getSolver();
        solver2.showStatistics();
        solver2.solve();
    }
}
