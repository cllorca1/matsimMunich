package org.matsim.munichArea;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.configMatsim.replanning.StayAtWorkOvernightProvider;

/**
 * Created by carlloga on 9/12/2016.
 */
public class MatsimRunFromFile {


    private final static String STRATEGY_NAME = "stayAtWorkOvernight";

    public static void main(String[] args) {

        String configFileName = args[0];
        Config config = ConfigUtils.loadConfig(configFileName);

        config.network().setInputFile("networ.xml,gz");
        config.plans().setInputFile(".....");

        //modify configuration parameters
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.controler().setLastIteration(10);

        //add strategy:
        StrategyConfigGroup.StrategySettings strategySettings3 = new StrategyConfigGroup.StrategySettings();
        strategySettings3.setStrategyName("TimeAllocationMutator");
        strategySettings3.setWeight(1); //originally 0
        strategySettings3.setDisableAfter((int) (config.controler().getLastIteration() * 0.7));
        config.strategy().addStrategySettings(strategySettings3);

        //add a strategy to the config
        StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
        stratSets.setStrategyName(STRATEGY_NAME);
        stratSets.setWeight(1);
        config.strategy().addStrategySettings(stratSets);

        //rename input folder:
        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "withNewReplanning");

        //add the opening time, closing time and duration of activity
        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(9 * 60 * 60);
        workActivity.setLatestStartTime(8 * 60 * 60);
        workActivity.setEarliestEndTime(17 *60 * 60);
        workActivity.setClosingTime(17.5 * 60*60) ;
        workActivity.setOpeningTime(7.5 * 60*60);
        workActivity.setMinimalDuration(8.5 *60*60);
        config.planCalcScore().addActivityParams(workActivity);

        //load the scenario from the configuration settings
        Scenario scenario = ScenarioUtils.loadScenario(config);

        //create the controller
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding(STRATEGY_NAME).toProvider(StayAtWorkOvernightProvider.class);
            }
        });

        // This runs iterations:
        controler.run();
    }
}
