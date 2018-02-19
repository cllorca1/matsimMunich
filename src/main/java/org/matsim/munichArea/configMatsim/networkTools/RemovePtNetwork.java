package org.matsim.munichArea.configMatsim.networkTools;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.munichArea.NetworkCleaner;

public class RemovePtNetwork {

    public Network removePublicTransport(Network network){

        new org.matsim.core.network.algorithms.NetworkCleaner().run(network);

        return network;

    }
}
