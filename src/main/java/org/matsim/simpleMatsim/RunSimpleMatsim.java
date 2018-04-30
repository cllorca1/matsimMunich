package org.matsim.simpleMatsim;

import java.io.IOException;

public class RunSimpleMatsim {

    public static void main(String[] args) throws IOException {

        ConfigureAndRun configureAndRun = new ConfigureAndRun();
        configureAndRun.configureAndRunMatsim();
        configureAndRun.runEventHandler();

    }

}
