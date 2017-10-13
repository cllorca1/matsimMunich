package org.matsim.munichArea.outputCreation.transitSkim;

import com.pb.common.matrix.Matrix;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.munichArea.configMatsim.createDemandPt.PtSyntheticTraveller;

import java.util.Map;

/**
 * Created by carlloga on 3/2/17.
 */
public class TransitSkimCreator {


    public void runPtEventAnalyzer(String eventsFile, Map<Id, PtSyntheticTraveller> ptSyntheticTravellerMap, Network network){
        EventsManager eventsManager = EventsUtils.createEventsManager();
        TransitEventHandler transitEventHandler = new TransitEventHandler(ptSyntheticTravellerMap, network);
        eventsManager.addHandler(transitEventHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
    }

    public Matrix ptTotalTime(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix transitTravelTime) {

        //transitTravelTime.fill(-1F);

        System.out.println("Number of PT synthetic trips: " + ptSyntheticTravellerMap.size());
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){
            float tt = (float) (( ptst.getArrivalTime() - ptst.getDepartureTime())/60);

            if (tt > 300) tt=-1F;

            //System.out.println(ptst.getOrigLoc().getId() + "-" + tt);
            transitTravelTime.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), tt);
            transitTravelTime.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), tt);
        }

        return transitTravelTime;
    }

    public Matrix ptInTransitTime(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix transitTravelTime) {

        //transitTravelTime.fill(-1F);

        System.out.println("Number of PT synthetic trips: " + ptSyntheticTravellerMap.size());
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){

            if(!ptst.getBoardingMap().isEmpty() && !ptst.getAlightingMap().isEmpty()) {

                double end = ptst.getAlightingMap().get(ptst.getAlightingMap().keySet().size() - 1);

                double start = ptst.getBoardingMap().get(0);

                float tt = (float) ((end - start)/60);

                if (tt > 300) {tt=-1F;}

                transitTravelTime.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), tt);
                transitTravelTime.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), tt);
            }

        }

        return transitTravelTime;
    }

    public Matrix ptTransfers(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix transfers) {

       // transfers.fill(-1F);

        System.out.println("Number of PT synthetic trips: " + ptSyntheticTravellerMap.size());
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){

            //System.out.println(ptst.getOrigLoc().getId() + "-" + tt);

            float numberOfTransfers = (float) ptst.getTransfers();

            transfers.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), numberOfTransfers);
            transfers.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), numberOfTransfers);

        }

        return transfers;
    }

    public Matrix inVehicleTt(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix ptInVehicleTt) {

        // transfers.fill(-1F);

        System.out.println("Number of PT synthetic trips: " + ptSyntheticTravellerMap.size());
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){

            //System.out.println(ptst.getOrigLoc().getId() + "-" + tt);

            float time = (float) ptst.getInVehicleTime()/60;

            ptInVehicleTt.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), time);
            ptInVehicleTt.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), time);

        }

        return ptInVehicleTt;
    }

    public Matrix transitAccessTt(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix transitAccessTt) {

        // transfers.fill(-1F);

        System.out.println("Number of PT synthetic trips: " + ptSyntheticTravellerMap.size());
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){

            //System.out.println(ptst.getOrigLoc().getId() + "-" + tt);
            float time;
            if (ptst.getAccessTimeByWalk() != -1 ){
                time = (float) ptst.getAccessTimeByWalk()/60;
            } else {
                time = -1;
            }



            transitAccessTt.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), time);
            transitAccessTt.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), time);

        }

        return transitAccessTt;
    }

    public Matrix transitEgressTt(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix transitEgressTt) {

        // transfers.fill(-1F);

        System.out.println("Number of PT synthetic trips: " + ptSyntheticTravellerMap.size());
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){

            //System.out.println(ptst.getOrigLoc().getId() + "-" + tt);
            float time;
            if (ptst.getEgressTimeByWalk() != -1 ){
                time = (float) ptst.getEgressTimeByWalk()/60;
            } else {
                time = -1;
            }

            transitEgressTt.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), time);
            transitEgressTt.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), time);

        }

        return transitEgressTt;
    }


    public String[][] ptRouteMatrix(Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, String [][] routeMatrix) {

        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()) {

            String route = "";
            int seq = 0;
            Map<Integer, String> stopMap = ptst.getStopMap();
            for (String leg : stopMap.values()){
                route += leg;
                seq ++;
                if (seq < stopMap.size()) {
                    route += "&";
                }
            }
            routeMatrix[ptst.getOrigLoc().getId()][ptst.getDestLoc().getId()] =  route;
            routeMatrix[ptst.getDestLoc().getId()][ptst.getOrigLoc().getId()] =  route;
        }
        return routeMatrix;
    }

    public Matrix transitDistance (Map<Id,PtSyntheticTraveller> ptSyntheticTravellerMap, Matrix transitDistance) {

        System.out.println("Transit distance matrix written");
        for (PtSyntheticTraveller ptst : ptSyntheticTravellerMap.values()){

            //System.out.println(ptst.getOrigLoc().getId() + "-" + tt);



            float distance = ptst.getDistanceInTransit();

            if (distance>0) {
                transitDistance.setValueAt(ptst.getOrigLoc().getId(), ptst.getDestLoc().getId(), distance);
                transitDistance.setValueAt(ptst.getDestLoc().getId(), ptst.getOrigLoc().getId(), distance);
            }
        }

        return transitDistance;

    }







}



