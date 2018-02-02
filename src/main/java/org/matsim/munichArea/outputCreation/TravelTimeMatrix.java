package org.matsim.munichArea.outputCreation;

import com.pb.common.matrix.Matrix;
import omx.*;
import omx.hdf5.*;
import org.apache.log4j.Logger;
import org.matsim.munichArea.configMatsim.zonalData.Location;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by carlloga on 9/14/2016.
 */
public class TravelTimeMatrix {

    public static Logger logger = Logger.getLogger(TravelTimeMatrix.class);

    public static void createOmxFile(String omxFileName, ArrayList<Location> locationList) {

        try (OmxFile omxFile = new OmxFile(omxFileName)) {

            int dim0 = locationList.size();

            int dim1 = dim0;
            int[] shape = {dim0, dim1};

            int lookup1NA = -1;
            int[] lookup1Data = new int[dim0];
            Set<Integer> lookup1Used = new HashSet<>();
            for (int i = 0; i < lookup1Data.length; i++) {
                int lookup = i + 1;
                lookup1Data[i] = lookup1Used.add(lookup) ? lookup : lookup1NA;
            }
            OmxLookup.OmxIntLookup lookup1 = new OmxLookup.OmxIntLookup("lookup1", lookup1Data, lookup1NA);

            omxFile.openNew(shape);
            omxFile.addLookup(lookup1);
            omxFile.save();
            omxFile.close();

        }

    }

    public static void createOmxSkimMatrix(Matrix autoTravelTime, String omxFileName, String omxMatrixName) {


        try (OmxFile omxFile = new OmxFile(omxFileName)) {

            omxFile.openReadWrite();
            float mat1NA = -1;


            OmxMatrix.OmxFloatMatrix mat1 = new OmxMatrix.OmxFloatMatrix(omxMatrixName, autoTravelTime.getValues(), mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(), "travelTimes");
            omxFile.addMatrix(mat1);
            omxFile.save();
            System.out.println(omxFile.summary());

            omxFile.close();
            System.out.println(omxMatrixName + "matrix written");

        }
// clean the matrix if not needed ?
//        try (OmxFile omxFile = new OmxFile(f)) {
//            omxFile.openReadWrite();
//            System.out.println(omxFile.summary());
//            omxFile.deleteMatrix("mat1");
//            omxFile.deleteLookup("lookup1");
//            System.out.println(omxFile.summary());
//        }

    }

    public static void createStringCSVSkimMatrix(String[][] matrix, ArrayList<Location> locationList, String omxFileName, String omxMatrixName) {


        try {


            PrintWriter pw = new PrintWriter(new FileWriter(omxFileName, false));

            pw.println("origin,destination,route");

            for (int i = 1; i < locationList.size(); i++) {
                for (int j = 1; j < locationList.size(); j++) {

                    pw.print(i);
                    pw.print(",");
                    pw.print(j);
                    pw.print(",");
                    pw.println(matrix[i][j]);
                }
            }

            pw.flush();
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Matrix assignIntrazonals(Matrix matrix, int numberOfNeighbours, float maximum, float proportionOfTime) {

        for (int i : matrix.getExternalRowNumbers()) {
            float[] minRowValues = new float[numberOfNeighbours];

            for (int k = 0; k < numberOfNeighbours; k++) {
                minRowValues[k] = maximum;
            }

            //find the  n closest neighbors
            for (int j : matrix.getExternalRowNumbers()) {
                int minimumPosition = 0;
                while (minimumPosition < numberOfNeighbours) {
                    if (minRowValues[minimumPosition] > matrix.getValueAt(i, j) && matrix.getValueAt(i, j) != 0) {
                        for (int k = numberOfNeighbours - 1; k > minimumPosition; k--) {
                            minRowValues[k] = minRowValues[k - 1];
                        }
                        minRowValues[minimumPosition] = matrix.getValueAt(i, j);
                        break;
                    }
                    minimumPosition++;
                }
            }

            float globalMin = 0;
            for (float minRowValue : minRowValues) {
                globalMin += minRowValue;
            }
            globalMin = globalMin / numberOfNeighbours * proportionOfTime;

            for (int j : matrix.getExternalRowNumbers()) {
                if (matrix.getValueAt(i, j) == 0) {
                    matrix.setValueAt(i, j, globalMin);
                }
            }
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
    }


}







