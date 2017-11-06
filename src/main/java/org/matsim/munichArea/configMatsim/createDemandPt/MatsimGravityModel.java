package org.matsim.munichArea.configMatsim.createDemandPt;

/**
 * Created by carlloga on 9/14/2016.
 */

import com.pb.common.matrix.Matrix;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.outputCreation.accessibilityCalculator.Accessibility;
import org.matsim.munichArea.configMatsim.planCreation.Location;

import java.util.*;
import java.util.Map.Entry;

import static org.matsim.munichArea.MatsimExecuter.rb;


/**
 * discontinued
 */
public class MatsimGravityModel {

    public static boolean ASC = true;
    public static boolean DESC = false;


    public static void createMatsimPopulation(ArrayList<Location> locationList, /*HouseholdDataManager householdDataManager,*/ int year,
                                                    /*Map<Integer,SimpleFeature>zoneFeatureMap, String crs, */ boolean writePopulation,
                                                    double tripScalingFactor) {


        // MATSim "infrastructure"
        Config matsimConfig = ConfigUtils.createConfig();
        Scenario matsimScenario = ScenarioUtils.createScenario(matsimConfig);

        Network matsimNetwork = matsimScenario.getNetwork();
        Population matsimPopulation = matsimScenario.getPopulation();
        PopulationFactory matsimPopulationFactory = matsimPopulation.getFactory();


        ArrayList<Location> destList = locationList;
        ArrayList<Location> origList = locationList;

        int personId = 0;

        SkimMatrixReader acc = new SkimMatrixReader();
        Matrix autoTravelTime = acc.readSkim(rb.getString("base.skim.file"), "mat1");

        Random rnd = new Random();
        double alpha = 1.5;
        double g = 0.0000247;
        double minTravelTime = 5;
        Matrix odTrips = new Matrix(locationList.size(), locationList.size());
        float totalTrips = 0;
        float sumTripsLenght = 0;
        int totalEmployments = 0;
        int totalTripsAllDestinations = 0;

        //histogram to calibrate gravity model
        float ttintervalSize = 10;
        float maxTravelTime = 200;
        int numberOfClasses = (int) (maxTravelTime/ttintervalSize);
        float[][] classes = new float[2][numberOfClasses];
        for (int i = 0; i< numberOfClasses; i++){
            classes [0][i] = ttintervalSize*(i+1);
            classes [1][i] = 0;
        }



        //start loop to generate trips to work
        for (Location destLoc : destList) {
            Map<Integer, Double> origZoneWeight = new HashMap<>();
//            double[] origRate = new double[origList.size()];
            double sumOrigRates = 0;
            //first loop to calculate weights
            for (Location origLoc : origList) {
                double weight;
                double travelTime = autoTravelTime.getValueAt(origLoc.getId(), destLoc.getId());
                float trips;
                if (travelTime < minTravelTime) {
//                    origRate[origLoc.getId() - 1] = (origLoc.getPopulation() * g / Math.pow(minTravelTime, alpha));
                    weight = (origLoc.getPopulation() * g / Math.pow(minTravelTime, alpha));
                    trips = (float) weight * destLoc.getEmployment();
                    totalTrips += trips;
                    sumTripsLenght += trips*minTravelTime;

                } else {
//                    origRate[origLoc.getId() - 1] = (origLoc.getPopulation() * g / Math.pow(travelTime, alpha));
                    weight = (origLoc.getPopulation() * g / Math.pow(travelTime, alpha));
                    trips = (float) weight * destLoc.getEmployment();
                    totalTrips += trips;
                    sumTripsLenght += trips*minTravelTime;
                }



                odTrips.setValueAt(origLoc.getId(), destLoc.getId(),trips);

                int i = 0;
                while (travelTime > classes[0][i]){
                    i++;
                }
                classes[1][i]+=trips;


//                sumOrigRates += origRate[origLoc.getId() - 1];
                sumOrigRates += weight;
                origZoneWeight.put(origLoc.getId(), weight);
            }

            totalEmployments += destLoc.getEmployment();

        }

        sumTripsLenght = sumTripsLenght/totalTrips;

        System.out.println("total trips = " + totalTrips);
        float error = totalEmployments/totalTrips;
        System.out.println("employment / trips --> " + error);
        System.out.println("average trip length = " + sumTripsLenght);

        try (OmxFile omxFile = new OmxFile("./data/tripsGravityModel.omx")) {

            int dim0 = locationList.size();

            int dim1 = dim0;
            int[] shape = {dim0,dim1};

            float mat1NA = -1;

            OmxMatrix.OmxFloatMatrix mat1 = new OmxMatrix.OmxFloatMatrix("mat1",odTrips.getValues(),mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(),"trips");

            int lookup1NA = -1;
            int[] lookup1Data = new int[dim0];
            Set<Integer> lookup1Used = new HashSet<>();
            for (int i = 0; i < lookup1Data.length; i++) {
                int lookup = i+1;
                lookup1Data[i] = lookup1Used.add(lookup) ? lookup : lookup1NA;
            }
            OmxLookup.OmxIntLookup lookup1 = new OmxLookup.OmxIntLookup("lookup1",lookup1Data,lookup1NA);

            omxFile.openNew(shape);
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup1);
            omxFile.save();
            System.out.println(omxFile.summary());

            System.out.println("trip matrix written");

            //TODO print histogram


            for (int i=0; i < numberOfClasses; i++) {
                System.out.println(classes[0][i] + "," + classes[1][i]);
            }

            /*float carShare = Float.parseFloat(rb.getString("car.modal.share"));


            //order the map according to decreasing weights
            origZoneWeight = sortByComparator(origZoneWeight, DESC);

            //second loop to normalize weights and calculate integer trips
            Map<Integer, Integer> origZoneTrips = new HashMap<>();
            List<Integer> origOrderedList = new ArrayList<>(origZoneWeight.keySet());
            int totalTripsToDestination = Math.round(destLoc.getEmployment() * carShare);
            int tripsAssigned = 0;

            for (int loc : origOrderedList) {
                int trips = (int) Math.ceil(totalTripsToDestination * origZoneWeight.get(loc) / sumOrigRates);
                if (tripsAssigned <= totalTripsToDestination) {
                    tripsAssigned += trips;
                } else {
                    trips = 0;
                }
                origZoneTrips.put(loc, trips);
            }

            totalTripsAllDestinations += tripsAssigned;

            //third loop to create trips
            for (Location origLoc : origList) {
                int trips = origZoneTrips.get(origLoc.getId());
                for (int i = 0; i < trips; i++) {
                    //select randomly trips whithin the population
                    double randomNumber = Math.random();
                    if (randomNumber < tripScalingFactor) {


                        Person matsimPerson =
                                matsimPopulationFactory.createPerson(Id.create(personId, Person.class));
                        matsimPopulation.addPerson(matsimPerson);
                        personId++;

                        Plan matsimPlan = matsimPopulationFactory.createPlan();
                        matsimPerson.addPlan(matsimPlan);

                        //SimpleFeature homeFeature = zoneFeatureMap.get(origLoc.getId());
                        Coord homeCoordinates = new Coord(origLoc.getX() + origLoc.getSize() * (Math.random() - 0.5), origLoc.getY() + origLoc.getSize() * (Math.random() - 0.5));

//    		Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
                        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        //randomly between 7 and 9 AM
                        double time = 8 * 60 * 60 + rnd.nextGaussian() * 60 * 60;
                        activity1.setEndTime(Math.max(4 * 60 * 60, Math.min(time, 12 * 60 * 60)));
                        matsimPlan.addActivity(activity1);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

//    		SimpleFeature workFeature = featureMap.get(workPuma);
                        //SimpleFeature workFeature = zoneFeatureMap.get(destLoc.getId());
                        Coord workCoordinates = new Coord(destLoc.getX() + destLoc.getSize() * (Math.random() - 0.5), destLoc.getY() + destLoc.getSize() * (Math.random() - 0.5));
//    		Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", ct.transform(workCoordinates));
                        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
                        //randomly between 4 and 8 PM

                        time = 17 * 60 * 60 + rnd.nextGaussian() * 60 * 60;
                        activity2.setEndTime(Math.max(14 * 60 * 60, Math.min(time, 22 * 60 * 60)));
                        matsimPlan.addActivity(activity2);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

                        Activity activity3 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        matsimPlan.addActivity(activity3);
                    }
                }


            }*/


/*            if (destLoc.getId()==(1989)){
                for (Location origLoc : origList){
                    int tripsToAirport= (int)(0.003*origLoc.getPopulation());
                    Coord airportCoordinates = new Coord(4484789, 5357507);
                    for (int i =0; i< tripsToAirport; i++){
                        org.matsim.api.core.v01.population.Person matsimPerson =
                                matsimPopulationFactory.createPerson(Id.create(personId, org.matsim.api.core.v01.population.Person.class));
                        matsimPopulation.addPerson(matsimPerson);
                        personId++;
                        Plan matsimPlan = matsimPopulationFactory.createPlan();
                        matsimPerson.addPlan(matsimPlan);

                        //SimpleFeature homeFeature = zoneFeatureMap.get(origLoc.getId());
                        Coord homeCoordinates = new Coord (origLoc.getX()+200*(Math.random()-0.5),origLoc.getY()+200*(Math.random()-0.5));

//    		Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
                        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        //randomly between 7 and 9 AM
                        double time = 5*60*60+rnd.nextDouble()*15*60*60;
                        activity1.setEndTime(time);
                        matsimPlan.addActivity(activity1);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));


                        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("airport", airportCoordinates);
                        //randomly between 4 and 8 PM

                        time += rnd.nextDouble()*3*60*60;
                        activity2.setEndTime(time);
                        matsimPlan.addActivity(activity2);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

                        Activity activity3 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        matsimPlan.addActivity(activity3);
                    }
                }
            }*/

        }

