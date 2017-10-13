package org.matsim.munichArea.outputCreation.transitSkim;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.munichArea.configMatsim.createDemandPt.PtSyntheticTraveller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by carlloga on 3/2/17.
 */
public class TransitEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler, AgentWaitingForPtEventHandler, LinkEnterEventHandler, TransitDriverStartsEventHandler {

    private Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap;

    //stores which passengers are on board of which vehicle along time
    private Map<Id, ArrayList<Id>> vehiclesWithPassengersMap;
    private Network network;

    public TransitEventHandler(Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap, Network network) {
        this.network = network;
        this.ptSyntheticTravellerMap = ptSyntheticTravellerMap;
        vehiclesWithPassengersMap = new HashMap<>();

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
        //try {
            if(!event.getPersonId().equals(event.getVehicleId())) {
                Id vehicleId = event.getVehicleId();
                //this is a transit boarding event, otherwise might be a private car trip
                if (ptSyntheticTravellerMap.containsKey(event.getPersonId())) {
                    PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
                    ptSyntheticTraveller.boards(event.getTime());

                    ArrayList<Id> passengers = vehiclesWithPassengersMap.get(vehicleId);
                    passengers.add(event.getPersonId());
                    vehiclesWithPassengersMap.put(event.getVehicleId(),passengers);
                }
            }
        //} catch (Exception e) {}
    }

    public void handleEvent(PersonLeavesVehicleEvent event) {
        try {
            PtSyntheticTraveller ptSyntheticTraveller = ptSyntheticTravellerMap.get(event.getPersonId());
            ptSyntheticTraveller.alights(event.getTime());
            Id vehicleId = event.getVehicleId();
            ArrayList<Id> passengers = vehiclesWithPassengersMap.get(vehicleId);
            passengers.remove(event.getPersonId());
            vehiclesWithPassengersMap.put(event.getVehicleId(),passengers);

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


    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        try {
            if(linkEnterEvent.getVehicleId().toString().contains("train")) {
                //takes only public transport vehicles
                ArrayList<Id> passengers = vehiclesWithPassengersMap.get(linkEnterEvent.getVehicleId());
                for (Id id : passengers) {
                    if (!id.toString().contains("train")) {
                        PtSyntheticTraveller ptst = ptSyntheticTravellerMap.get(id);
                        double distance = network.getLinks().get(linkEnterEvent.getLinkId()).getLength();
                        ptst.addDistanceInTransit(distance);
                    }
                }
            }
        } catch (Exception e) {}

    }

    @Override
    public void handleEvent(TransitDriverStartsEvent transitDriverStartsEvent) {
        ArrayList<Id> passengers = new ArrayList<>();
        passengers.add(transitDriverStartsEvent.getDriverId());
        Id vehicleId = transitDriverStartsEvent.getVehicleId();
        vehiclesWithPassengersMap.put(vehicleId, passengers);
    }
}
