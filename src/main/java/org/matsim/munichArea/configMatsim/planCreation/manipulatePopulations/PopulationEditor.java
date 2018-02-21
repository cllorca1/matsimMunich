package org.matsim.munichArea.configMatsim.planCreation.manipulatePopulations;

import com.sun.org.apache.bcel.internal.generic.POP;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.PopulationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

// this class is to convert an open data scenario population into a usable population file with the Munich Config

public class PopulationEditor {

    public static Logger logger = Logger.getLogger(PopulationEditor.class);


    public static void main(String[] args) throws IOException {

        PopulationEditor populationEditor = new PopulationEditor();
        String folder = args[0];
        String file = args[1];

        Population population = populationEditor.readPopulationFile(folder + file);

        populationEditor.summarizePopulation(population);

        Population newPopulation;
        PopulationWriter populationWriter;


        //WRITES A 1/1000
        newPopulation = populationEditor.filterCarTripsAndRescale(population, 0.01);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter = new PopulationWriter(newPopulation);
        populationWriter.write(args[2]);

//        WRITES A 1%
        newPopulation = populationEditor.filterCarTripsAndRescale(population, .1);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter = new PopulationWriter(newPopulation);
        populationWriter.write(args[3]);

        //WRITES THE 5%
        newPopulation = populationEditor.filterCarTripsAndRescale(population, 0.5);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter = new PopulationWriter(newPopulation);
        populationWriter.write(args[4]);

//        WRITES A 10%
        newPopulation = populationEditor.filterCarTripsAndRescale(population, 1);
        populationEditor.summarizePopulation(newPopulation);
        populationWriter = new PopulationWriter(newPopulation);
        populationWriter.write(args[5]);

        Population scaledUpPopulation;
//        WRITES A 20%
        scaledUpPopulation = populationEditor.filterCarTripsAndRescale(newPopulation, 2);
        populationEditor.summarizePopulation(scaledUpPopulation);
        populationWriter = new PopulationWriter(scaledUpPopulation);
        populationWriter.write(args[6]);

//        WRITES A 50%
        scaledUpPopulation = populationEditor.filterCarTripsAndRescale(newPopulation, 5);
        populationEditor.summarizePopulation(scaledUpPopulation);
        populationWriter = new PopulationWriter(scaledUpPopulation);
        populationWriter.write(args[7]);


    }

    public Population readPopulationFile(String originalPlansFile) {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(originalPlansFile);
        return scenario.getPopulation();
    }


    public void summarizePopulation(Population population) {

        logger.info("Population size: " + population.getPersons().size());

        ArrayList<String> activityTypes = new ArrayList<>();
        for (Person person : population.getPersons().values()) {
            boolean storePerson = true;
            for (Plan plan : person.getPlans()) {
                //ArrayList<Leg> legs = new ArrayList<>();
                for (PlanElement element : plan.getPlanElements()) {
                    //can be activity or leg //careful I select the first interface. What if there is more than one?
                    if (element.getClass().getInterfaces()[0].equals(Activity.class)) {
                        String type = ((Activity) element).getType();
                        if (!activityTypes.contains(type)) {
                            activityTypes.add(type);
                        }
                    }
                }
            }
        }
        int i = 0;
        for (String type : activityTypes) {
            logger.info("activity type " + i + ": " + type);
            i++;
        }
    }


    public Population filterCarTripsAndRescale(Population originalPopulation, double relativeScale) throws IOException {

        if (relativeScale <= 1) {
            return scaleDownPopulation(originalPopulation, relativeScale);
        } else {
            return scaleUpPopulation(originalPopulation, relativeScale);
        }

    }

