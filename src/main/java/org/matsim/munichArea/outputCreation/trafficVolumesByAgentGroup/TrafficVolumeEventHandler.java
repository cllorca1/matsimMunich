package org.matsim.munichArea.outputCreation.trafficVolumesByAgentGroup;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.Map;

public class TrafficVolumeEventHandler implements LinkEnterEventHandler {

    private Map<Id, Multiset<AgentGroup>> volumeByLinkAndGroup;

    public TrafficVolumeEventHandler(Map<Id, Multiset<AgentGroup>> volumeByLinkAndGroup){
        this.volumeByLinkAndGroup = volumeByLinkAndGroup;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id linkId = event.getLinkId();
        Id personId = event.getVehicleId();
        if (volumeByLinkAndGroup.containsKey(linkId)){
            volumeByLinkAndGroup.get(linkId).add(AgentGroup.getGroupFromPersonId(personId));
        } else {
            Multiset<AgentGroup> volumeByGroup = HashMultiset.create();
            volumeByGroup.add(AgentGroup.getGroupFromPersonId(personId));
            volumeByLinkAndGroup.put(linkId, volumeByGroup);
        }
    }

    @Override
    public void reset(int iteration) {

    }
}
