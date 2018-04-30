package org.matsim.simpleMatsim;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.IOException;

public class ConfigureAndRun {

    private final Config config = ConfigUtils.createConfig();
    private double flowCapacityFactor = 1;
    private double storageCapacityFactor = flowCapacityFactor;
    private String outputDirectory = "./output";
    private String runId = "example";

    private static double capacity = 3600;


    public void configureAndRunMatsim(){


        // Global
        config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
        config.global().setRandomSeed(1);


        //public transport
        config.transit().setUseTransit(false);

        // Simulation
        config.controler().setMobsim("qsim");
        config.qsim().setFlowCapFactor(flowCapacityFactor);
        config.qsim().setStorageCapFactor(storageCapacityFactor);
        config.qsim().setRemoveStuckVehicles(false);
        config.qsim().setStartTime(0);
        //config.qsim().setEndTime(24*60*60);
        config.qsim().setStuckTime(100000);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);



        // Controler
        config.qsim().setTimeStepSize(1.0);

        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDirectory);
        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setWriteEventsInterval(1);
        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        // QSim and other
        config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

        //Strategy
        StrategyConfigGroup.StrategySettings strategySettings1 = new StrategyConfigGroup.StrategySettings();
        strategySettings1.setStrategyName("ChangeExpBeta");
        strategySettings1.setWeight(1); //originally 0.8
        config.strategy().addStrategySettings(strategySettings1);
        config.strategy().setMaxAgentPlanMemorySize(4);

        // Plan Scoring (planCalcScore)
        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.planCalcScore().addActivityParams(homeActivity);

        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(workActivity);

        config.qsim().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
        config.parallelEventHandling().setNumberOfThreads(16);
        config.qsim().setUsingThreadpool(false);

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);


        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
        scenario.setNetwork(SimpleNetworkGenerator.createNetwork(capacity));

        Population population = SimplePopulationGenerator.generatePopulation(config, scenario, capacity, flowCapacityFactor);
        //create population here
        scenario.setPopulation(population);

        final Controler controler = new Controler(scenario);
        controler.run();

    }

    public void runEventHandler() throws IOException {

        SimpleEventAnalyzer.run(outputDirectory + "/" + runId + ".output_events.xml.gz", outputDirectory + "/" + runId + ".csv");

    }
}
