package org.matsim.munichArea.outputCreation.routeAnalyzer;

import org.matsim.api.core.v01.Id;

import java.util.ArrayList;

public class Agent {

    private Id id;
    private ArrayList<Id> route;
    private double timeAtOrigin;
    private double timeAtDestination;

    public Agent(Id id, double timeAtOrigin) {
        this.id = id;
        this.timeAtOrigin = timeAtOrigin;
        route = new ArrayList<>();
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public ArrayList<Id> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<Id> route) {
        this.route = route;
    }


    public void addLinkToRoute(Id linkId) {
        this.route.add(linkId);
    }

    public double getTimeAtOrigin() {
        return timeAtOrigin;
    }

    public void setTimeAtOrigin(double timeAtOrigin) {
        this.timeAtOrigin = timeAtOrigin;
    }

    public double getTimeAtDestination() {
        return timeAtDestination;
    }

    public void setTimeAtDestination(double timeAtDestination) {
        this.timeAtDestination = timeAtDestination;
    }
}
