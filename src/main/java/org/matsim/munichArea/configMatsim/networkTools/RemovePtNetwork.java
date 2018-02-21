package org.matsim.munichArea.configMatsim.networkTools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.munichArea.NetworkCleaner;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;

public class RemovePtNetwork {

    public Network removePublicTransport(Network network){

        Network newNetwork = NetworkUtils.createNetwork();

        TransportModeNetworkFilter transportModeNetworkFilter = new TransportModeNetworkFilter(network);
        TreeSet<String> extractModes = new TreeSet<>();
        extractModes.add(TransportMode.car);
        transportModeNetworkFilter.filter(newNetwork, extractModes);

        //clean the network links with length 0

        NetworkFactory factory = newNetwork.getFactory();
        Iterator iterator = newNetwork.getLinks().values().iterator();
        Set<Id<Link>> linksToRemove = new HashSet();

        while (iterator.hasNext()) {
            Link link = (Link) (iterator.next());
            if (link.getLength() == 0) {
                linksToRemove.add(link.getId());
            }
        }

        Iterator iterator2 = linksToRemove.iterator();
        while (iterator2.hasNext()) {
            Id<Link> id = (Id) iterator2.next();
            newNetwork.removeLink(id);
        }

        //requires cleaning
        new org.matsim.core.network.algorithms.NetworkCleaner().run(newNetwork);



        return newNetwork;

    }
}