        //add demand to munichArea transit line
        /*float transitShare = Float.parseFloat(rb.getString("transit.modal.share"));

        int numberOfS1travelers = (int) (500 * transitShare);


        for (int i = 0; i < numberOfS1travelers; i++) {
            //select randomly trips within the population




                Person matsimPerson =
                        matsimPopulationFactory.createPerson(Id.create(personId, Person.class));
                matsimPopulation.addPerson(matsimPerson);
                personId++;

                Plan matsimPlan = matsimPopulationFactory.createPlan();
                matsimPerson.addPlan(matsimPlan);

                //SimpleFeature homeFeature = zoneFeatureMap.get(origLoc.getId());
                Coord homeCoordinates = new Coord(4484162 + 500 * (Math.random() - 0.5), 5357245 + 500 * (Math.random() - 0.5));

//    		Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
                Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                //randomly between 7 and 9 AM
                double time = 8 * 60 * 60 + rnd.nextGaussian() * 60 * 60;
                activity1.setEndTime(Math.max(4 * 60 * 60, Math.min(time, 12 * 60 * 60)));
                matsimPlan.addActivity(activity1);
                matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.pt));

//    		SimpleFeature workFeature = featureMap.get(workPuma);
                //SimpleFeature workFeature = zoneFeatureMap.get(destLoc.getId());
                Coord workCoordinates = new Coord(4470602 +500*(Math.random() - 0.5), 5332173+2000 * (Math.random() - 0.5));
//    		Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", ct.transform(workCoordinates));
                Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
                //randomly between 4 and 8 PM

//                time = 17 * 60 * 60 + rnd.nextGaussian() * 60 * 60;
//                activity2.setEndTime(Math.max(14 * 60 * 60, Math.min(time, 22 * 60 * 60)));
                matsimPlan.addActivity(activity2);
                //matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

                //Activity activity3 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                //matsimPlan.addActivity(activity3);

        }


        if (writePopulation) {
            MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
            popWriter.write("./input/population_" + year + ".xml");
        }

        System.out.println("The total number of trips by car is = " + totalTripsAllDestinations);

        return matsimPopulation;*/

    }


    private static Map<Integer, Double> sortByComparator(Map<Integer, Double> unsortMap, final boolean order) {

        List<Entry<Integer, Double>> list = new LinkedList<>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<Integer, Double>>() {
            public int compare(Entry<Integer, Double> o1,
                               Entry<Integer, Double> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });
        // Maintaining insertion order with the help of LinkedList
        Map<Integer, Double> sortedMap = new LinkedHashMap<>();
        for (Entry<Integer, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
}
