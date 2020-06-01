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
    private String[] folders = new String[] {"new/all", "new/bus_tram_metro", "new/bus"};
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
            String fileName = "./data/" + folders[i] + "/total_t_new_2.omx";
            totalTt[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/eggress_t_new_2.omx";
            egress[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/access_t_new_2.omx";
            access[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/in_transit_t_new_2.omx";
            inTransit[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/in_vehicle_t_new_2.omx";
            inVehicle[i] = skimReader.readSkim(fileName,"mat1");
            fileName = "./data/" + folders[i] + "/transfers_new_2.omx";
            transfer[i] = skimReader.readSkim(fileName,"mat1");
        }

    }

    public void compareAndEditMatrices(){

        //train matrices are cleaned when train is not used, if the travel time is equal as with the rest of modes, set to -1 (=NA)
        System.out.println("train vs. metro:");
        compareAndCleanIfEqual(0,1);

        //train matrices are cleaned when train and metro is not used, if the travel time is equal as with the rest of modes, set to -1 (=NA)
        System.out.println("train vs. metro:");
        compareAndCleanIfEqual(0,2);

        //metro/tram matrices are cleaned when neither metro nor tram are not used
        System.out.println("metro vs. bus");
        compareAndCleanIfEqual(1,2);

    }

    public void writeMatrices(){
        //do not write again the bus?
        for (int i = 0; i< modes.length; i++ ) {
            String fileName = "./data/" + folders[i] + "/total_t_" + modes[i] + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(totalTt[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/access_t_" +modes[i] + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(access[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/eggress_t_" + modes[i] + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(egress[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/in_transit_t_" + modes[i] + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(inTransit[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/in_vehicle_t_" + modes[i] + ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(inVehicle[i],  fileName, "mat1");
            fileName = "./data/" + folders[i] + "/transfers_" + modes[i]+ ".omx";
            TravelTimeMatrix.createOmxFile(fileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(transfer[i],  fileName, "mat1");
        }
    }

    public void compareAndCleanIfEqual(int modeToEdit, int modeReference){


        int counter = 0;
         for (int i : totalTt[modeToEdit].getExternalColumnNumbers()){
             for (int j : totalTt[modeToEdit].getExternalRowNumbers()){
                 if (inVehicle[modeToEdit].getValueAt(i,j) == inVehicle[modeReference].getValueAt(i,j)){
                     //if the in vehicle time of the mode A is equal to the one in mode B, being A of
                     // higher hierarchy, one cannot take A as the fastest option
                     egress[modeToEdit].setValueAt(i,j, -1f);
                     access[modeToEdit].setValueAt(i,j, -1f);
                     inVehicle[modeToEdit].setValueAt(i,j, -1f);
                     inTransit[modeToEdit].setValueAt(i,j, -1f);
                     totalTt[modeToEdit].setValueAt(i,j, -1f);
                     transfer[modeToEdit].setValueAt(i,j, -1f);
                     if (totalTt[modeReference].getValueAt(i,j)> 0){
                         counter++;
                     }

                 }
             }
         }
        System.out.println("Total number of od pairs that are not available = " + counter);

    }
}
