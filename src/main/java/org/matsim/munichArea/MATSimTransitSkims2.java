package org.matsim.munichArea;

import com.google.common.math.LongMath;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.munichArea.configMatsim.zonalData.Location;
import org.matsim.munichArea.outputCreation.TravelTimeMatrix;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MATSimTransitSkims2 {

    private static ResourceBundle rb;
    private static Logger logger = Logger.getLogger(MATSimTransitSkims2.class);

    public static void main(String[] args) {
        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        MATSimTransitSkims2 matSimTransitSkims2 = new MATSimTransitSkims2();
        matSimTransitSkims2.run();
    }

    public void run() {

        Config config = configureMATSim();
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Map<Integer, ModelTAZ> zoneMap = readCSVOfZones();
        int size = zoneMap.size();
        Matrix transitTotalTime = new Matrix(size, size);
        transitTotalTime.fill(-1F);
        Matrix transitInTime = new Matrix(size, size);
        transitInTime.fill(-1F);
        Matrix transitTransfers = new Matrix(size, size);
        transitTransfers.fill(-1F);
        Matrix inVehicleTime = new Matrix(size, size);
        inVehicleTime.fill(-1F);
        Matrix transitAccessTt = new Matrix(size, size);
        transitAccessTt.fill(-1F);
        Matrix transitEgressTt = new Matrix(size, size);
        transitEgressTt.fill(-1F);
        Matrix transitDistance = new Matrix(size, size);
        transitDistance.fill(-1F);

        TransitRouterConfig transitConfig = new TransitRouterConfig(config);
        TransitRouter transitRouter = new TransitRouterImpl(transitConfig, scenario.getTransitSchedule());
        ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();

        AtomicInteger counter = new AtomicInteger(0);

        long startTime_s = System.currentTimeMillis()/1000;

        zoneMap.values().parallelStream().forEach(originTAZ -> {

            Node originNode = NetworkUtils.getNearestNode(scenario.getNetwork(), originTAZ.coord);
            Id<Link> originLink = originNode.getInLinks().values().iterator().next().getId();
            ActivityFacility originFacility = activityFacilitiesFactory.createActivityFacility(null, originNode.getCoord(), originLink);

            for (ModelTAZ destinationTAZ : zoneMap.values()) {
                if (originTAZ.id <= destinationTAZ.id) {
                    Node destinationNode = NetworkUtils.getNearestNode(scenario.getNetwork(), destinationTAZ.coord);
                    Id<Link> destinationLink = destinationNode.getInLinks().values().iterator().next().getId();
                    ActivityFacility destinationFacility = activityFacilitiesFactory.createActivityFacility(null, destinationNode.getCoord(), destinationLink);

                    List<? extends PlanElement> route2 = transitRouter.calcRoute(originFacility, destinationFacility, 10 * 60 * 60, null);
                    float sumTravelTime_min = 0;

                    int sequence = 0;
                    float access_min = 0;
                    float egress_min = 0;
                    float inVehicle = 0;
                    float distance = 0;
                    int pt_legs = 0;
                    for (PlanElement pe : route2) {
                        if (pe instanceof Activity) {
                            //activities do not seem to appear in the routes as provided by transit router
                        } else if (pe instanceof Leg) {
                            double this_leg_time = (((Leg) pe).getRoute().getTravelTime() / 60.);
                            double this_leg_distance = (((Leg) pe).getRoute().getDistance());
                            sumTravelTime_min += this_leg_time;
                            if (((Leg) pe).getMode().equals("transit_walk") && sequence == 0) {
                                access_min += this_leg_time;
                            } else if (((Leg) pe).getMode().equals("transit_walk") && sequence == route2.size() - 1) {
                                egress_min += this_leg_time;
                            } else if (((Leg) pe).getMode().equals("pt")) {
                                inVehicle += this_leg_time;
                                distance += this_leg_distance;
                                pt_legs++;
                            }

                        }
                        sequence++;
                    }

                    counter.incrementAndGet();

                    transitTotalTime.setValueAt(originTAZ.id, destinationTAZ.id, sumTravelTime_min);
                    transitInTime.setValueAt(originTAZ.id, destinationTAZ.id, sumTravelTime_min - access_min - egress_min);
                    transitAccessTt.setValueAt(originTAZ.id, destinationTAZ.id, access_min);
                    transitEgressTt.setValueAt(originTAZ.id, destinationTAZ.id, egress_min);
                    transitTransfers.setValueAt(originTAZ.id, destinationTAZ.id, pt_legs - 1);
                    inVehicleTime.setValueAt(originTAZ.id, destinationTAZ.id, inVehicle);
                    transitDistance.setValueAt(originTAZ.id, destinationTAZ.id, distance);


                    transitTotalTime.setValueAt(destinationTAZ.id,originTAZ.id, sumTravelTime_min);
                    transitInTime.setValueAt(destinationTAZ.id,originTAZ.id, sumTravelTime_min - access_min - egress_min);
                    transitAccessTt.setValueAt(destinationTAZ.id,originTAZ.id, access_min);
                    transitEgressTt.setValueAt(destinationTAZ.id,originTAZ.id, egress_min);
                    transitTransfers.setValueAt(destinationTAZ.id,originTAZ.id, pt_legs - 1);
                    inVehicleTime.setValueAt(destinationTAZ.id, originTAZ.id,  inVehicle);
                    transitDistance.setValueAt(destinationTAZ.id,originTAZ.id, distance);


                    if (LongMath.isPowerOfTwo(counter.get())) {
                        long duration = System.currentTimeMillis()/1000- startTime_s;
                        logger.info(counter + " completed in " + duration + " seconds" );
                    }

                }
            }

        });

        String tag = "new";

        String omxPtFileName = rb.getString("pt.total.skim.file") + "_" + tag + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, size);
        TravelTimeMatrix.createOmxSkimMatrix(transitTotalTime, omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.in.skim.file") + "_" + tag + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, size);
        TravelTimeMatrix.createOmxSkimMatrix(transitInTime, omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.transfer.skim.file") + "_" + tag + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, size);
        TravelTimeMatrix.createOmxSkimMatrix(transitTransfers, omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.in.vehicle.skim.file") + "_" + tag + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, size);
        TravelTimeMatrix.createOmxSkimMatrix(inVehicleTime, omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.access.skim.file") + "_" + tag + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, size);
        TravelTimeMatrix.createOmxSkimMatrix(transitAccessTt, omxPtFileName, "mat1");

        omxPtFileName = rb.getString("pt.egress.skim.file") + "_" + tag + ".omx";
        TravelTimeMatrix.createOmxFile(omxPtFileName, size);
        TravelTimeMatrix.createOmxSkimMatrix(transitEgressTt, omxPtFileName, "mat1");


    }


    private Config configureMATSim() {

        Config config = ConfigUtils.createConfig();

        String networkFile = rb.getString("network.folder") + rb.getString("xml.network.file");
        String scheduleFile = rb.getString("network.folder") + rb.getString("schedule.file");
        String vehicleFile = rb.getString("network.folder") + rb.getString("vehicle.file");

        config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);

        // Network
        config.network().setInputFile(networkFile);

        //public transport
        config.transit().setUseTransit(true);
        config.transit().setTransitScheduleFile(scheduleFile);
        config.transit().setVehiclesFile(vehicleFile);
        Set<String> transitModes = new TreeSet<>();
        transitModes.add("pt");
        config.transit().setTransitModes(transitModes);

        config.controler().setOutputDirectory(rb.getString("output.folder") + "transit_tests/");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(1);

        config.controler().setWriteEventsUntilIteration(0);

        return config;
    }

    class ModelTAZ {
        Coord coord;
        boolean served;
        int id;

        public ModelTAZ(int id, boolean served, Coord coord) {
            this.coord = coord;
            this.served = served;
            this.id = id;
        }

    }

    private Map<Integer, ModelTAZ> readCSVOfZones() {

        Map<Integer, ModelTAZ> map = new HashMap<>();
        String line;
        try {
            BufferedReader bufferReader = new BufferedReader(new FileReader(rb.getString("zone.coordinates.pt.skims")));

            String headerLine = bufferReader.readLine();
            String[] header = headerLine.split("\\s*,\\s*");

            int posId = Util.findPositionInArray("id", header);
            int posX = Util.findPositionInArray("x", header);
            int posY = Util.findPositionInArray("y", header);
            int posServed = Util.findPositionInArray("served", header);

            while ((line = bufferReader.readLine()) != null) {
                String[] splitLine = line.split(",");

                int id = Integer.parseInt(splitLine[posId]);
                double x = Double.parseDouble(splitLine[posX]);
                double y = Double.parseDouble(splitLine[posY]);
                boolean served = Boolean.parseBoolean(splitLine[posServed]);

                map.put(id, new ModelTAZ(id, served, new Coord(x, y)));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Read " + map.size() + " TAZs");
        return map;

    }

}




