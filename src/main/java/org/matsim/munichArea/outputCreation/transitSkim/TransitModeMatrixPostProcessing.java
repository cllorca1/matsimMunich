package org.matsim.munichArea.outputCreation.transitSkim;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class TransitModeMatrixPostProcessing {

    private String[] modes = new  String []{"train", "metro", "bus"};

    //train = 0; metro = 1; bus = 2
    private String[] folders = new String[] {"withTrain", "withMetroTram", "withBus"};
    private Matrix[] totalTt = new Matrix[3];
    private Matrix[] egress = new Matrix[3];
    private Matrix[] access = new Matrix[3];
    private Matrix[] inTransit = new Matrix[3];
    private Matrix[] transfer = new Matrix[3];
    private Matrix[] inVehicle = new Matrix[3];

    private static ResourceBundle rb;
    private ArrayList<Location> locationList;


    public static void main (String[] args){


        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);



        TransitModeMatrixPostProcessing postProcessing = new TransitModeMatrixPostProcessing();
        postProcessing.readMatrices();
        postProcessing.compareAndEditMatrices();
        postProcessing.writeMatrices();



    }

    public TransitModeMatrixPostProcessing(){
        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
        this.locationList = centroidsToLocations.readCentroidList();


    }

    public void readMatrices(){
        SkimMatrixReader skimReader = new SkimMatrixReader();
        for (int i = 0; i< modes.length; i++ ){
            String fileName = "./data/" + folders[i] + "/ttTransitTotal" + rb.getString("simulation.name") + ".omx";
            totalTt[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/ttTRansitEgress" + rb.getString("simulation.name") + ".omx";
            egress[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/ttTransitAccess" + rb.getString("simulation.name") + ".omx";
            access[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/ttTransitIn" + rb.getString("simulation.name") + ".omx";
            inTransit[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/ttTransitInVehicle" + rb.getString("simulation.name") + ".omx";
            inVehicle[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/ttTransitTransfer" + rb.getString("simulation.name") + ".omx";
            transfer[i] = skimReader.readSkim(fileName,"mat1");
        }

    }

    public void compareAndEditMatrices(){

        //train matrices are cleaned when train is not used, if the travel time is equal as with the rest of modes, set to -1 (=NA)
        System.out.println("train vs. metro:");
        compareAndCleanIfEqual(0,1);
        //metro matrices are cleaned when neither metro nor tram are not used
        System.out.println("metro vs. bus");
        compareAndCleanIfEqual(1,2);

    }

    public void writeMatrices(){
        //do not write again the bus?
        for (int i = 0; i< modes.length; i++ ) {
            String fileName = "./data/" + folders[i] + "/ttTransitTotal" + rb.getString("simulation.name") + "Clean" + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList);
            TravelTimeMatrix.createOmxSkimMatrix(totalTt[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/ttTransitAccess" + rb.getString("simulation.name") + "Clean" + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList);
            TravelTimeMatrix.createOmxSkimMatrix(access[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/ttTransitEgress" + rb.getString("simulation.name") + "Clean" + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList);
            TravelTimeMatrix.createOmxSkimMatrix(egress[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/ttTransitIn" + rb.getString("simulation.name") + "Clean" + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList);
            TravelTimeMatrix.createOmxSkimMatrix(inTransit[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/ttTransitInVehicle" + rb.getString("simulation.name") + "Clean" + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList);
            TravelTimeMatrix.createOmxSkimMatrix(inVehicle[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/ttTransitTransfer" + rb.getString("simulation.name") + "Clean" + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList);
            TravelTimeMatrix.createOmxSkimMatrix(transfer[i],  fileName, "mat1");
        }
    }

    public void compareAndCleanIfEqual(int modeToEdit, int modeReference){


        int counter = 0;
         for (int i : totalTt[modeToEdit].getExternalColumnNumbers()){
             for (int j : totalTt[modeToEdit].getExternalRowNumbers()){
                 if (totalTt[modeToEdit].getValueAt(i,j) == totalTt[modeReference].getValueAt(i,j)){
                     egress[modeToEdit].setValueAt(i,j, -1f);
                     access[modeToEdit].setValueAt(i,j, -1f);
                     inVehicle[modeToEdit].setValueAt(i,j, -1f);
                     inTransit[modeToEdit].setValueAt(i,j, -1f);
                     totalTt[modeToEdit].setValueAt(i,j, -1f);
                     transfer[modeToEdit].setValueAt(i,j, -1f);
                     counter++;
                 }
             }
         }
        System.out.println("Total number of od pairs that are not available = " + counter);

    }
}
