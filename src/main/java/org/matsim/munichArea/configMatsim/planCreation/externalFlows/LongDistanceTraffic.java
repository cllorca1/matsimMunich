package org.matsim.munichArea.configMatsim.planCreation.externalFlows;

import com.google.common.collect.HashBasedTable;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.munichArea.Util;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;



public class LongDistanceTraffic {

    private ResourceBundle rb;

    private static Logger logger = Logger.getLogger(LongDistanceTraffic.class);
    private PopulationFactory matsimPopulationFactory;
    private Map<Integer, ExternalFlowZone> zones;
    private Map<ExternalFlowType, HashBasedTable<Integer, Integer, Float>> externalFlows;
    private String fileNameZones;
    private String shapeFileZones;
    private String idFieldName;
    private String fileNamePkw ;
    private String fileNameGV;
    private String fileNamePkwPWV;
    private String fileNameSZM;
    private Map<ExternalFlowType, String> matrixFileNames;

    //hard coded for the specific format of matrices
    private int startOrigId = 3;
    private int endOrigId = 11  ;
    private int startDestId = 14 ;
    private int endDestId = 21;
    private int startFlow = 23;

    private String departureTimeFileName;
    private int[] hours = new int[24];
    private double[] probabilities = new double[24];

    //for pre-analysis only
    private Map<Integer, Float> totalGeneratedFlows = new HashMap<>();
    private Map<Integer, Float> totalAttractedFlows = new HashMap<>();
    private float internalFlow = 0;
    private  float outboundFlow = 0;
    private float inboundFlow = 0;
    private float thruFlow = 0;


    public LongDistanceTraffic( ResourceBundle rb){

        this.rb = rb;

        zones = new HashMap<>();

        fileNameZones = rb.getString("external.zones.list");
        shapeFileZones = rb.getString("external.zones.shp");
        idFieldName = rb.getString("external.zones.shp.idname");
        fileNamePkw = rb.getString("external.matrix.pkw");
        fileNameGV = rb.getString("external.matrix.gv");
        fileNamePkwPWV = rb.getString("external.matrix.pkw_pwv");
        fileNameSZM = rb.getString("external.matrix.szm");

        departureTimeFileName = rb.getString("external.departure.time");

        externalFlows = new HashMap<>();
        matrixFileNames = new HashMap<>();
        matrixFileNames.put(ExternalFlowType.GV_andere, fileNameGV);
        matrixFileNames.put(ExternalFlowType.Pkw, fileNamePkw);
        matrixFileNames.put(ExternalFlowType.Pkw_PWV, fileNamePkwPWV);
        matrixFileNames.put(ExternalFlowType.SZM, fileNameSZM);

        readDepartureTimeDistribution(departureTimeFileName);

    }

