package org.matsim.munichArea.configMatsim.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;


public class MyPlanStrategyModule implements PlanStrategyModule, ActivityEndEventHandler {

    Scenario scenario;
    Network net;
    Population pop;

    Logger logger = Logger.getLogger(MyPlanStrategyModule.class);

    public MyPlanStrategyModule(Scenario scenario) {
        this.scenario = scenario;
        this.net = scenario.getNetwork();
        this.pop = scenario.getPopulation();
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {

    }

    @Override
    public void prepareReplanning(ReplanningContext replanningContext) {

    }

    @Override
    public void handlePlan(Plan plan) {
        boolean remove = false;
        for (PlanElement element : plan.getPlanElements()){
            if (element instanceof Activity) {
                Activity act = (Activity) element;
                if (act.getType().equals("work")) {
                    act.setEndTime(24 * 60 * 60);
                    remove = true;
                }
            } else if (remove){
                plan.getPlanElements().remove(element);
            }
            logger.warn("A person has decided to stay longer at work!!");
        }


    }

    @Override
    public void finishReplanning() {

    }
}
