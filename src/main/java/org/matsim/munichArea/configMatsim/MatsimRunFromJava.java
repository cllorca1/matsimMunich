package org.matsim.munichArea.configMatsim;

import com.pb.common.matrix.Matrix;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.configMatsim.zonalData.Location;

import org.matsim.munichArea.roadSafety.VolumeAnalysisListener;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;


/**
 * Created by carlloga on 9/14/2016.
 */
public class MatsimRunFromJava {

    private ResourceBundle rb;
    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;
    private final Config config = ConfigUtils.createConfig();

    private MutableScenario scenario;
    private Controler controler;

    public MatsimRunFromJava(ResourceBundle rb) {
        this.rb = rb;
    }

    public void configureMatsim(String inputNetworkFile, int year, String crs, int numberOfIterations, String runId,
                                String outputDirectoryRoot, double flowCapacityFactor, double storageCapacityFactor,
                                String scheduleFile, String vehicleFile, float stuckTime, boolean useTransit){
        // Global
        config.global().setCoordinateSystem(crs);

        // Network
        config.network().setInputFile(inputNetworkFile);

        //public transport
        config.transit().setUseTransit(useTransit);
        if (useTransit) {
            config.transit().setTransitScheduleFile(scheduleFile);
            config.transit().setVehiclesFile(vehicleFile);
            Set<String> transitModes = new TreeSet<>();
            transitModes.add("pt");
            config.transit().setTransitModes(transitModes);
        }

        // Simulation
        config.qsim().setFlowCapFactor(flowCapacityFactor);
        config.qsim().setStorageCapFactor(storageCapacityFactor);
        config.qsim().setRemoveStuckVehicles(false);
        config.qsim().setStartTime(0);
        config.qsim().setEndTime(24*60*60);
        config.qsim().setStuckTime(stuckTime);

        // Controller
        runId = runId + "_" + year;
        String outputDirectory = outputDirectoryRoot;
        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDirectory);
        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(numberOfIterations);
        config.controler().setMobsim("qsim");
        config.controler().setWritePlansInterval(numberOfIterations);
        config.controler().setWriteEventsInterval(numberOfIterations);
        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        // QSim and other
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

        //Strategy
        StrategyConfigGroup.StrategySettings strategySettings1 = new StrategyConfigGroup.StrategySettings();
        strategySettings1.setStrategyName("ChangeExpBeta");
        strategySettings1.setWeight(0.5); //originally 0.8
        config.strategy().addStrategySettings(strategySettings1);

        StrategyConfigGroup.StrategySettings strategySettings2 = new StrategyConfigGroup.StrategySettings();
        strategySettings2.setStrategyName("ReRoute");
        strategySettings2.setWeight(1);//originally 0.2
        strategySettings2.setDisableAfter((int) (numberOfIterations * 0.7));
        config.strategy().addStrategySettings(strategySettings2);

        StrategyConfigGroup.StrategySettings strategySettings3 = new StrategyConfigGroup.StrategySettings();
        strategySettings3.setStrategyName("TimeAllocationMutator");
        strategySettings3.setWeight(1); //originally 0
        strategySettings3.setDisableAfter((int) (numberOfIterations * 0.7));
        config.strategy().addStrategySettings(strategySettings3);

        //TODO this strategy is implemented to test the pt modes (in general do not include)
//        StrategyConfigGroup.StrategySettings strategySettings4 = new StrategyConfigGroup.StrategySettings();
//        strategySettings4.setStrategyName("ChangeTripMode");
//        strategySettings4.setWeight(0); //originally 0
//        strategySettings4.setDisableAfter((int) (numberOfIterations * 0.7));
//        config.strategy().addStrategySettings(strategySettings4);

        config.strategy().setMaxAgentPlanMemorySize(4);

        // Plan Scoring (planCalcScore)
        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.planCalcScore().addActivityParams(homeActivity);

        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(workActivity);

        PlanCalcScoreConfigGroup.ActivityParams otherActivity = new PlanCalcScoreConfigGroup.ActivityParams("other");
        otherActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(otherActivity);

        config.qsim().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
        config.parallelEventHandling().setNumberOfThreads(16);
        config.qsim().setUsingThreadpool(false);

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
    }

    public void setMatsimPopulationAndInitialize(Population population){
        scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
        scenario.setPopulation(population);
        controler = new Controler(scenario);
    }

    public void addRoadSafetyAnalyzer(double tripScalingFactor){
        VolumeAnalysisListener volumeAnalysisListener = new VolumeAnalysisListener(config.controler().getLastIteration(), scenario.getNetwork(), tripScalingFactor, controler);
        controler.addControlerListener(volumeAnalysisListener);
    }

    public Matrix addTimeSkimMatrixCalculator(int timeOfDay, int numberOfCalcPoints,
                                                    ArrayList<Location> locationList){

        TimeListener timeListener = new TimeListener(
                controler, scenario.getNetwork(), config.controler().getLastIteration(),
                locationList, timeOfDay, numberOfCalcPoints);

        controler.addControlerListener(timeListener);

        return timeListener.getAutoTravelTime();
    }


    public Map<String, Matrix> addDistanceSkimMatrixCalculator(int timeOfDay, int numberOfCalcPoints,
                                                        ArrayList<Location> locationList){

        DistListener distListener = new DistListener(
                controler, scenario.getNetwork(), config.controler().getLastIteration(),
                locationList, timeOfDay, numberOfCalcPoints, Float.parseFloat(rb.getString("distance.threshold")));
        controler.addControlerListener(distListener);

        Map<String, Matrix> matrices = new HashMap<>();
        matrices.put("distanceByDistance", distListener.getShortDistByDistance());
        matrices.put("timeByDistance", distListener.getShortTimeByDistance());
        matrices.put("distanceByTime", distListener.getShortDistByTime());
        matrices.put("timeByTime", distListener.getShortTimeByTime());

        return matrices;
    }


    public void runMatsim(){

        controler.run();

    }

    public Network getNetwork() {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getNetwork();
    }

    public void addIntrazonalTravelTimeCalculator(ArrayList<Location> locationList, String fileName, Map<Integer, SimpleFeature> zoneFeatureMap) {
        IntrazonalTravelTimeCalculator intrazonalTravelTimeCalculator = new IntrazonalTravelTimeCalculator(controler, scenario.getNetwork(),
                config.controler().getLastIteration(),zoneFeatureMap,locationList,fileName );
        controler.addControlerListener(intrazonalTravelTimeCalculator);
    }
}
