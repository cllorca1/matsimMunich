package org.matsim.munichArea.roadSafety;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.configMatsim.networkTools.RoadBuilder;

public class RoadSafetyAnalyzer {

    public static void main (String[] args){

        String configFileName = args[0];
        String networkFileName = args[1];
        String plansFileName = args[2];
        String scheduleFiles = args[3];
        String vehiclesFile = args[4];

        RoadSafetyAnalyzer roadSafetyAnalyzer = new RoadSafetyAnalyzer();
        roadSafetyAnalyzer.setupAndRun(configFileName, networkFileName, plansFileName);

    }


    public void setupAndRun(String configFileName,  String networkFileName,  String plansFileName){

        Config config = ConfigUtils.loadConfig(configFileName);
        //modify configuration parameters
        config.controler().setLastIteration(1);
        config.network().setInputFile(networkFileName);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);



        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansFileName);

        //create the controller
        Controler controler = new Controler(scenario);
        VolumeAnalysisListener volumeAnalysisListener = new VolumeAnalysisListener(1,
                scenario.getNetwork(),
                config.qsim().getFlowCapFactor(),
                controler);
        controler.addControlerListener(volumeAnalysisListener);

        // This runs iterations:
        controler.run();
    }


}
