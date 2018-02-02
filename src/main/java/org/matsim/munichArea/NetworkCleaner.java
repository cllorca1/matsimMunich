package org.matsim.munichArea;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by carlloga on 17-05-17.
 */
public class NetworkCleaner {

    public static void main(String[] args) {

		/*
		 * The input file name.
		 */

		/*
		 * The coordinate system to use. OpenStreetMap uses WGS84, but for MATSim, we need a projection where distances
		 * are (roughly) euclidean distances in meters.
		 *
		 * UTM 33N is one such possibility (for parts of Europe, at least).
		 *
		 */
        /*CoordinateTransformation ct =
                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");*/

        // CoordinateTransformation ct =
        TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);


        //31468 is the coordinate system DHDN_3 zone 4
		/*
		 * First, create a new Config and a new Scenario. One always has to do this when working with the MATSim
		 * data containers.
		 *
		 */
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("./input/FFB.xml.gz");
//        config.network().setInputFile("./cottbus_robotaxi/FFBLight.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();


        new org.matsim.core.network.algorithms.NetworkCleaner().run(network);

        new NetworkWriter(network).write("./input/FFBClean.xml.gz");
        //new NetworkWriter(network).write("./cottbus_robotaxi/FFBLightClean.xml");

        System.out.println("MATSIM network created");

    }
}
