package org.matsim.simpleMatsim;

import java.io.IOException;

public class RunSimpleMatsim {

    public static void main(String[] args) throws IOException {

        double[] lengths = new double[]{100,500,1000,5000,50000};

        double[] capacities = new double[]{100,500,1000,2000};

        for (int replication = 1; replication < 6; replication ++ ) {
            //10 replications
            for (int i = 0; i < args.length; i++) {
                //scale factors
                for (double leng : lengths) {
                    for (double capacity : capacities) {
                        ConfigureAndRun configureAndRun = new ConfigureAndRun(Double.parseDouble(args[i]), replication, capacity, leng);
                        configureAndRun.configureAndRunMatsim();
                        configureAndRun.runEventHandler();
                    }
                }
                }
        }
    }

}
