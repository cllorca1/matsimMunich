package org.matsim.munichArea.configMatsim.networkTools;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.networkEditor.run.RunNetworkEditor;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.util.ResourceBundle;

public class NetworkEditor {

    private static ResourceBundle rb;

    public static void main (String[] args){

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(rb.getString("editor.old.network.file"));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        //edit the network and return it to be written again
        //example of edition
//        RoadBuilder roadBuilder = new RoadBuilder(rb);
//        roadBuilder.buildRoads(network);
//        Network newNetwork = roadBuilder.getNetwork();
//
//        //add opposing direction
//        OpposingDirectionGenerator opposingDirectionGenerator = new OpposingDirectionGenerator(rb);
//
        RemovePtNetwork removePtNetwork = new RemovePtNetwork();
        Network newNetwork = removePtNetwork.removePublicTransport(network);
//        newNetwork = opposingDirectionGenerator.addOpposingDirection(network);
//
//        if (Boolean.parseBoolean(rb.getString("add.single.link"))) {
//            newNetwork = roadBuilder.addNewLink(newNetwork);
//        }

        new NetworkWriter(newNetwork).write(rb.getString("editor.new.network.file"));

    }
}
