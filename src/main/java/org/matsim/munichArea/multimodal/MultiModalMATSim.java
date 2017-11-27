package org.matsim.munichArea.multimodal;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.ControlerDefaultsWithMultiModalModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.PrepareMultiModalScenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.configMatsim.planCreation.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.munichArea.configMatsim.planCreation.ReadSyntheticPopulation;

import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by carlloga on 08-05-17.
 */
public class MultiModalMATSim {

    public static ResourceBundle rb;


    public static void main (String args[]){

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);


        if (Boolean.parseBoolean(rb.getString("create.demand"))) {
            //read locations

            CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
            ArrayList<Location> locationList = centroidsToLocations.readCentroidList();

            System.out.println(locationList.size());

            //demand creation

            ReadSyntheticPopulation readSyntheticPopulation = new ReadSyntheticPopulation(rb, locationList);
            readSyntheticPopulation.demandFromSyntheticPopulation(0, 1, rb.getString("plan.file"));
            readSyntheticPopulation.printHistogram();
            readSyntheticPopulation.printSyntheticPlansList("./input/" + rb.getString("plan.csv.file"),  0);

        }

        if (Boolean.parseBoolean(rb.getString("create.cycling"))){

            AddCyclingHighway ach = new AddCyclingHighway(rb);
            ach.addCyclingHighway();

        }


        if (Boolean.parseBoolean(rb.getString("run.matsim"))) {
            //read config



            Config config = ConfigUtils.loadConfig("input/config_multimodal.xml", new MultiModalConfigGroup());

//            Config config = ConfigUtils.loadConfig("input/config_for_transit.xml");

            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
            config.controler().setOutputDirectory(rb.getString("out.folder"));


            config.network().setInputFile(rb.getString("run.network.file"));

            config.plans().setInputFile(rb.getString("plan.file"));

            int numberOfIterations = Integer.parseInt(rb.getString("last.iteration"));
            config.controler().setLastIteration(numberOfIterations);

            config.qsim().setStartTime(0);
            config.qsim().setEndTime(24 * 60 * 60);

            config.qsim().setFlowCapFactor(1);
            config.qsim().setStorageCapFactor(1);

            config.qsim().setRemoveStuckVehicles(false);

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

            config.transit().setTransitScheduleFile("C:/models/munich/input/current/schedule20171026.xml");
            config.transit().setVehiclesFile("C:/models/munich/input/current/vehicles20171026.xml");
            config.transit().setUseTransit(Boolean.parseBoolean(rb.getString("use.transit")));
            Set<String> transitModes = new TreeSet<>();
            transitModes.add("pt");
            config.transit().setTransitModes(transitModes);

            //start matsim simulation

            //add scenario preparation
            Scenario scenario = ScenarioUtils.loadScenario(config);



            PrepareMultiModalScenario.run(scenario);

            Controler controler = new Controler(scenario);

            //add new module
            controler.setModules(new ControlerDefaultsWithMultiModalModule());


            controler.run();

        }
    }

}