    public Population scaleDownPopulation(Population originalPopulation, double relativeScale) throws IOException {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population newPopulation = scenario.getPopulation();
        PopulationFactory populationFactory = newPopulation.getFactory();

        PrintWriter pw = new PrintWriter(new FileWriter(relativeScale + "population.csv"));

        pw.println("person,sequence,type,endTime,x,y");

        int numberOfUndefinedActs = 0;
        int personCounter = 0;
        for (Person person : originalPopulation.getPersons().values()) {
            Person newPerson = populationFactory.createPerson(Id.createPersonId(personCounter));
            personCounter++;
            if (Math.random() < relativeScale) {
                boolean useCar = false;
                for (Plan plan : person.getPlans()) {
                    Plan newPlan = populationFactory.createPlan();
                    Coord firstLocation = new Coord(0, 0);
                    String firstType = "home";

                    int sequence = 0;
                    Activity previousAct = new Activity() {
                        @Override
                        public double getEndTime() {
                            return 0;
                        }

                        @Override
                        public void setEndTime(double v) {

                        }

                        @Override
                        public String getType() {
                            return null;
                        }

                        @Override
                        public void setType(String s) {

                        }

                        @Override
                        public Coord getCoord() {
                            return null;
                        }

                        @Override
                        public double getStartTime() {
                            return 0;
                        }

                        @Override
                        public void setStartTime(double v) {

                        }

                        @Override
                        public double getMaximumDuration() {
                            return 0;
                        }

                        @Override
                        public void setMaximumDuration(double v) {

                        }

                        @Override
                        public Id<Link> getLinkId() {
                            return null;
                        }

                        @Override
                        public Id<ActivityFacility> getFacilityId() {
                            return null;
                        }

                        @Override
                        public void setLinkId(Id<Link> id) {

                        }

                        @Override
                        public void setFacilityId(Id<ActivityFacility> id) {

                        }

                        @Override
                        public void setCoord(Coord coord) {

                        }

                        @Override
                        public Attributes getAttributes() {
                            return null;
                        }
                    };
                    for (PlanElement element : plan.getPlanElements()) {
                        //can be activity or leg //careful I select the first interface. What if there is more than one?
                        Activity act;
                        if (element.getClass().getInterfaces()[0].equals(Leg.class)) {
                            Leg leg = (Leg) element;
                            if (leg.getMode().equals(TransportMode.car)) {
                                useCar = true;
                            }
                            ;
                        } else {
                            act = (Activity) element;
                            String type = act.getType();
                            double endTime = act.getEndTime();
                            if (endTime > 0) {
                                if (sequence == 0) {
                                    firstLocation = act.getCoord();
                                    if (type.contains("home")) {
                                        act.setType("home");
                                    } else if (type.contains("work")) {
                                        act.setType("work");
                                    } else {
                                        act.setType("other");
                                    }
                                    firstType = act.getType();
                                    Activity act1 = populationFactory.createActivityFromCoord(act.getType(), act.getCoord());
                                    act1.setEndTime(act.getEndTime());
                                    newPlan.addActivity(act1);
                                    pw.print(newPerson.getId() + ",");
                                    pw.print(sequence + ",");
                                    pw.print(act.getType() + ",");
                                    pw.print(act.getEndTime() + ",");
                                    pw.println(act.getCoord().getX() + "," + act.getCoord().getY());
                                    sequence++;

                                    previousAct = (Activity) element;
                                } else {
                                    if (!act.getCoord().equals(previousAct.getCoord()) && (act.getEndTime() - act.getStartTime()) > 0) {
                                        newPlan.addLeg(populationFactory.createLeg(TransportMode.car));
                                        if (type.contains("home")) {
                                            act.setType("home");
                                        } else if (type.contains("work")) {
                                            act.setType("work");
                                        } else {
                                            act.setType("other");
                                        }
                                        Activity act2 = populationFactory.createActivityFromCoord(act.getType(), act.getCoord());
                                        act2.setEndTime(act.getEndTime());
                                        act2.setStartTime(act.getStartTime());
                                        newPlan.addActivity(act2);
                                        pw.print(newPerson.getId() + ",");
                                        pw.print(sequence + ",");
                                        pw.print(act.getType() + ",");
                                        pw.print(act.getEndTime() + ",");
                                        pw.println(act.getCoord().getX() + "," + act.getCoord().getY());
                                        sequence++;
                                        previousAct = (Activity) element;
                                    }
                                }
                            } else {
                                numberOfUndefinedActs++;
                            }
                        }
                    }
                    if (!previousAct.getCoord().equals(firstLocation)) {
                        newPlan.addLeg(populationFactory.createLeg(TransportMode.car));
                        newPlan.addActivity(populationFactory.createActivityFromCoord(firstType, firstLocation));
                        pw.print(newPerson.getId() + ",");
                        pw.print(sequence + ",");
                        pw.print(firstType + ",");
                        pw.print(-1 + ",");
                        pw.println(firstLocation.getX() + "," + firstLocation.getY());
                    }
                    newPerson.addPlan(newPlan);
                }
                if (useCar) {
                    newPopulation.addPerson(newPerson);
                }
            }
        }
        logger.warn("The number of activities without end time is: " + numberOfUndefinedActs);

        pw.flush();
        pw.close();

        return newPopulation;
    }

    public Population scaleUpPopulation(Population originalPopulation, double relativeScale) throws IOException {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population newPopulation = scenario.getPopulation();

        PopulationFactory populationFactory = originalPopulation.getFactory();

        long expansionFactor = Math.min(Math.round(relativeScale), 10);
        int personIndex = 1;
        ArrayList<Person> newPersons = new ArrayList<>();
        for (int i = 0; i < expansionFactor; i++) {
            for (Person person : originalPopulation.getPersons().values()) {
                Person newPerson = populationFactory.createPerson(Id.createPersonId(personIndex));
                personIndex++;
                for (Plan plan : person.getPlans()) {
                    Plan newPlan = populationFactory.createPlan();
                    for (PlanElement element : plan.getPlanElements()) {
                        if (element.getClass().getInterfaces()[0].equals(Leg.class)) {
                            newPlan.addLeg(populationFactory.createLeg(TransportMode.car));
                        } else {
                            Activity act = (Activity) element;
                            if (org.matsim.core.population.PopulationUtils.getLastActivity(plan).equals(act)){
                                if (!org.matsim.core.population.PopulationUtils.getFirstActivity(plan).equals(act)){
                                    act.setCoord(org.matsim.core.population.PopulationUtils.getFirstActivity(newPlan).getCoord());
                                } else {
                                    act.setCoord(new Coord(act.getCoord().getX() + (Math.random() - 0.5) * 5000, act.getCoord().getY() + (Math.random() - 0.5) * 5000));
                                }
                            } else {
                                act.setCoord(new Coord(act.getCoord().getX() + (Math.random() - 0.5) * 5000, act.getCoord().getY() + (Math.random() - 0.5) * 5000));
                                act.setEndTime(act.getEndTime());
                            }
                            newPlan.addActivity(act);
                        }
                    }
                    newPerson.addPlan(newPlan);
                }
                newPersons.add(newPerson);
            }
        }


        for (Person person : newPersons){
            newPopulation.addPerson(person);
        }
        return newPopulation;
    }


}
