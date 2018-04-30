package org.matsim.simpleMatsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;

public class SimplePopulationGenerator {

    public static Population generatePopulation(Config config, MutableScenario scenario, double capacity, double scaleFactor){

        Population population = PopulationUtils.createPopulation(config);
        Network network = scenario.getNetwork();

        PopulationFactory factory = population.getFactory();

        int personId = 0;
        double time = 0;
        double headway;

        while (time < 24 * 60 *60) {

            if (time < 2*60*60){
                headway = 3600 / capacity / .5 / scaleFactor;
            } else if (time < 4*60*60){
                headway = 3600 / capacity / 1/ scaleFactor;
            } else if (time < 6*60*60){
                headway = 3600 / capacity / 1.5 / scaleFactor;
            } else {
                headway = 3600 / capacity / 2 / scaleFactor;
            }

            Person person;
            Plan plan;
            Activity activity;

            person = factory.createPerson(Id.createPersonId(personId));
            plan = factory.createPlan();
            person.addPlan(plan);
            activity = factory.createActivityFromCoord("home", network.getNodes().get(Id.createNodeId("origin")).getCoord());
            activity.setEndTime(time);
            plan.addActivity(activity);
            plan.addLeg(factory.createLeg(TransportMode.car));
            activity = factory.createActivityFromCoord("work", network.getNodes().get(Id.createNodeId("destination")).getCoord());
            plan.addActivity(activity);

            population.addPerson(person);

            time += headway;
            personId++;
        }

        return population;


    }

}
