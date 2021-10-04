package solver;

import grid.neighborhood.INeighborhood;
import grid.neighborhood.Neighborhoods;
import grid.regular.square.RegularSquareGrid;
import org.opengis.referencing.FactoryException;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws FactoryException, IOException {
        int nRow = 2000;
        int nCol = 2000;
        RegularSquareGrid grid = new RegularSquareGrid(nRow, nCol);
        INeighborhood neighborhood = Neighborhoods.FOUR_CONNECTED;
        INeighborhood bufferNeighborhood = Neighborhoods.TWO_WIDE_FOUR_CONNECTED;
        LandscapeGenerator landscapeGenerator = new LandscapeGenerator(grid, neighborhood, bufferNeighborhood);
        landscapeGenerator.exportDem(
                0, 0, 0.0001, "EPSG:4326",
                "/home/djusteau/Documents/testPolyomino/testDem.tif"
        );
    }
}
