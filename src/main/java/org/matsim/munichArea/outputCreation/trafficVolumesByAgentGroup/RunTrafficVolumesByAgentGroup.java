package org.matsim.munichArea.outputCreation.trafficVolumesByAgentGroup;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.munichArea.outputCreation.routeAnalyzer.FindAgentsHandler;
import org.matsim.munichArea.outputCreation.routeAnalyzer.RouteAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class RunTrafficVolumesByAgentGroup {




    public static void main(String[] args) throws IOException {


        Map<Id, Multiset<AgentGroup>> volumeByLinkAndGroup = new HashMap<>();

        String eventsFile = args[0];
        String outputFileName = args[1];

        EventsManager eventsManager = EventsUtils.createEventsManager();
        TrafficVolumeEventHandler eventHandler = new TrafficVolumeEventHandler(volumeByLinkAndGroup);
        eventsManager.addHandler(eventHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputFileName)));

        StringBuilder builder = new StringBuilder();
        builder.append("link");
        for (AgentGroup agentGroup : AgentGroup.values()){
            builder.append(",");
            builder.append(agentGroup.toString());
        }
        pw.println(builder);


        for (Id id : volumeByLinkAndGroup.keySet()){
            builder = new StringBuilder();
            builder.append(id.toString());
            for (AgentGroup agentGroup : AgentGroup.values()){
                builder.append(",");
                builder.append(volumeByLinkAndGroup.get(id).count(agentGroup));
            }
            pw.println(builder);
        }

        pw.close();

    }

}
