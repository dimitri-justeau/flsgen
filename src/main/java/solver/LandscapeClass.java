package solver;

import grid.regular.square.RegularSquareGrid;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import solver.choco.PropSumOfSquares;

import java.util.Arrays;

/**
 * Class representing a landscape class that must be present in the generated landscape
 */
public class LandscapeClass {

    public String name;
    public int index;
    public RegularSquareGrid grid;
    public int landscapeSize;

    // Bounds for number of patches
    public int minNbPatches;
    public int maxNbPatches;

    // Bounds for size of patches
    public int minPatchSize;
    public int maxPatchSize;

    // MESH bounds
    public double mesh_lb;
    public double mesh_ub;

    // Choco variables
    public Model model;
    public IntVar[] patchSizes;
    public IntVar[] squaredPatchSizes;
    public IntVar sum;
    public IntVar sumOfSquares;
    public IntVar nbPatches;

    public LandscapeClass(String name, int index, RegularSquareGrid grid, Model model, int minNbPatches, int maxNbPatches, int minPatchSize, int maxPatchSize) {
        this.name = name;
        this.index = index;
        this.model = model;
        this.grid = grid;
        this.landscapeSize = grid.getNbCells();
        if (maxPatchSize < minPatchSize) {
            throw  new ValueException("Max patch area must be greater than or equal to min patch area");
        }
        if (maxNbPatches < minNbPatches) {
            throw  new ValueException("Max patch area must be greater than or equal to min patch area");
        }
        this.minNbPatches = minNbPatches;
        this.maxNbPatches = maxNbPatches;
        this.minPatchSize = minPatchSize;
        this.maxPatchSize = maxPatchSize;
        this.mesh_lb = -1;
        this.mesh_ub = -1;
        // Init patch size choco variables
        this.patchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize);
        IntVar limit = model.intVar(0, maxNbPatches - minNbPatches);
        model.count(0, patchSizes, limit).post();
        this.nbPatches = model.intVar(minNbPatches, maxNbPatches);
        model.arithm(nbPatches, "=", model.intVar(maxNbPatches), "-", limit).post();
        for (int i = 0; i < patchSizes.length - 1; i++) {
            model.ifThen(model.arithm(patchSizes[i], "!=", 0).reify(), model.arithm(patchSizes[i], ">=", minPatchSize));
            model.arithm(patchSizes[i], "<=", patchSizes[i + 1]).post();
        }
        this.sum = model.intVar(minPatchSize * minNbPatches, maxPatchSize * maxNbPatches);
        model.sum(patchSizes, "=", sum).post();
    }

