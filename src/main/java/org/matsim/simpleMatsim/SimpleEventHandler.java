package org.matsim.simpleMatsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private BufferedWriter bw;
    //Atomic because parallel event handling?
    private double enterTime = 0;
    private double leaveTime = 0;
    private AtomicInteger counter = new AtomicInteger(0);
    private static DecimalFormat df2 = new DecimalFormat(".##");

    public void initialize(String fileName) throws IOException {
        bw = IOUtils.getBufferedWriter(fileName);
        bw.write("id,link,action,time,vehicles,enterHeadway,exitHeadway,enterFlow,exitFlow,density");
        bw.newLine();
    }



    @Override
    public void handleEvent(LinkEnterEvent event) {


        try {

            if(event.getLinkId().equals(Id.createLinkId("analyzedLink"))) {
                bw.write(event.getVehicleId().toString());
                bw.write(",");
                bw.write(event.getLinkId().toString());
                bw.write(",");
                bw.write("enter");
                bw.write(",");
                double time = event.getTime();
                bw.write(df2.format(time));
                bw.write(",");

                bw.write(Integer.toString(counter.incrementAndGet()));
                bw.write(",");
                bw.write(df2.format(time - enterTime));
                bw.write(",");
                bw.write("NA");
                bw.write(",");
                bw.write(df2.format(3600/(time - enterTime)));
                bw.write(",");
                bw.write("NA");
                bw.write(",");
                bw.write(df2.format(counter.get()/20));
                bw.newLine();
                enterTime = time;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

        try {
            if(event.getLinkId().equals(Id.createLinkId("analyzedLink"))) {
                bw.write(event.getVehicleId().toString());
                bw.write(",");
                bw.write(event.getLinkId().toString());
                bw.write(",");
                bw.write("leave");
                bw.write(",");
                double time = event.getTime();
                bw.write(df2.format(time));
                bw.write(",");

                bw.write(Integer.toString(counter.decrementAndGet()));
                bw.write(",");
                bw.write("NA");
                bw.write(",");
                bw.write(df2.format(time - leaveTime));
                bw.write(",");
                bw.write("NA");
                bw.write(",");
                bw.write(df2.format(3600/(time - leaveTime)));
                bw.write(",");
                bw.write(df2.format(counter.get()/20));
                leaveTime = time;
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void close() throws IOException {
        bw.close();
    }


    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {

    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {

    }
}
