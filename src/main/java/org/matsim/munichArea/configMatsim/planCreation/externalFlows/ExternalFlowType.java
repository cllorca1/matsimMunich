package org.matsim.munichArea.configMatsim.planCreation.externalFlows;

import org.matsim.api.core.v01.TransportMode;


public enum ExternalFlowType {

    Pkw, Pkw_PWV, SZM, GV_andere;


    public static String getMatsimMode(ExternalFlowType type){
        if (type.equals(Pkw) || type.equals(Pkw_PWV)){
            return TransportMode.car;
        } else {
            return TransportMode.car;
        }

    }

}
