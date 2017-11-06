package org.matsim.munichArea.outputCreation.accessibilityCalculator;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import omx.OmxFile;
import omx.OmxMatrix;
import omx.hdf5.OmxHdf5Datatype;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.configMatsim.planCreation.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.planCreation.Location;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

import static java.lang.System.exit;

/**
 * Created by carlloga on 9/15/2016.
 */
public class Accessibility {

    private static ResourceBundle rb;
    private Matrix autoTravelTime;
    private String skimFileName;
    private String matrixName;
    private ArrayList<Location> locationList;
    private Map<Integer, Float> travelTimeMap;
    private Map<Integer, Float> accessibilityMap;


    public static void main (String[] args){

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        Accessibility acc = new Accessibility();
        acc.loadData();
        acc.calculateAccessibility();
        acc.calculateIntrazonalTimes();
        acc.printAccessibility(rb.getString("output.accessibility.file"));


    }


    public void loadData() {

        skimFileName = rb.getString("omx.access.calc") + ".omx";
        matrixName = "mat1";
        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
        locationList = centroidsToLocations.readCentroidList();

        SkimMatrixReader skmReader1 = new SkimMatrixReader();
        autoTravelTime = skmReader1.readSkim(skimFileName, matrixName);


    }



    public void calculateIntrazonalTimes() {

        for (Location orig : locationList){
            float travelTime = autoTravelTime.getValueAt(orig.getId(), orig.getId());
            travelTimeMap.put(orig.getId(), travelTime);
        }

    }



    public void calculateAccessibility() {

        for (Location orig : locationList) {
            float accessibility = 0;
            for (Location dest : locationList) {

                double travelTime = autoTravelTime.getValueAt(orig.getId(), dest.getId());

                if (travelTime == -1) {
                    travelTime = Double.POSITIVE_INFINITY;
                }

                accessibility += Math.pow(dest.getPopulation(), 1.25) * Math.exp(-0.1 * travelTime);
            }
            accessibilityMap.put(orig.getId(), accessibility);
        }

    }

    public void printAccessibility(String fileName) {

        BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
        try {
            bw.write("ID, X, Y, access, timeToZone");
            bw.newLine();
            for (Location loc : locationList) {
                bw.write(loc.getId() + "," +
                        loc.getX() + "," +
                        loc.getY() + "," +
                        accessibilityMap.get(loc.getId()) +"," +
                        travelTimeMap.get(loc.getId()));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}



