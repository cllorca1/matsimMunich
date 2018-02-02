package org.matsim.munichArea.outputCreation;

import com.pb.common.matrix.Matrix;
import org.matsim.munichArea.configMatsim.zonalData.Location;

import java.util.ArrayList;

/**
 * Created by carlloga on 3/2/17.
 */
public class EuclideanDistanceCalculator {

    public Matrix createEuclideanDistanceMatrix(ArrayList<Location> locationList){

        Matrix euclideanDistanceMatrix = new Matrix(locationList.size(),locationList.size());

        for (Location origLoc : locationList){
            for (Location destLoc : locationList){

                euclideanDistanceMatrix.setValueAt(origLoc.getId(), destLoc.getId(),getDistanceFrom(origLoc, destLoc));
            }
        }


        return euclideanDistanceMatrix;
    }

    public float getDistanceFrom(Location origLoc, Location destLoc){
        double distance = Math.pow(origLoc.getX() - destLoc.getX(),2);
        distance += Math.pow(origLoc.getY() - destLoc.getY(),2);
        distance = Math.sqrt(distance);
        return (float) distance;
    }
}
