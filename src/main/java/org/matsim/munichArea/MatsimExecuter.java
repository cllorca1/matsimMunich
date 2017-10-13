package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimGravityModel;
import org.matsim.munichArea.configMatsim.createDemandPt.MatsimPopulationCreator;
import org.matsim.munichArea.configMatsim.MatsimRunFromJava;
import org.matsim.munichArea.configMatsim.createDemandPt.PtSyntheticTraveller;
import org.matsim.munichArea.configMatsim.createDemandPt.ReadZonesServedByTransit;
import org.matsim.munichArea.outputCreation.EuclideanDistanceCalculator;
import org.matsim.munichArea.outputCreation.accessibilityCalculator.Accessibility;
import org.matsim.munichArea.outputCreation.transitSkim.TransitSkimCreator;
import org.matsim.munichArea.configMatsim.planCreation.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;
import org.matsim.munichArea.configMatsim.planCreation.ReadSyntheticPopulation;


import java.io.File;
import java.util.*;

import static java.lang.Integer.valueOf;


/**
 * Created by carlloga on 9/12/2016.
 */
public class MatsimExecuter {

    public static ResourceBundle rb;

    public static void main(String[] args) {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        boolean createNetwork = ResourceUtil.getBooleanProperty(rb, "create.network");
        boolean runMatsim = ResourceUtil.getBooleanProperty(rb, "run.matsim");
        boolean runGravityModel = ResourceUtil.getBooleanProperty(rb, "run.gravity.model");
        boolean autoTimeSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.times");
        boolean autoDistSkims = ResourceUtil.getBooleanProperty(rb, "skim.auto.dist");
        boolean ptSkimsFromEvents = ResourceUtil.getBooleanProperty(rb, "skim.pt.events");
        boolean eucliddistSkims = ResourceUtil.getBooleanProperty(rb, "skim.eucliddist");
        boolean analyzeAccessibility = ResourceUtil.getBooleanProperty(rb, "analyze.accessibility");
        boolean visualize = ResourceUtil.getBooleanProperty(rb, "run.oftvis");
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

        //to make test reduce the size
        /*ArrayList<Location> shortLocationList = new ArrayList<>();
        shortLocationList.addAll(locationList.subList(0, 200));
        locationList = shortLocationList;
        System.out.println(locationList.size());*/

        ReadZonesServedByTransit servedZoneReader = new ReadZonesServedByTransit(rb);
        ArrayList<Location> servedZoneList = servedZoneReader.readZonesServedByTransit(locationList);

        //get arrays of parameters for single runs
        double[] tripScalingFactorVector = ResourceUtil.getDoubleArray(rb, "trip.scaling.factor");
        int[] lastIterationVector = ResourceUtil.getIntegerArray(rb, "last.iteration");

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


        if (runMatsim) {
            for (int iterations : lastIterationVector) //loop iteration vector
                for (double tripScalingFactor : tripScalingFactorVector) {  //loop trip Scaling


                    double flowCapacityExponent = Double.parseDouble(rb.getString("cf.exp"));
                    double stroageFactorExponent = Double.parseDouble(rb.getString("sf.exp"));


                    double flowCapacityFactor =  Math.pow(tripScalingFactor,flowCapacityExponent);
                    System.out.println("Starting MATSim simulation. Sampling factor = " + tripScalingFactor);
                    double storageCapacityFactor = Math.pow(tripScalingFactor,stroageFactorExponent);

                    //update simulation name
                    String singleRunName = String.format("TF%.2fCF%.2fSF%.2fIT%d", tripScalingFactor, flowCapacityFactor, storageCapacityFactor, iterations) + simulationName;
                    String outputFolder = rb.getString("output.folder") + singleRunName;

                    int maxSubRuns = 1;
                    int min;
                    int max = 0;

                    //start new loop
                    if (ptSkimsFromEvents) {
                        maxSubRuns = Integer.parseInt(rb.getString("number.submatrices"));
                    }

                    for (int subRun = 0; subRun < maxSubRuns; subRun++) {

                        min = max;
                        max = (int) (Math.sqrt(Math.pow(servedZoneList.size(), 2)/ maxSubRuns + Math.pow(min, 2)));

                        max = Math.min(max, servedZoneList.size());

                        ArrayList<Location> shortServedZoneList = new ArrayList<>();
                        shortServedZoneList.addAll(servedZoneList.subList(min, max));


                        if (maxSubRuns>1) {
                            System.out.println("sub-iteration: " + subRun);
                            System.out.println("getting PT skim matrix between zone " + min + " and zone " + max + " which count a total of " + shortServedZoneList.size());
                        }

                        //alternative methods --> alternative gravity model that doesn't create other than a OD matrix with counts
                        if (runGravityModel) {
                            MatsimGravityModel.createMatsimPopulation(locationList, 2013, false, tripScalingFactor);
                        }

                        Population matsimPopulation;
                        Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap = new HashMap<>();

                        //two alternative methods to create the demand, the second one allows the use of transit synt. travellers
                        if (useSp){
                            ReadSyntheticPopulation readSp = new ReadSyntheticPopulation(rb, locationList);
                            readSp.demandFromSyntheticPopulation(0, (float) tripScalingFactor, "sp/plans.xml");
                            matsimPopulation = readSp.getMatsimPopulation();
                            readSp.printHistogram();
                            readSp.printSyntheticPlansList("./sp/plansAuto.csv" , 0);
                            readSp.printSyntheticPlansList("./sp/plansWalk.csv" , 1);
                            readSp.printSyntheticPlansList("./sp/plansCycle.csv" , 2);
                            readSp.printSyntheticPlansList("./sp/plansTransit.csv" , 3);
                        } else{
                            MatsimPopulationCreator matsimPopulationCreator = new MatsimPopulationCreator();
                            matsimPopulationCreator.createMatsimPopulation(locationList, 2013, true, tripScalingFactor);
                            matsimPopulation= matsimPopulationCreator.getMatsimPopulation();
                            if (ptSkimsFromEvents) {
                                matsimPopulationCreator.createSyntheticPtPopulation(servedZoneList, shortServedZoneList);
                                ptSyntheticTravellerMap = matsimPopulationCreator.getPtSyntheticTravellerMap();
                            }
                        }


                        //get travel times and run Matsim
                        MatsimRunFromJava matsimRunner = new MatsimRunFromJava(rb);
                        matsimRunner.runMatsim(hourOfDay * 60 * 60, 1,
                                networkFile, matsimPopulation, year,
                                TransformationFactory.WGS84, iterations, simulationName,
                                outputFolder, tripScalingFactor, flowCapacityFactor, storageCapacityFactor , locationList, autoTimeSkims, autoDistSkims, scheduleFile, vehicleFile);

                        if (autoTimeSkims) autoTravelTime = matsimRunner.getAutoTravelTime();
                        if (autoDistSkims) autoTravelDistance = matsimRunner.getAutoTravelDistance();




                        //visualization
                        if (visualize) {
                            //program arguments
                            String arguments[] = new String[5];
                            arguments[1] = outputFolder + "/" + simulationName + "_" + year + ".output_events.xml.gz";
                            arguments[2] = outputFolder + "/" + simulationName + "_" + year + ".output_network.xml.gz";
                            arguments[3] = rb.getString("output.mvi.file");
                            arguments[4] = rb.getString("seconds.frame");
                            //run the conversion
                            org.matsim.contrib.otfvis.OTFVis.convert(arguments);
                            //run the visualization
                            org.matsim.contrib.otfvis.OTFVis.playMVI(arguments[3]);
                        }

                        if (ptSkimsFromEvents) {
                            String eventFile = outputFolder + "/" + simulationName + "_" + year + ".output_events.xml.gz";
                            TransitSkimCreator ptEH = new TransitSkimCreator();

                            ptEH.runPtEventAnalyzer(eventFile, ptSyntheticTravellerMap, matsimRunner.getNetwork() );

                            transitTotalTime = ptEH.ptTotalTime(ptSyntheticTravellerMap, transitTotalTime);
                            transitInTime = ptEH.ptInTransitTime(ptSyntheticTravellerMap, transitInTime);
                            transitTransfers = ptEH.ptTransfers(ptSyntheticTravellerMap, transitTransfers);
                            inVehicleTime = ptEH.inVehicleTt(ptSyntheticTravellerMap, inVehicleTime);
                            transitAccessTt = ptEH.transitAccessTt(ptSyntheticTravellerMap, transitAccessTt);
                            transitEgressTt = ptEH.transitEgressTt(ptSyntheticTravellerMap, transitEgressTt);
                            transitDistance = ptEH.transitDistance(ptSyntheticTravellerMap, transitDistance);
                            routeMatrix = ptEH.ptRouteMatrix(ptSyntheticTravellerMap, routeMatrix);

                        }

                    }
                    //end of the new loop

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
                    if (autoDistSkims){
                        String omxFileName = rb.getString("out.skim.auto.dist") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(autoTravelDistance, locationList, omxFileName, "mat1");
                    }

                    if (ptSkimsFromEvents) {
                        String omxPtFileName = rb.getString("pt.total.skim.file") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(transitTotalTime, locationList, omxPtFileName, "mat1");

                        omxPtFileName = rb.getString("pt.in.skim.file") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(transitInTime, locationList, omxPtFileName, "mat1");

                        omxPtFileName = rb.getString("pt.transfer.skim.file") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(transitTransfers, locationList, omxPtFileName, "mat1");

                        omxPtFileName = rb.getString("pt.in.vehicle.skim.file") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(inVehicleTime, locationList, omxPtFileName, "mat1");

                        omxPtFileName = rb.getString("pt.access.skim.file") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(transitAccessTt, locationList, omxPtFileName, "mat1");

                        omxPtFileName = rb.getString("pt.egress.skim.file") + simulationName + ".omx";
                        TravelTimeMatrix.createOmxSkimMatrix(transitEgressTt, locationList, omxPtFileName, "mat1");

                        omxPtFileName = rb.getString("pt.route.skim.file") + simulationName + ".csv";
                        TravelTimeMatrix.createStringCSVSkimMatrix(routeMatrix, locationList, omxPtFileName, "mat1");
                    }
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
