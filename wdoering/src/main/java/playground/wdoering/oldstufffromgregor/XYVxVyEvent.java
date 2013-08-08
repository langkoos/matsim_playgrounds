/* *********************************************************************** *
 * project: org.matsim.*
 * XYZAzimuthEvent.java
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
package playground.wdoering.oldstufffromgregor;

import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Event that describes the coordinate (x,y) and the velocity (vx,vy) of an entity in the simulation
 * 
 * @author laemmel
 * 
 */
public interface XYVxVyEvent {

	public double getX();

	public double getY();

	public double getVX();

	public double getVY();

	// convenience method
	public Coordinate getCoordinate();
	
	public Id getPersonId();
	
	public double getTime();
	
}
