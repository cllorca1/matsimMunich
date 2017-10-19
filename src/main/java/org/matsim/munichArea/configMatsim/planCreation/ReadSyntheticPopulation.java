package org.matsim.munichArea.configMatsim.planCreation;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.munichArea.SkimMatrixReader;
import org.matsim.munichArea.Util;

import java.io.*;
import java.util.*;

/**
 * Created by carlloga on 16.03.2017.
 */
public class ReadSyntheticPopulation {

    private ResourceBundle rb;
    private Config matsimConfig;
    private Scenario matsimScenario;
    private Network matsimNetwork;
    private PopulationFactory matsimPopulationFactory;
    private Map<Integer, Location> locationMap = new HashMap<>();
    private Matrix autoTravelTime;
    private Matrix travelDistances;
    private Population matsimPopulation;
    private Plan matsimPlan;
    private double time;
    private org.matsim.api.core.v01.population.Person matsimPerson;
    private TableDataSet timeOfDayDistributions;
    private int[] timeClasses;
    private double[] departure2WProb;
    private double[] departure2OProb;
    private double[] wDurationProb;
    private double[] oDurationProb;
    private TableDataSet modeChoiceCoefs;
    private double oTripRatePerPerson;
    private double oTripRateDispersion;
    private TableDataSet oDistanceDistribution;
    private int[] oDistanceClasses;
    private double[] oDistanceProb;

    private int h2wTripCount;
    private int h2oTripCount;
    private int travelerCount;

    private double[] occupancyW;
    private double[] occupancyO;

    private int[] distanceClasses = new int[60];

    private int[][] frequencies = new int[60][4];

    //maps to store duration of H2W trips
//    private Map<Integer, Double> jobArrivalsMap = new HashMap<>();
//    private Map<Integer, Double> jobDeparturesMap = new HashMap<>();
//    private Map<Integer, Double> otherDeparturesMap = new HashMap<>();
//    private Map<Integer, Double> otherArrivalsMap = new HashMap<>();
//    private Map<Integer, Float> jobDistances = new HashMap<>();
//    private Map<Integer, Float> otherDistances = new HashMap<>();

    Random rnd = new Random();

    private float b_auto;
    private float b_walk;
    private float b_bicycle;
    private float b_transit;
    private float alpha_auto;
    private float alpha_walk;
    private float alpha_bicycle;
    private float alpha_transit;
    private float beta_auto;
    private float beta_walk;
    private float beta_bicycle;
    private float beta_transit;

    private int selectedMode;
    private InputTrip plannedTrip;
    private ArrayList<InputTrip> trips;
    private int sequence;


