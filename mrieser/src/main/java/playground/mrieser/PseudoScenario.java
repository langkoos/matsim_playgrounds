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

package playground.mrieser;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Households;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * Provides a real scenario, but exchanges the population.
 * Still, network and facilities can be reused that way.
 *
 * @author mrieser
 */
public class PseudoScenario implements Scenario {

	private final ScenarioImpl scenario;
	private final Population myPopulation;

	public PseudoScenario(final ScenarioImpl scenario, final Population population) {
		this.scenario = scenario;
		this.myPopulation = population;
	}

	@Override
	public Population getPopulation() {
		return this.myPopulation;
	}

	@Override
	public TransitSchedule getTransitSchedule() {
		return null;
	}
	
	@Override
	public ActivityFacilities getActivityFacilities() {
		return null;
	}

	@Override
	public Coord createCoord(final double x, final double y) {
		return this.scenario.createCoord(x, y);
	}

	@Override
	public Config getConfig() {
		return this.scenario.getConfig();
	}

	@Override
	public Network getNetwork() {
		return this.scenario.getNetwork();
	}

	@Override
	public void addScenarioElement(final String name, final Object o) {
		this.scenario.addScenarioElement(name, o);
	}

	@Override
	public Object getScenarioElement(final String name) {
		return this.scenario.getScenarioElement(name);
	}

	@Override
	public Object removeScenarioElement(final String name) {
		return this.scenario.removeScenarioElement(name);
	}

	@Override
	public Vehicles getTransitVehicles() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Households getHouseholds() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Vehicles getVehicles() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
