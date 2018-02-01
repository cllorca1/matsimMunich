package org.matsim.munichArea.configMatsim.networkTools;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.util.ResourceBundle;

public class OpposingDirectionGenerator {

    private final static Logger log = Logger.getLogger(OpposingDirectionGenerator.class);

    private ResourceBundle rb;
    private Network network;

    public OpposingDirectionGenerator(ResourceBundle rb) {
        this.rb = rb;
    }


    public Network addOpposingDirection(Network network) {

        int[] listOfLinkIds = ResourceUtil.getIntegerArray(rb,"editor.list.ids");

        for (int id : listOfLinkIds){

            Link directLink = network.getLinks().get(Id.createLinkId(id));

            NetworkUtils.createAndAddLink(network,Id.createLinkId(id + "opposing"), directLink.getToNode(), directLink.getFromNode(),
                    directLink.getLength(), directLink.getFreespeed(), directLink.getCapacity(), directLink.getNumberOfLanes());

            log.info("created a new opposing link for " + id);

          /*  double length, double freespeed, double capacity, double numLanes) */

        }

        return  network;


    }



}
