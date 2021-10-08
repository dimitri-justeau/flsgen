package solver;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

public class TestSquare {

        int LANDSCAPE_SIZE = 200 * 200;
        int NB_LANDSCAPE_CLASSES = 4;
        int[] MIN_NB_PATCHES = new int[] {5, 2, 1, 2};
        int[] MAX_NB_PATCHES = new int[] {30, 25, 15, 9};
        int[] MIN_PATCH_SIZE = new int[] {300, 200, 800, 710};
        int[] MAX_PATCH_SIZE = new int[] {4000, 1200, 1200, 1200};
        int[] MIN_NET_PRODUCT = new int[] {300 * LANDSCAPE_SIZE, 0, 0, 50 * LANDSCAPE_SIZE};
        int[] MAX_NET_PRODUCT = new int[] {800 * LANDSCAPE_SIZE, LANDSCAPE_SIZE * LANDSCAPE_SIZE, LANDSCAPE_SIZE * LANDSCAPE_SIZE, 200 * LANDSCAPE_SIZE};

        Model model;
        IntVar[][] patchSizes;
        IntVar[][] squaredPatchSizes;
        IntVar[] nbPatches;
        IntVar[] sumOfPatchSizes;
        IntVar[] sumOfSquaredPatchSized;
        IntVar totalSum;

        public TestSquare() {
            model = new Model();
            patchSizes = new IntVar[NB_LANDSCAPE_CLASSES][];
            squaredPatchSizes = new IntVar[NB_LANDSCAPE_CLASSES][];
            nbPatches = new IntVar[NB_LANDSCAPE_CLASSES];
            sumOfPatchSizes = new IntVar[NB_LANDSCAPE_CLASSES];
            sumOfSquaredPatchSized = new IntVar[NB_LANDSCAPE_CLASSES];
            for (int c = 0; c < NB_LANDSCAPE_CLASSES; c++) {
                patchSizes[c] = model.intVarArray(MAX_NB_PATCHES[c], 0, MAX_PATCH_SIZE[c]);
                IntVar limit = model.intVar(0, MAX_NB_PATCHES[c] - MIN_NB_PATCHES[c]);
                nbPatches[c] = model.intVar(MIN_NB_PATCHES[c], MAX_NB_PATCHES[c]);
                model.arithm(nbPatches[c], "=", model.intVar(MAX_NB_PATCHES[c]), "-", limit).post();
                for (int i = 0; i < MAX_NB_PATCHES[c] - 1; i++) {
                    model.ifThen(
                            model.arithm(patchSizes[c][i], "!=", 0).reify(),
                            model.arithm(patchSizes[c][i], ">=", MIN_PATCH_SIZE[c])
                    );
                    model.arithm(patchSizes[c][i], "<=", patchSizes[c][i + 1]).post();
                }
                sumOfPatchSizes[c] = model.intVar(MIN_PATCH_SIZE[c] * MIN_NB_PATCHES[c], MAX_PATCH_SIZE[c] * MAX_NB_PATCHES[c]);
                model.sum(patchSizes[c], "=", sumOfPatchSizes[c]).post();
                squaredPatchSizes[c] = model.intVarArray(MAX_NB_PATCHES[c], 0, MAX_PATCH_SIZE[c] * MAX_PATCH_SIZE[c]);
                for (int i = 0; i < MAX_NB_PATCHES[c]; i++) {
//                model.times(patchSizes[c][i], patchSizes[c][i], squaredPatchSizes[c][i]).post();
                    model.square(squaredPatchSizes[c][i], patchSizes[c][i]).post();
                }
                sumOfSquaredPatchSized[c] = model.intVar(MIN_NET_PRODUCT[c], MAX_NET_PRODUCT[c]);
                model.sum(squaredPatchSizes[c], "=", sumOfSquaredPatchSized[c]).post();
            }
            totalSum = model.intVar(0, LANDSCAPE_SIZE);
            model.sum(sumOfPatchSizes, "=", totalSum).post();
        }

        public static void main(String[] args) {
            TestSquare test = new TestSquare();
            test.model.getSolver().showStatistics();
            test.model.getSolver().setSearch(Search.inputOrderLBSearch(test.model.retrieveIntVars(true)));
            test.model.getSolver().findOptimalSolution(test.sumOfSquaredPatchSized[3], true);
//            for (int i = 0; i < 10000; i++) {
//                test.model.getSolver().solve();
//            }
        }
}
