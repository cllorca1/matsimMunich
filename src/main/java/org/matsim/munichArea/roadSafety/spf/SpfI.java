package org.matsim.munichArea.roadSafety.spf;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;

import java.util.Map;

public interface SpfI {

    void setupFramework(Controler controler, Network network, double scaleFactor);

    void setupParameters(Map<String, Double> parameters);

    void calculateCrashes(Object networkObject, int timeOfDay);

}
