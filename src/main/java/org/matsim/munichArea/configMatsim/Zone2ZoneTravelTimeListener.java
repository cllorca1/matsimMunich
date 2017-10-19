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
    private int numberOfCalcPoints;
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
        this.numberOfCalcPoints = numberOfCalcPoints;
//		this.ct = ct;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() == this.finalIteration) {

            log.info("Starting to calculate average zone-to-zone travel times based on MATSim.");
            TravelTime travelTime = controler.getLinkTravelTimes();
            TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime);
//            TravelDisutility travelTimeAsTravelDisutility = new MyTravelTimeDisutility(controler.getLinkTravelTimes());

            LeastCostPathTree leastCoastPathTree = new LeastCostPathTree(travelTime, travelDisutility);

//            Dijkstra dijkstra = new Dijkstra(network, travelTimeAsTravelDisutility, travelTime);

            autoTravelTime = new Matrix(locationList.size(), locationList.size());

            //Map to assign a node to each zone
            Map<Integer, Node> zoneCalculationNodesMap = new HashMap<>();

            //TODO re-clean the network will remove all pt links and will make possible getting auto travel times
            //NetworkCleaner networkCleaner = new NetworkCleaner();
            //networkCleaner.run(network);

            for (Location loc : locationList) {
                Coord originCoord = new Coord(loc.getX(), loc.getY());
                Link originLink = NetworkUtils.getNearestLink(network, originCoord);
                Node originNode = originLink.getFromNode();
                zoneCalculationNodesMap.put(loc.getId(), originNode);
            }

            int counter = 0;

            long startTime = System.currentTimeMillis();

            for (Location originZone : locationList) { // going over all origin zones

                Node originNode = zoneCalculationNodesMap.get(originZone.getId());
                leastCoastPathTree.calculate(network, originNode, departureTime);
                Map<Id<Node>, LeastCostPathTree.NodeData> tree = leastCoastPathTree.getTree();

                locationList.parallelStream().forEach((Location destinationZone) -> {
                    //for (Location destinationZone : locationList) { // going over all destination zones
                    //nex line to fill only half matrix and use half time
                    if (originZone.getId() <= destinationZone.getId()) {
                        Node destinationNode = zoneCalculationNodesMap.get(destinationZone.getId());

                        double arrivalTime = tree.get(destinationNode.getId()).getTime();
                        //congested car travel times in minutes
                        float congestedTravelTimeMin = (float) ((arrivalTime - departureTime) / 60.);
                        //float congestedTravelTimeMin = (float) (path.travelTime/60);
                        autoTravelTime.setValueAt(originZone.getId(), destinationZone.getId(), congestedTravelTimeMin);
                        //if only done half matrix need to add next line
                        autoTravelTime.setValueAt(destinationZone.getId(), originZone.getId(), congestedTravelTimeMin);

                       /* counter++;
                        if (counter % 100000 == 0) {
                            log.info("pairs already calculated = " + counter);
                        }*/

                    }
                    //}
                });
                log.info("Completed origin zone: " + originZone.getId());
           }


            long duration = (System.currentTimeMillis() - startTime)/1000;
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

    public Matrix getAutoTravelTime() {
        return autoTravelTime;
    }



    /**
     * @author dziemke
     */
}