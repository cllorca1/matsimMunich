package org.matsim.munichArea.configMatsim.planCreation.externalFlows;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.util.ResourceBundle;

public class RunExternalFlowsOnly {

    private static Logger logger = Logger.getLogger(RunExternalFlowsOnly.class);
    private static ResourceBundle rb;

    public static void main (String args[]){

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        LongDistanceTraffic longDistanceTraffic = new LongDistanceTraffic(rb);
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
