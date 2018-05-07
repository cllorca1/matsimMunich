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
import java.text.DecimalFormat;

public class ConfigureAndRun {



    private final Config config = ConfigUtils.createConfig();
    private double flowCapacityFactor;
    private double storageCapacityFactor;
    private String outputDirectory;
    private String runId;
    private int replication;

    private double capacity;
    private double leng;

    public ConfigureAndRun(double flowCapacityFactor, int replication, double capacity, double leng){
        this.flowCapacityFactor = flowCapacityFactor;
//        storageCapacityFactor = Math.pow(flowCapacityFactor,0.75);
        storageCapacityFactor = flowCapacityFactor;
        outputDirectory = "./output/C=" + capacity + "/L=" + leng + "/";
        runId = "scale" + String.valueOf(flowCapacityFactor*100) + "replication" + replication;
        this.replication = replication;
        this.capacity = capacity;
        this.leng = leng;
    }


    public void configureAndRunMatsim(){


        // Global
        config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
        config.global().setRandomSeed(replication);


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
        //config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);

        // Controler
        config.qsim().setTimeStepSize(1.0);

        config.controler().setRunId(runId);
        outputDirectory = outputDirectory + runId;
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
        scenario.setNetwork(SimpleNetworkGenerator.createNetwork(capacity, leng));

        Population population = SimplePopulationGenerator.generatePopulation(config, scenario, capacity * 2, flowCapacityFactor);
        //create population here
        scenario.setPopulation(population);

        final Controler controler = new Controler(scenario);
        controler.run();

    }

    public void runEventHandler() throws IOException {

        SimpleEventAnalyzer.run(outputDirectory + "/" + runId + ".output_events.xml.gz", outputDirectory + "/" + runId + ".csv");

    }
}
