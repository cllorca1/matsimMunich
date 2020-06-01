package org.matsim.munichArea.configMatsim.replanning;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

public class MyPlanSelector implements PlanSelector<Plan, Person>, ActivityEndEventHandler {


    @Override
    public void handleEvent(ActivityEndEvent event) {

    }

    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> member) {

        for (Plan plan : member.getPlans()){
            if (plan.getPlanElements().size() == 5){
                return plan;
            }
        }

        return  member.getPlans().get(member.getPlans().size() - 1);
    }

    @Override
    public void reset(int iteration) {

    }
}
