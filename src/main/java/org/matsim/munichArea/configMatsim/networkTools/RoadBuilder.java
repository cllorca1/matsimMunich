package org.matsim.munichArea.configMatsim.networkTools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.munichArea.configMatsim.Zone2ZoneTravelDistanceListener;
import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.ResourceBundle;

public class RoadBuilder {

    private final static Logger log = Logger.getLogger(RoadBuilder.class);

    private ResourceBundle rb;
    private Network network;

    public RoadBuilder(ResourceBundle rb) {
        this.rb = rb;
    }

    public void buildRoads(Network network) {

        this.network = network;
        //create new nodes
        //example node n1
        double x1 = 4541826;
        double y1 = 5302196;
        Coord c1 = new Coord(x1, y1);
        Node n1 = NetworkUtils.createNode(Id.createNodeId("node1"), c1);
        network.addNode(n1);
        //example node n2
        double x2 = 4542289;
        double y2 = 5301248;
        Coord c2 = new Coord(x2, y2);
        Node n2 = NetworkUtils.createNode(Id.createNodeId("node2"), c2);
        network.addNode(n2);

        //get Node from its id
        Node exampleNode = network.getNodes().get(Id.createNodeId(265776702));
        log.info("A node has been found by its id : " + exampleNode.getId().toString());
        //remove two links because the new roads intersects them
        //get node from its id
        Link l1 = network.getLinks().get(Id.createLinkId(141381));
        log.info("A node has been found by its id : " + l1.getToNode().getId().toString());
        //get nodes from to link
        Node n3 = l1.getFromNode();
        Node n4 = l1.getToNode();
        //inverse direction
        Link l2 = NetworkUtils.getConnectingLink(l1.getToNode(), l1.getFromNode());
        network.removeLink(l1.getId());
        network.removeLink(l2.getId());

        //find the intersection and add the new node
        //slope of line 1-2
        double m12 = (y2 - y1) / (x2 - x1);
        //slope of line 3-4
        double m34 = (n4.getCoord().getY() - n3.getCoord().getY()) / (n4.getCoord().getX() - n3.getCoord().getX());
        //coordinates of the intersection
        double x5 = (n3.getCoord().getY() - y1 - m34 * n3.getCoord().getX() + m12 * x1) / (m12 - m34);
        double y5 = y1 + m12 * (x5 - x1);
        Coord c3 = new Coord(x5, y5);
        Node n5 = NetworkUtils.createNode(Id.createNodeId("node5"), c3);
        network.addNode(n5);

        //create the new links connecting these 5 nodes
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);
        nodes.add(n4);
        nodes.add(n5);

        int i = 0;
        for (Node d : nodes) {
            if (!n5.equals(d)) {
                i++;
                String linkId;
                linkId = "link" + i;
                Link centerLink;
                centerLink = NetworkUtils.createLink(Id.createLinkId(linkId), n5, d, network, NetworkUtils.getEuclideanDistance(n5.getCoord(), d.getCoord()), 30, 200, 2);
                network.addLink(centerLink);
                //and the same for the return link
                i++;
                linkId = "link" + i;
                centerLink = NetworkUtils.createLink(Id.createLinkId(linkId), d, n5, network, NetworkUtils.getEuclideanDistance(n5.getCoord(), d.getCoord()), 30, 200, 2);
                network.addLink(centerLink);
            }

        }

    }

    public Network getNetwork() {
        return network;
    }
}
