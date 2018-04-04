package org.matsim.munichArea.outputCreation.routeAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.ArrayList;
import java.util.Map;

public class CollectRoutesHandler implements LinkEnterEventHandler {

    private Map<Id, Agent> selectedAgents;

    public CollectRoutesHandler( Map<Id, Agent>  selectedAgents) {
        this.selectedAgents = selectedAgents;
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        Agent agent;
        if(selectedAgents.containsKey(linkEnterEvent.getVehicleId())){
            agent = selectedAgents.get(linkEnterEvent.getVehicleId());
            if (linkEnterEvent.getTime() >= agent.getTimeAtOrigin() && linkEnterEvent.getTime()<= agent.getTimeAtDestination() ) {
                agent.addLinkToRoute(linkEnterEvent.getLinkId());
            }
        }


    }

    @Override
    public void reset(int iteration) {

    }
}
