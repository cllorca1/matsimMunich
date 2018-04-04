package org.matsim.munichArea.configMatsim;

/**
 * Created by carlloga on 9/14/2016. copyed from siloMatsim package in github silo
 */

import com.pb.common.matrix.Matrix;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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
    private Matrix shortDistByDistance;
    private Matrix shortTimeByDistance;
    private Matrix shortDistByTime;
    private Matrix shortTimeByTime;


    public DistListener(Controler controler, Network network,
                        int finalIteration, /*Map<Integer, SimpleFeature> zoneFeatureMap*/
                                           ArrayList<Location> locationList,
                        int timeOfDay,
                        int numberOfCalcPoints,
                        float upperDistanceThreshold) {

        shortDistByDistance = new Matrix(locationList.size(), locationList.size());
        shortTimeByDistance = new Matrix(locationList.size(), locationList.size());
        shortDistByTime = new Matrix(locationList.size(), locationList.size());
        shortTimeByTime = new Matrix(locationList.size(), locationList.size());

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
            TravelTime travelDistance;
            TravelDisutility travelTimeDisutility;
            TravelDisutility travelDistanceDisutility;

            travelDistance = new TravelDistanceAsTime();
            travelDistanceDisutility = new DistanceAsDisutility();

            travelTime = controler.getLinkTravelTimes();
            travelTimeDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);

            DijkstraTree dijkstraDistance = new DijkstraTree(network, travelDistanceDisutility, travelDistance);
            DijkstraTree dijkstraTime = new DijkstraTree(network, travelTimeDisutility, travelTime);

            LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelTimeDisutility);

            //Map to assign a node to each zone
            Map<Integer, Node> zoneCalculationNodesMap = new HashMap<>();


            long startTime = System.currentTimeMillis();

            Person person = getPerson();
            Vehicle vehicle = getVehicle();

            for (Location originZone : locationList) { // going over all origin zones


                Coord originCoord = new Coord(originZone.getX(), originZone.getY());
                Link originLink = NetworkUtils.getNearestLink(network, originCoord);
                Node originNode = originLink.getFromNode();

                Node node = NetworkUtils.getNearestNode(network, new Coord(10,10));
                //zoneCalculationNodesMap.put(loc.getId(), originNode);


                //leastCoaptPathTree.calculate(network, originNode, departureTime);

                //Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();

                dijkstraDistance.calcLeastCostPathTree(originNode, departureTime);
                dijkstraTime.calcLeastCostPathTree(originNode, departureTime);

                //BACKUP METHOD TO GET TRAVEL TIMES - TEST
                //leastCoastPathTree.calculate(network, originNode, departureTime);
                //Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();

                //for (Location destinationZone : locationList) {
                locationList.parallelStream().forEach((Location destinationZone) -> {
                    //nex line to fill only half matrix and use half time
                    if (originZone.getId() <= destinationZone.getId()) {

                        Coord destCoord = new Coord(destinationZone.getX(), destinationZone.getY());
                        Link destLink = NetworkUtils.getNearestLink(network, destCoord);
                        Node destinationNode = destLink.getFromNode();

                        //with the next if tense it is possible to limit the distance calculation to certain threshold, over it --> eucl.dist.
                        float euclideanDistance = euclideanDistanceCalculator.getDistanceFrom(originZone, destinationZone);
                        if (euclideanDistance < upperDistanceThreshold) {
                            //Dijkstra dijkstra = new Dijkstra(network, travelDisutility, travelTime);
                            LeastCostPathCalculator.Path pathInDistance = dijkstraDistance.getLeastCostPath(destinationNode);
                            LeastCostPathCalculator.Path pathInTime = dijkstraTime.getLeastCostPath(destinationNode);

                            float distanceInDistance = 0;
                            float timeInDistance = 0;
                            for (Link link : pathInDistance.links) {
                                distanceInDistance += link.getLength();
                                timeInDistance += controler.getLinkTravelTimes().getLinkTravelTime(link, departureTime, person, vehicle);

                            }

                            shortDistByDistance.setValueAt(originZone.getId(), destinationZone.getId(), distanceInDistance);
                            shortDistByDistance.setValueAt(destinationZone.getId(), originZone.getId(), distanceInDistance);

                            shortTimeByDistance.setValueAt(originZone.getId(), destinationZone.getId(), timeInDistance);
                            shortTimeByDistance.setValueAt(destinationZone.getId(), originZone.getId(), timeInDistance);

                            float distanceInTime = 0;
                            float timeInTime = 0;

                            for (Link link : pathInTime.links) {
                                distanceInTime += link.getLength();
                                timeInTime += controler.getLinkTravelTimes().getLinkTravelTime(link, departureTime, person, vehicle);

                            }


                            //double arrivalTime = tree.get(destinationNode.getId()).getTime();
                            //double congestedTimeMetohd2 = arrivalTime - departureTime;

                            shortDistByTime.setValueAt(originZone.getId(), destinationZone.getId(), distanceInTime);
                            shortDistByTime.setValueAt(destinationZone.getId(), originZone.getId(), distanceInTime);

                            shortTimeByTime.setValueAt(originZone.getId(), destinationZone.getId(), timeInTime);
                            shortTimeByTime.setValueAt(destinationZone.getId(), originZone.getId(), timeInTime);


                        } else {
                            //do nothing now
                        }
                    }
                //}
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


    static class DistanceAsDisutility implements TravelDisutility {


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


    static class TravelDistanceAsTime implements TravelTime {

        @Override
        public double getLinkTravelTime(Link link, double v, Person person, Vehicle vehicle) {
            return link.getLength();
        }
    }

    public Matrix getShortDistByDistance() {
        return shortDistByDistance;
    }

    public Matrix getShortTimeByDistance() {
        return shortTimeByDistance;
    }

    public Matrix getShortDistByTime() {
        return shortDistByTime;
    }

    public Matrix getShortTimeByTime() {
        return shortTimeByTime;
    }

    public static Person getPerson(){
        return new Person() {
            @Override
            public Map<String, Object> getCustomAttributes() {
                return null;
            }

            @Override
            public List<? extends Plan> getPlans() {
                return null;
            }

            @Override
            public boolean addPlan(Plan plan) {
                return false;
            }

            @Override
            public boolean removePlan(Plan plan) {
                return false;
            }

            @Override
            public Plan getSelectedPlan() {
                return null;
            }

            @Override
            public void setSelectedPlan(Plan plan) {

            }

            @Override
            public Plan createCopyOfSelectedPlanAndMakeSelected() {
                return null;
            }

            @Override
            public Id<Person> getId() {
                return null;
            }

            @Override
            public Attributes getAttributes() {
                return null;
            }
        };
    }

    public static Vehicle getVehicle(){
        return new Vehicle() {
            @Override
            public VehicleType getType() {
                return null;
            }

            @Override
            public Id<Vehicle> getId() {
                return null;
            }
        };
    }

}