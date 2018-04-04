package org.matsim.munichArea.configMatsim.networkTools;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;



/**
 * "P" has to do with "Potsdam" and "Z" with "Zurich", but P and Z are mostly used to show which classes belong together.
 */
public class CreateNetwork {


    public static ResourceBundle rb;

    public static void main (String[] args){

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        createNetwork();

    }


    public static void createNetwork() {

		/*
		 * The input file name.
		 */
        String networkFolder = rb.getString("network.folder");
        String osm = networkFolder + rb.getString("osm.input.file");


		/*
		 * The coordinate system to use. OpenStreetMap uses WGS84, but for MATSim, we need a projection where distances
		 * are (roughly) euclidean distances in meters.
		 *
		 * UTM 33N is one such possibility (for parts of Europe, at least).
		 *
		 */
        /*CoordinateTransformation ct =
                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");*/

        CoordinateTransformation ct =
                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);


        //31468 is the coordinate system DHDN_3 zone 4
		/*
		 * First, create a new Config and a new Scenario. One always has to do this when working with the MATSim
		 * data containers.
		 *
		 */
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);


		/*
		 * Pick the Network from the Scenario for convenience.
		 */
        Network network = scenario.getNetwork();

        OsmNetworkReader onr = new OsmNetworkReader(network,ct);
        onr.parse(osm);



        /*
		 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
		 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
		 */
        new NetworkCleaner().run(network);

		/*
		 * Write the Network to a MATSim network file.
		 */
        new NetworkWriter(network).write(networkFolder + rb.getString("xml.network.file"));

        System.out.println("MATSIM network created");


    }

}
