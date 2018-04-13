package org.matsim.munichArea.outputCreation.transitSkim;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.configMatsim.createDemandPt.ReadZonesServedByTransit;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by carlloga on 02.03.2017.
 */
public class AddAccessAndEgressAtNonServedZones {

    private final static Logger log = Logger.getLogger(AddAccessAndEgressAtNonServedZones.class);

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

    public double minutesThreshold = 30;

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
        AddAccessAndEgressAtNonServedZones postProcess = new AddAccessAndEgressAtNonServedZones(munich, locationList, servedZoneList);
        postProcess.fillTransitSkims();
        postProcess.writeFilledMatrices();



    }



    public AddAccessAndEgressAtNonServedZones(ResourceBundle munich, ArrayList<Location> locationList, ArrayList<Location> servedZoneList) {
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
        autoTravelDistance = skimReader.readSkim(munich.getString("skim.file.dist.access") + ".omx",
                munich.getString("skim.matrix.dist.access"));
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

        AtomicInteger counter = new AtomicInteger(0);

        //for each origin i
        locationList.parallelStream().forEach((Location origLoc) -> {
            int i = origLoc.getId();
            float access;
            float egress;
            float tt;
            //fir each destination j
            for (int j = 1; j <= totalTime.getColumnCount(); j++) {
                if (totalTime.getValueAt(i, j) == -1 & i <= j) {
                    tt = Float.MAX_VALUE;
                    float addAccessTime = 0;
                    float addEgressTime = 0;
                    int startZoneIndex = 0;
                    int finalZoneIndex = 0;
                    //look for boarding station
                    for (Location k : servedZoneList) {
                        access = (float) (autoTravelDistance.getValueAt(i, k.getId()) / 1.4 / 60);
                        if (access < minutesThreshold ) {
                            //look for boarding station k and alighting station l
                            for (Location l : servedZoneList) {
                                egress = (float) (autoTravelDistance.getValueAt(l.getId(), j) / 1.4 / 60);
                                if (egress < minutesThreshold) {
                                    //the access and egress time at k and l are discarded because the agent walks from other zone
                                    if (inTransit.getValueAt(k.getId(), l.getId()) > 0) {
                                        if (tt > inTransit.getValueAt(k.getId(), l.getId()) + access + egress) {
                                            //a better OD tranist pair has been found
                                            tt = inTransit.getValueAt(k.getId(), l.getId()) + access + egress;
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

                        //todo add the if to check if there is no invehicle time

                        if (tt < minutesThreshold & tt > 0) {
                        //found the best k and l that link i and j by transit inn tt mins
                        inTransitCompleteMatrix.setValueAt(i,j,inTransit.getValueAt(startZoneIndex, finalZoneIndex));
                        inTransitCompleteMatrix.setValueAt(j,i,inTransit.getValueAt(startZoneIndex, finalZoneIndex));

                        accessTimeCompleteMatrix.setValueAt(i,j,addAccessTime);
                        accessTimeCompleteMatrix.setValueAt(j,i,addAccessTime);

                        egressTimeCompleteMatrix.setValueAt(i,j,addEgressTime);
                        egressTimeCompleteMatrix.setValueAt(j,i,addEgressTime);

                        transfersCompleteMatrix.setValueAt(i,j,transfers.getValueAt(startZoneIndex, finalZoneIndex));
                        transfersCompleteMatrix.setValueAt(j,i,transfers.getValueAt(startZoneIndex, finalZoneIndex));

                        inVehicleTimeCompleteMatrix.setValueAt(i,j,inVehicle.getValueAt(startZoneIndex, finalZoneIndex));
                        inVehicleTimeCompleteMatrix.setValueAt(j,i,inVehicle.getValueAt(startZoneIndex, finalZoneIndex));

                        totalTimeCompleteMatrix.setValueAt(i, j, tt);
                        totalTimeCompleteMatrix.setValueAt(j, i, tt);
                        }
                    }

                } else if (accessTime.getValueAt(i, j)> minutesThreshold || egressTime.getValueAt(i, j) > minutesThreshold) {

                    inTransitCompleteMatrix.setValueAt(i,j,-1f);
                    inTransitCompleteMatrix.setValueAt(j,i,-1f);

                    accessTimeCompleteMatrix.setValueAt(i,j,-1f);
                    accessTimeCompleteMatrix.setValueAt(j,i,-1f);

                    egressTimeCompleteMatrix.setValueAt(i,j,-1f);
                    egressTimeCompleteMatrix.setValueAt(j,i,-1f);

                    transfersCompleteMatrix.setValueAt(i,j,-1f);
                    transfersCompleteMatrix.setValueAt(j,i,-1f);

                    inVehicleTimeCompleteMatrix.setValueAt(i,j,-1f);
                    inVehicleTimeCompleteMatrix.setValueAt(j,i,-1f);

                    totalTimeCompleteMatrix.setValueAt(i, j, -1f);
                    totalTimeCompleteMatrix.setValueAt(j, i, -1f);

                }
                //if not found a -1 then skip this


            }

        log.info(counter.incrementAndGet() +  " zones completed");
        });
    }

    public void writeFilledMatrices(){

        String omxPtFileName = munich.getString("pt.total.skim.file") + simulationName + "Full.omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(totalTimeCompleteMatrix,  omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.in.skim.file") + simulationName + "Full.omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(inTransitCompleteMatrix,  omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.access.skim.file") + simulationName + "Full.omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(accessTimeCompleteMatrix,  omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.egress.skim.file") + simulationName + "Full.omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(egressTimeCompleteMatrix,  omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.transfer.skim.file") + simulationName + "Full.omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(transfersCompleteMatrix,  omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.in.vehicle.skim.file") + simulationName + "Full.omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(inVehicleTimeCompleteMatrix,  omxPtFileName, "mat1");



    }



}