//    public void initNetProduct(int netProductLB, int netProductUB) {
//        this.squaredPatchSizes = model.intVarArray(maxNbPatches, 0, maxPatchSize * maxPatchSize);
//        for (int i = 0; i < maxNbPatches; i++) {
//            model.times(patchSizes[i], patchSizes[i], squaredPatchSizes[i]).post();
//        }
//        this.sumOfSquares = model.intVar(netProductLB, netProductUB);
//        model.sum(squaredPatchSizes, "=", sumOfSquares).post();
//        model.post(new Constraint("sumOfSquares", new PropSumOfSquares(patchSizes, netProductLB, netProductUB)));
//    }

    ///--- USER TARGETS ---///

     // CA - Total class area
    /**
     * Set a total class area (CA) target
     * @param minClassArea
     * @param maxClassArea
     */
    public void setClassArea(int minClassArea, int maxClassArea) {
        if (maxClassArea < minClassArea) {
            throw  new ValueException("Max class area must be greater than or equal to min class area");
        }
        model.arithm(sum, ">=", minClassArea).post();
        model.arithm(sum, "<=", maxClassArea).post();
    }

    // PLAND - Proportion of landscape
    /**
     * Set a proportion of landscape (PLAND) target
     * @param minProportion
     * @param maxProportion
     * @throws ValueException
     */
    public void setLandscapeProportion(double minProportion, double maxProportion) throws ValueException {
        if (minProportion < 0 || minProportion > 100 || maxProportion < 0 || maxProportion > 100) {
            throw new ValueException("Min and max class proportion must be between 0 and 100");
        }
        if (maxProportion < minProportion) {
            throw  new ValueException("Max proportion must be greater than or equal to min proportion");
        }
        int min = (int) (landscapeSize * minProportion / 100);
        int max = (int) (landscapeSize * maxProportion / 100);
        setClassArea(min, max);
    }

    // PD - Patch density
    /**
     * Set a patch density (PD) target
     * @param minDensity
     * @param maxDensity
     */
    public void setPatchDensity(double minDensity, double maxDensity) {
        if (maxDensity < minDensity) {
            throw  new ValueException("Max patch density area must be greater than or equal to min patch density");
        }
        int minNbPatches = (int) (minDensity * landscapeSize);
        int maxNbPatches = (int) (maxDensity * landscapeSize);
        model.arithm(nbPatches, ">=", minNbPatches).post();
        model.arithm(nbPatches, "<=", maxNbPatches).post();
    }

    // SPI - Smallest patch index
    /**
     * Set a smallest patch index (SPI) target
     * @param minSize
     * @param maxSize
     */
    public void setSmallestPatchSize(int minSize, int maxSize) {
        if (maxSize < minSize) {
            throw  new ValueException("Max SPI must be greater than or equal to min SPI");
        }
        model.min(model.intVar(minSize, maxSize), patchSizes).post();
    }

    // LPI - Largest patch index
    /**
     * Set a largest patch index (LPI) target
     * @param minSize
     * @param maxSize
     */
    public void setLargestPatchSize(int minSize, int maxSize) {
        if (maxSize < minSize) {
            throw  new ValueException("Max LPI must be greater than or equal to min LPI");
        }
        model.arithm(patchSizes[patchSizes.length - 1], ">=", minSize).post();
        model.arithm(patchSizes[patchSizes.length - 1], "<=", maxSize).post();
    }

    // MESH - Effective mesh size
    /**
     * Set an effective mesh size (MESH) target.
     * @param mesh_lb
     * @param mesh_ub
     */
    public void setMesh(double mesh_lb, double mesh_ub) {
        if (mesh_ub < mesh_lb) {
            throw  new ValueException("Max MESH must be greater than or equal to min MESH");
        }
        this.mesh_lb = mesh_lb;
        this.mesh_ub = mesh_ub;
        long netProduct_lb = (long) (mesh_lb * landscapeSize);
        long netProduct_ub = (long) (mesh_ub * landscapeSize);
        setNetProduct(netProduct_lb, netProduct_ub);
    }

    // NPRO - Net product
    /**
     * Set a net product (NPRO) target
     * @param minNetProduct
     * @param maxNetProduct
     */
    public void setNetProduct(long minNetProduct, long maxNetProduct) {
        if (maxNetProduct < minNetProduct) {
            throw  new ValueException("Max NPRO must be greater than or equal to min NPRO");
        }
//        if (sumOfSquares == null) {
//            initNetProduct(minNetProduct, maxNetProduct);
//        }
        model.post(new Constraint("sumOfSquares", new PropSumOfSquares(patchSizes, minNetProduct, maxNetProduct)));
//        model.arithm(sumOfSquares, ">=", minNetProduct).post();
//        model.arithm(sumOfSquares, "<=", maxNetProduct).post();
    }

    // SPLI - Splitting index
    /**
     * Set a splitting index (SPLI) target
     * @param minSplittingIndex
     * @param maxSplittingIndex
     */
    public void setSplittingIndex(double minSplittingIndex, double maxSplittingIndex) {
        if (maxSplittingIndex < minSplittingIndex) {
            throw  new ValueException("Max SPLI must be greater than or equal to min SPLI");
        }
        long netProductLB = (long) (landscapeSize * landscapeSize / maxSplittingIndex);
        long netProductUB = (long) (landscapeSize * landscapeSize / minSplittingIndex);
        setNetProduct(netProductLB, netProductUB);
    }

    // SDEN - Splitting density
    /**
     * Set a splitting density (SDEN) target
     * @param minSplittingDensity
     * @param maxSplittingDensity
     */
    public void setSplittingDensity(double minSplittingDensity, double maxSplittingDensity) {
        if (maxSplittingDensity < minSplittingDensity) {
            throw  new ValueException("Max SDEN must be greater than or equal to min SDEN");
        }
        long netProductLB = (long) (landscapeSize / maxSplittingDensity);
        long netProductUB = (long) (landscapeSize / minSplittingDensity);
        setNetProduct(netProductLB, netProductUB);
    }

    // COHE - Degree of coherence
    /**
     * Set a degree of coherence (COHE) index
     * @param minCoherence
     * @param maxCoherence
     */
    public void setDegreeOfCoherence(double minCoherence, double maxCoherence) {
        if (minCoherence < 0 || minCoherence > 1 || maxCoherence < 0 || maxCoherence > 1) {
            throw new ValueException("Min and max coherence must be between 0 and 1");
        }
        if (maxCoherence < minCoherence) {
            throw  new ValueException("Max COHE must be greater than or equal to min COHE");
        }
        long netProductLB = (long) (minCoherence * landscapeSize * landscapeSize);
        long netProductUB = (long) (maxCoherence * landscapeSize * landscapeSize);
        setNetProduct(netProductLB, netProductUB);
    }

    // DIVI - Degree of landscape division
    /**
     * Set a degree of division (DIVI) target
     * @param minDivision
     * @param maxDivision
     */
    public void setDegreeOfDivision(double minDivision, double maxDivision) {
        if (minDivision < 0 || minDivision > 1 || maxDivision < 0 || maxDivision > 1) {
            throw new ValueException("Min and max degree of division must be between 0 and 1");
        }
        if (maxDivision < minDivision) {
            throw  new ValueException("Max DIVI must be greater than or equal to min DIVI");
        }
        setDegreeOfCoherence(1 - maxDivision, 1 -minDivision);
    }

    public void setAllPatchesDifferentSize() {
        model.allDifferentExcept0(patchSizes).post();
    }

    public int getNbPatches() {
        if (nbPatches.isInstantiated()) {
            return nbPatches.getValue();
        }
        return -1;
    }

    public int[] getPatchSizes() {
        return Arrays.stream(patchSizes)
                .mapToInt(v -> v.isInstantiated() ? v.getValue() : -1)
                .filter(i -> i > 0)
                .toArray();
    }

    public int getTotalSize() {
        if (sum.isInstantiated()) {
            return sum.getValue();
        }
        return -1;
    }

    public double getLandscapeProportion() {
        return 100 * (1.0 * getTotalSize()) / (1.0 * landscapeSize);
    }

    public double getPatchDensity() {
        return (1.0 * getNbPatches()) / (1.0 * landscapeSize);
    }

    public int getSmallestPatchIndex() {
        return getPatchSizes()[0];
    }

    public int getLargestPatchIndex() {
        return getPatchSizes()[getNbPatches() - 1];
    }

    public long getNetProduct() {
        long npro = 0;
        for (IntVar p : patchSizes) {
            if (!p.isInstantiated()) {
                return -1;
            }
            long v = new Long(p.getValue()).longValue();
            npro += v * v;
        }
        return npro;
    }

    public double getMesh() {
        return (1.0 * getNetProduct()) / (1.0 * landscapeSize);
    }

    public double getSplittingIndex() {
        return (1.0 * landscapeSize * landscapeSize) / (1.0 * getNetProduct());
    }

    public double getSplittingDensity() {
        return (1.0 * landscapeSize) / (1.0 * getNetProduct());
    }

    public double getDegreeOfCoherence() {
        return (1.0 * getNetProduct()) / (1.0 * landscapeSize * landscapeSize);
    }

    public double getDegreeOfDivision() {
        return 1 - getDegreeOfCoherence();
    }
}
