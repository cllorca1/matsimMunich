package org.matsim.munichArea.multimodal;

import com.pb.common.datafile.TableDataSet;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.munichArea.Util;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;



/**
 * Created by carlloga on 12-05-17.
 */
public class AddCyclingHighway {
    private ResourceBundle rb;
    TableDataSet cyclingHighway;
    int[] pointList;

    public AddCyclingHighway(ResourceBundle rb) {
        this.rb = rb;
    }

    public void addCyclingHighway() {

        cyclingHighway = Util.readCSVfile(rb.getString("cycling.highway.points"));
        cyclingHighway.buildIndex(1);
        pointList = cyclingHighway.getColumnAsInt(1);


        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(rb.getString("old.network.file"));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        Coord fromCoord = new Coord(cyclingHighway.getIndexedValueAt(1, "x"), cyclingHighway.getIndexedValueAt(1, "y"));
        Coord toCoord;
        Node fromNode;
        Node toNode;
        Link link;

        float speedFactor = Float.parseFloat(rb.getString("speed.factor"));

        for (int i=2; i< pointList.length - 1; i++) {

            toCoord = new Coord(cyclingHighway.getIndexedValueAt(i, "x"), cyclingHighway.getIndexedValueAt(i, "y"));

            if (!fromCoord.equals(toCoord)){
                fromNode = NetworkUtils.getNearestNode(network, fromCoord);
                toNode = NetworkUtils.getNearestNode(network, toCoord);
                link = NetworkUtils.createLink(Id.createLinkId("CH1_" + i),
                        fromNode, toNode, network, NetworkUtils.getEuclideanDistance(fromCoord, toCoord)*speedFactor,
                        10, 500, 1);
                network.addLink(link);
                link = NetworkUtils.createLink(Id.createLinkId("CH2_" + i),
                        toNode, fromNode, network, NetworkUtils.getEuclideanDistance(fromCoord, toCoord)*speedFactor,
                        10, 500, 1);
                network.addLink(link);

                fromCoord = toCoord;
            }

//            link = NetworkUtils.createLink(Id.createLinkId("a2"), toNode, fromNode, network, 10, 10, 10000, 1);
//            network.addLink(link);

        }

        new NetworkWriter(network).write(rb.getString("new.network.file"));
    }
}
