package org.matsim.munichArea.outputCreation.routeAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.munichArea.outputCreation.tripDurationAnalyzer.ActivityStartEndHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RouteAnalyzer {

    private Map<Id, Agent> selectedAgents;


    public RouteAnalyzer() {
        selectedAgents = new HashMap<>();
    }


    public void runRouteAnalyzer(String eventsFile, Id originLink, Id destinationLink) {


        EventsManager eventsManager = EventsUtils.createEventsManager();
        FindAgentsHandler findAgentsHandler = new FindAgentsHandler(selectedAgents, originLink, destinationLink);
        eventsManager.addHandler(findAgentsHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        System.out.println("There are " + selectedAgents.size() + " vehicles driving from link " + originLink.toString() + " to link " + destinationLink.toString());

        EventsManager eventsManager2 = EventsUtils.createEventsManager();
        CollectRoutesHandler collectRoutesHandler = new CollectRoutesHandler(selectedAgents);
        eventsManager2.addHandler(collectRoutesHandler);
        new MatsimEventsReader(eventsManager2).readFile(eventsFile);


    }

    public void prinOutRoutes(String fileName) {

        BufferedWriter bw = IOUtils.getBufferedWriter(fileName);

        try {
            //print out the data
            bw.write("origin_link,destination_link,person_id,sequence,link,departure_time,arrival_time");
            bw.newLine();

            for (Id id : selectedAgents.keySet()) {
                Agent agent = selectedAgents.get(id);
                double time =  - agent.getTimeAtOrigin();
                for (int i = 0; i < agent.getRoute().size(); i++) {
                    bw.write(agent.getRoute().get(0).toString() + "," +
                            agent.getRoute().get(agent.getRoute().size() - 1).toString() + "," +
                            agent.getId().toString() + "," +
                            i + "," +
                            agent.getRoute().get(i).toString() + "," +
                            agent.getTimeAtOrigin() + "," +
                            agent.getTimeAtDestination());
                    bw.newLine();
                }
            }


            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
