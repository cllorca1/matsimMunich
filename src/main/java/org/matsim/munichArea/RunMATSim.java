package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.configMatsim.MatsimRunFromJava;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimPopulationCreator;
import org.matsim.munichArea.configMatsim.planCreation.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.munichArea.configMatsim.planCreation.ReadSyntheticPopulation;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;
import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;

//class to run single matsim scenarios and collect travel time matrices (road network)

public class RunMATSim {

    public static ResourceBundle rb;

    public static void main(String[] args) {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        boolean autoTimeSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.times");
        boolean autoDistSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.dist");
        String networkFile = rb.getString("network.folder") + rb.getString("xml.network.file");
        String scheduleFile = rb.getString("network.folder") + rb.getString("schedule.file");
        String vehicleFile = rb.getString("network.folder") + rb.getString("vehicle.file");
        String simulationName = rb.getString("simulation.name");
        int year = Integer.parseInt(rb.getString("simulation.year"));
        int hourOfDay = Integer.parseInt(rb.getString("hour.of.day"));

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

        //initialize auto skim matrices
        Matrix autoTravelTime = new Matrix(locationList.size(), locationList.size());
        Matrix autoTravelDistance = new Matrix(locationList.size(), locationList.size());

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
            readSp.demandFromSyntheticPopulation(0, (float) tripScalingFactor, "sp/plans.xml");
            matsimPopulation = readSp.getMatsimPopulation();
            readSp.printHistogram();
            readSp.printSyntheticPlansList("./sp/plansAuto.csv", 0);
            readSp.printSyntheticPlansList("./sp/plansWalk.csv", 1);
            readSp.printSyntheticPlansList("./sp/plansCycle.csv", 2);
            readSp.printSyntheticPlansList("./sp/plansTransit.csv", 3);
        } else {
            MatsimPopulationCreator matsimPopulationCreator = new MatsimPopulationCreator(rb);
            matsimPopulationCreator.createMatsimPopulation(locationList, 2013, false, tripScalingFactor);
            matsimPopulation = matsimPopulationCreator.getMatsimPopulation();
        }

        //run Matsim and get travel times
        MatsimRunFromJava matsimRunner = new MatsimRunFromJava(rb);
        matsimRunner.runMatsim(hourOfDay * 60 * 60, Integer.parseInt(rb.getString("max.calc.points")),
                networkFile, matsimPopulation, year,
                TransformationFactory.WGS84, iterations, simulationName,
                outputFolder, tripScalingFactor, flowCapacityFactor, storageCapacityFactor, locationList, autoTimeSkims, autoDistSkims, scheduleFile, vehicleFile,
                10, Boolean.parseBoolean(rb.getString("use.transit")));

        if (autoTimeSkims) autoTravelTime = matsimRunner.getAutoTravelTime();
        if (autoDistSkims) autoTravelDistance = matsimRunner.getAutoTravelDistance();


        //skim matrices if needed
        if (autoTimeSkims) {
            String omxFileName = rb.getString("out.skim.auto.time") + singleRunName + ".omx";
            TravelTimeMatrix.createOmxSkimMatrix(autoTravelTime, locationList, omxFileName, "mat1");

        }
        if (autoDistSkims) {
            String omxFileName = rb.getString("out.skim.auto.dist") + simulationName + ".omx";
            TravelTimeMatrix.createOmxSkimMatrix(autoTravelDistance, locationList, omxFileName, "mat1");
        }

    }

}
