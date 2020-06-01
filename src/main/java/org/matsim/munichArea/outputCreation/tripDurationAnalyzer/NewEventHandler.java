package org.matsim.munichArea.outputCreation.tripDurationAnalyzer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


public class NewEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler,
PersonLeavesVehicleEventHandler, AgentWaitingForPtEventHandler, TeleportationArrivalEventHandler {


    public static void main(String[] args) throws FileNotFoundException {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        NewEventHandler newEventHandler = new NewEventHandler();
        eventsManager.addHandler(newEventHandler);

        pw = new PrintWriter(new File(args[0] + ".csv"));

        pw.println("type,person,time,link");
        new MatsimEventsReader(eventsManager).readFile(args[0]);
    }

    private static PrintWriter pw;
    private static  Logger logger = Logger.getLogger(NewEventHandler.class);

    public NewEventHandler() {


    }

    @Override
    public void handleEvent(ActivityEndEvent event) {

        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(event.getLinkId());

            pw.println(line);

        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {


        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(event.getLinkId());

            pw.println(line);

        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(event.getLinkId());

            pw.println(line);

        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(event.getLinkId());

            pw.println(line);

        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(-1);

            pw.println(line);

        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(-1);



            pw.println(line);

        }

    }

    @Override
    public void handleEvent(AgentWaitingForPtEvent event) {
        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(-1);



            pw.println(line);

        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if (event.getPersonId().toString().contains("mm")) {

            StringBuilder line = new StringBuilder();
            line.append(event.getEventType()).append(",");
            line.append(event.getPersonId()).append(",");
            line.append(event.getTime()).append(",");
            line.append(-1);

            pw.println(line);

        }
    }
}
