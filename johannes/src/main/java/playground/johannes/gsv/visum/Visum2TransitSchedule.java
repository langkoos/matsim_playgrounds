/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.johannes.gsv.visum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.VehicleCombination;
import org.matsim.visum.VisumNetwork.VehicleUnit;

import playground.johannes.gsv.analysis.TransitLineAttributes;

public class Visum2TransitSchedule {

	private static final Logger log = Logger.getLogger(Visum2TransitSchedule.class);

	private final VisumNetwork visum;
	private final TransitSchedule schedule;
	private final Vehicles vehicles;
	//	private final CoordinateTransformation coordinateTransformation = new Kilometer2MeterTransformation();
	private final CoordinateTransformation coordinateTransformation = new IdentityTransformation();
	private final Map<String, String> transportModes = new HashMap<String, String>();

	private Map<String, String> transportSystems;
	
	public Visum2TransitSchedule(final VisumNetwork visum, final TransitSchedule schedule, final Vehicles vehicles) {
		this.visum = visum;
		this.schedule = schedule;
		this.vehicles = vehicles;
	}

	public void registerTransportMode(final String visumTransportMode, final String transportMode) {
		this.transportModes.put(visumTransportMode, transportMode);
	}

	public void setTransportSystems(Map<String, String> map) {
		transportSystems = map;
	}
	
	public void convert() {
		TransitLineAttributes attrs = new TransitLineAttributes();
		long vehId = 0;

		TransitScheduleFactory builder = this.schedule.getFactory();

		// 1st step: convert vehicle types
		VehiclesFactory vb = this.vehicles.getFactory();
		for (VehicleCombination vehComb : this.visum.vehicleCombinations.values()) {
			VehicleType type = vb.createVehicleType(new IdImpl(vehComb.id));
			type.setDescription(vehComb.name);
			VehicleCapacity capacity = vb.createVehicleCapacity();
			VehicleUnit vu = this.visum.vehicleUnits.get(vehComb.vehUnitId);
			capacity.setSeats(Integer.valueOf(vehComb.numOfVehicles * vu.seats));
			capacity.setStandingRoom(Integer.valueOf(vehComb.numOfVehicles * (vu.passengerCapacity - vu.seats)));
			type.setCapacity(capacity);
			this.vehicles.addVehicleType(type);
		}
		// insert a dummy vehicle type if no type is specified
		Id dummyVehId = new IdImpl(0);
		VehicleType type = vb.createVehicleType(dummyVehId);
		type.setDescription("unknown");
		VehicleCapacity capacity = vb.createVehicleCapacity();
		//VehicleUnit vu = this.visum.vehicleUnits.get(vehComb.vehUnitId);
		capacity.setSeats(Integer.MAX_VALUE);
		capacity.setStandingRoom(Integer.MAX_VALUE);
		type.setCapacity(capacity);
		this.vehicles.addVehicleType(type);

		// 2nd step: convert stop points
		final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();

		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			Coord coord = this.coordinateTransformation.transform(this.visum.stops.get(this.visum.stopAreas.get(stopPoint.stopAreaId).StopId).coord);
			TransitStopFacility stop = builder.createTransitStopFacility(stopPoint.id, coord, false);
			stop.setName(stopPoint.name);
			stopFacilities.put(stopPoint.id, stop);
			this.schedule.addStopFacility(stop);
		}

		// 3rd step: convert lines
		int i = 0;
		for (VisumNetwork.TransitLine line : this.visum.lines.values()){
			i++;
			if(i % 10 == 0) {
				log.info(String.format("Converting %s of %s lines...", i, this.visum.lines.size()));
			}
			TransitLine tLine = builder.createTransitLine(line.id);
			attrs.setTransportSystem(tLine.getId().toString(), transportSystems.get(line.tCode));

			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()){
				VisumNetwork.VehicleCombination defaultVehCombination = this.visum.vehicleCombinations.get(timeProfile.vehCombNr);
				if (defaultVehCombination == null) {
					defaultVehCombination = this.visum.vehicleCombinations.get(line.vehCombNo);
				}
//				VehicleType defaultVehType = null;
				VehicleType defaultVehType = this.vehicles.getVehicleTypes().get(dummyVehId);
				if (defaultVehCombination != null) {
					defaultVehType = this.vehicles.getVehicleTypes().get(new IdImpl(defaultVehCombination.id));
				}
				// convert line routes
				if (timeProfile.lineName.equals(line.id)) {
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
					//  convert route profile
					for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
						if (tpi.lineName.equals(line.id.toString()) && tpi.lineRouteName.equals(timeProfile.lineRouteName.toString()) && tpi.timeProfileName.equals(timeProfile.index.toString()) && tpi.DCode.equals(timeProfile.DCode.toString())){
							TransitRouteStop s = builder.createTransitRouteStop(stopFacilities.get(this.visum.lineRouteItems.get(line.id.toString() +"/"+ timeProfile.lineRouteName.toString()+"/"+ tpi.lRIIndex.toString()+"/"+tpi.DCode).stopPointNo),Time.parseTime(tpi.arr),Time.parseTime(tpi.dep));
							stops.add(s);
						}
					}
					String mode = this.transportModes.get(line.tCode);
					if (mode == null) {
						log.error("Could not find TransportMode for " + line.tCode + ", more info: " + line.id);
					}
					TransitRoute tRoute = builder.createTransitRoute(new IdImpl(timeProfile.lineName.toString()+"."+timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()+"."+timeProfile.DCode.toString()),null,stops,mode);
					//  convert departures
					for (VisumNetwork.Departure d : this.visum.departures.values()){
						if (d.lineName.equals(line.id.toString()) && d.lineRouteName.equals(timeProfile.lineRouteName.toString()) && d.TRI.equals(timeProfile.index.toString()) && d.DCode.equals(timeProfile.DCode)) {
							Departure departure = builder.createDeparture(new IdImpl(d.index), Time.parseTime(d.dep));
							VehicleType vehType = defaultVehType;
							if (d.vehCombinationNo != null) {
								vehType = this.vehicles.getVehicleTypes().get(new IdImpl(d.vehCombinationNo));
								if (vehType == null) {
									vehType = defaultVehType;
								}
							}
							if (vehType == null) {
								log.error("Could not find any vehicle combination for deparutre " + d.index + " used in line " + line.id.toString() + ".");
							} else {
								Vehicle veh = vb.createVehicle(new IdImpl("tr_" + vehId++), vehType);
								this.vehicles.addVehicle( veh);
								departure.setVehicleId(veh.getId());
								tRoute.addDeparture(departure);
							}
						}
					}
					if (tRoute.getDepartures().size() > 0) {
						tLine.addRoute(tRoute);
					} else {
						log.warn("The route " + tRoute.getId() + " was not added to the line " + tLine.getId() + " because it does not contain any departures.");
					}
				}
			}
			if (tLine.getRoutes().size() > 0) {
				this.schedule.addTransitLine(tLine);
			} else {
				log.warn("The line " + tLine.getId() + " was not added to the transit schedule because it does not contain any routes.");
			}
		}
		
		TransitLineAttributes.writeToFile(attrs, "/home/johannes/gsv/matsim/studies/netz2030/data/transitLineAttributes.net");
	}
}

