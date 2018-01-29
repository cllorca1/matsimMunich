package org.matsim.munichArea.outputCreation.transitSkim;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.configMatsim.createDemandPt.ReadZonesServedByTransit;
import org.matsim.munichArea.configMatsim.planCreation.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 02.03.2017.
 */
public class TransitSkimPostProcessing {

    private ResourceBundle munich;
    private Matrix inTransitCompleteMatrix;
    private Matrix totalTimeCompleteMatrix;
    private Matrix accessTimeCompleteMatrix;
    private Matrix egressTimeCompleteMatrix;
    private Matrix transfersCompleteMatrix;
    private Matrix inVehicleTimeCompleteMatrix;
    private ArrayList<Location> locationList;
    private ArrayList<Location> servedZoneList;

    private Matrix inTransit;
    private Matrix totalTime;
    private Matrix accessTime;
    private Matrix egressTime;
    private Matrix transfers;
    private Matrix autoTravelDistance;
    private Matrix inVehicle;

    public double minutesThreshold = 200;

    private static String simulationName;


    public static void main (String[] args){

        File propFile = new File(args[0]);
        ResourceBundle munich = ResourceUtil.getPropertyBundle(propFile);

        simulationName = munich.getString("simulation.name");

        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(munich);
        ArrayList<Location> locationList = centroidsToLocations.readCentroidList();

        ReadZonesServedByTransit servedZoneReader = new ReadZonesServedByTransit(munich);
        ArrayList<Location> servedZoneList = servedZoneReader.readZonesServedByTransit(locationList);


        //fill matrices for non-served locations
        TransitSkimPostProcessing postProcess = new TransitSkimPostProcessing(munich, locationList, servedZoneList);
        postProcess.fillTransitSkims();
        postProcess.writeFilledMatrices();



    }



    public TransitSkimPostProcessing(ResourceBundle munich, ArrayList<Location> locationList, ArrayList<Location> servedZoneList) {
        this.munich = munich;
        this.locationList = locationList;
        this.servedZoneList = servedZoneList;

    }



    public void fillTransitSkims() {
        SkimMatrixReader skimReader = new SkimMatrixReader();
        //read original matrices
        inTransit = skimReader.readSkim(munich.getString("pt.in.skim.file") + simulationName + ".omx", "mat1");
        totalTime = skimReader.readSkim(munich.getString("pt.total.skim.file") + simulationName + ".omx", "mat1");
        accessTime = skimReader.readSkim(munich.getString("pt.access.skim.file") + simulationName + ".omx", "mat1");
        egressTime = skimReader.readSkim(munich.getString("pt.egress.skim.file") + simulationName + ".omx", "mat1");
        transfers = skimReader.readSkim(munich.getString("pt.transfer.skim.file") + simulationName + ".omx", "mat1");
        inVehicle = skimReader.readSkim(munich.getString("pt.in.vehicle.skim.file") + simulationName + ".omx", "mat1");
        //read the distances
        autoTravelDistance = skimReader.readSkim(munich.getString("out.skim.auto.dist.base") + ".omx", "mat1");
        //fill in the locations without access by transit
        fillTransitMatrix();

    }

