package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.configMatsim.DistListener;
import org.matsim.munichArea.configMatsim.IntrazonalTravelTimeCalculator;
import org.matsim.munichArea.configMatsim.MatsimRunFromJava;
import org.matsim.munichArea.configMatsim.TimeListener;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimPopulationCreator;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.munichArea.configMatsim.planCreation.ReadSyntheticPopulation;
import org.matsim.munichArea.configMatsim.planCreation.externalFlows.LongDistanceTraffic;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;

import java.io.File;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

//class to run single matsim scenarios and collect travel time matrices (road network)

public class RunMATSim {

    public static ResourceBundle rb;

    public static void main(String[] args) {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        boolean autoTimeSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.times");
        boolean autoDistSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.dist");
        boolean addExternalFlows = ResourceUtil.getBooleanProperty(rb, "add.external.flows");
        boolean calculateIntrazonals = ResourceUtil.getBooleanProperty(rb, "get.intrazonals");
        String networkFile = rb.getString("network.folder") + rb.getString("xml.network.file");
        String scheduleFile = rb.getString("network.folder") + rb.getString("schedule.file");
        String vehicleFile = rb.getString("network.folder") + rb.getString("vehicle.file");
        String simulationName = rb.getString("simulation.name");
        int year = Integer.parseInt(rb.getString("simulation.year"));
        //int hourOfDay = Integer.parseInt(rb.getString("hour.of.day"));
        int[] hoursOfDay = ResourceUtil.getIntegerArray(rb, "hours.of.day");

        boolean useSp = ResourceUtil.getBooleanProperty(rb, "use.sp");

        //read centroids and get list of locations
        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
        ArrayList<Location> locationList = centroidsToLocations.readCentroidList();

        //get parameters for single run
        double[] tripScalingFactorVector = ResourceUtil.getDoubleArray(rb, "trip.scaling.factor");
        double tripScalingFactor = tripScalingFactorVector[0];
        int[] lastIterationVector = ResourceUtil.getIntegerArray(rb, "last.iteration");
        int iterations = lastIterationVector[0];
        double flowCapacityExponent = Double.parseDouble(rb.getString("cf.exp"));
        double stroageFactorExponent = Double.parseDouble(rb.getString("sf.exp"));

        Map<Integer, Matrix> autoTravelTimes = new HashMap<>(); //map by hour
        Map<String, Matrix> autoTravelDistances = new HashMap<>(); //map by type
        int sizeOfMatrix = locationList.size();
        //initialize auto skim matrices
        for (int hourOfDay : hoursOfDay) {
            autoTravelTimes.put(hourOfDay, new Matrix(sizeOfMatrix, sizeOfMatrix));
            //autoTravelDistances.put(hourOfDay, new Matrix(sizeOfMatrix, sizeOfMatrix));
        }

        //calculate capacity factors
        double flowCapacityFactor = Math.pow(tripScalingFactor, flowCapacityExponent);
        System.out.println("Starting MATSim simulation. Sampling factor = " + tripScalingFactor);
        double storageCapacityFactor = Math.pow(tripScalingFactor, stroageFactorExponent);

        //update simulation name
        String singleRunName = String.format("TF%.2fCF%.2fSF%.2fIT%d", tripScalingFactor, flowCapacityFactor, storageCapacityFactor, iterations) + simulationName;
        String outputFolder = rb.getString("output.folder") + singleRunName;


        //alternative methods --> alternative gravity model that doesn't create other than a OD matrix with counts
        Population matsimPopulation;

        //two alternative methods to create the demand
        if (useSp) {
            ReadSyntheticPopulation readSp = new ReadSyntheticPopulation(rb, locationList);
            readSp.demandFromSyntheticPopulation(0, (float) tripScalingFactor, "sp/output/plans.xml.gz");
            matsimPopulation = readSp.getMatsimPopulation();
            readSp.printHistogram();
            readSp.printSyntheticPlansList("./sp/output/plansAuto.csv", 0);
            readSp.printSyntheticPlansList("./sp/output/plansWalk.csv", 1);
            readSp.printSyntheticPlansList("./sp/output/plansCycle.csv", 2);
            readSp.printSyntheticPlansList("./sp/output/plansTransit.csv", 3);
        } else {
            MatsimPopulationCreator matsimPopulationCreator = new MatsimPopulationCreator(rb);
            matsimPopulationCreator.createMatsimPopulation(locationList, 2013, false, tripScalingFactor);
            matsimPopulation = matsimPopulationCreator.getMatsimPopulation();
        }

        if (addExternalFlows) {
            LongDistanceTraffic longDistanceTraffic = new LongDistanceTraffic(rb);
            longDistanceTraffic.readZones();
            longDistanceTraffic.readMatrices();
            matsimPopulation = longDistanceTraffic.addLongDistancePlans(tripScalingFactor, matsimPopulation);
        }
        //run Matsim and get travel times
        MatsimRunFromJava matsimRunner = new MatsimRunFromJava(rb);
        matsimRunner.configureMatsim(networkFile, year, TransformationFactory.DHDN_GK4, iterations, simulationName, outputFolder,
                flowCapacityFactor, storageCapacityFactor, scheduleFile, vehicleFile, 10, Boolean.parseBoolean(rb.getString("use.transit")));

        matsimRunner.setMatsimPopulationAndInitialize(matsimPopulation);

        if (autoTimeSkims) {
            for (int hourOfDay : hoursOfDay) {
                autoTravelTimes.put(hourOfDay, matsimRunner.addTimeSkimMatrixCalculator(hourOfDay, 1, locationList));
            }
        }

        if (autoDistSkims) {
            for (int hourOfDay : hoursOfDay) {
                autoTravelDistances = matsimRunner.addDistanceSkimMatrixCalculator(hourOfDay, 1, locationList);
            }
        }


        if (calculateIntrazonals){
            matsimRunner.addIntrazonalTravelTimeCalculator(locationList, rb.getString("intrazonal.file"), Util.loadZoneShapeFile(rb.getString("zone.shapefile"),"id" ));


        }

        matsimRunner.runMatsim();

        if (autoTimeSkims) {
            String omxFileName = rb.getString("out.skim.auto.time") + simulationName + ".omx";
            TravelTimeMatrix.createOmxFile(omxFileName, locationList.size());
            for (int hourOfDay : hoursOfDay) {
                TravelTimeMatrix.createOmxSkimMatrix(autoTravelTimes.get(hourOfDay),  omxFileName, "tt" + hourOfDay);
            }
        }
        if (autoDistSkims) {
            String omxFileName = rb.getString("out.skim.auto.dist") + simulationName + ".omx";
            TravelTimeMatrix.createOmxFile(omxFileName, locationList.size());
            for (String type : autoTravelDistances.keySet()) {
                TravelTimeMatrix.createOmxSkimMatrix(autoTravelDistances.get(type), omxFileName, type);
            }
        }


    }
}
