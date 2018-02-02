package org.matsim.munichArea.configMatsim.zonalData;

/**
 * Created by carlloga on 9/12/2016.
 * Class to hold centroids/zones while there is no other information
 */
public class Location {

    private int id;
    private double x;
    private double y;
    private long population;
    private double accessibility;
    private double travelTime;
    private long employment;
    private float size;

    public Location(int id, double x, double y, long population, long employment, float size) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.population = population;
        this.employment = employment;
        this.size=size;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getPopulation() {
        return population;
    }

    public void setAccessibility(double accessibility) {
        this.accessibility = accessibility;
    }

    public double getAccessibility() {
        return accessibility;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(double travelTime) {
        this.travelTime = travelTime;
    }

    public long getEmployment() {
        return employment;
    }

    public float getSize() {
        return size;
    }
}

