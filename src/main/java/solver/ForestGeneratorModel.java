package solver;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;

public class ForestGeneratorModel {

    private int width;
    private int height;
    private RegularSquareGrid grid;
    private INeighborhood neighborhood;
    public Model model;
    public SetVar pixelSet;
    public BoolVar[] pixelBools;
    public UndirectedGraphVar graph;

    public ForestGeneratorModel(int width, int height) {
        this.width = width;
        this.height = height;
        this.neighborhood = Neighborhoods.FOUR_CONNECTED;
        this.grid = new RegularSquareGrid(height, width);
        this.model = new Model("Forest generator");
        // Construct graph
        this.graph = model.nodeInducedGraphVar(
                "graph",
                GraphFactory.makeStoredUndirectedGraph(model, width * height, SetType.BITSET, SetType.BITSET),
                neighborhood.getFullGraph(grid, model, SetType.BITSET)
        );
//        this.pixelSet = model.graphNodeSetView(graph);
        this.pixelBools = model.boolVarArray(width * height);
        model.nodesChanneling(graph, pixelBools).post();
    }
}
