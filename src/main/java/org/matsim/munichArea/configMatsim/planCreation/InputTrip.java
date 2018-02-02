package org.matsim.munichArea.configMatsim.planCreation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.munichArea.configMatsim.zonalData.Location;

public class InputTrip {

    private static int tripCounter = 0;
    private int tripId;
    private Person person;
    private double departureTime = -1;
    private double arrivalTime = -1;
    private Location origin = null;
    private Location destination = null;
    private int mode = -1;
    private Coord origCoord = new Coord(0,0);
    private Coord destCoord = new Coord(0,0);
    private String purpose = "";
    private boolean simulated = false;


    private int sequence;
    private double distance;

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }


    public InputTrip(Person person) {
        tripCounter++;
        this.tripId = tripCounter;
        this.person = person;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setDepartureTime(double departureTime) {
        this.departureTime = departureTime;
    }


    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setOrigin(Location origin) {
        this.origin = origin;
    }


    public void setDestination(Location destination) {
        this.destination = destination;
    }


    public void setMode(int mode) {
        this.mode = mode;
    }


    public void setOrigCoord(Coord origCoord) {
        this.origCoord = origCoord;
    }


    public void setDestCoord(Coord destCoord) {
        this.destCoord = destCoord;
    }


    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }



    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public  String toString(){
        String out;
        out = this.tripId + ",";
        out+= this.person.getId().toString() + ",";
        out+= this.purpose + ",";
        out+= this.sequence + ",";
        out+= this.mode + ",";
        out+= this.origin.getId() + ",";
        out+= this.destination.getId() + ",";
        out+= this.departureTime + ",";
        out+= this.arrivalTime + ",";
        out+= this.origCoord.getX() + ",";
        out+= this.origCoord.getY() + ",";
        out+= this.destCoord.getX() + ",";
        out+= this.destCoord.getY() + ",";
        out+= this.distance;
        return out;

    }

    public static String getHeader(){
        String out;
        out = "tripId" + ",";
        out+= "personId" + ",";
        out+= "purpose" + ",";
        out+= "sequence" + ",";
        out+= "mode" + ",";
        out+= "origZone" + ",";
        out+= "destZone" + ",";
        out+= "depTime" + ",";
        out+= "arrTime" + ",";
        out+= "origX" + ",";
        out+= "origY" + ",";
        out+= "destX" + ",";
        out+= "destY" + ",";
        out+= "travelDistance";
        return out;

    }

    public int getMode() {
        return this.mode;
    }
}


