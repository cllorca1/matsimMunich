package org.matsim.simpleMatsim;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.IOException;


public class SimpleEventAnalyzer {

    public static void run(String inputFileName, String outputFileName) throws IOException {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        SimpleEventHandler simpleEventHandler = new SimpleEventHandler();
        simpleEventHandler.initialize(outputFileName);
        eventsManager.addHandler(simpleEventHandler);
        new MatsimEventsReader(eventsManager).readFile(inputFileName);
        simpleEventHandler.close();


    }

}
