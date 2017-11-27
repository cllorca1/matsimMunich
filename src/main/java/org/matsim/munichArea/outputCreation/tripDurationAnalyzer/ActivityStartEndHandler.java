package org.matsim.munichArea.outputCreation.tripDurationAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.network.NetworkUtils;
import sun.nio.ch.Net;

import java.util.Collection;
import java.util.Map;

/**
 * Created by carlloga on 17.03.2017.
 */

public class ActivityStartEndHandler implements ActivityEndEventHandler,
        ActivityStartEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler, AgentWaitingForPtEventHandler {




    private Map<Id, Trip> tripMap;



    public ActivityStartEndHandler(Map<Id, Trip> tripMapH2W) {
        this.tripMap = tripMapH2W;


    }

    @Override
    public void reset(int iteration) {
    }


    @Override
    public void handleEvent(ActivityEndEvent event) {


       /* if (event.getPersonId().toString().equals("968188")){
            System.out.println("TracePerson968188" + event.getEventType() + " at " + event.getTime() + " the " + event.getActType());
        }*/


        //detects end of activity home
        //fails at the second activity we are collecting
        if (event.getActType().equals("home")){
            Trip t = new Trip(event.getPersonId());
            tripMap.put(event.getPersonId(), t);
        }

    }

    public void handleEvent(PersonDepartureEvent event) {

        /*if (event.getPersonId().toString().equals("968188")){
            System.out.println("TracePerson968188" + event.getEventType() + " at " + event.getTime()  );
        }*/
        //detects the event of departing from home and assigns departure time and mode
        try {
        Trip t = tripMap.get(event.getPersonId());

        //only if not yet at work
        if (t.isAtHome() && !t.isTraveling()) {
            t.setDepartureTime(event.getTime());
            t.setMode(event.getLegMode().toString());
            t.setTraveling(true);
        }
        }catch (Exception e){

        }
    }

    @Override
    public void handleEvent (PersonEntersVehicleEvent event){

       /* if (event.getPersonId().toString().equals("968188")){
            System.out.println("TracePerson968188" + event.getEventType() + " at " + event.getTime()  );
        }
*/
        try {
            Trip t = tripMap.get(event.getPersonId());
            //only if not yet at work
            if (t.isTraveling()) {
                t.setVehicleStartTime(event.getTime());
            }
        }catch (Exception e){

        }
        //}
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {

        /*if (event.getPersonId().toString().equals("968188")){
            System.out.println("TracePerson968188" + event.getEventType() + " at " + event.getTime()  );
        }*/
        //detects the event of arriving to work
        try {
        Trip t = tripMap.get(event.getPersonId());
        //only if coming from home
        if (t.isTraveling()) {
            t.setArrivalTime(event.getTime());
        }
        }catch (Exception e){

        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {

       /* if (event.getPersonId().toString().equals("968188")){
            System.out.println("TracePerson968188" + event.getEventType() + " at " + event.getTime() + " the " + event.getActType() );
        }*/
        //detects the event of arriving to work
        try {
        if (event.getActType().equals("work")){
            Trip t = tripMap.get(event.getPersonId());
            t.setAtWorkPlace(true);
            t.setAtHome(false);
            t.setTraveling(false);
            t.setPurpose('w');
        } else if (event.getActType().equals("other")){
            Trip t = tripMap.get(event.getPersonId());
            t.setAtOther(true);
            t.setAtHome(false);
            t.setTraveling(false);
            t.setPurpose('o');
        }

        }catch (Exception e){

        }

    }

    @Override
    public void handleEvent (LinkEnterEvent event){

        //todo assumes that person id and vehicle id is the same !!!!!
        try {

            Trip t = tripMap.get(event.getVehicleId());
            if (t.isTraveling()){
                t.addLinkToList(event.getLinkId());
            }
        } catch (Exception e){}

    }

    public void handleEvent(AgentWaitingForPtEvent event){

       /* if (event.getPersonId().toString().equals("968188")){
            System.out.println("TracePerson968188" + event.getEventType()  + " at " + event.getTime() );
        }*/
        try {

            Trip t = tripMap.get(event.getPersonId());
            if (t.isTraveling() && t.getArrivalAtTransitStop() == 0){
                t.setArrivalAtTransitStop(event.getTime());
            }
        } catch (Exception e){}


    }



}

