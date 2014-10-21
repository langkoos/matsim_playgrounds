/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkTravelTimeAndDisutilityVariableWW.java
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

package playground.artemc.transitRouterEventsBased;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import playground.artemc.heterogeneity.HeterogeneityConfig;
import playground.artemc.transitRouterEventsBased.TransitRouterNetworkWW.TransitRouterNetworkLink;
import playground.artemc.transitRouterEventsBased.stopStopTimes.StopStopTime;
import playground.artemc.transitRouterEventsBased.waitTimes.WaitTime;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityHeteroWS extends TransitRouterNetworkTravelTimeAndDisutility implements TravelDisutility {

	private Link previousLink;
	private double previousTime;
	private double cachedLinkTime;
	private final Map<Id, double[]> linkTravelTimes = new HashMap<Id, double[]>();
	private final Map<Id, double[]> linkWaitingTimes = new HashMap<Id, double[]>();
	private final int numSlots;
	private final double timeSlot;

	private final double marginalUtilityOfTravelTimePt;
	private final double marginalUtilityOfWaitingPt;
	private final double marginalUtilityOfDistancePt;
	private final double marginalUtilityOfLineSwitch;
	private final double marginalUtilityOfTravelTimeWalk;
	private HashMap<Id<Person>, Double> incomeFactors;

	public TransitRouterNetworkTravelTimeAndDisutilityHeteroWS(final TransitRouterConfig config, TransitRouterNetworkWW routerNetwork, WaitTime waitTime, StopStopTime stopStopTime, TravelTimeCalculatorConfigGroup tTConfigGroup, QSimConfigGroup qSimConfigGroup, PreparedTransitSchedule preparedTransitSchedule, HeterogeneityConfig heterogeneityConfig) {
		this(config, routerNetwork, waitTime, stopStopTime, tTConfigGroup, qSimConfigGroup.getStartTime(), qSimConfigGroup.getEndTime(), preparedTransitSchedule, heterogeneityConfig);
	}
	public TransitRouterNetworkTravelTimeAndDisutilityHeteroWS(final TransitRouterConfig config, TransitRouterNetworkWW routerNetwork, WaitTime waitTime, StopStopTime stopStopTime, TravelTimeCalculatorConfigGroup tTConfigGroup, double startTime, double endTime, PreparedTransitSchedule preparedTransitSchedule, 	HeterogeneityConfig heterogeneityConfig) {
		super(config, preparedTransitSchedule);

		this.marginalUtilityOfTravelTimePt = config.getMarginalUtilityOfTravelTimePt_utl_s();
		this.marginalUtilityOfWaitingPt = config.getMarginalUtilityOfWaitingPt_utl_s();
		this.marginalUtilityOfDistancePt = config.getMarginalUtilityOfTravelDistancePt_utl_m();
		this.marginalUtilityOfLineSwitch = config.getUtilityOfLineSwitch_utl();
		this.marginalUtilityOfTravelTimeWalk = config.getMarginalUtilityOfTravelTimeWalk_utl_s();

		this.incomeFactors = heterogeneityConfig.getIncomeFactors();

		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((endTime-startTime)/timeSlot);
		for(TransitRouterNetworkLink link:routerNetwork.getLinks().values())
			if(link.route!=null) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = stopStopTime.getStopStopTime(link.fromNode.stop.getStopFacility().getId(), link.toNode.stop.getStopFacility().getId(), startTime+slot*timeSlot);
				linkTravelTimes.put(link.getId(), times);
			}
			else if(link.toNode.route!=null) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = waitTime.getRouteStopWaitTime(link.toNode.line.getId(), link.toNode.route.getId(), link.fromNode.stop.getStopFacility().getId(), startTime+slot*timeSlot);
				linkWaitingTimes.put(link.getId(), times);
			}

	}

	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		previousLink = link;
		previousTime = time;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route!=null)
			//in line link
			cachedLinkTime = linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		else if(wrapped.toNode.route!=null)
			//wait link
			cachedLinkTime = linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		else if(wrapped.fromNode.route==null)
			//walking link
			cachedLinkTime = wrapped.getLength()/this.config.getBeelineWalkSpeed();
		else
			//inside link
			cachedLinkTime = 0;
		return cachedLinkTime;
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		boolean cachedTravelDisutility = false; 
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			return -(cachedTravelDisutility?cachedLinkTime:linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*marginalUtilityOfTravelTimeWalk * (1.0/this.incomeFactors.get(person.getId()))
					- link.getLength() *  marginalUtilityOfDistancePt;
		else if (wrapped.toNode.route!=null)
			// it's a wait link
			return -(cachedTravelDisutility?cachedLinkTime:linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*marginalUtilityOfWaitingPt * (1.0/this.incomeFactors.get(person.getId()))
					- marginalUtilityOfLineSwitch;
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (walk)
			return -(cachedTravelDisutility?cachedLinkTime:wrapped.getLength()/this.config.getBeelineWalkSpeed()) * marginalUtilityOfTravelTimeWalk * (1.0/this.incomeFactors.get(person.getId()));
		else
			//inside link
			return 0;
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			return - linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)]*marginalUtilityOfTravelTimePt * (1.0/this.incomeFactors.get(person.getId()))
					- link.getLength() * marginalUtilityOfDistancePt;
		else if (wrapped.toNode.route!=null)
			// it's a wait link
			return - linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)]*marginalUtilityOfWaitingPt* (1.0/this.incomeFactors.get(person.getId()))
					- marginalUtilityOfLineSwitch;
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (walk)
			return -wrapped.getLength()/this.config.getBeelineWalkSpeed() * marginalUtilityOfTravelTimeWalk * (1.0/this.incomeFactors.get(person.getId()));
		else
			//inside link
			return 0;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
	
	@Override
	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		//  getMarginalUtilityOfTravelTimeWalk INCLUDES the opportunity cost of time.  kai, dec'12
		double timeCost = - getTravelTime(person, coord, toCoord) * config.getMarginalUtilityOfTravelTimeWalk_utl_s()  * (1.0/this.incomeFactors.get(person.getId()));
		// (sign: margUtl is negative; overall it should be positive because it is a cost.)
		
		double distanceCost = - CoordUtils.calcDistance(coord,toCoord) * config.getMarginalUtilityOfTravelDistancePt_utl_m() ;
		// (sign: same as above)
		
		return timeCost + distanceCost ;
	}

}
