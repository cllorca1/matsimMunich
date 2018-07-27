package org.matsim.munichArea.outputCreation.trafficVolumesByAgentGroup;

import org.matsim.api.core.v01.Id;

public enum AgentGroup {

    RESIDENT, REFUGEE;

    public static AgentGroup getGroupFromPersonId(Id personId) {
        if (personId.toString().contains("ref")){
            return REFUGEE;
        } else {
            return RESIDENT;
        }

    }
}
