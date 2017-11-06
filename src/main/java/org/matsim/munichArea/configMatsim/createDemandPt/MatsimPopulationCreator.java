package org.matsim.munichArea.configMatsim.createDemandPt;

/**
 * Created by carlloga on 9/14/2016.
 */

import java.util.*;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.outputCreation.accessibilityCalculator.Accessibility;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import com.pb.common.matrix.Matrix;




/**
 * discontinued
 */
public class MatsimPopulationCreator {

    public static boolean ASC = true;
    public static boolean DESC = false;
    private ResourceBundle rb;

    private Population matsimPopulation;
    private Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap;

    public MatsimPopulationCreator(ResourceBundle rb) {
        this.rb = rb;
    }

    public void createMatsimPopulation(ArrayList<Location> locationList, /*HouseholdDataManager householdDataManager,*/ int year,
                                                    /*Map<Integer,SimpleFeature>zoneFeatureMap, String crs, */ boolean writePopulation,
                                       double tripScalingFactor) {


        //    	Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zoneShapeFile);
//
//    	Map<Integer,SimpleFeature> featureMap = new HashMap<Integer, SimpleFeature>();
//		for (SimpleFeature feature: features) {
//			int fipsPuma5 = Integer.parseInt(feature.getAttribute("FIPS_PUMA5").toString());
//			featureMap.put(fipsPuma5,feature);
//		}

        // MATSim "infrastructure"
        Config matsimConfig = ConfigUtils.createConfig();
        Scenario matsimScenario = ScenarioUtils.createScenario(matsimConfig);

        Network matsimNetwork = matsimScenario.getNetwork();
        matsimPopulation = matsimScenario.getPopulation();
        PopulationFactory matsimPopulationFactory = matsimPopulation.getFactory();

//    	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
//    			TransformationFactory.WGS84, crs);

//    	Random random = new Random();
        // Random random = MatsimRandom.getLocalInstance() ;
        // make sure that stream of random variables is reproducible. kai, apr'16

   /*

        for (Person siloPerson : siloPersons) {
            if (random.nextDouble() > scalingFactor) {
                // e.g. if scalingFactor = 0.01, there will be a 1% chance that the loop is not
                // continued in the next step, i.e. that the person is added to the population
                continue;
            }

            if (siloPerson.getOccupation() != 1) { // person does not work
                continue;
            }

            int siloWorkplaceId = siloPerson.getWorkplace();
            if (siloWorkplaceId == -2) { // person has workplace outside study area
                continue;
            }

            int householdId = siloPerson.getHhId();
            Household household = Household.getHouseholdFromId(householdId);
            int numberOfWorkers = household.getNumberOfWorkers();
            int numberOfAutos = household.getAutos();
            if (numberOfWorkers == 0) {
                throw new RuntimeException("If there are no workers in the household, the loop must already"
                        + " have been continued by finfing that the given person is not employed!");
            }
            if ((double) numberOfAutos/numberOfWorkers < 1.) {
                if (random.nextDouble() > (double) numberOfAutos/numberOfWorkers) {
                    continue;
                }
            }



            int siloPersonId = siloPerson.getId();

            int siloHomeTazId = siloPerson.getHomeTaz();
//    		int homePuma = geoData.getPUMAofZone(siloHomeTazId);
//    		System.out.println("siloPersonId = " + siloPersonId + "; siloHomeTazId = " + siloHomeTazId);

            Job job = Job.getJobFromId(siloWorkplaceId);
            int workZoneId = job.getZone();
//    		int workPuma = geoData.getPUMAofZone(workZoneId);
//    		System.out.println("siloPersonId = " + siloPersonId + "; siloWorkplaceId = " + siloWorkplaceId);
//    		System.out.println("siloPersonId = " + siloPersonId + "; workZoneId = " + workZoneId);
*/
        ArrayList<Location> destList = locationList;
        ArrayList<Location> origList = locationList;

        int personId = 0;

        SkimMatrixReader acc = new SkimMatrixReader();
        Matrix autoTravelTime = acc.readSkim(rb.getString("base.skim.file"), "mat1");


        Random rnd = new Random();
        double alpha = 1.5;
        double g = 1;
        double minTravelTime = 3;

        int totalTripsAllDestinations = 0;


        //start loop to generate trips to work
        for (Location destLoc : destList) {
            Map<Integer, Double> origZoneWeight = new HashMap<>();
//            double[] origRate = new double[origList.size()];
            double sumOrigRates = 0;
            //first loop to calculate weights
            for (Location origLoc : origList) {
                double weight;
                double travelTime = autoTravelTime.getValueAt(origLoc.getId(), destLoc.getId());

                if (travelTime < minTravelTime) {
//                    origRate[origLoc.getId() - 1] = (origLoc.getPopulation() * g / Math.pow(minTravelTime, alpha));
                    weight = (origLoc.getPopulation() * g / Math.pow(minTravelTime, alpha));


                } else {
//                    origRate[origLoc.getId() - 1] = (origLoc.getPopulation() * g / Math.pow(travelTime, alpha));
                    weight = (origLoc.getPopulation() * g / Math.pow(travelTime, alpha));

                }


//                sumOrigRates += origRate[origLoc.getId() - 1];
                sumOrigRates += weight;
                origZoneWeight.put(origLoc.getId(), weight);
            }


            float carShare = Float.parseFloat(rb.getString("car.modal.share"));


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


                        org.matsim.api.core.v01.population.Person matsimPerson =
                                matsimPopulationFactory.createPerson(Id.create(personId, org.matsim.api.core.v01.population.Person.class));
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


            }

        }

        System.out.println("The total number of trips by car is = " + totalTripsAllDestinations);

        if (writePopulation) {
            MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
            popWriter.write("./input/population.xml");
        }
    }

    public void createSyntheticPtPopulation(ArrayList<Location> servedZonesList, ArrayList<Location> shortServedZonesList) {

        //public transportation demand to get skims

        int personId = 10000000;

        TransitDemandForSkim tdSkims = new TransitDemandForSkim();
        ptSyntheticTravellerMap = tdSkims.createDemandForSkims(servedZonesList, shortServedZonesList, personId, matsimPopulation);


        //add demand to munichArea transit line
        float transitShare = Float.parseFloat(rb.getString("transit.modal.share"));

        //int numberOfS1travelers = (int) (500 * transitShare);

    }


    public Population getMatsimPopulation() {
        return matsimPopulation;
    }

    public Map<Id, PtSyntheticTraveller> getPtSyntheticTravellerMap() {
        return ptSyntheticTravellerMap;
    }

    private static Map<Integer, Double> sortByComparator(Map<Integer, Double> unsortMap, final boolean order) {

        List<Map.Entry<Integer, Double>> list = new LinkedList<>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
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