    public ReadSyntheticPopulation(ResourceBundle rb, ArrayList<Location> locationList) {
        this.rb = rb;
        SkimMatrixReader skmReader1 = new SkimMatrixReader();
        autoTravelTime = skmReader1.readSkim(rb.getString("base.skim.file"), "mat1");
        SkimMatrixReader skmReader2 = new SkimMatrixReader();
        travelDistances = skmReader2.readSkim(rb.getString("out.skim.auto.dist.base") + ".omx", "mat1");
        matsimConfig = ConfigUtils.createConfig();
        matsimScenario = ScenarioUtils.createScenario(matsimConfig);
        matsimNetwork = matsimScenario.getNetwork();
        matsimPopulation = matsimScenario.getPopulation();
        matsimPopulationFactory = matsimPopulation.getFactory();
        modeChoiceCoefs = Util.readCSVfile(rb.getString("mode.choice.coefs"));
        modeChoiceCoefs.buildStringIndex(1);
        readModeChoiceCoefficients();
        timeOfDayDistributions = Util.readCSVfile(rb.getString("time.of.day.distr"));
        timeClasses = timeOfDayDistributions.getColumnAsInt("classes");
        departure2WProb = timeOfDayDistributions.getColumnAsDouble("H2W_departure");
        wDurationProb = timeOfDayDistributions.getColumnAsDouble("W_duration");
        departure2OProb = timeOfDayDistributions.getColumnAsDouble("H2O_departure");
        oDurationProb = timeOfDayDistributions.getColumnAsDouble("O_duration");
        oDistanceDistribution = Util.readCSVfile(rb.getString("other.distance.distr"));
        oDistanceClasses = oDistanceDistribution.getColumnAsInt("distanceClass");
        oDistanceProb = oDistanceDistribution.getColumnAsDouble("H20_length");
        oTripRatePerPerson = Double.parseDouble(rb.getString("mid.other.trip.rate"));
        oTripRateDispersion = 1; //this parameter is needed to set to 0 the number of H2O trips
        occupancyW = new double[] {1,1,1,1};
        occupancyO = new double[] {1,1,1,1};
        occupancyO[0] = Double.parseDouble(rb.getString("mid.other.car.occup"));
        occupancyW[0] = Double.parseDouble(rb.getString("mid.work.car.occup"));
        h2wTripCount = 0;
        h2oTripCount = 0;
        travelerCount = 0;
        selectedMode = 0; //this variable defines the mode to create trips, in general is equal to 0 --> generate auto trips
        trips = new ArrayList<>();

        //creates a map to look up locations from their ID
        for (Location loc : locationList) {
            locationMap.put(loc.getId(), loc);
        }

        for (int i = 0; i < distanceClasses.length; i++) {
            distanceClasses[i] = 2 + i * 2;
        }
    }

