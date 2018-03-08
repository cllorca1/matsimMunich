package org.matsim.munichArea.configMatsim.networkTools;

import com.pb.common.util.ResourceUtil;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.ResourceBundle;

public class ExtractOSMIds {

    public static ResourceBundle rb;
    public static void main (String[] args) throws FileNotFoundException {

        File propFile = new File(args[0]);
        rb = ResourceUtil.getPropertyBundle(propFile);

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("input/" + rb.getString("xml.network.file"));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        Map<Id<Link>, ? extends Link> linkMap = network.getLinks();


        PrintWriter pw = new PrintWriter("input/" + rb.getString("xml.network.file") + ".csv");
        pw.println("matsimId,osmId,fromNode,toNode,inverseMatsimId");

        for (Link link : linkMap.values()){
            String osmDirectLink = link.getAttributes().getAttribute("origid").toString();
            pw.print(link.getId().toString());
            pw.print(",");
            pw.print(osmDirectLink);
            pw.print(",");
            pw.print(link.getFromNode().getId().toString());
            pw.print(",");
            pw.print(link.getToNode().getId().toString());
            pw.print(",");


            Link opposingLink = NetworkUtils.getConnectingLink(link.getToNode(), link.getFromNode());

            try {
                String osmOpposingLink = opposingLink.getAttributes().getAttribute("origid").toString();

                if (osmDirectLink.equals(osmOpposingLink)) {
                    pw.print(opposingLink.getId().toString());
                } else {
                    pw.print(-1);
                }
            } catch (Exception e){
                pw.print(-1);
            }
            pw.println();
        }

        pw.close();

    }


}
