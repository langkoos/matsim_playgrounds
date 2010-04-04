/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.QPersonAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.core.population.PersonImpl;

/*
 * Creates WithinDayPersonAgents instead of PersonAgents.
 * They are able to reset their cachedNextLink what is
 * necessary when doing LeaveLinkReplanning.
 */
public class WithinDayAgentFactory extends AgentFactory {

	public WithinDayAgentFactory(final QSim simulation)
	{
		super(simulation);
	}

	@Override
	public QPersonAgent createPersonAgent(final Person p)
	{
		WithinDayPersonAgent agent = new WithinDayPersonAgent((PersonImpl) p, this.simulation);
		return agent;
	}
}