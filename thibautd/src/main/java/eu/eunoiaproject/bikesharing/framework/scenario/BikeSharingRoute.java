/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingRoute.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;

/**
 * Defines a route for bike sharing legs.
 * @author thibautd
 */
public class BikeSharingRoute implements GenericRoute {
	private final Route routeDelegate;
	private Id originStation;
	private Id destinationStation;

	public BikeSharingRoute(
			final Facility originStation,
			final Facility destinationStation) {
		this( new GenericRouteImpl(
					originStation.getLinkId(),
					destinationStation.getLinkId() ) ,
				originStation.getId(),
				destinationStation.getId() );
	}

	public BikeSharingRoute(
			final Id originStation,
			final Id destinationStation) {
		this( new GenericRouteImpl( null , null ) ,
				originStation,
				destinationStation );
	}

	public BikeSharingRoute(
			final Route routeDelegate,
			final Id originStation,
			final Id destinationStation) {
		this.routeDelegate = routeDelegate;
		this.originStation = originStation;
		this.destinationStation = destinationStation;
	}

	@Override
	public BikeSharingRoute clone() {
		return new BikeSharingRoute(
				routeDelegate.clone(),
				originStation,
				destinationStation);
	}

	@Override
	@Deprecated
	public double getDistance() {
		return routeDelegate.getDistance();
	}

	@Override
	public void setDistance(double distance) {
		routeDelegate.setDistance(distance);
	}

	@Override
	public double getTravelTime() {
		return routeDelegate.getTravelTime();
	}

	@Override
	public void setTravelTime(double travelTime) {
		routeDelegate.setTravelTime(travelTime);
	}

	@Override
	public Id getStartLinkId() {
		return routeDelegate.getStartLinkId();
	}

	@Override
	public Id getEndLinkId() {
		return routeDelegate.getEndLinkId();
	}

	@Override
	public void setStartLinkId(Id linkId) {
		routeDelegate.setStartLinkId(linkId);
	}

	@Override
	public void setEndLinkId(Id linkId) {
		routeDelegate.setEndLinkId(linkId);
	}

	public Id getOriginStation() {
		return originStation;
	}

	public Id getDestinationStation() {
		return destinationStation;
	}

	// /////////////////////////////////////////////////////////////////////////
	// generic route (necessary for IO)
	@Override
	public void setRouteDescription(
			final Id startLinkId,
			final String routeDescription,
			final Id endLinkId) {
		this.routeDelegate.setStartLinkId( startLinkId );
		this.routeDelegate.setEndLinkId( endLinkId );
		final String[] stations = routeDescription.trim().split( " " );
		if ( stations.length != 2 ) throw new IllegalArgumentException( routeDescription );
		this.originStation = new IdImpl( stations[ 0 ] );
		this.destinationStation = new IdImpl( stations[ 1 ] );
	}

	@Override
	public String getRouteDescription() {
		return originStation+" "+destinationStation;
	}

	@Override
	public String getRouteType() {
		return "bikeSharing";
	}
}
