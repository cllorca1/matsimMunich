package org.matsim.munichArea.configMatsim;

/**
 * Created by carlloga on 9/14/2016. copyed from siloMatsim package in github silo
 */

import java.util.*;


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
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.munichArea.outputCreation.EuclideanDistanceCalculator;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.vehicles.Vehicle;


/**
 * @author dziemke
 */
public class Zone2ZoneTravelTimeListener implements IterationEndsListener {
    private final static Logger log = Logger.getLogger(Zone2ZoneTravelTimeListener.class);

    private Controler controler;
    private Network network;
    private int finalIteration;
    //private Map<Integer, SimpleFeature> zoneFeatureMap;
    private ArrayList<Location> locationList;
    private int departureTime;
    private int maxNumberOfCalcPoints;
    //	private CoordinateTransformation ct;
    private Matrix autoTravelTime;


    public Zone2ZoneTravelTimeListener(Controler controler, Network network,
                                       int finalIteration, /*Map<Integer, SimpleFeature> zoneFeatureMap*/
                                       ArrayList<Location> locationList,
                                       int timeOfDay,
                                       int numberOfCalcPoints //CoordinateTransformation ct,
    ) {
        this.controler = controler;
        this.network = network;
        this.finalIteration = finalIteration;
        //this.zoneFeatureMap = zoneFeatureMap;
        this.locationList = locationList;
        this.departureTime = timeOfDay;
        this.maxNumberOfCalcPoints = numberOfCalcPoints;
//		this.ct = ct;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() == this.finalIteration) {
            float startTime = System.currentTimeMillis();
            EuclideanDistanceCalculator euclideanDistanceCalculator = new EuclideanDistanceCalculator();
            log.info("Starting to calculate average zone-to-zone travel times based on MATSim.");
            TravelTime travelTime = controler.getLinkTravelTimes();
            TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
//            TravelDisutility travelTimeAsTravelDisutility = new MyTravelTimeDisutility(controler.getLinkTravelTimes());
//            Dijkstra dijkstra = new Dijkstra(network, travelTimeAsTravelDisutility, travelTime);

            autoTravelTime = new Matrix(locationList.size(), locationList.size());

            //Maps to assign a node/tree to each zone
            Map<Integer, ArrayList<Node>> nodeMap = new HashMap<>();
            Map<Integer, ArrayList<Map<Id<Node>, LeastCostPathTree.NodeData>>> treeMap = new HashMap<>();

            locationList.parallelStream().forEach(loc -> {
                //for (Location loc : locationList) {
                LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);
                ArrayList<Map<Id<Node>, LeastCostPathTree.NodeData>> treesByZone = new ArrayList<>();
                ArrayList<Node> nodesByZone = new ArrayList<>();
                for (int nnode = 0; nnode < maxNumberOfCalcPoints; nnode++) {
                    Coord originCoord = new Coord(loc.getX() + (Math.random() - 1) * loc.getSize(), loc.getY() + (Math.random() - 1) * loc.getSize());
                    Node originNode = NetworkUtils.getNearestLink(network, originCoord).getFromNode();
                    nodesByZone.add(originNode);
                    leastCoastPathTree.calculate(network, originNode, departureTime);
                    Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();
                    treesByZone.add(tree);
                }
                treeMap.put(loc.getId(), treesByZone);
                nodeMap.put(loc.getId(), nodesByZone);
                //log.info("assigned nodes and trees for zone: " + loc.getId());
                //}
            });

            float duration = (System.currentTimeMillis() - startTime) / 1000 / 60;
            log.info("assigned nodes and trees in " + duration + " minutes");

            //locationList.parallelStream().forEach((Location originZone) -> {
            for (Location originZone : locationList) { // going over all origin zones
                locationList.parallelStream().forEach(destinationZone -> {
                    if (originZone.getId() <= destinationZone.getId()) {

                        //for (Location destinationZone : locationList) {
                        //here could check the number of calculated points depending on the distance if wanted to keep a better accuracy
                        float congestedTravelTimeMin = 0;
                        for (int i = 0; i < maxNumberOfCalcPoints; i++) {
                            for (int j = 0; j < maxNumberOfCalcPoints; j++) {
                                Map<Id<Node>, LeastCostPathTree.NodeData> tree = treeMap.get(originZone.getId()).get(i);
                                Node destinationNode = nodeMap.get(destinationZone.getId()).get(j);
                                double arrivalTime = tree.get(destinationNode.getId()).getTime();
                                //congested car travel times in minutes
                                congestedTravelTimeMin += (float) ((arrivalTime - departureTime) / 60.);
                            }
                        }
                        autoTravelTime.setValueAt(originZone.getId(), destinationZone.getId(), congestedTravelTimeMin / maxNumberOfCalcPoints / maxNumberOfCalcPoints);
                        //if only done half matrix need to add next line
                        autoTravelTime.setValueAt(destinationZone.getId(), originZone.getId(), congestedTravelTimeMin / maxNumberOfCalcPoints / maxNumberOfCalcPoints);

                    }
                });
//              }
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