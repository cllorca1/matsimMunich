package org.matsim.munichArea.configMatsim;

/**
 * Created by carlloga on 9/14/2016. copyed from siloMatsim package in github silo
 */

import com.pb.common.matrix.Matrix;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.router.DijkstraTree;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.munichArea.outputCreation.EuclideanDistanceCalculator;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author dziemke
 */
public class DistListener implements IterationEndsListener {
    private final static Logger log = Logger.getLogger(DistListener.class);

    private Controler controler;
    private Network network;
    private int finalIteration;
    //private Map<Integer, SimpleFeature> zoneFeatureMap;
    private ArrayList<Location> locationList;
    private int departureTime;
    private int numberOfCalcPoints;
    private float upperDistanceThreshold;
    //	private CoordinateTransformation ct;
    private Matrix autoTravelDistance;


    public DistListener(Controler controler, Network network,
                        int finalIteration, /*Map<Integer, SimpleFeature> zoneFeatureMap*/
                                           ArrayList<Location> locationList,
                        int timeOfDay,
                        int numberOfCalcPoints,
                        float upperDistanceThreshold) {

        autoTravelDistance = new Matrix(locationList.size(), locationList.size());

        this.controler = controler;
        this.network = network;
        this.finalIteration = finalIteration;
        this.locationList = locationList;
        this.departureTime = timeOfDay*3600;
        this.numberOfCalcPoints = numberOfCalcPoints;
        this.upperDistanceThreshold = upperDistanceThreshold;

    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() == this.finalIteration) {

            EuclideanDistanceCalculator euclideanDistanceCalculator = new EuclideanDistanceCalculator();

            log.info("Starting to calculate average zone-to-zone travel distances based on MATSim.");

            TravelTime travelTime;
            TravelDisutility travelDisutility;

            boolean distanceDisutility = Boolean.parseBoolean("distance.is.disutility");

            if (distanceDisutility) {
                travelDisutility = new DistanceAsDisutility();
                travelTime = new TravelDistanceAsTime();
            } else {
                travelTime = controler.getLinkTravelTimes();
                travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
            }

            DijkstraTree dijkstra = new DijkstraTree(network, travelDisutility, travelTime);


            //Map to assign a node to each zone
            Map<Integer, Node> zoneCalculationNodesMap = new HashMap<>();

            for (Location loc : locationList) {
                Coord originCoord = new Coord(loc.getX(), loc.getY());
                Link originLink = NetworkUtils.getNearestLink(network, originCoord);
                Node originNode = originLink.getFromNode();
                zoneCalculationNodesMap.put(loc.getId(), originNode);
            }

            long startTime = System.currentTimeMillis();


            for (Location originZone : locationList) { // going over all origin zones
                Node originNode = zoneCalculationNodesMap.get(originZone.getId());
                //leastCoaptPathTree.calculate(network, originNode, departureTime);

                //Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();

                dijkstra.calcLeastCostPathTree(originNode, departureTime);

                locationList.parallelStream().forEach((Location destinationZone) -> {
                    //nex line to fill only half matrix and use half time
                    if (originZone.getId() <= destinationZone.getId()) {
                        Node destinationNode = zoneCalculationNodesMap.get(destinationZone.getId());
                        //with the next if tense it is possible to limit the distance calculation to certain threshold, over it --> eucl.dist.
                        float euclideanDistance = euclideanDistanceCalculator.getDistanceFrom(originZone, destinationZone);
                        if (euclideanDistance < upperDistanceThreshold) {
                            //Dijkstra dijkstra = new Dijkstra(network, travelDisutility, travelTime);
                            LeastCostPathCalculator.Path path = dijkstra.getLeastCostPath(destinationNode);

                            float distance = 0;
                            for (Link link : path.links) {
                                distance += link.getLength();
                            }
                            //double arrivalTime = tree.get(destinationNode.getId()).getTime();
                            //congested car travel times in minutes
                            //float congestedTravelTimeMin = (float) ((arrivalTime - departureTime) / 60.);
                            autoTravelDistance.setValueAt(originZone.getId(), destinationZone.getId(), distance);
                            //if only done half matrix need to add next line
                            autoTravelDistance.setValueAt(destinationZone.getId(), originZone.getId(), distance);

                        } else {
                            autoTravelDistance.setValueAt(originZone.getId(), destinationZone.getId(), euclideanDistance);
                            //if only done half matrix need to add next line
                            autoTravelDistance.setValueAt(destinationZone.getId(), originZone.getId(), euclideanDistance);
                        }
                    }

                });
                log.info("Completed origin zone: " + originZone.getId());
            }


            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Completed in: " + duration);
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


    class DistanceAsDisutility implements TravelDisutility {


        public DistanceAsDisutility() {

        }

        @Override
        public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength();
        }


        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return link.getLength(); // minimum travel time
        }
    }


    class TravelDistanceAsTime implements TravelTime {

        @Override
        public double getLinkTravelTime(Link link, double v, Person person, Vehicle vehicle) {
            return link.getLength();
        }
    }

    public Matrix getAutoTravelDistance() {
        return autoTravelDistance;
    }

/**
 * @author dziemke
 */
}