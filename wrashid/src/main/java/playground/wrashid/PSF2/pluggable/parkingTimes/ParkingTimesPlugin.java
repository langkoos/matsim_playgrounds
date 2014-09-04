/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingTimesPlugin.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.pluggable.parkingTimes;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;


/**
 * 
 * 
 * The first/last parking is at the last position in the list.
 * 
 * TODO: tidy up this handler -> see ParkingSimulation class as example
 * => current implementation is too complicated.
 * 
 * 
 * @author wrashid
 * 
 */

public class ParkingTimesPlugin implements Wait2LinkEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler,
		ActivityStartEventHandler {

	// agent Id, linked list of parkingInterval
	LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals;
	// agent Id, linkId
	HashMap<Id, Id> lastLinkEntered;
	// act types
	LinkedList<String> actTypesFilter = null;

	public LinkedList<String> getActTypesFilter() {
		return actTypesFilter;
	}

	public void setActTypesFilter(LinkedList<String> actTypesFilter) {
		this.actTypesFilter = actTypesFilter;
	}

	public LinkedListValueHashMap<Id, ParkingIntervalInfo> getParkingTimeIntervals() {
		return parkingTimeIntervals;
	}

	public ParkingTimesPlugin(Controler controler) {
		controler.addControlerListener(new AfterMobSimParkingPluginCleaner(this));
	}

	/**
	 * When using this constructor, then have to call method
	 * "closeLastAndFirstParkingInterval" after the simulation ends by yourself.
	 */
	public ParkingTimesPlugin() {
		reset(0);
	}

	public void closeLastAndFirstParkingIntervals() {
		for (Id personId : parkingTimeIntervals.getKeySet()) {
			if (parkingTimeIntervals.get(personId).size()==0){
				return;
			}
			
			ParkingIntervalInfo firstParkingInterval = parkingTimeIntervals.get(personId).getFirst();
			ParkingIntervalInfo lastParkingInterval = parkingTimeIntervals.get(personId).getLast();

			if (!isLastParkingOfDayFilteredOut(lastParkingInterval)) {
				lastParkingInterval.setDepartureTime(firstParkingInterval.getDepartureTime());
	//			checkFirstLastLinkConsistency(firstParkingInterval.getLinkId(), lastParkingInterval.getLinkId());
			}

			parkingTimeIntervals.get(personId).removeFirst();
		}
	}

	private boolean isLastParkingOfDayFilteredOut(ParkingIntervalInfo lastParkingInterval) {
		return lastParkingInterval.getDepartureTime() > 0.0;
	}

	private void checkFirstLastLinkConsistency(Id firstParkingIntervalLinkId, Id lastParkingIntervalLinkId) {
		if (!firstParkingIntervalLinkId.equals(lastParkingIntervalLinkId)) {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	public void reset(int iteration) {
		parkingTimeIntervals = new LinkedListValueHashMap<Id, ParkingIntervalInfo>();
		lastLinkEntered = new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (isValidArrivalEventWithCar(event.getPersonId(), event.getLinkId())) {
			ParkingIntervalInfo parkingIntervalInfo = new ParkingIntervalInfo();
			parkingIntervalInfo.setArrivalTime(event.getTime());
			parkingIntervalInfo.setLinkId(event.getLinkId());

			parkingTimeIntervals.put(event.getPersonId(), parkingIntervalInfo);

	//		resetLastLinkEntered(event.getPersonId());
		}
	}

	private void resetLastLinkEntered(Id personId) {
		lastLinkEntered.put(personId, null);
	}

	private boolean isValidArrivalEventWithCar(Id personId, Id linkId) {
		return lastLinkEntered.containsKey(personId) && lastLinkEntered.get(personId) != null
				&& lastLinkEntered.get(personId).equals(linkId);
	}

	private void updateDepartureTimeInfo(Wait2LinkEvent event) {

		if (parkingTimeIntervals.get(event.getPersonId()).size()==0){
			System.out.println();
		}
		
		ParkingIntervalInfo lastParkingInterval = parkingTimeIntervals.get(event.getPersonId()).getLast();

		// don't overwrite previous values!
		if (lastParkingInterval.getDepartureTime() > 0) {
			return;
		}
		lastParkingInterval.setDepartureTime(event.getTime());
	}


	private void initializeParkingTimeIntervalsForPerson(Id personId, Id linkId) {
		ParkingIntervalInfo parkingIntervalInfo = new ParkingIntervalInfo();
		parkingIntervalInfo.setLinkId(linkId);
		parkingTimeIntervals.put(personId, parkingIntervalInfo);
	}

	private boolean leavingFirstParking(Id personId) {
		return !parkingTimeIntervals.containsKey(personId);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastLinkEntered.put(event.getPersonId(), event.getLinkId());
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (leavingFirstParking(event.getPersonId())) {
			initializeParkingTimeIntervalsForPerson(event.getPersonId(), event.getLinkId());
		}

		updateDepartureTimeInfo(event);
	}

	private void setActTypeOfParkingInterval(ActivityStartEvent event) {
		
	//	if (parkingIntervalsOfCurrentAgent.size()>0 && parkingIntervalsOfCurrentAgent.getLast().getActTypeOfFirstActDuringParking() == null
		//		&& parkingIntervalsOfCurrentAgent.getLast().getDepartureTime() < 0) {
		if (isValidArrivalEventWithCar(event.getPersonId(), event.getLinkId())) {
		
			LinkedList<ParkingIntervalInfo> parkingIntervalsOfCurrentAgent = parkingTimeIntervals.get(event.getPersonId());
			
			if (actTypesFilter == null || actTypesFilter.contains(event.getActType())) {
				parkingIntervalsOfCurrentAgent.getLast().setActTypeOfFirstActDuringParking(event.getActType());
				parkingIntervalsOfCurrentAgent.getLast().setFacilityId(event.getFacilityId());
			} else {
				parkingIntervalsOfCurrentAgent.removeLast();
			}
			
			resetLastLinkEntered(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		setActTypeOfParkingInterval(event);
	}

}