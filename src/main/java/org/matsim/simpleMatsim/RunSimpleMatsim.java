package org.matsim.simpleMatsim;

import java.io.IOException;

public class RunSimpleMatsim {

    public static void main(String[] args) throws IOException {

        //double[] lengths = new double[]{100,500,1000,5000,50000};

        //double[] capacities = new double[]{100,500,1000,2000};

        double[] lengths = new double[]{100, 1000, 10000};
        double[] capacities = new double[]{750,1000,1250};
        double[] exponents = new double[]{1.00, 0.95, 0.90, 0.85, 0.80, 0.75, 0.70, 0.65};
        double capacityToFlowFactor = 1.5;
        String mainFolderName = "withHoles";

        for (int i = 0; i < args.length; i++) {
            //scale factors
            for (double leng : lengths) {
                //link lengths
                for (double capacity : capacities) {
                    //link capacities
                    for (double exponent : exponents) {
                        int replication = 1;
                        int counter = 0;
                        while (replication < 6 && counter < 1000) {
                            //replications

                            ConfigureAndRun configureAndRun = new ConfigureAndRun(Double.parseDouble(args[i]), replication,
                                    capacity, leng, exponent, capacityToFlowFactor, mainFolderName);
                            configureAndRun.configureAndRunMatsim();
                            configureAndRun.runEventHandler();
                            replication++;
                            counter += configureAndRun.getPopulationSize();
                        }
                    }
                }
            }
        }

    }

}
