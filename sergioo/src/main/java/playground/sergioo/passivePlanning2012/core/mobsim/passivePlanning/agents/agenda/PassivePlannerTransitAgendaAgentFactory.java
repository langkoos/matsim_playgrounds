/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.households.PersonHouseholdMapping;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerTransitAgent;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public final class PassivePlannerTransitAgendaAgentFactory implements AgentFactory {

	//Attributes
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	private final PersonHouseholdMapping personHouseholdMapping;
	private Set<String> modes;
	private Map<Id, Agenda> agendas;

	//Constructors
	public PassivePlannerTransitAgendaAgentFactory(final Netsim simulation, final PersonHouseholdMapping personHouseholdMapping) {
		this(simulation, null, personHouseholdMapping);
	}
	public PassivePlannerTransitAgendaAgentFactory(final Netsim simulation, final PassivePlannerManager passivePlannerManager, final PersonHouseholdMapping personHouseholdMapping) {
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		this.personHouseholdMapping = personHouseholdMapping;
		modes = new HashSet<String>();
		modes.addAll(simulation.getScenario().getConfig().plansCalcRoute().getNetworkModes());
		if(simulation.getScenario().getConfig().scenario().isUseTransit())
			modes.add("pt");
		//TODO
	}

	//Methods
	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person person) {
		Agenda agenda = new Agenda();
		agenda.addElement("home", new NormalDistributionImpl(10, 2), new NormalDistributionImpl(10*3600, 2*3600));
		for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals("home"))
				agenda.addElement(((Activity)planElement).getType(), new NormalDistributionImpl(8, 2), new NormalDistributionImpl(8*3600, 2*3600));
		agenda.addElement("shop", new NormalDistributionImpl(5, 1), new NormalDistributionImpl(2*3600, 1*3600));
		agenda.addElement("sport", new NormalDistributionImpl(2, 0.5), new NormalDistributionImpl(2*3600, 0.5*3600));
		PassivePlannerTransitAgent agent = new PassivePlannerTransitAgendaAgent((BasePerson)person, simulation, passivePlannerManager, personHouseholdMapping.getHousehold(person.getId()), modes, agenda); 
		return agent;
	}

}