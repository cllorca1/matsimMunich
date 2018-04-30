package org.matsim.munichArea.roadSafety;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import java.util.Arrays;

//test class to analyze the vkt or major/minor flows at intersections

public class VolumeAnalysisListener implements IterationEndsListener {

    private static Logger log = Logger.getLogger(VolumeAnalysisListener.class);
    private int finalIteration;
    private Controler controler;
    private Network network;
    private double scaleFactor;

    public VolumeAnalysisListener(int finalIteration, Network network, double scaleFactor, Controler controler) {
        this.finalIteration = finalIteration;
        this.network = network;
        this.scaleFactor = scaleFactor;
        this.controler = controler;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {

        if (iterationEndsEvent.getIteration() == this.finalIteration) {

            VolumesAnalyzer analyzer = controler.getVolumes();

            double vkt = 0;

            for (Link link : network.getLinks().values()) {

                double daylyVolume = 0;
                try {
                    int[] linkVolume = analyzer.getVolumesForLink(link.getId());


                    daylyVolume = Arrays.stream(linkVolume).sum() / scaleFactor;
                } catch (NullPointerException e){

                }
                vkt += daylyVolume*link.getLength()/1000;
            }

            log.info("the total vkt travelled is " + vkt );


            double intersectionInFlows = 0;

            for (Node node : network.getNodes().values()) {

                int numberOfInLinks = node.getInLinks().size();
                int numberOfOutLinks = node.getOutLinks().size();

                if (numberOfInLinks > 1 || numberOfOutLinks > 1){
                    for (Link inLink : node.getInLinks().values()){
                        try {
                            int[] linkVolume = analyzer.getVolumesForLink(inLink.getId());
                            intersectionInFlows += Arrays.stream(linkVolume).sum() / scaleFactor;
                        } catch (NullPointerException e){

                        }
                    }
                }

            }

            log.info("the total in flow at intersections travelled is " + intersectionInFlows );

        }
    }
}
