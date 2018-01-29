package org.matsim.munichArea;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.configMatsim.planCreation.longDistance.LongDistanceTraffic;

public class RunExternalFlowsOnly {

    private static Logger logger = Logger.getLogger(RunExternalFlowsOnly.class);

    public static void main (String args[]){

        LongDistanceTraffic longDistanceTraffic = new LongDistanceTraffic();
        longDistanceTraffic.readZones();
        longDistanceTraffic.readMatrices();

        float scalingFactor = 0.1f;

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population matsimPopulation = scenario.getPopulation();

        matsimPopulation = longDistanceTraffic.addLongDistancePlans(scalingFactor, matsimPopulation);

        PopulationWriter populationWriter = new PopulationWriter(matsimPopulation);
        populationWriter.write("input/externalFlows/population.xml");

        //only for debugging
        //longDistanceTraffic.printOutTripGenerationAndAttraction();
        longDistanceTraffic.printOutTotals();

    }
}
