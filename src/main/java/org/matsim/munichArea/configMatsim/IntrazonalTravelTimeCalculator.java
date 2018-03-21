package org.matsim.munichArea.configMatsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.router.DijkstraTree;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.munichArea.Util;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class IntrazonalTravelTimeCalculator implements IterationEndsListener {

    private final static Logger log = Logger.getLogger(IntrazonalTravelTimeCalculator.class);
    private final Controler controler;
    private final Map<Integer, SimpleFeature> zoneFeatureMap;
    private final Network network;
    private final int finalIteration;
    private ArrayList<Location> locationList;
    private String fileName;
    private Map<Integer,ArrayList<Node>> nodesByZone;

    private final double departureTime = 1*60*60;

    public IntrazonalTravelTimeCalculator(Controler controler, Network network,
                                          int finalIteration, Map<Integer, SimpleFeature> zoneFeatureMap,
                                          ArrayList<Location> locationList, String fileName) {

        this.locationList = locationList;
        this.finalIteration = finalIteration;
        this.controler = controler;
        this.zoneFeatureMap = zoneFeatureMap;
        this.network = network;
        this.fileName = fileName;

        nodesByZone = new HashMap<>();


    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {

        if (iterationEndsEvent.getIteration() == this.finalIteration) {

            network.getNodes().values().parallelStream().forEach(node -> {
                for(Location location : locationList){
                    if (Util.checkCoordinatesInFeature(node.getCoord(), zoneFeatureMap.get(location.getId()))) {
                        if (!node.equals(null)) {
                            ArrayList<Node> nodes;
                            if (nodesByZone.containsKey(location.getId())) {
                                nodes = nodesByZone.get(location.getId());
                            } else {
                                nodes = new ArrayList<>();
                                nodesByZone.put(location.getId(), nodes);
                            }
                            nodes.add(node);
                            break;
                        }
                    }
                }
            });


            try {
                PrintWriter pw = new PrintWriter(new FileWriter(fileName));
                pw.println("zone,nodes,intra_distance,intra_time");

                TravelTime travelDistance;
                TravelDisutility travelDistanceDisutility;

                travelDistance = new DistListener.TravelDistanceAsTime();
                travelDistanceDisutility = new DistListener.DistanceAsDisutility();



                Person person = DistListener.getPerson();
                Vehicle vehicle = DistListener.getVehicle();

                locationList.parallelStream().forEach(location ->{
                //for (Location location : locationList) {
                    log.info("starting to calculate intrazonal for zone " + location.getId());
                    ArrayList<Node> intrazonalNodes;
                    intrazonalNodes = nodesByZone.get(location.getId());

                    try {
                        ArrayList<Double> intrazonalDistances = new ArrayList<>();
                        ArrayList<Double> intrazonalTimes = new ArrayList<>();
                        ArrayList<Node> intrazonalDestNodes = getRandomNodes(20, intrazonalNodes);
                        ArrayList<Node> intrazonalOrigNodes = getRandomNodes(20, intrazonalNodes);

                        intrazonalOrigNodes.forEach(originNode -> {
                            DijkstraTree dijkstraDistance = new DijkstraTree(network, travelDistanceDisutility, travelDistance);
                            dijkstraDistance.calcLeastCostPathTree(originNode, departureTime);
                            intrazonalDestNodes.forEach(destinationNode -> {
                                if (!originNode.equals(destinationNode) && !destinationNode.equals(null) & !originNode.equals(null)) {
                                    LeastCostPathCalculator.Path path = dijkstraDistance.getLeastCostPath(destinationNode);
                                    if (!path.equals(null)) {
                                        double distance = 0;
                                        double time = 0;
                                        for (Link link : path.links) {
                                            distance += link.getLength();
                                            time += controler.getLinkTravelTimes().getLinkTravelTime(link, departureTime, person, vehicle);
                                        }
                                        intrazonalDistances.add(distance);
                                        intrazonalTimes.add(time);
                                    }
                                }
                            });

                        });


                        log.info("Distances calculated for zone " + location.getId());

                        if (!intrazonalDistances.isEmpty()) {
                            pw.print(location.getId() + ",");
                            pw.print(intrazonalNodes.size() + ",");
                            double avg = aggregateValues(intrazonalDistances);
                            pw.print(avg);
                            pw.print(",");
                            avg = aggregateValues(intrazonalTimes);
                            pw.print(avg);
                            pw.println();
                        } else {
                            pw.println(location.getId() + ",noRoutes,noRoutes");
                        }
                    } catch (NullPointerException e) {
                        pw.println(location.getId() + ",0,noNodes,noNodes");
                    }

                });
                pw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    double aggregateValues(ArrayList<Double> listOfDoubles){
        double sum = 0;
        for (Double d : listOfDoubles){
            sum += d;
        }

        return sum / listOfDoubles.size();
    }

    ArrayList<Node> getRandomNodes(int numberOfElements, ArrayList<Node> listOfElements){
        ArrayList<Node> newList = new ArrayList<>();
        int actualNumberOfElements = Math.min(numberOfElements, listOfElements.size());
        Collections.shuffle(listOfElements);
        for (int i = 0; i < actualNumberOfElements; i++){
            newList.add(listOfElements.get(i));
        }
        return newList;
    }
}