    public void demandFromSyntheticPopulation(float avPenetrationRate, float scalingFactor, String plansFileName) {

        String fileName = rb.getString("syn.pop.file");
        String cvsSplitBy = ",";
        BufferedReader br = null;
        String line = "";

        try {

            br = new BufferedReader(new FileReader(fileName));

            int lines = 0;
            while ((line = br.readLine()) != null) {
                if (lines > 0 ) {

                    sequence = 0;
                    String[] row = line.split(cvsSplitBy);
                    //int origin = Integer.parseInt(row[12]); //old version
                    int origin = Integer.parseInt(row[11]); //new version
                    int destinationWork = Integer.parseInt(row[7]);
                    int age = Integer.parseInt(row[2]);
                    boolean occupation = destinationWork == 0 ? false : true;
                    //only for adults
                    if (age > 17) {
                        time = 0;
                        matsimPlan = matsimPopulationFactory.createPlan();
                        matsimPerson = createMatsimPerson(row);
                        matsimPerson.addPlan(matsimPlan);
                        boolean storePerson = false; // do not store persons that do not travel or discarded because of scaling
                        //be at home
                        Location origLoc = locationMap.get(origin);
                        Coord homeCoordinates = new Coord(origLoc.getX() + origLoc.getSize() * (Math.random() - 0.5), origLoc.getY() + origLoc.getSize() * (Math.random() - 0.5));
                        //generate H-2-W-2
                        if (occupation) {
                            float travelDistance = travelDistances.getValueAt(origin, destinationWork);
                            int mode = selectMode(travelDistance);
                            boolean automatedVehicle = chooseAv(avPenetrationRate);
                            if (travelDistance < 80000 && rnd.nextFloat() < 1 / occupancyW[mode]) {
                                plannedTrip = new InputTrip(matsimPerson);
                                trips.add(plannedTrip);
                                plannedTrip.setMode(mode);
                                time = Math.max(new EnumeratedIntegerDistribution(timeClasses, departure2WProb).sample() * 60
                                        + (rnd.nextDouble() - .5) * 60 * 60, 0);
                                boolean simulated = rnd.nextFloat()<scalingFactor? true:false;
                                plannedTrip.setOrigin(origLoc);
                                plannedTrip.setOrigCoord(homeCoordinates);
                                plannedTrip.setDepartureTime(time);
                                plannedTrip.setPurpose("h-w");
                                plannedTrip.setDistance(travelDistance);
                                plannedTrip.setSequence(sequence);
                                plannedTrip.setSimulated(simulated);
                                sequence++;
                                Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                                activity1.setEndTime(time);
                                matsimPlan.addActivity(activity1);
                                time += autoTravelTime.getValueAt(origin, destinationWork) * 60;
                                plannedTrip.setArrivalTime(time);

                                createMatsimWorkTrip(origin, destinationWork, automatedVehicle);

                                plannedTrip.setPurpose("w-h");
                                plannedTrip.setMode(mode);
                                plannedTrip.setDestination(origLoc);
                                plannedTrip.setDestCoord(homeCoordinates);
                                plannedTrip.setDistance(travelDistance);
                                plannedTrip.setSimulated(simulated);
                                time += autoTravelTime.getValueAt(destinationWork, origin) * 60;
                                plannedTrip.setArrivalTime(time);

                                if (simulated  && mode == selectedMode) {
                                    storePerson = true;
                                }
                            }
                        }

                        //generate H-2-O-2- for all adults
                        for (int trip = 0; trip < Math.round(oTripRateDispersion * rnd.nextGaussian() + oTripRatePerPerson); trip++) {

                            float travelDistance = selectDistanceOtherTrip();
                            int mode = selectMode(travelDistance);
                            if (time < 20 * 60 * 60 && travelDistance < 80000 && rnd.nextFloat() < 1 / occupancyO[mode]) {
                                plannedTrip = new InputTrip(matsimPerson);
                                trips.add(plannedTrip);
                                plannedTrip.setMode(mode);
                                boolean simulated = rnd.nextFloat()<scalingFactor? true:false;
                                int destinationOther = selectDestionationOtherTrip(origin, travelDistance);
                                plannedTrip.setSequence(sequence);
                                sequence++;
                                plannedTrip.setSimulated(simulated);
                                plannedTrip.setDistance(travelDistances.getValueAt(origin, destinationOther));
                                time = Math.max(time, new EnumeratedIntegerDistribution(timeClasses, departure2OProb).sample() * 60 +
                                        (rnd.nextDouble() - 0.5) * 60 * 60);
                                plannedTrip.setDepartureTime(time);
                                plannedTrip.setOrigin(origLoc);
                                plannedTrip.setOrigCoord(homeCoordinates);
                                Activity activity10 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                                activity10.setEndTime(time);
                                matsimPlan.addActivity(activity10);
                                time += autoTravelTime.getValueAt(origin, destinationOther) * 60;
                                plannedTrip.setArrivalTime(time);
                                plannedTrip.setPurpose("h-o");

                                createMatsimOtherTrip(destinationOther);

                                time += autoTravelTime.getValueAt(destinationOther, origin) * 60;
                                plannedTrip.setPurpose("o-h");
                                plannedTrip.setDistance(travelDistances.getValueAt(origin, destinationOther));
                                plannedTrip.setArrivalTime(time);
                                plannedTrip.setDestination(origLoc);
                                plannedTrip.setDestCoord(homeCoordinates);
                                plannedTrip.setMode(mode);
                                plannedTrip.setSimulated(simulated);
                                if (simulated  && mode == selectedMode) {
                                    storePerson = true;
                                }
                            }
                        }
                        //add the person to the matsim population
                        if (storePerson) {
                            //generate -H
                            Activity activity100 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
                            matsimPlan.addActivity(activity100);
                            travelerCount++;
                            matsimPopulation.addPerson(matsimPerson);
                        }
                    }
                }
                lines++;

            }

            System.out.println("Read " + lines + "lines from the SP csv file");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        System.out.println("Travelers = " + travelerCount);
        System.out.println("H2W trips = " + h2wTripCount);
        System.out.println("H2O trips = " + h2oTripCount);
        MatsimWriter popWriter = new PopulationWriter(matsimPopulation, matsimNetwork);
        popWriter.write(plansFileName);
    }


    private org.matsim.api.core.v01.population.Person createMatsimPerson(String[] row) {
        org.matsim.api.core.v01.population.Person matsimPerson =
                matsimPopulationFactory.createPerson(Id.create(row[0], org.matsim.api.core.v01.population.Person.class));
        return matsimPerson;
    }

    private int selectMode(float travelDistance) {
        //0: car, 1: walk, 2: bicycle: 3: transit
        int[] alternatives = new int[]{0, 1, 2, 3};
        double[] utilities = calculateUtilities(travelDistance, alternatives);
        double probability_denominator = Arrays.stream(utilities).sum();
        double[] probabilities = Arrays.stream(utilities).map(u -> u / probability_denominator).toArray();
        int chosen = new EnumeratedIntegerDistribution(alternatives, probabilities).sample();
        int i = 0;
        while (travelDistance > distanceClasses[i] * 1000 & i < distanceClasses.length - 1) {
            i++;
        }
        frequencies[i][chosen]++;
        //System.out.println(chosen);
        return chosen;
    }

    public void readModeChoiceCoefficients() {
        b_auto = modeChoiceCoefs.getStringIndexedValueAt("intercept", "car");
        b_walk = modeChoiceCoefs.getStringIndexedValueAt("intercept", "walk");
        b_bicycle = modeChoiceCoefs.getStringIndexedValueAt("intercept", "bicycle");
        b_transit = modeChoiceCoefs.getStringIndexedValueAt("intercept", "transit");
        alpha_auto = modeChoiceCoefs.getStringIndexedValueAt("alphaTD", "car");
        alpha_walk = modeChoiceCoefs.getStringIndexedValueAt("alphaTD", "walk");
        alpha_bicycle = modeChoiceCoefs.getStringIndexedValueAt("alphaTD", "bicycle");
        alpha_transit = modeChoiceCoefs.getStringIndexedValueAt("alphaTD", "transit");
        beta_auto = modeChoiceCoefs.getStringIndexedValueAt("betaTD", "car");
        beta_walk = modeChoiceCoefs.getStringIndexedValueAt("betaTD", "walk");
        beta_bicycle = modeChoiceCoefs.getStringIndexedValueAt("betaTD", "bicycle");
        beta_transit = modeChoiceCoefs.getStringIndexedValueAt("betaTD", "transit");
    }


    private double[] calculateUtilities(float travelDistance, int[] alternatives) {
        double[] utilities = new double[alternatives.length];
        //todo set upper thresholds for trips by walk and cycle!
        utilities[0] = Math.exp(b_auto + alpha_auto * Math.exp(beta_auto * travelDistance / 1000));
        utilities[1] = Math.exp(b_walk + alpha_walk * Math.exp(beta_walk * travelDistance / 1000));
        utilities[2] = Math.exp(b_bicycle + alpha_bicycle * Math.exp(beta_bicycle * travelDistance / 1000));
        utilities[3] = Math.exp(b_transit + alpha_transit * Math.exp(beta_transit * travelDistance / 1000));
        return utilities;
    }

    private boolean chooseAv(float penetrationRate) {
        boolean automated = false;
        if (Math.random() < penetrationRate) automated = true;
        return automated;

    }

    private void createMatsimOtherTrip(int destination) {

        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));
        Location destLoc = locationMap.get(destination);
        Coord destCoordinate = new Coord(destLoc.getX() + destLoc.getSize() * (Math.random() - 0.5), destLoc.getY() + destLoc.getSize() * (Math.random() - 0.5));
        plannedTrip.setDestCoord(destCoordinate);
        plannedTrip.setDestination(destLoc);

