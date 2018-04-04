package org.matsim.munichArea.outputCreation.routeAnalyzer;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.munichArea.outputCreation.tripDurationAnalyzer.AgentTripDurationEventAnalyzer;

import java.io.File;
import java.util.ResourceBundle;

public class RunRouteAnalyzer {



    //filenames are passed to the application as args and as absolute paths

    public static void main(String[] args) {



        RouteAnalyzer analyzer = new RouteAnalyzer();
        analyzer.runRouteAnalyzer(args[0], Id.createLinkId(args[1]), Id.createLinkId(args[2]));
        analyzer.prinOutRoutes(args[3]);



    }
}



