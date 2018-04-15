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
    private ArrayList<Location> nonServedZoneList;

    private Matrix inTransit;
    private Matrix totalTime;
    private Matrix accessTime;
    private Matrix egressTime;
    private Matrix transfers;
    private Matrix autoTravelDistance;
    private Matrix inVehicle;

    public double minutesThreshold = 30;

    private static String simulationName;


    public static void main(String[] args) {

        File propFile = new File(args[0]);
        ResourceBundle munich = ResourceUtil.getPropertyBundle(propFile);

        simulationName = munich.getString("simulation.name");

        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(munich);
        ArrayList<Location> locationList = centroidsToLocations.readCentroidList();

        ReadZonesServedByTransit servedZoneReader = new ReadZonesServedByTransit(munich);
        ArrayList<Location> servedZoneList = servedZoneReader.readZonesServedByTransit(locationList);


        //fill matrices for non-served locations
        AddAccessAndEgressAtNonServedZones postProcess = new AddAccessAndEgressAtNonServedZones(munich, locationList, servedZoneList);
        postProcess.readSkims();
        postProcess.generateTheNonServedZoneList();
        postProcess.removeNonTransitTrips();
        postProcess.fillTransitSkims();
        postProcess.writeFilledMatrices();


    }


    public AddAccessAndEgressAtNonServedZones(ResourceBundle munich, ArrayList<Location> locationList, ArrayList<Location> servedZoneList) {
        this.munich = munich;
        this.locationList = locationList;
        this.servedZoneList = servedZoneList;
        nonServedZoneList = new ArrayList<>();

    }


    public void readSkims(){
        SkimMatrixReader skimReader = new SkimMatrixReader();
        //read original matrices
        inTransit = skimReader.readSkim(munich.getString("pt.in.skim.file") + "_served" + ".omx", "mat1");
        totalTime = skimReader.readSkim(munich.getString("pt.total.skim.file") + "_served" + ".omx", "mat1");
        accessTime = skimReader.readSkim(munich.getString("pt.access.skim.file") + "_served" + ".omx", "mat1");
        egressTime = skimReader.readSkim(munich.getString("pt.egress.skim.file") + "_served" + ".omx", "mat1");
        transfers = skimReader.readSkim(munich.getString("pt.transfer.skim.file") + "_served" + ".omx", "mat1");
        inVehicle = skimReader.readSkim(munich.getString("pt.in.vehicle.skim.file") + "_served" + ".omx", "mat1");
        //read the distances
        autoTravelDistance = skimReader.readSkim(munich.getString("skim.file.dist.access") + ".omx",
                munich.getString("skim.matrix.dist.access"));
    }


    public void fillTransitSkims() {

        //fill in the locations without access by transit
        fillTransitMatrix();

    }


    public void removeNonTransitTrips() {

        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2= new AtomicInteger(0);

        servedZoneList.parallelStream().forEach((Location origLoc) -> {
            int i = origLoc.getId();
            for (Location destLoc : servedZoneList) {
                int j = destLoc.getId();
                if (inVehicle.getValueAt(i, j) == 0) {
                    inTransit.setValueAt(i, j, -1f);
                    inTransit.setValueAt(j, i, -1f);

                    accessTime.setValueAt(i, j, -1f);
                    accessTime.setValueAt(j, i, -1f);

                    egressTime.setValueAt(i, j, -1f);
                    egressTime.setValueAt(j, i, -1f);

                    transfers.setValueAt(i, j, -1f);
                    transfers.setValueAt(j, i, -1f);

                    inVehicle.setValueAt(i, j, -1f);
                    inVehicle.setValueAt(j, i, -1f);

                    totalTime.setValueAt(i, j, -1f);
                    totalTime.setValueAt(j, i, -1f);

                    counter1.incrementAndGet();
                } else if (accessTime.getValueAt(i, j) > minutesThreshold || egressTime.getValueAt(i, j) > minutesThreshold) {

                    inTransit.setValueAt(i, j, -1f);
                    inTransit.setValueAt(j, i, -1f);

                    accessTime.setValueAt(i, j, -1f);
                    accessTime.setValueAt(j, i, -1f);

                    egressTime.setValueAt(i, j, -1f);
                    egressTime.setValueAt(j, i, -1f);

                    transfers.setValueAt(i, j, -1f);
                    transfers.setValueAt(j, i, -1f);

                    inVehicle.setValueAt(i, j, -1f);
                    inVehicle.setValueAt(j, i, -1f);

                    totalTime.setValueAt(i, j, -1f);
                    totalTime.setValueAt(j, i, -1f);
                    counter2.incrementAndGet();

                }
            }

        });

        log.info("Removed " + counter1.toString() + " trips where there is not in-vehicle time");
        log.info("Removed " + counter2.toString() + " because access or egress is higher then " + minutesThreshold + " minutes");
    }

    public void generateTheNonServedZoneList(){
        for (Location loc : locationList){
            if (!servedZoneList.contains(loc)){
                nonServedZoneList.add(loc);
            }
        }
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

        int numberOfNonservedZones = nonServedZoneList.size();

        //for each origin i non served
        nonServedZoneList.parallelStream().forEach((Location origLoc) -> {
            int i = origLoc.getId();
            float access;
            float egress;
            float tt;
            //for each destination j including all the possible destinations
            for (Location destLoc : locationList) {
                int j = destLoc.getId();
                if (i != j) {
                    tt = Float.MAX_VALUE;
                    float addAccessTime = 0;
                    float addEgressTime = 0;
                    int startZoneIndex = 0;
                    int finalZoneIndex = 0;
                    //look for boarding station
                    for (Location k : servedZoneList) {
                        access = (float) (autoTravelDistance.getValueAt(i, k.getId()) / 1.4 / 60);
                        if (access < minutesThreshold) {
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

                    if (startZoneIndex != 0 && finalZoneIndex != 0) {
                        if (tt > 0) {
                            //found the best k and l that link i and j by transit inn tt mins
                            inTransitCompleteMatrix.setValueAt(i, j, inTransit.getValueAt(startZoneIndex, finalZoneIndex));
                            inTransitCompleteMatrix.setValueAt(j, i, inTransit.getValueAt(startZoneIndex, finalZoneIndex));

                            accessTimeCompleteMatrix.setValueAt(i, j, addAccessTime);
                            accessTimeCompleteMatrix.setValueAt(j, i, addEgressTime);

                            egressTimeCompleteMatrix.setValueAt(i, j, addEgressTime);
                            egressTimeCompleteMatrix.setValueAt(j, i, addAccessTime);

                            transfersCompleteMatrix.setValueAt(i, j, transfers.getValueAt(startZoneIndex, finalZoneIndex));
                            transfersCompleteMatrix.setValueAt(j, i, transfers.getValueAt(startZoneIndex, finalZoneIndex));

                            inVehicleTimeCompleteMatrix.setValueAt(i, j, inVehicle.getValueAt(startZoneIndex, finalZoneIndex));
                            inVehicleTimeCompleteMatrix.setValueAt(j, i, inVehicle.getValueAt(startZoneIndex, finalZoneIndex));

                            totalTimeCompleteMatrix.setValueAt(i, j, tt);
                            totalTimeCompleteMatrix.setValueAt(j, i, tt);
                        }
                    }
                }
            }
            counter.incrementAndGet();
            int percentage = Math.round(counter.floatValue()/numberOfNonservedZones*100);
            log.info(percentage+ "% zones completed");
        });
    }

    public void writeFilledMatrices() {

        String omxPtFileName = munich.getString("pt.total.skim.file") + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(totalTimeCompleteMatrix, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.in.skim.file") + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(inTransitCompleteMatrix, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.access.skim.file") + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(accessTimeCompleteMatrix, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.egress.skim.file") + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(egressTimeCompleteMatrix, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.transfer.skim.file") + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(transfersCompleteMatrix, omxPtFileName, "mat1");

        omxPtFileName = munich.getString("pt.in.vehicle.skim.file") + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createOmxSkimMatrix(inVehicleTimeCompleteMatrix, omxPtFileName, "mat1");


    }


}
