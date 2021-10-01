package solver;

import grid.regular.square.RegularSquareGrid;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.json.simple.parser.ParseException;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.Arrays;

public class TestSevClasses {

    public static void main(String[] args) throws FactoryException, IOException, ParseException {
        LandscapeStructureSolver l = LandscapeStructureSolver.readFromJSON("/home/djusteau/Documents/testPolyomino/struct_test.json");
        System.out.println(l.toJSON());
//        RegularSquareGrid grid = new RegularSquareGrid(200, 200);
//        LandscapeStructureSolver structModel = new LandscapeStructureSolver(grid);
//        LandscapeClass ls1 = structModel.landscapeClass(
//                "Class 1",
//                5, 10, 10, 1000
//        );
//        ls1.setMesh(199, 200);
//        LandscapeClass ls2 = structModel.landscapeClass(
//                "Class 2",
//                5, 10, 10, 1000
//        );
//        ls2.setMesh(199, 1000);
//        LandscapeClass ls3 = structModel.landscapeClass(
//                "Class 2",
//                5, 10, 10, 1000
//        );
//        ls3.setTotalSize(100 * 100, 100 * 100);
//        structModel.build();
//        Model model = structModel.model;
//        Solver solver = model.getSolver();
//        solver.showStatistics();
////        solver.findOptimalSolution(ls1.squaredSum, true);
//        if (solver.solve()) {
//            for (LandscapeClass ls : structModel.landscapeClasses) {
//                System.out.println(" ---- ");
//                System.out.println("LS " + ls.name);
//                System.out.println("    Total size " + ls.sum.getValue());
//                System.out.println("    Nb Patches " + ls.nbPatches.getValue());
//                if (ls.mesh_lb > -1) {
//                    System.out.println("    MESH " + (ls.squaredSum.getValue() / grid.getNbCells()));
//                }
//                int[] sizes = Arrays.stream(ls.patchSizes)
//                        .mapToInt(v -> v.getValue())
//                        .filter(i -> i > 0)
//                        .toArray();
//                System.out.println("    Patch sizes " + Arrays.toString(sizes));
//                System.out.println(" ---- ");
//            }
//            System.out.println("Total classes size = " + structModel.totalSum.getValue());
//            System.out.println("Nodata class size = " + (structModel.nbCells - structModel.totalSum.getValue()));
//        }
    }
}
