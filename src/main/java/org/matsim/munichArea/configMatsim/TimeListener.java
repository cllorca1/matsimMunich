package org.matsim.munichArea.configMatsim;

/**
 * Created by carlloga on 9/14/2016. copyed from siloMatsim package in github silo
 */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import com.pb.common.matrix.Matrix;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.vehicles.Vehicle;


/**
 * @author dziemke
 */
public class TimeListener implements IterationEndsListener {
    private final static Logger log = Logger.getLogger(TimeListener.class);

    private Controler controler;
    private Network network;
    private int finalIteration;
    //private Map<Integer, SimpleFeature> zoneFeatureMap;
    private ArrayList<Location> locationList;
    private int departureTime;
    private int maxNumberOfCalcPoints;
    //	private CoordinateTransformation ct;
    private Matrix autoTravelTime;


    public TimeListener(Controler controler, Network network,
                        int finalIteration, /*Map<Integer, SimpleFeature> zoneFeatureMap*/
                                       ArrayList<Location> locationList,
                        int timeOfDay,
                        int numberOfCalcPoints //CoordinateTransformation ct,
    ) {

        this.autoTravelTime = new Matrix(locationList.size(), locationList.size());
        this.controler = controler;
        this.network = network;
        this.finalIteration = finalIteration;
        //this.zoneFeatureMap = zoneFeatureMap;
        this.locationList = locationList;
        this.departureTime = timeOfDay*3600;
        this.maxNumberOfCalcPoints = numberOfCalcPoints;
//		this.ct = ct;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() == this.finalIteration) {
            float startTime = System.currentTimeMillis();
            //EuclideanDistanceCalculator euclideanDistanceCalculator = new EuclideanDistanceCalculator();
            log.info("Starting to calculate average zone-to-zone travel times based on MATSim at " + departureTime);
            TravelTime travelTime = controler.getLinkTravelTimes();
            TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
//            TravelDisutility travelTimeAsTravelDisutility = new MyTravelTimeDisutility(controler.getLinkTravelTimes());
//            Dijkstra dijkstra = new Dijkstra(network, travelTimeAsTravelDisutility, travelTime);

            //Maps to assign a node/tree to each zone
            Map<Integer, ArrayList<Node>> nodeMap = new ConcurrentHashMap<>();
            Map<Integer, ArrayList<Map<Id<Node>, LeastCostPathTree.NodeData>>> treeMap = new ConcurrentHashMap<>();

            locationList.parallelStream().forEach(loc -> {
                //for (Location loc : locationList) {
                LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);
                ArrayList<Map<Id<Node>, LeastCostPathTree.NodeData>> treesByZone = new ArrayList<>();
                ArrayList<Node> nodesByZone = new ArrayList<>();
                for (int nNode = 0; nNode < maxNumberOfCalcPoints; nNode++) {
                    Coord originCoord = new Coord(loc.getX() + (Math.random() - 0.5) * loc.getSize(), loc.getY() + (Math.random() - 0.5) * loc.getSize());
                    Node originNode = NetworkUtils.getNearestLink(network, originCoord).getToNode();
                    nodesByZone.add(originNode);
                    leastCoastPathTree.calculate(network, originNode, departureTime);
                    Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();
                    treesByZone.add(tree);
                }
                treeMap.put(loc.getId(), treesByZone);
                nodeMap.put(loc.getId(), nodesByZone);
                //log.info("assigned nodes and trees for zone: " + loc.getId());
                //}
                //log.info(treesByZone.size() + " found for zone " + loc.getId());
            });


            float duration = (System.currentTimeMillis() - startTime) / 1000 / 60;
            log.info("assigned nodes and trees in " + duration + " minutes");


            for (Location originZone : locationList) { // going over all origin zones
                for (Location destinationZone : locationList) {
                    //do only half matrix:

                    if (originZone.getId() <= destinationZone.getId()) {
                        //here could check the number of calculated points depending on the distance if wanted to keep a better accuracy
                        float congestedTravelTimeMin = 0; //this is a cumulative sum if more than one point
                        for (int i = 0; i < maxNumberOfCalcPoints; i++) {
                            for (int j = 0; j < maxNumberOfCalcPoints; j++) {
                                try {
                                    ArrayList<Map<Id<Node>, LeastCostPathTree.NodeData>> treesByZone = treeMap.get(originZone.getId());
                                    Map<Id<Node>, LeastCostPathTree.NodeData> tree = treesByZone.get(i);
                                    Node destinationNode = nodeMap.get(destinationZone.getId()).get(j);
                                    double arrivalTime = tree.get(destinationNode.getId()).getTime();
                                    //congested car travel times in minutes
                                    congestedTravelTimeMin += (float) ((arrivalTime - departureTime) / 60.);
                                } catch (Exception e) {
                                    log.error(originZone.getId() + " to " + destinationZone.getId());
                                }
                            }
                        }
                        autoTravelTime.setValueAt(originZone.getId(), destinationZone.getId(), congestedTravelTimeMin / maxNumberOfCalcPoints / maxNumberOfCalcPoints);
                        //if only done half matrix need to add next line
                        autoTravelTime.setValueAt(destinationZone.getId(), originZone.getId(), congestedTravelTimeMin / maxNumberOfCalcPoints / maxNumberOfCalcPoints);
                    }
//                });
                }
                //log.info("Completed origin zone: " + originZone.getId());
            }
//          });
            duration = (System.currentTimeMillis() - startTime) / 1000 / 60;
            log.info("Completed in: " + duration + " minutes");

        }


    }


    // inner class to use travel time as travel disutility
    class MyTravelTimeDisutility implements TravelDisutility {
        TravelTime travelTime;

        public MyTravelTimeDisutility(TravelTime travelTime) {
            this.travelTime = travelTime;
        }


        @Override
        public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
            return travelTime.getLinkTravelTime(link, time, person, vehicle);
        }


        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return link.getLength() / link.getFreespeed(); // minimum travel time
        }

    }

    public Matrix getAutoTravelTime() {
        return autoTravelTime;
    }


    /**
     * @author dziemke
     */
}