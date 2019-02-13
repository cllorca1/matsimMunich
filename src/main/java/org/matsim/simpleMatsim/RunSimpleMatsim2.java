package org.matsim.simpleMatsim;

import java.io.IOException;

public class RunSimpleMatsim2 {

    public static void main(String[] args) throws IOException {

        //double[] lengths = new double[]{100,500,1000,5000,50000};

        //double[] capacities = new double[]{100,500,1000,2000};

        double[] lengths = new double[]{1000};
        double[] capacities = new double[]{750,1000,1250};
        double[] exponents = new double[]{1.00};
        double[] scaleFactors = new double[]{1.00};
        double[] flowToCapacityFactors = new double[]{0.5,1,1.5,2,2.5,3};

        for (double scaleFactor : scaleFactors) {
            //scale factors
            for (double leng : lengths) {
                //link lengths
                for (double capacity : capacities) {
                    //link capacities
                    for (double exponent : exponents) {
                        for (double flowToCapacityFactor : flowToCapacityFactors) {
                            ConfigureAndRun configureAndRun = new ConfigureAndRun(scaleFactor, 1, capacity, leng, exponent,
                                    flowToCapacityFactor, "testsUpstream10K" +
                                    "");
                            configureAndRun.configureAndRunMatsim();
                            configureAndRun.runEventHandler();
                        }
                    }
                }
            }
        }
    }
}


