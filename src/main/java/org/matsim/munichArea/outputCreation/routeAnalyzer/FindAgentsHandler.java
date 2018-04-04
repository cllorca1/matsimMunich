package org.matsim.munichArea.outputCreation.routeAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FindAgentsHandler implements LinkEnterEventHandler {

    private  Map<Id, Agent>  candidateAgents;
    private Map<Id, Agent> selectedAgents;
    private Id originLink;
    private Id destinationLink;

    public FindAgentsHandler (Map<Id, Agent>  selectedAgents, Id originLink, Id destinationLink) {
        this.selectedAgents = selectedAgents;
        this.originLink = originLink;
        this.destinationLink = destinationLink;
        candidateAgents = new HashMap<>();
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {

        if (linkEnterEvent.getLinkId().equals(originLink)){
            Agent agent = new Agent(linkEnterEvent.getVehicleId(), linkEnterEvent.getTime());
            candidateAgents.put(linkEnterEvent.getVehicleId(), agent);
        } else if (linkEnterEvent.getLinkId().equals(destinationLink)){
            if(candidateAgents.containsKey(linkEnterEvent.getVehicleId())){
                Agent agent = candidateAgents.get(linkEnterEvent.getVehicleId());
                agent.setTimeAtDestination(linkEnterEvent.getTime());
                selectedAgents.put(agent.getId(), agent);
            }
        }

    }

    @Override
    public void reset(int iteration) {

    }
}