        Activity activity4 = matsimPopulationFactory.createActivityFromCoord("other", destCoordinate);
        time = Math.max(time, Math.min(time + new EnumeratedIntegerDistribution(timeClasses, oDurationProb).sample() * 60, 22 * 60 * 60 +
                (rnd.nextDouble() - 0.5) * 60 * 60));
        activity4.setEndTime(time);
        matsimPlan.addActivity(activity4);
        matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));

        plannedTrip = new InputTrip(matsimPerson);
        trips.add(plannedTrip);
        plannedTrip.setSequence(sequence);
        sequence++;
        plannedTrip.setOrigCoord(destCoordinate);
        plannedTrip.setOrigin(destLoc);
        plannedTrip.setDepartureTime(time);
        plannedTrip.setPurpose("o-h");
        h2oTripCount++;

    }

    private int selectDestionationOtherTrip(int origin, float travelDistance) {

        int[] alternatives = new int[travelDistances.getRowCount()];
        //get 4953 length empty int
        double[] probabilities = new double[alternatives.length];
        for (int i = 0; i < alternatives.length; i++) {
            float distanceDiff = Math.abs(travelDistances.getValueAt(origin, i + 1) - travelDistance);
            //System.out.println(distanceDiff);
            alternatives[i] = i + 1;
            //TODO re-calibrate this parameter to get the most appropriate distance distribution
            probabilities[i] = distanceDiff > 0 ? 1 / distanceDiff / distanceDiff : 1;
        }
        //selects a random destination of travelDistance +- 1 km
        return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();
    }

    private float selectDistanceOtherTrip() {
        //randomly select a distance at 1 km intervals according to mid distributions
        return (float) (new EnumeratedIntegerDistribution(oDistanceClasses, oDistanceProb).sample() * 1000);
    }


    private void createMatsimWorkTrip(int origin, int destinationWork, boolean automatedVehicle) {
        //Location origLoc = locationMap.get(origin);
        Location destLoc = locationMap.get(destinationWork);
        plannedTrip.setDestination(destLoc);
        if (automatedVehicle) {
            matsimPlan.addLeg(matsimPopulationFactory.createLeg("taxi"));
        } else {
            matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));
            //todo manually change of mode only for multimodal - tests
