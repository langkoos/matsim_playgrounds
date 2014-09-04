/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerTransitAgent;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerTransitAgendaAgent extends PassivePlannerTransitAgent  {

	//Constructors
	public PassivePlannerTransitAgendaAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, Set<String> modes, Agenda agenda) {
		super(basePerson, simulation, passivePlannerManager);
		boolean carAvailability = false;
		Collection<String> mainModes = simulation.getScenario().getConfig().qsim().getMainModes();
		for(PlanElement planElement:basePerson.getBasePlan().getPlanElements())
			if(planElement instanceof Leg)
				if(mainModes.contains(((Leg)planElement).getMode()))
					carAvailability = true;
		planner = new SinglePlannerAgendaAgent(simulation.getScenario(), carAvailability, modes, basePerson.getBasePlan(), this, agenda);
		planner.setPlanElementIndex(0);
	}
	
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		double time = 0;
		for(PlanElement planElement:getBasePerson().getSelectedPlan().getPlanElements()) {
			if(planElement == prevAct)
				break;
			if(planElement instanceof Activity)
				if(((Activity)planElement).getEndTime()==Time.UNDEFINED_TIME)
					time += ((Activity)planElement).getMaximumDuration();
				else
					time = ((Activity)planElement).getEndTime();
			else
				time += ((Leg)planElement).getTravelTime();
		}
		((SinglePlannerAgendaAgent)planner).shareKnownPlace(prevAct.getFacilityId(), time, prevAct.getType());
		super.endActivityAndComputeNextState(now);
	}

}