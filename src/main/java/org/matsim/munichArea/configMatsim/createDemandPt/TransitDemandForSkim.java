package org.matsim.munichArea.configMatsim.createDemandPt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.munichArea.configMatsim.zonalData.Location;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by carlloga on 3/2/17.
 */
public class TransitDemandForSkim {


    public Map< Id, PtSyntheticTraveller> createDemandForSkims(ArrayList<Location> servedZonesList,ArrayList<Location> shortServedZonesList, int personId, Population matsimPopulation) {

        Map< Id, PtSyntheticTraveller> ptSyntheticTravellerMap = new HashMap<>();
        Map<Integer, Location> locationMap =  new HashMap<>();
        for (Location loc : servedZonesList){
            locationMap.put(loc.getId(), loc);
        }

        PopulationFactory matsimPopulationFactory = matsimPopulation.getFactory();

            for (Location origLoc : shortServedZonesList) {
                double time = 8 * 60 * 60;
                for (Location destLoc : servedZonesList) {


//                        Location origLoc = locationMap.get(i);
//                        Location destLoc = locationMap.get(j);

                    if (origLoc.getId() >= destLoc.getId()) {

                        org.matsim.api.core.v01.population.Person matsimPerson =
                                matsimPopulationFactory.createPerson(Id.create(personId, org.matsim.api.core.v01.population.Person.class));
                        matsimPopulation.addPerson(matsimPerson);


                        PtSyntheticTraveller ptSyntheticTraveller = new PtSyntheticTraveller(personId, origLoc, destLoc, matsimPerson);
                        ptSyntheticTravellerMap.put(matsimPerson.getId(), ptSyntheticTraveller);


                        personId++;
                        Plan matsimPlan = matsimPopulationFactory.createPlan();
                        matsimPerson.addPlan(matsimPlan);

                        //the coordinates of the centroid are inside a square 10% of the equivalent squared zone
                        Coord homeCoordinates = new Coord(origLoc.getX() + origLoc.getSize() * 0.1*(Math.random() - 0.5), origLoc.getY() + origLoc.getSize() *0.1 * (Math.random() - 0.5));
                        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        activity1.setEndTime(time + 5*60*60*Math.random());
                        matsimPlan.addActivity(activity1);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.pt));

                        Coord workCoordinates = new Coord(destLoc.getX() + destLoc.getSize() * 0.1* (Math.random() - 0.5), destLoc.getY() + destLoc.getSize() * 0.1 * (Math.random() - 0.5));
                        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
                        matsimPlan.addActivity(activity2);


                    }
                }
            }


        return ptSyntheticTravellerMap;

    }



}
