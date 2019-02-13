package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.configMatsim.MatsimRunFromJava;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimPopulationCreator;
import org.matsim.munichArea.configMatsim.createDemandPt.PtSyntheticTraveller;
import org.matsim.munichArea.configMatsim.createDemandPt.ReadZonesServedByTransit;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;
import org.matsim.munichArea.outputCreation.transitSkim.TransitSkimCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class RunMATSimTransitSkims {
    public static ResourceBundle rb;

    public static void main(String[] args) {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        boolean autoTimeSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.times");
        boolean autoDistSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.dist");
        //boolean ptSkimsFromEvents = ResourceUtil.getBooleanProperty(rb, "skim.pt.events");

        String networkFile = rb.getString("network.folder") + rb.getString("xml.network.file");
        String scheduleFile = rb.getString("network.folder") + rb.getString("schedule.file");
        String vehicleFile = rb.getString("network.folder") + rb.getString("vehicle.file");
        String simulationName = rb.getString("simulation.name");
        int year = Integer.parseInt(rb.getString("simulation.year"));
        int hourOfDay = Integer.parseInt(rb.getString("hour.of.day"));


        //read centroids and get list of locations
        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
        ArrayList<Location> locationList = centroidsToLocations.readCentroidList();

        ReadZonesServedByTransit servedZoneReader = new ReadZonesServedByTransit(rb);
        ArrayList<Location> servedZoneList = servedZoneReader.readZonesServedByTransit(locationList);

        //get arrays of parameters for single runs
        double[] tripScalingFactorVector = ResourceUtil.getDoubleArray(rb, "trip.scaling.factor");
        double tripScalingFactor = tripScalingFactorVector[0];
        int[] lastIterationVector = ResourceUtil.getIntegerArray(rb, "last.iteration");
        int iterations = lastIterationVector[0];
        double flowCapacityExponent = Double.parseDouble(rb.getString("cf.exp"));
        double stroageFactorExponent = Double.parseDouble(rb.getString("sf.exp"));

        //initialize matrices
        Matrix autoTravelTime = new Matrix(locationList.size(), locationList.size());
        Matrix autoTravelDistance = new Matrix(locationList.size(), locationList.size());
        Matrix transitTotalTime = new Matrix(locationList.size(), locationList.size());
        transitTotalTime.fill(-1F);
        Matrix transitInTime = new Matrix(locationList.size(), locationList.size());
        transitInTime.fill(-1F);
        Matrix transitTransfers = new Matrix(locationList.size(), locationList.size());
        transitTransfers.fill(-1F);
        Matrix inVehicleTime = new Matrix(locationList.size(), locationList.size());
        inVehicleTime.fill(-1F);
        Matrix transitAccessTt = new Matrix(locationList.size(), locationList.size());
        transitAccessTt.fill(-1F);
        Matrix transitEgressTt = new Matrix(locationList.size(), locationList.size());
        transitEgressTt.fill(-1F);
        Matrix transitDistance = new Matrix(locationList.size(), locationList.size());
        transitDistance.fill(-1F);

        String[][] routeMatrix = new String[locationList.size()][locationList.size()];

        double flowCapacityFactor = Math.pow(tripScalingFactor, flowCapacityExponent);
        System.out.println("Starting MATSim simulation. Sampling factor = " + tripScalingFactor);
        double storageCapacityFactor = Math.pow(tripScalingFactor, stroageFactorExponent);

        //update simulation name
        String singleRunName = String.format("TF%.2fCF%.2fSF%.2fIT%d", tripScalingFactor, flowCapacityFactor, storageCapacityFactor, iterations) + simulationName;
        String outputFolder = rb.getString("output.folder") + singleRunName;

        int maxSubRuns;
        int min;
        int max = 0;


        //if (ptSkimsFromEvents) {
        String omxPtFileName = rb.getString("pt.total.skim.file")  + "_served" + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createOmxSkimMatrix(transitTotalTime,  omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.in.skim.file")  + "_served" + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createOmxSkimMatrix(transitInTime,  omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.transfer.skim.file") + "_served"  + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createOmxSkimMatrix(transitTransfers,  omxPtFileName, "mat1");
//
        omxPtFileName = rb.getString("pt.in.vehicle.skim.file")  + "_served" + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createOmxSkimMatrix(inVehicleTime,  omxPtFileName, "mat1");
//
        omxPtFileName = rb.getString("pt.access.skim.file") + "_served"  + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createOmxSkimMatrix(transitAccessTt,  omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.egress.skim.file") + "_served"  + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createOmxSkimMatrix(transitEgressTt,  omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.route.skim.file") + "_served"  + ".csv";
        TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
//            TravelTimeMatrix.createStringCSVSkimMatrix(routeMatrix, locationList, omxPtFileName, "mat1");
        // }


        //start new loop
        //if (ptSkimsFromEvents) {
        maxSubRuns = Integer.parseInt(rb.getString("number.submatrices"));
        //}

        for (int subRun = 0; subRun <= maxSubRuns; subRun++) {

            min = max;
            max = (int) (Math.sqrt(Math.pow(servedZoneList.size(), 2) / maxSubRuns + Math.pow(min, 2)));

            max = Math.min(max, servedZoneList.size());


            ArrayList<Location> shortServedZoneList = new ArrayList<>();
            shortServedZoneList.addAll(servedZoneList.subList(min, max));


            if (maxSubRuns > 1) {
                System.out.println("sub-iteration: " + subRun);
                System.out.println("getting PT skim matrix between zone " + min + " and zone " + max + " which count a total of " + shortServedZoneList.size());
            }


            Population matsimPopulation;
            Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap;

            //two alternative methods to create the demand, the second one allows the use of transit synt. travellers

            MatsimPopulationCreator matsimPopulationCreator = new MatsimPopulationCreator(rb);
            matsimPopulationCreator.createMatsimPopulation(locationList, 2013, true, 0);
            matsimPopulation = matsimPopulationCreator.getMatsimPopulation();
            //if (ptSkimsFromEvents) {
            matsimPopulationCreator.createSyntheticPtPopulation(servedZoneList, shortServedZoneList);
            ptSyntheticTravellerMap = matsimPopulationCreator.getPtSyntheticTravellerMap();
            //}

            //get travel times and run Matsim
            MatsimRunFromJava matsimRunner = new MatsimRunFromJava(rb);
            matsimRunner.configureMatsim(networkFile, year, TransformationFactory.DHDN_GK4, iterations, simulationName, outputFolder,
                    flowCapacityFactor, storageCapacityFactor, scheduleFile, vehicleFile, 10, Boolean.parseBoolean(rb.getString("use.transit")));

            matsimRunner.setMatsimPopulationAndInitialize(matsimPopulation);
            matsimRunner.runMatsim();


            //if (ptSkimsFromEvents) {
            String eventFile = outputFolder + "/" + simulationName + "_" + year + ".output_events.xml.gz";
            TransitSkimCreator ptEH = new TransitSkimCreator();

            ptEH.runPtEventAnalyzer(eventFile, ptSyntheticTravellerMap, matsimRunner.getNetwork());

            transitTotalTime = ptEH.ptTotalTime(ptSyntheticTravellerMap, transitTotalTime);
            transitInTime = ptEH.ptInTransitTime(ptSyntheticTravellerMap, transitInTime);
            transitTransfers = ptEH.ptTransfers(ptSyntheticTravellerMap, transitTransfers);
            inVehicleTime = ptEH.inVehicleTt(ptSyntheticTravellerMap, inVehicleTime);
            transitAccessTt = ptEH.transitAccessTt(ptSyntheticTravellerMap, transitAccessTt);
            transitEgressTt = ptEH.transitEgressTt(ptSyntheticTravellerMap, transitEgressTt);
            transitDistance = ptEH.transitDistance(ptSyntheticTravellerMap, transitDistance);
            routeMatrix = ptEH.ptRouteMatrix(ptSyntheticTravellerMap, routeMatrix);


            omxPtFileName = rb.getString("pt.total.skim.file")  + "_served" + ".omx";
            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(transitTotalTime, omxPtFileName, "mat1");

            omxPtFileName = rb.getString("pt.in.skim.file")  + "_served" + ".omx";
            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(transitInTime, omxPtFileName, "mat1");

            omxPtFileName = rb.getString("pt.transfer.skim.file")  + "_served"+ ".omx";
            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(transitTransfers, omxPtFileName, "mat1");

            omxPtFileName = rb.getString("pt.in.vehicle.skim.file") + "_served" + ".omx";
            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(inVehicleTime, omxPtFileName, "mat1");

            omxPtFileName = rb.getString("pt.access.skim.file") + "_served" + ".omx";
            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(transitAccessTt, omxPtFileName, "mat1");

            omxPtFileName = rb.getString("pt.egress.skim.file") + "_served" + ".omx";
            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList.size());
            TravelTimeMatrix.createOmxSkimMatrix(transitEgressTt, omxPtFileName, "mat1");


            // }


        }
        //end of the new loop


        //if (ptSkimsFromEvents) {

        omxPtFileName = rb.getString("pt.route.skim.file")  + "_served" + ".csv";
//            TravelTimeMatrix.createOmxFile(omxPtFileName, locationList);
        TravelTimeMatrix.createStringCSVSkimMatrix(routeMatrix, locationList, omxPtFileName, "mat1");
        //}

    }

}

