/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim3;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class TargetDistanceHamiltonian implements Hamiltonian {
	
	private final double detourFactor = 1.3;

	private static final Object TARGET_DISTANCE_KEY = new Object();

	public double evaluate(ProxyPerson person) {
		double errSum = 0;
		
		for (int i = 1; i < person.getPlan().getActivities().size(); i++) {
			ProxyObject leg = person.getPlan().getLegs().get(i - 1);
			Double targetDistance = (Double) leg.getUserData(TARGET_DISTANCE_KEY);
			
			if (targetDistance == null) {
				String val = leg.getAttribute(CommonKeys.LEG_DISTANCE);
				if (val != null) {
					targetDistance = new Double(val);
				}
			}

			if (targetDistance != null) {
				ProxyObject prev = person.getPlan().getActivities().get(i - 1);
				ProxyObject next = person.getPlan().getActivities().get(i);
				
				double dist = distance(prev, next);
				targetDistance = Math.max(targetDistance, 100);
				dist = dist * detourFactor;
				double delta = Math.abs(dist - targetDistance)/targetDistance;
				errSum += delta;
			}
		}
		
		return errSum;
	}
	
	private double distance(ProxyObject origin, ProxyObject destination) {
		ActivityFacility orgFac = (ActivityFacility) origin.getUserData(ActivityLocationMutator.USER_DATA_KEY);
		ActivityFacility destFac = (ActivityFacility) destination.getUserData(ActivityLocationMutator.USER_DATA_KEY);

		Coord c1 = orgFac.getCoord();
		Coord c2 = destFac.getCoord();

		double dx = c1.getX() - c2.getX();
		double dy = c1.getY() - c2.getY();
		double d = Math.sqrt(dx*dx + dy*dy); 
		
		return d;
	}

}
