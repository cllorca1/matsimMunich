package org.matsim.munichArea.outputCreation.routeAnalyzer;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.munichArea.outputCreation.tripDurationAnalyzer.AgentTripDurationEventAnalyzer;

import java.io.File;
import java.util.ResourceBundle;

public class RunRouteAnalyzer {

    private static ResourceBundle rb;

    //filenames are passed to the application as args and as absolute paths

    public static void main(String[] args) {


        File propFile = new File("routeAnalyzer.properties");
        rb = ResourceUtil.getPropertyBundle(propFile);

        String eventsFile = rb.getString("events.file");
        int[] listOfOrigins = ResourceUtil.getIntegerArray(rb, "origin.links");
        int[] listOfDestinations = ResourceUtil.getIntegerArray(rb, "destination.links");
        int counter = 1;
        int odPairs = listOfDestinations.length*listOfDestinations.length-listOfDestinations.length;
        for (int origin : listOfOrigins){
            for (int destination : listOfDestinations){
                if (origin != destination){
                    RouteAnalyzer analyzer = new RouteAnalyzer();
                    analyzer.runRouteAnalyzer(eventsFile, Id.createLinkId(origin), Id.createLinkId(destination));
                    String outputFileName = counter + ".csv";
                    analyzer.prinOutRoutes(outputFileName);
                    System.out.println("Completed the origin-destination pair number "  + counter + " of a total of " +odPairs  );
                    counter++;
                }
            }
        }
    }
}



