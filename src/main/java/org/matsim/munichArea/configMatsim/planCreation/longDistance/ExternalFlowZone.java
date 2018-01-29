package org.matsim.munichArea.configMatsim.planCreation.longDistance;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.munichArea.Util;
import org.opengis.feature.simple.SimpleFeature;

public class ExternalFlowZone {

    private int id;
    private Coord coordinates;
    private ExternalFlowZoneType zoneType;
    private SimpleFeature feature;

    public ExternalFlowZone(int id, Coord coordinates, ExternalFlowZoneType zoneType, SimpleFeature feature) {
        this.id = id;
        this.coordinates = coordinates;
        this.zoneType = zoneType;
        this.feature = feature;
    }

    public Coord getCoordinatesForTripGeneration(){
        if (zoneType.equals(ExternalFlowZoneType.BORDER)){
            return coordinates;
        } else /*if (zoneType.equals(ExternalFlowZoneType.BEZIRKE)) {
            double radii = Math.random() * 2000;
            //math.acos(-1) gets pi?
            double angle = Math.random() * 2 * Math.acos(-1);
            return new Coord( coordinates.getX() + radii * Math.cos(angle), coordinates.getY() + radii * Math.sin(angle));
        } else */{
            return Util.getRandomCoordinateInGeometry(feature);
        }

    }

    public ExternalFlowZoneType getZoneType() {
        return zoneType;
    }
}
