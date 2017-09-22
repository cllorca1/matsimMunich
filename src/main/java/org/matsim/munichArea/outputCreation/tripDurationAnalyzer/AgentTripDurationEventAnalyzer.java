package org.matsim.munichArea.outputCreation.tripDurationAnalyzer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;


/**
 * Created by carlloga on 17.03.2017.
 */
public class AgentTripDurationEventAnalyzer {

    private ResourceBundle rb;

    private Map<Id, Trip> tripMap;
    private Network network;

    public AgentTripDurationEventAnalyzer(ResourceBundle rb) {
        this.rb = rb;
        this.tripMap = new HashMap<>();


        //load the matsim network
        Config config = ConfigUtils.createConfig();
        //todo manually input the desired network!!
        config.network().setInputFile("./input/studyNetworkLight.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        network = scenario.getNetwork();

    }

    public void runAgentTripDurationAnalyzer(String eventsFile) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        ActivityStartEndHandler eventAnalyzer = new ActivityStartEndHandler(tripMap);
        eventsManager.addHandler(eventAnalyzer);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        System.out.println(tripMap.size());
    }

    public void writeListTripDurations(String eventsFile){

        BufferedWriter bw = IOUtils.getBufferedWriter(eventsFile + ".csv");
        try {

            bw.write("id,mode,departure,arrival,tripDuration,waitingTime,purpose");
            bw.write(",dist,distOnCh");
            bw.newLine();

        for (Id id : tripMap.keySet()){
            double tripDuration = tripMap.get(id).getDuration();
            double waitingTimeBefore = tripMap.get(id).getWaitingTimeBefore();

            if(!id.toString().contains("taxi")) {
                bw.write(id.toString() + "," +
                        tripMap.get(id).getMode() + "," +
                        tripMap.get(id).getDepartureTime() + "," +
                        tripMap.get(id).getArrivalTime() + "," +
                        tripDuration + "," +
                        waitingTimeBefore + "," +
                        tripMap.get(id).getPurpose());




                double dist = 0;
                double distOnCH = 0;
                for (Link l : NetworkUtils.getLinks(network,tripMap.get(id).getListOfLinks())){
                    dist += l.getLength();

                    //these lines are only needed if
                    if(l.getId().toString().contains("CH")){
                        distOnCH += l.getLength();
                    }

                }

                bw.write("," + dist + "," +distOnCH);
                bw.newLine();

            }

        }

            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