    private void readDepartureTimeDistribution(String departureTimeFileName) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(departureTimeFileName));

            String[] header = br.readLine().split(",");

            int posHour = Util.findPositionInArray("hour", header);
            int posProbability = Util.findPositionInArray("probability", header);

            String line;
            while ((line = br.readLine()) != null){

                int hour = Integer.parseInt(line.split(",")[posHour]);
                double probability = Double.parseDouble(line.split(",")[posProbability]);

                hours[hour] = hour;
                probabilities[hour] = probability;

            }

            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readZones(){
        Map<Integer, SimpleFeature> features = new HashMap<>();
        for (SimpleFeature feature : ShapeFileReader.getAllFeatures(shapeFileZones)){
            int zoneId = Integer.parseInt(feature.getAttribute(idFieldName).toString());
            features.put(zoneId, feature);
        }
        try {

            BufferedReader br = new BufferedReader(new FileReader(fileNameZones));

            String[] header = br.readLine().split(",");
            int positionId = Util.findPositionInArray("NO", header);
            int positionName = Util.findPositionInArray("NAME", header);;
            int positionType = Util.findPositionInArray("TYPENO", header);;
            int positionX = Util.findPositionInArray("POINT_X", header);;
            int positionY = Util.findPositionInArray("POINT_Y", header);;


            String line;
            while ((line = br.readLine()) != null ) {
                int id = Integer.parseInt(line.split(",")[positionId]);
                String name = line.split(",")[positionName];
                ExternalFlowZoneType type = ExternalFlowZoneType.getExternalFlowZoneTypeFromInt(Integer.parseInt(line.split(",")[positionType]));
                Coord coordinates = new Coord(Float.parseFloat(line.split(",")[positionX]),Float.parseFloat(line.split(",")[positionY]));
                SimpleFeature feature;
                if (!type.equals(ExternalFlowZoneType.BORDER)) {
                     feature = features.get(id);
                } else {
                    feature = null;
                }
                zones.put(id, new ExternalFlowZone(id, coordinates, type, feature));
                initialize(id);
            }

            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Loaded " + zones.size() + " zones for external flows");


    }

    public void readMatrices() {


        for ( ExternalFlowType type : ExternalFlowType.values()) {
            String fileName = matrixFileNames.get(type);

            HashBasedTable<Integer, Integer, Float> matrix = HashBasedTable.create();

            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));

                String line;
                boolean hasOdPairs = false;
                while ((line = br.readLine()) != null) {
                    if (hasOdPairs) {
                        if (line.contains("Netzobjektnamen")) {
                            hasOdPairs = false;
                        } else {
                            //readOdPairs()
                            int originId = Integer.parseInt(line.substring(startOrigId, endOrigId).replaceAll(" ", ""));
                            int destId = Integer.parseInt(line.substring(startDestId, endDestId).replaceAll(" ", ""));
                            float flow = Float.parseFloat(line.substring(startFlow, line.length()).replaceAll(" ", ""));

                            validate(originId, destId);
                            matrix.put(originId, destId, flow);

                        }
                    } else if (line.contains("18.01.18")) {
                        hasOdPairs = true;
                    }


                }

                br.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info("Created a matrix of external flows for vehicle type " + type.toString());
            externalFlows.put(type, matrix);
        }

    }

    private void validate(int originId, int destId) {
        if (!zones.keySet().contains(originId) || !zones.keySet().contains(destId)){
            logger.error("Od pair could not found in the zone list");
            throw new RuntimeException();
        }


    }

    public Population addLongDistancePlans(double scalingFactor, Population matsimPopulation){

        matsimPopulationFactory = matsimPopulation.getFactory();

        long personId = 0;

        for ( ExternalFlowType type : ExternalFlowType.values()) {

            HashBasedTable<Integer, Integer, Float> matrix = externalFlows.get(type);

            for (int originId : matrix.rowKeySet() ){
                for (int destId : matrix.columnKeySet()){

                    if (matrix.contains(originId, destId)){
                        addFlow(originId, destId, matrix.get(originId, destId));
                        countTotals(originId, destId, matrix.get(originId, destId));
                        long trips = Math.round(matrix.get(originId, destId) * scalingFactor);
                        for (long trip = 0; trip < trips; trip++){
                            Plan matsimPlan = matsimPopulationFactory.createPlan();
                            Person matsimPerson = matsimPopulationFactory.createPerson(Id.createPersonId("ld" + personId));
                            matsimPerson.addPlan(matsimPlan);


                            Activity homeActivity =
                                    matsimPopulationFactory.createActivityFromCoord("home", zones.get(originId).getCoordinatesForTripGeneration());

                            homeActivity.setEndTime(selectDepartureTimeInSeconds());

                            matsimPlan.addActivity(homeActivity);

                            Activity destinationActivity =
                                    matsimPopulationFactory.createActivityFromCoord("other", zones.get(destId).getCoordinatesForTripGeneration());


                            matsimPlan.addLeg(matsimPopulationFactory.createLeg(ExternalFlowType.getMatsimMode(type)));
                            matsimPlan.addActivity(destinationActivity);

                            matsimPopulation.addPerson(matsimPerson);
                            personId++;

                        }
                    }

                }
            }

        }

        return matsimPopulation;


    }

    private double selectDepartureTimeInSeconds() {

        return (new EnumeratedIntegerDistribution(hours, probabilities).sample()  + Math.random())*3600;

    }


    public void initialize(int zone){
        //for analysis
        totalGeneratedFlows.put(zone, 0f);
        totalAttractedFlows.put(zone, 0f);
    }


    public void addFlow(int origin, int dest, float flow){
        //for analysis
        totalGeneratedFlows.put(origin, totalGeneratedFlows.get(origin) + flow);
        totalAttractedFlows.put(dest, totalAttractedFlows.get(dest) + flow);
    }

    public void printOutTripGenerationAndAttraction(){
        logger.info("zoneId,generated,attracted");
        for (int zoneId: zones.keySet()){
            logger.info(zoneId + "," + totalGeneratedFlows.get(zoneId) + "," + totalAttractedFlows.get(zoneId) );
        }
    }

    public void countTotals(int origin, int dest, float flow){
        ExternalFlowZoneType origType = zones.get(origin).getZoneType();
        ExternalFlowZoneType destType = zones.get(dest).getZoneType();

        if (origType.equals(ExternalFlowZoneType.BORDER) && destType.equals(ExternalFlowZoneType.BORDER)){
            thruFlow += flow;
        } else if ((origType.equals(ExternalFlowZoneType.BEZIRKE)|| origType.equals(ExternalFlowZoneType.NUTS3))
                && destType.equals(ExternalFlowZoneType.BORDER)){
            outboundFlow += flow;
        } else if ((destType.equals(ExternalFlowZoneType.BEZIRKE)|| destType.equals(ExternalFlowZoneType.NUTS3))
                && origType.equals(ExternalFlowZoneType.BORDER)){
            inboundFlow += flow;
        } else {
            internalFlow += flow;
        }
    }

    public void printOutTotals(){
        logger.info("thru flow: " + thruFlow);
        logger.info("outbound " + outboundFlow);
        logger.info("inbound " + inboundFlow);
        logger.info("internal " + internalFlow);
    }

}
