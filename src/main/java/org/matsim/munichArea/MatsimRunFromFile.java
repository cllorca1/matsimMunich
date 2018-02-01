package org.matsim.munichArea;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by carlloga on 9/12/2016.
 */
public class MatsimRunFromFile {


    public static void main(String[] args) {

        String configFileName = args[0];
        Config config = ConfigUtils.loadConfig(configFileName);

        //modify configuration parameters
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        /*//add strategy:
        StrategyConfigGroup.StrategySettings strategySettings3 = new StrategyConfigGroup.StrategySettings();
        strategySettings3.setStrategyName("TimeAllocationMutator");
        strategySettings3.setWeight(1); //originally 0
        strategySettings3.setDisableAfter((int) (config.controler().getLastIteration() * 0.7));
        config.strategy().addStrategySettings(strategySettings3);

        //rename input folder:
        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "withDefiningActivity");

        //add the opening time, closing time and duration of activity
        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(9 * 60 * 60);
        workActivity.setLatestStartTime(8 * 60 * 60);
        workActivity.setEarliestEndTime(17 *60 * 60);
        workActivity.setClosingTime(17 * 60*60) ;
        workActivity.setOpeningTime(8*60*60);
        workActivity.setMinimalDuration(9*60*60);
        config.planCalcScore().addActivityParams(workActivity);
*/

        //load the scenario from the configuration settings
        Scenario scenario = ScenarioUtils.loadScenario(config);

        //create the controller
        Controler controler = new Controler(scenario);

        // This runs iterations:
        controler.run();
    }
}
