/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.artemc.socialCost;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

/**
 * @author ikaddoura
 *
 */
public class TripAnalysisHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler{

	private final static Logger log = Logger.getLogger(TripAnalysisHandler.class);

	private Map<Id, Double> personId2departureTime = new HashMap<Id, Double>();
	private double totalTravelTimeAllModes = 0.;
	private double totalTravelTimeCarMode = 0.;
	private int agentStuckEvents = 0;
	private int carLegs = 0;
	private int ptLegs = 0;
	private int walkLegs = 0;
	private int transitWalkLegs = 0;
	private int busLegs=0;

	@Override
	public void reset(int iteration) {
		this.personId2departureTime.clear();
		this.totalTravelTimeAllModes = 0.;
		this.totalTravelTimeCarMode = 0.;
		this.agentStuckEvents = 0;
		this.carLegs = 0;
		this.ptLegs = 0;
		this.walkLegs = 0;
		this.transitWalkLegs = 0;
		this.busLegs = 0;

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double travelTime = event.getTime() - this.personId2departureTime.get(event.getPersonId());

		totalTravelTimeAllModes = totalTravelTimeAllModes + travelTime;

		if (event.getLegMode().toString().equals(TransportMode.car)) {

			if(!event.getPersonId().toString().startsWith("pt")){
				totalTravelTimeCarMode = totalTravelTimeCarMode + travelTime;
				this.carLegs++;
			}
			else{
				this.busLegs++;
			}

		} else if (event.getLegMode().toString().equals(TransportMode.pt)) {
			this.ptLegs++;

		} else if (event.getLegMode().toString().equals(TransportMode.walk)) {
			this.walkLegs++;

		} else if (event.getLegMode().toString().equals(TransportMode.transit_walk)) {
			this.transitWalkLegs++;

		} else {
			log.warn("Unknown mode. This analysis only allows for 'car', 'pt' and 'walk'. For the simulated public transport, e.g. 'transit_walk' this analysis has to be revised.");
		}

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2departureTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		agentStuckEvents++;
	}
	
	public double getTotalTravelTimeAllModes() {
		return totalTravelTimeAllModes;
	}

	public double getTotalTravelTimeCarMode() {
		return totalTravelTimeCarMode;
	}

	public int getAgentStuckEvents() {
		return agentStuckEvents;
	}

	public int getCarLegs() {
		return carLegs;
	}

	public int getPtLegs() {
		return ptLegs;
	}

	public int getWalkLegs() {
		return walkLegs;
	}

	public int getTransitWalkLegs() {
		return transitWalkLegs;
	}

	public int getBusLegs() {
		return busLegs;
	}
	
}
