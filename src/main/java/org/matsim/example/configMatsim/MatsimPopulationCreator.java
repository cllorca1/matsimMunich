package org.matsim.example.configMatsim;

/**
 * Created by carlloga on 9/14/2016.
 */
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.example.planCreation.Location;
import org.opengis.feature.simple.SimpleFeature;




/**
 * @author dziemke
 */
public class MatsimPopulationCreator {

    public static Population createMatsimPopulation(ArrayList<Location> locationList, /*HouseholdDataManager householdDataManager,*/ int year,
                                                    /*Map<Integer,SimpleFeature>zoneFeatureMap, String crs, */ boolean writePopulation /*, double scalingFactor*/) {


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
        Population matsimPopulation = matsimScenario.getPopulation();
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
        int maxOrig = 5;
        int maxDest = 5;
        int origCount;
        int destCount;
        destCount = 0;
        int personId = 0;

        double time = 0;


        for (Location destLoc: destList) {
            destCount++;
            origCount = 0;
            if (destCount < maxDest) {
                for (Location origLoc : origList) {
                    origCount++;
                    if (origCount < maxOrig) {

                        org.matsim.api.core.v01.population.Person matsimPerson =
                                matsimPopulationFactory.createPerson(Id.create(personId, org.matsim.api.core.v01.population.Person.class));
                        matsimPopulation.addPerson(matsimPerson);
                        personId++;

                        Plan matsimPlan = matsimPopulationFactory.createPlan();
                        matsimPerson.addPlan(matsimPlan);

                        //SimpleFeature homeFeature = zoneFeatureMap.get(origLoc.getId());
                        Coord homeCoordinates = new Coord (origLoc.getX(),origLoc.getY());


//    		Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
                        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        activity1.setEndTime(time);
                        matsimPlan.addActivity(activity1);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

//    		SimpleFeature workFeature = featureMap.get(workPuma);
                        //SimpleFeature workFeature = zoneFeatureMap.get(destLoc.getId());
                        Coord workCoordinates = new Coord (destLoc.getX(),destLoc.getY());
//    		Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", ct.transform(workCoordinates));
                        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
                        matsimPlan.addActivity(activity2);
                        //matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

                    }
                }

            }
            time += 10;
        }







        if (writePopulation == true) {
            MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
            popWriter.write("./input/population_" + year + ".xml");
        }

        return matsimPopulation;
    }
}