    public void fillTransitMatrix() {

        //duplicate the matrices
        inTransitCompleteMatrix = inTransit;
        totalTimeCompleteMatrix = totalTime;
        accessTimeCompleteMatrix = accessTime;
        egressTimeCompleteMatrix = egressTime;
        transfersCompleteMatrix = transfers;
        inVehicleTimeCompleteMatrix = inVehicle;

        locationList.parallelStream().forEach((Location origLoc) -> {
            int i = origLoc.getId();
            float access;
            float egress;
            float tt;
            //total time matrix is used as check if -1
            for (int j = 1; j <= totalTime.getColumnCount(); j++) {
                if (totalTime.getValueAt(i, j) == -1 & i <= j) {
                    tt = Float.MAX_VALUE;
                    float addAccessTime = 0;
                    float addEgressTime = 0;
                    int startZoneIndex = 0;
                    int finalZoneIndex = 0;
                    for (Location k : servedZoneList) {
                        access = (float) (autoTravelDistance.getValueAt(i, k.getId()) / 1.4 / 60);
                        if (access < minutesThreshold ) {
                            for (Location l : servedZoneList) {
                                egress = (float) (autoTravelDistance.getValueAt(l.getId(), j) / 1.4 / 60);
                                if (egress < minutesThreshold) {
                                    if (totalTime.getValueAt(k.getId(), l.getId()) > 0) {
                                        if (tt > totalTime.getValueAt(k.getId(), l.getId()) + access + egress) {
                                            //a better OD tranist pair has been found
                                            tt = totalTime.getValueAt(k.getId(), l.getId()) + access + egress;
                                            //stores the access and egress points
                                            startZoneIndex = k.getId();
                                            finalZoneIndex = l.getId();
                                            addAccessTime = access;
                                            addEgressTime = egress;
                                        }
                                    }
                                }
                            }
                        }

                    }

                    if (startZoneIndex != 0 & finalZoneIndex != 0){

                        if (tt < minutesThreshold & tt > 0) {
                        //found the best k and l that link i and j by transit inn tt mins
                        inTransitCompleteMatrix.setValueAt(i,j,inTransit.getValueAt(startZoneIndex, finalZoneIndex));
                        inTransitCompleteMatrix.setValueAt(j,i,inTransit.getValueAt(startZoneIndex, finalZoneIndex));

                        accessTimeCompleteMatrix.setValueAt(i,j,addAccessTime + accessTime.getValueAt(startZoneIndex, finalZoneIndex));
                        accessTimeCompleteMatrix.setValueAt(j,i,addAccessTime + accessTime.getValueAt(startZoneIndex, finalZoneIndex));

                        egressTimeCompleteMatrix.setValueAt(i,j,addEgressTime + egressTime.getValueAt(startZoneIndex, finalZoneIndex));
                        egressTimeCompleteMatrix.setValueAt(j,i,addEgressTime + egressTime.getValueAt(startZoneIndex, finalZoneIndex));

                        transfersCompleteMatrix.setValueAt(i,j,transfers.getValueAt(startZoneIndex, finalZoneIndex));
                        transfersCompleteMatrix.setValueAt(j,i,transfers.getValueAt(startZoneIndex, finalZoneIndex));

                        inVehicleTimeCompleteMatrix.setValueAt(i,j,inVehicle.getValueAt(startZoneIndex, finalZoneIndex));
                        inVehicleTimeCompleteMatrix.setValueAt(j,i,inVehicle.getValueAt(startZoneIndex, finalZoneIndex));

                        totalTimeCompleteMatrix.setValueAt(i, j, tt);
                        totalTimeCompleteMatrix.setValueAt(j, i, tt);
                        }
                    }

                }
                //if not found a -1 then skip this


            }

        System.out.println("zone completed " + i);
        });
    }

    public void writeFilledMatrices(){

        String omxPtFileName = munich.getString("pt.total.skim.file") + simulationName + "Complete.omx";
        TravelTimeMatrix.createOmxSkimMatrix(totalTimeCompleteMatrix, locationList, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.in.skim.file") + simulationName + "Complete.omx";
        TravelTimeMatrix.createOmxSkimMatrix(inTransitCompleteMatrix, locationList, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.access.skim.file") + simulationName + "Complete.omx";
        TravelTimeMatrix.createOmxSkimMatrix(accessTimeCompleteMatrix, locationList, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.egress.skim.file") + simulationName + "Complete.omx";
        TravelTimeMatrix.createOmxSkimMatrix(egressTimeCompleteMatrix, locationList, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.transfer.skim.file") + simulationName + "Complete.omx";
        TravelTimeMatrix.createOmxSkimMatrix(transfersCompleteMatrix, locationList, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.in.vehicle.skim.file") + simulationName + "Complete.omx";
        TravelTimeMatrix.createOmxSkimMatrix(inVehicleTimeCompleteMatrix, locationList, omxPtFileName, "mat1");



    }



}
