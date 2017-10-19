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
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.munichArea.outputCreation.EuclideanDistanceCalculator;
import org.matsim.munichArea.configMatsim.planCreation.Location;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author dziemke
 */
public class Zone2ZoneTravelDistanceListener implements IterationEndsListener {
    private final static Logger log = Logger.getLogger(Zone2ZoneTravelDistanceListener.class);

    private Controler controler;
    private Network network;
    private int finalIteration;
    //private Map<Integer, SimpleFeature> zoneFeatureMap;
    private ArrayList<Location> locationList;
    private int departureTime;
    private int numberOfCalcPoints;
    //	private CoordinateTransformation ct;
    private Matrix autoTravelDistance;


    public Zone2ZoneTravelDistanceListener(Controler controler, Network network,
                                           int finalIteration, /*Map<Integer, SimpleFeature> zoneFeatureMap*/
                                           ArrayList<Location> locationList,
                                           int timeOfDay,
                                           int numberOfCalcPoints) {
        this.controler = controler;
        this.network = network;
        this.finalIteration = finalIteration;
        //this.zoneFeatureMap = zoneFeatureMap;
        this.locationList = locationList;
        this.departureTime = timeOfDay;
        this.numberOfCalcPoints = numberOfCalcPoints;
//		this.ct = ct;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() == this.finalIteration) {

            EuclideanDistanceCalculator euclideanDistanceCalculator = new EuclideanDistanceCalculator();

            log.info("Starting to calculate average zone-to-zone travel distances based on MATSim.");
            TravelTime travelTime = controler.getLinkTravelTimes();
            TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
            //TravelDisutility travelTimeAsTravelDisutility = new MyTravelTimeDisutility(controler.getLinkTravelTimes());

            //LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);


            autoTravelDistance = new Matrix(locationList.size(), locationList.size());

            //Map to assign a node to each zone
            Map<Integer, Node> zoneCalculationNodesMap = new HashMap<>();

            //TODO re-clean the network will remove all pt links and will make possible getting auto travel times
            //NetworkCleaner networkCleaner = new NetworkCleaner();
            //networkCleaner.run(network);


            ;

            for (Location loc : locationList) {
                Coord originCoord = new Coord(loc.getX(), loc.getY());
                Link originLink = NetworkUtils.getNearestLink(network, originCoord);
                Node originNode = originLink.getFromNode();
                zoneCalculationNodesMap.put(loc.getId(), originNode);
            }

            //int counter = 0;

            long startTime = System.currentTimeMillis();
            for (Location originZone : locationList) { // going over all origin zones
                Node originNode = zoneCalculationNodesMap.get(originZone.getId());
                //leastCoaptPathTree.calculate(network, originNode, departureTime);

                //Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();
                DijkstraTree dijkstra = new DijkstraTree(network, travelDisutility, travelTime);
                dijkstra.calcLeastCostPathTree(originNode, departureTime);

                locationList.parallelStream().forEach((Location destinationZone) -> {

                    //for (Location destinationZone : locationList) { // going over all destination zones
                    //nex line to fill only half matrix and use half time
                    if (originZone.getId() <= destinationZone.getId()) {
                        //alternative 1
                        //Node destinationNode = zoneCalculationNodesMap.get(destinationZone.getId());
//                      double arrivalTime = leastCoastPathTree.getTree().get(zoneCalculationNodesMap.get(destinationZone.getId()).getId()).getTime();
                        Node destinationNode = zoneCalculationNodesMap.get(destinationZone.getId());
                        //with the next if tense it is possible to limit the distance calculation to certain threshold, over it --> eucl.dist.
                        float euclideanDistance = euclideanDistanceCalculator.getDistanceFrom(originZone, destinationZone);
                        if (euclideanDistance < 5e3) {


                            //Dijkstra dijkstra = new Dijkstra(network, travelDisutility, travelTime);
                            LeastCostPathCalculator.Path path = dijkstra.getLeastCostPath(destinationNode);
                            float distance = 0;
                            for (Link link : path.links) {
                                distance += link.getLength();
                            }
                            ;
                            //double arrivalTime = tree.get(destinationNode.getId()).getTime();
                            //congested car travel times in minutes
                            //float congestedTravelTimeMin = (float) ((arrivalTime - departureTime) / 60.);

                            autoTravelDistance.setValueAt(originZone.getId(), destinationZone.getId(), distance);
                            //if only done half matrix need to add next line
                            autoTravelDistance.setValueAt(destinationZone.getId(), originZone.getId(), distance);

                       /* counter++;
                        if (counter % 100000 == 0) {
                            log.info("pairs already calculated = " + counter);
                        }*/

                        } else {
                            autoTravelDistance.setValueAt(originZone.getId(), destinationZone.getId(), euclideanDistance);
                            //if only done half matrix need to add next line
                            autoTravelDistance.setValueAt(destinationZone.getId(), originZone.getId(), euclideanDistance);
                        }
                    }

                    //    }
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

    public Matrix getAutoTravelDistance() {
        return autoTravelDistance;
    }

/**
 * @author dziemke
 */
}