//                matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.bike));
//                matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.walk));
            //matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.pt));
        }
        Coord workCoordinates = new Coord(destLoc.getX() + destLoc.getSize() * (Math.random() - 0.5), destLoc.getY() + destLoc.getSize() * (Math.random() - 0.5));
        plannedTrip.setDestCoord(workCoordinates);
       Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
        activity2.setStartTime(time);
        //add the duration of the job to time and send person back home
        time = Math.max(time, Math.min(time + new EnumeratedIntegerDistribution(timeClasses, wDurationProb).sample() * 60, 20 * 60 * 60 +
                (rnd.nextDouble() - 0.5) * 60 * 60));
        activity2.setEndTime(time);
        matsimPlan.addActivity(activity2);
        if (automatedVehicle) {
            matsimPlan.addLeg(matsimPopulationFactory.createLeg("taxi"));
        } else {
            matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car));
            //todo manually change of mode only for multimodal - tests
//                matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.bike));
//                matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.walk));
            //matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.pt));
        }

        h2wTripCount++;
        plannedTrip = new InputTrip(matsimPerson);
        trips.add(plannedTrip);
        plannedTrip.setSequence(sequence);
        sequence++;
        plannedTrip.setDepartureTime(time);
        plannedTrip.setOrigin(destLoc);
        plannedTrip.setOrigCoord(workCoordinates);
    }


    public Population getMatsimPopulation() {
        return matsimPopulation;
    }

    public void printHistogram() {

        BufferedWriter bw = IOUtils.getBufferedWriter(rb.getString("td.hist.file"));
        try {
            bw.write("distance, car, walk, bicycle, transit");
            bw.newLine();
            for (int i = 0; i < distanceClasses.length; i++) {
                bw.write(distanceClasses[i] + "," + frequencies[i][0] + "," + frequencies[i][1] + "," + frequencies[i][2] + "," + frequencies[i][3]);
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void printSyntheticPlansList(String fileName, int mode) {
        BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
        try {
            bw.write(InputTrip.getHeader());
            bw.newLine();

            for (InputTrip plannedTrip : trips){

                if (plannedTrip.getMode() == mode) {
                    bw.write(plannedTrip.toString());
                    bw.newLine();
                }
            }

            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
