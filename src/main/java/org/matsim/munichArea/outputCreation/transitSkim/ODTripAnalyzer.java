package org.matsim.munichArea.outputCreation.transitSkim;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.munichArea.configMatsim.createDemandPt.PtSyntheticTraveller;

import java.util.Map;

/**
 * Created by carlloga on 3/2/17.
 */
public class ODTripAnalyzer implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler, AgentWaitingForPtEventHandler {

    private Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap;


    public ODTripAnalyzer(Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap) {
        this.ptSyntheticTravellerMap = ptSyntheticTravellerMap;
    }


    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        try {
            PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
            if (event.getActType().equals("home")) ptSyntheticTraveller.setDepartureTime(event.getTime());

        } catch (Exception e) {}
    }

    public void handleEvent(PersonEntersVehicleEvent event) {
        try {
            PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
            ptSyntheticTraveller.boards(event.getTime());

        } catch (Exception e) {}
    }

    public void handleEvent(PersonLeavesVehicleEvent event) {
        try {
            PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
            ptSyntheticTraveller.alights(event.getTime());
        } catch (Exception e) {}
    }

    public void handleEvent(ActivityStartEvent event) {
        try {
            PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
            if (event.getActType().equals("work")) ptSyntheticTraveller.setArrivalTime(event.getTime());
        } catch (Exception e) {}
    }


    @Override
    public void handleEvent(AgentWaitingForPtEvent event) {
        try {
            PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
            ptSyntheticTraveller.addLeg(event.getWaitingAtStopId().toString(), event.getDestinationStopId().toString());
        } catch (Exception e) {}
    }
}
