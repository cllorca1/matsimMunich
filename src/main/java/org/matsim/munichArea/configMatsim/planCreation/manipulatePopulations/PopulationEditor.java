package org.matsim.munichArea.configMatsim.planCreation.manipulatePopulations;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationEditor {

    public static Logger logger = Logger.getLogger(PopulationEditor.class);


    public static void main (String[] args){

        PopulationEditor populationEditor = new PopulationEditor();
        String folder  = args[0];
        String file = args[1];

        Population population = populationEditor.readPopulationFile(folder + file);

        populationEditor.summarizePopulation(population);

        Population newPopulation;
        PopulationWriter populationWriter;
        //WRITES THE ORIGINAL 10%
        newPopulation = populationEditor.filterCarTripsAndRescale(population, 1);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter= new PopulationWriter(newPopulation);
        populationWriter.write(args[2]);
        //WRITES THE 5%
        newPopulation = populationEditor.filterCarTripsAndRescale(population, 0.5);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter= new PopulationWriter(newPopulation);
        populationWriter.write(args[3]);
        //WRITES A 1%
        newPopulation = populationEditor.filterCarTripsAndRescale(population, 0.1);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter= new PopulationWriter(newPopulation);
        populationWriter.write(args[4]);

    }

    public Population readPopulationFile(String originalPlansFile){
        Config config  = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(originalPlansFile);
        return scenario.getPopulation();
    }


    public void summarizePopulation(Population population){

        logger.info("Population size: " + population.getPersons().size());

    }


    public Population filterCarTripsAndRescale(Population originalPopulation, double relativeScale){
        Config config  = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population newPopulation = scenario.getPopulation();
        //PopulationFactory populationFactory = newPopulation.getFactory();
        for (Person person : originalPopulation.getPersons().values()){
            boolean storePerson = false;
            for (Plan plan : person.getPlans()){
                for (PlanElement element : plan.getPlanElements()){
                    //can be activity or leg //careful I select the first interface. What if there is more than one?
                    if (element.getClass().getInterfaces()[0].equals(Leg.class)){
                        Leg leg = (Leg) element;
                        if (leg.getMode().equals(TransportMode.car)){
                            storePerson = true;
                        }


                    }
                }

            }
            if (storePerson && relativeScale > Math.random()) newPopulation.addPerson(person);
        }
        return newPopulation;
    }



}
