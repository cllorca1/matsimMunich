package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.configMatsim.MatsimRunFromJava;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimGravityModel;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimPopulationCreator;
import org.matsim.munichArea.configMatsim.createDemandPt.PtSyntheticTraveller;
import org.matsim.munichArea.configMatsim.createDemandPt.ReadZonesServedByTransit;
import org.matsim.munichArea.configMatsim.planCreation.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.munichArea.configMatsim.planCreation.ReadSyntheticPopulation;
import org.matsim.munichArea.outputCreation.EuclideanDistanceCalculator;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;
import org.matsim.munichArea.outputCreation.accessibilityCalculator.Accessibility;
import org.matsim.munichArea.outputCreation.transitSkim.TransitSkimCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class RunMATSimScalingAnalysis {


    public static ResourceBundle rb;

    public static void main(String[] args) {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        boolean createNetwork = ResourceUtil.getBooleanProperty(rb, "create.network");
        boolean autoTimeSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.times");
        boolean autoDistSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.dist");
        boolean eucliddistSkims = ResourceUtil.getBooleanProperty(rb, "skim.eucliddist");
        boolean analyzeAccessibility = ResourceUtil.getBooleanProperty(rb, "analyze.accessibility");
        String networkFile = rb.getString("network.folder") + rb.getString("xml.network.file");
        String scheduleFile = rb.getString("network.folder") + rb.getString("schedule.file");
        String vehicleFile = rb.getString("network.folder") + rb.getString("vehicle.file");
        String simulationName = rb.getString("simulation.name");
        int year = Integer.parseInt(rb.getString("simulation.year"));
        int hourOfDay = Integer.parseInt(rb.getString("hour.of.day"));

        boolean useSp = ResourceUtil.getBooleanProperty(rb, "use.sp");

        //create network from OSM file
        if (createNetwork) CreateNetwork.createNetwork();

        //read centroids and get list of locations
        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
        ArrayList<Location> locationList = centroidsToLocations.readCentroidList();


        //get arrays of parameters for single runs
        double[] tripScalingFactorVector = ResourceUtil.getDoubleArray(rb, "trip.scaling.factor");
        int[] lastIterationVector = ResourceUtil.getIntegerArray(rb, "last.iteration");

        //initialize matrices
        Matrix autoTravelTime = new Matrix(locationList.size(), locationList.size());
        Matrix autoTravelDistance = new Matrix(locationList.size(), locationList.size());


        for (int iterations : lastIterationVector) //loop iteration vector
            for (double tripScalingFactor : tripScalingFactorVector) {  //loop trip Scaling


                double flowCapacityExponent = Double.parseDouble(rb.getString("cf.exp"));
                double stroageFactorExponent = Double.parseDouble(rb.getString("sf.exp"));


                double flowCapacityFactor = Math.pow(tripScalingFactor, flowCapacityExponent);
                System.out.println("Starting MATSim simulation. Sampling factor = " + tripScalingFactor);
                double storageCapacityFactor = Math.pow(tripScalingFactor, stroageFactorExponent);

                //update simulation name
                String singleRunName = String.format("TF%.2fCF%.2fSF%.2fIT%d", tripScalingFactor, flowCapacityFactor, storageCapacityFactor, iterations) + simulationName;
                String outputFolder = rb.getString("output.folder") + singleRunName;


                Population matsimPopulation;

                //two alternative methods to create the demand, the second one allows the use of transit synt. travellers
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
                    matsimPopulationCreator.createMatsimPopulation(locationList, 2013, true, tripScalingFactor);
                    matsimPopulation = matsimPopulationCreator.getMatsimPopulation();

                }

                //calculate stuck time based on assumptions for scaling - default = 10
                double stuckTime;
                //scale up:
                stuckTime = 10 / tripScalingFactor;


                //get travel times and run Matsim
                MatsimRunFromJava matsimRunner = new MatsimRunFromJava(rb);
                matsimRunner.runMatsim(hourOfDay * 60 * 60, 1,
                        networkFile, matsimPopulation, year,
                        TransformationFactory.WGS84, iterations, simulationName,
                        outputFolder, tripScalingFactor, flowCapacityFactor, storageCapacityFactor, locationList,
                        autoTimeSkims, autoDistSkims, scheduleFile, vehicleFile, (float) stuckTime, false);

                if (autoTimeSkims) autoTravelTime = matsimRunner.getAutoTravelTime();
                if (autoDistSkims) autoTravelDistance = matsimRunner.getAutoTravelDistance();


                if (eucliddistSkims) {
                    EuclideanDistanceCalculator edc = new EuclideanDistanceCalculator();
                    Matrix euclideanDistanceMatrix = edc.createEuclideanDistanceMatrix(locationList);
                    String omxDistFileName = rb.getString("skim.eucliddist.file") + simulationName + ".omx";
                    TravelTimeMatrix.createOmxSkimMatrix(euclideanDistanceMatrix, locationList, omxDistFileName, "distance");
                }


                if (autoTimeSkims) {
//                        String omxFileName = rb.getString("out.skim.auto.time") + simulationName + ".omx";
                    String omxFileName = rb.getString("out.skim.auto.time") + singleRunName + ".omx";
                    TravelTimeMatrix.createOmxSkimMatrix(autoTravelTime, locationList, omxFileName, "mat1");

                }
                if (autoDistSkims) {
                    String omxFileName = rb.getString("out.skim.auto.dist") + simulationName + ".omx";
                    TravelTimeMatrix.createOmxSkimMatrix(autoTravelDistance, locationList, omxFileName, "mat1");
                }


            }


        if (analyzeAccessibility) {

            String omxFile = rb.getString("omx.access.calc") + ".omx";
            Accessibility acc = new Accessibility(omxFile, "mat1", rb);
            acc.calculateAccessibility(locationList);
            acc.calculateTravelTimesToZone(locationList, 3611);

            //acc.calculateTransfersToZone(servedZoneList, 3612);
            acc.printAccessibility(locationList);
        }


        //run MATSim from file configs
//        matsimRunFromFile();


    }


}

