package org.matsim.simpleMatsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class SimpleNetworkGenerator {

    private static Network network;

    private static int lanes = 1;
    private static double speed = 50/3.6;


    public static Network createNetwork( double capacity){

        network = NetworkUtils.createNetwork();

        Coord originCoord = new Coord(-6000,0);
        Coord origin2Coord = new Coord(-5000,0);
        Coord entranceCoord = new Coord(0,0);
        Coord exitCoord = new Coord(20000,0);
        Coord destinationCoord = new Coord(25000,0);

        Node origin = NetworkUtils.createNode(Id.createNodeId("origin"), originCoord);
        Node origin2 = NetworkUtils.createNode(Id.createNodeId("origin2"), origin2Coord);
        Node entrance = NetworkUtils.createNode(Id.createNodeId("entrance"), entranceCoord);
        Node exit = NetworkUtils.createNode(Id.createNodeId("exit"), exitCoord);
        Node destination = NetworkUtils.createNode(Id.createNodeId("destination"), destinationCoord);


        network.addNode(origin);
        network.addNode(origin2);
        network.addNode(entrance);
        network.addNode(exit);
        network.addNode(destination);

        Link preAccessLink = NetworkUtils.createLink(Id.createLinkId("preAccess"),origin,
                origin2, network,NetworkUtils.getEuclideanDistance(originCoord,origin2Coord),
                speed, capacity*10, lanes);

        Link accessLink = NetworkUtils.createLink(Id.createLinkId("access"),origin2,
                entrance, network,NetworkUtils.getEuclideanDistance(origin2Coord,entranceCoord),
                speed, capacity*10, lanes);

        Link analyzedLink = NetworkUtils.createLink(Id.createLinkId("analyzedLink"),entrance,
                exit, network,NetworkUtils.getEuclideanDistance(entranceCoord,exitCoord),
                speed, capacity, lanes);

        Link egressLink = NetworkUtils.createLink(Id.createLinkId("egress"),exit,
                destination, network,NetworkUtils.getEuclideanDistance(exitCoord,destinationCoord),
                speed, capacity/2, lanes);

        network.addLink(preAccessLink);
        network.addLink(accessLink);
        network.addLink(analyzedLink);
        network.addLink(egressLink);


        return network;

    }



}
