package org.matsim.munichArea.outputCreation.trafficVolumesByAgentGroup;

import org.matsim.api.core.v01.Id;

public enum AgentGroup {

    RESIDENT, EXTERNAL;

    public static AgentGroup getGroupFromPersonId(Id personId) {
        if (personId.toString().contains("ld")){
            return EXTERNAL;
        } else {
            return RESIDENT;
        }

    }
}
