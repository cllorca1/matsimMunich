package org.matsim.munichArea.outputCreation;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class AddIntrazonals {


    public static void main (String[] args){

        File propFile = new File(args[0]);
        ResourceBundle rb = ResourceUtil.getPropertyBundle(propFile);

        CentroidsToLocations ctl = new CentroidsToLocations(rb);
        ArrayList<Location> locationList = ctl.readCentroidList();


        Matrix matrix = SkimMatrixReader.readSkim("./data/skimAll.omx", "distanceByDistance");

        matrix = TravelTimeMatrix.assignIntrazonals(matrix, 3, 2000000, 0.5f);

        //TravelTimeMatrix.createOmxFile("./data/skimsAllIntrazonal.omx", locationList);
        TravelTimeMatrix.createOmxSkimMatrix(matrix, "./data/skimsAllIntrazonal.omx", "distanceByDistance");

    }
}
