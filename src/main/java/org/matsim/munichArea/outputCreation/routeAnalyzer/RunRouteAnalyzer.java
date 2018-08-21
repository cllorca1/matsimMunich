package org.matsim.munichArea.outputCreation.routeAnalyzer;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.munichArea.outputCreation.tripDurationAnalyzer.AgentTripDurationEventAnalyzer;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.io.File;
import java.util.Map;
import java.util.ResourceBundle;

public class RunRouteAnalyzer {

    private static ResourceBundle rb;

    //filenames are passed to the application as args and as absolute paths

    public static void main(String[] args) {


        File propFile = new File("c:/models/matsimRouteAnalyzer/routeAnalyzer.properties");
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
                    String outputFileName = rb.getString("prefix") + counter + ".csv";
                    analyzer.prinOutRoutes(outputFileName);
                    System.out.println("Completed the origin-destination pair number "  + counter + " of a total of " +odPairs  );
                    counter++;
                }
            }
        }
    }
}



