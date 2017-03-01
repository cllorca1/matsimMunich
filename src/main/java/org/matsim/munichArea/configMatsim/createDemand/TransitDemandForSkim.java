package org.matsim.munichArea.configMatsim.createDemand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.munichArea.planCreation.Location;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import static org.matsim.munichArea.MatsimExecuter.munich;

/**
 * Created by carlloga on 3/2/17.
 */
public class TransitDemandForSkim {


    public Map< Id, PtSyntheticTraveller> createDemandForSkims(ArrayList<Location> servedZonesList,ArrayList<Location> shortServedZonesList, int personId, Population matsimPopulation) {


        Collections.shuffle(servedZonesList);
        Collections.shuffle(shortServedZonesList);

        Map< Id, PtSyntheticTraveller> ptSyntheticTravellerMap = new HashMap<>();

        PopulationFactory matsimPopulationFactory = matsimPopulation.getFactory();

            for (int i = 0; i < shortServedZonesList.size(); i++) {
                double time = 8 * 60 * 60;
                for (int j = 0; j < servedZonesList.size(); j++) {


                        Location origLoc = shortServedZonesList.get(i);
                        Location destLoc = servedZonesList.get(j);

                    if (origLoc.getId() >= destLoc.getId()) {

                        org.matsim.api.core.v01.population.Person matsimPerson =
                                matsimPopulationFactory.createPerson(Id.create(personId, org.matsim.api.core.v01.population.Person.class));
                        matsimPopulation.addPerson(matsimPerson);


                        PtSyntheticTraveller ptSyntheticTraveller = new PtSyntheticTraveller(personId, origLoc, destLoc, matsimPerson);
                        ptSyntheticTravellerMap.put(matsimPerson.getId(), ptSyntheticTraveller);

                        personId++;
                        Plan matsimPlan = matsimPopulationFactory.createPlan();
                        matsimPerson.addPlan(matsimPlan);

                        Coord homeCoordinates = new Coord(origLoc.getX() + origLoc.getSize() * (Math.random() - 0.5), origLoc.getY() + origLoc.getSize() * (Math.random() - 0.5));
                        Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                        activity1.setEndTime(time + 5*60*60*Math.random());
                        matsimPlan.addActivity(activity1);
                        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.pt));

                        Coord workCoordinates = new Coord(destLoc.getX() + destLoc.getSize() * (Math.random() - 0.5), destLoc.getY() + destLoc.getSize() * (Math.random() - 0.5));
                        Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
                        matsimPlan.addActivity(activity2);


                    }
                }
            }


        return ptSyntheticTravellerMap;

    }



}
