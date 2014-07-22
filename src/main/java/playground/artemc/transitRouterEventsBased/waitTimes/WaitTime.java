/* *********************************************************************** *
 * project: org.matsim.*
 * WaitTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package playground.artemc.transitRouterEventsBased.waitTimes;

import org.matsim.api.core.v01.Id;

/**
 * Gives an average of the wait time of people for a line, route, stop and in a time of the day 
 * 
 * @author sergioo
 */

public interface WaitTime {

	//Methods
	public double getRouteStopWaitTime(Id line, Id route, Id stopId, double time);

}
