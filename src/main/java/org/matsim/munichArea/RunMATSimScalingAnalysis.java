package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.configMatsim.MatsimRunFromJava;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.munichArea.configMatsim.planCreation.ReadSyntheticPopulation;
import org.matsim.munichArea.outputCreation.EuclideanDistanceCalculator;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class RunMATSimScalingAnalysis {


    public static ResourceBundle rb;

    public static void main(String[] args) {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);


        boolean autoTimeSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.times");
        boolean autoDistSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.dist");
        boolean eucliddistSkims = ResourceUtil.getBooleanProperty(rb, "skim.eucliddist");
        String networkFile = rb.getString("network.folder") + rb.getString("xml.network.file");
        String scheduleFile = rb.getString("network.folder") + rb.getString("schedule.file");
        String vehicleFile = rb.getString("network.folder") + rb.getString("vehicle.file");
        String simulationName = rb.getString("simulation.name");
        int year = Integer.parseInt(rb.getString("simulation.year"));
        int hourOfDay = Integer.parseInt(rb.getString("hour.of.day"));

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

                //generate matsim plans based on SP
                ReadSyntheticPopulation readSp = new ReadSyntheticPopulation(rb, locationList);
                readSp.demandFromSyntheticPopulation(0, (float) tripScalingFactor, "sp/plans.xml");
                matsimPopulation = readSp.getMatsimPopulation();
                readSp.printHistogram();
                readSp.printSyntheticPlansList("./sp/plansAuto.csv", 0);
                readSp.printSyntheticPlansList("./sp/plansWalk.csv", 1);
                readSp.printSyntheticPlansList("./sp/plansCycle.csv", 2);
                readSp.printSyntheticPlansList("./sp/plansTransit.csv", 3);

                //calculate stuck time based on assumptions for scaling - default = 10
                double stuckTime;
                //scale up:
                stuckTime = 10;

                //get travel times and run Matsim
                MatsimRunFromJava matsimRunner = new MatsimRunFromJava(rb);
                matsimRunner.configureMatsim(networkFile, year, TransformationFactory.DHDN_GK4, iterations, simulationName, outputFolder,
                        flowCapacityFactor, storageCapacityFactor, scheduleFile, vehicleFile, (float) stuckTime, Boolean.parseBoolean(rb.getString("use.transit")));

                matsimRunner.setMatsimPopulationAndInitialize(matsimPopulation);

                if (autoTimeSkims) {
                    autoTravelTime = matsimRunner.addTimeSkimMatrixCalculator(hourOfDay, 1, locationList);
                }

                if (autoDistSkims && !autoTimeSkims) {
                    autoTravelDistance = matsimRunner.addDistanceSkimMatrixCalculator(hourOfDay, 1, locationList);
                }

                matsimRunner.runMatsim();

                if (autoTimeSkims) {
                    String omxFileName = rb.getString("out.skim.auto.time") + singleRunName + ".omx";
                    TravelTimeMatrix.createOmxSkimMatrix(autoTravelTime, omxFileName, "mat1");
                }
                if (autoDistSkims){
                    String omxFileName = rb.getString("out.skim.auto.dist") + simulationName + ".omx";
                    TravelTimeMatrix.createOmxSkimMatrix(autoTravelDistance,  omxFileName, "mat1");
                }


                if (eucliddistSkims) {
                    EuclideanDistanceCalculator edc = new EuclideanDistanceCalculator();
                    Matrix euclideanDistanceMatrix = edc.createEuclideanDistanceMatrix(locationList);
                    String omxDistFileName = rb.getString("skim.eucliddist.file") + simulationName + ".omx";
                    TravelTimeMatrix.createOmxSkimMatrix(euclideanDistanceMatrix,  omxDistFileName, "distance");
                }

                if (autoTimeSkims) {
//                        String omxFileName = rb.getString("out.skim.auto.time") + simulationName + ".omx";
                    String omxFileName = rb.getString("out.skim.auto.time") + singleRunName + ".omx";
                    TravelTimeMatrix.createOmxFile(omxFileName, locationList);
                    TravelTimeMatrix.createOmxSkimMatrix(autoTravelTime, omxFileName, "mat1");
                }
                if (autoDistSkims) {
                    String omxFileName = rb.getString("out.skim.auto.dist") + simulationName + ".omx";
                    TravelTimeMatrix.createOmxFile(omxFileName, locationList);
                    TravelTimeMatrix.createOmxSkimMatrix(autoTravelDistance,  omxFileName, "mat1");
                }
            }

    }


}

