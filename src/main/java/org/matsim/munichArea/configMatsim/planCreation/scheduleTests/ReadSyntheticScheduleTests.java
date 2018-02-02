package org.matsim.munichArea.configMatsim.planCreation.scheduleTests;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.Util;
import org.matsim.munichArea.configMatsim.zonalData.CentroidsToLocations;
import org.matsim.munichArea.configMatsim.zonalData.Location;

import java.io.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ReadSyntheticScheduleTests {


    private static Logger logger = Logger.getLogger(ReadSyntheticScheduleTests.class);
    private static String folder = "C:/models/matsimScheduleTests/";
    private static String csvPlans = "input/subPopulation.csv";
    private static String xmlPlans = "input/populationWorkSchedule.xml";
    private ArrayList<Location> locationList;
    private static ResourceBundle rb;

    public static void main(String[] args){

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        ReadSyntheticScheduleTests readSyntheticScheduleTests = new ReadSyntheticScheduleTests();
        readSyntheticScheduleTests.readZones();

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population matsimPopulation = scenario.getPopulation();

        matsimPopulation = readSyntheticScheduleTests.createPlans(matsimPopulation, false,
                true,true, false);

        PopulationWriter populationWriter = new PopulationWriter(matsimPopulation);
        populationWriter.write(folder + xmlPlans);

    }

    public void readZones(){
        CentroidsToLocations centroidsToLocations = new CentroidsToLocations(rb);
        locationList = centroidsToLocations.readCentroidList();
    }

    public Population createPlans(Population matsimPopulation,
                                  boolean giveDepartureFromHome, boolean giveArrivalAtWork, boolean giveDepartureFromWork, boolean gideArrivalAtHome) {

        PopulationFactory populationFactory = matsimPopulation.getFactory();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(folder + csvPlans));



        String[] header = br.readLine().split(",");
        int positionId = Util.findPositionInArray("id", header);
        int positionHomeZone = Util.findPositionInArray("homeZone", header);
        int positionWorkZone = Util.findPositionInArray("zone", header);
        int positionDepHW = Util.findPositionInArray("depHW", header);
        int positionArrHW = Util.findPositionInArray("arrHW", header);
        int positionDepWH = Util.findPositionInArray("depWH", header);
        int positionArrWH = Util.findPositionInArray("arrWH", header);


        String line;
        while ((line = br.readLine()) != null ) {

            Person matsimPerson = populationFactory.createPerson(Id.createPersonId(line.split(",")[positionId]));
            Plan matsimPlan = populationFactory.createPlan();
            int homeZone = Integer.parseInt(line.split(",")[positionHomeZone]);
            int workZone = Integer.parseInt(line.split(",")[positionWorkZone]);

            Coord homeCoord = new Coord(locationList.get(homeZone).getX(), locationList.get(homeZone).getY());
            Coord workCoord = new Coord(locationList.get(workZone).getX(), locationList.get(workZone).getY());

            Activity homeActivity = populationFactory.createActivityFromCoord("home", homeCoord);
            Activity workActivity = populationFactory.createActivityFromCoord("work", workCoord);
            Activity lateHomeActivity = populationFactory.createActivityFromCoord("home", homeCoord);

            if (giveDepartureFromHome) homeActivity.setEndTime(Double.parseDouble(line.split(",")[positionDepHW]));
            if (giveArrivalAtWork) workActivity.setEndTime(Double.parseDouble(line.split(",")[positionDepWH]));

            if (giveDepartureFromWork) workActivity.setStartTime(Double.parseDouble(line.split(",")[positionArrHW]));
            if (gideArrivalAtHome) lateHomeActivity.setStartTime(Double.parseDouble(line.split(",")[positionArrWH]));


            matsimPlan.addActivity(homeActivity);
            matsimPlan.addLeg(populationFactory.createLeg(TransportMode.car));
            matsimPlan.addActivity(workActivity);
            matsimPlan.addLeg(populationFactory.createLeg(TransportMode.car));
            matsimPlan.addActivity(lateHomeActivity);

            matsimPerson.addPlan(matsimPlan);
            matsimPopulation.addPerson(matsimPerson);
        };

        br.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matsimPopulation;

    }

}
