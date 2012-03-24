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

package playground.kai.gauteng.roadpricingscheme;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

public abstract class SanralTollFactor {
	private final static int carStartId = 0;
	private final static int carEndId = 157517;
	private final static int comStartId = 1000000;
	private final static int comEndId = 1009999;
	private final static int busStartId = 2000000;
	private final static int busEndId = 2000209;
	private final static int taxiStartId = 3000000;
	private final static int taxiEndId = 3008133;
	private final static int extStartId = 4000000;
	private final static int extEndId = 4020181;
	
	enum Type { carWithTag, carWithoutTag, commercialClassCWithTag, commercialClassBWithTag, commercialClassCWithoutTag, 
		commercialClassBWithoutTag, busWithTag, busWithoutTag, taxiWithTag, taxiWithoutTag, extWithTag, extWithoutTag } ;
	public static Type typeOf ( Id idObj ) {
		long id = Long.parseLong(idObj.toString());

		if (id < carStartId + (carEndId - carStartId)*TagPenetration.CAR) { 
			/* It is a private vehicle with a tag. */
			return Type.carWithTag ;
		} else if (id <= carEndId) {
			/* It is a private car without a tag. */
			return Type.carWithoutTag ;
		} else if (id < comStartId + fractionClassB*((comEndId - comStartId)*TagPenetration.COMMERCIAL)){
			/* It is a Class B commercial vehicle with a tag. */
			return Type.commercialClassBWithTag ;
		} else if (id < comStartId + (comEndId - comStartId)*TagPenetration.COMMERCIAL){
			/* It is a Class C commercial vehicle with a tag. */
			return Type.commercialClassCWithTag ;
		} else if (id < comEndId - (comEndId - comStartId)*(1-TagPenetration.COMMERCIAL)*fractionClassB){
			/* It is a Class B commercial vehicle without a tag. */
			return Type.commercialClassBWithoutTag ;
		} else if (id <= comEndId){
			/* It is a Class C commercial vehicle without a tag. */
			return Type.commercialClassCWithoutTag ;
		} else if (id < busStartId + (busEndId - busStartId)*TagPenetration.BUS) {
			/* It is a bus with a tag. */
			return Type.busWithTag ;
		} else if (id <= busEndId){
			/* It is a bus without a tag. */
			return Type.busWithoutTag ;
		} else if (id < taxiStartId + (taxiEndId - taxiStartId)*TagPenetration.TAXI){
			/* It is a minibus taxi with a tag. */
			return Type.taxiWithTag ;
		} else if (id <= taxiEndId) {
			/* It is a minibus taxi without a tag. */
			return Type.taxiWithoutTag ;
		} else if (id < extStartId + (extEndId - extStartId)*TagPenetration.EXTERNAL){
			/* It is an external (light) vehicle with a tag. */
			return Type.extWithTag ;
		} else {
			/* It is an external (light) vehicle without a tag.*/
			return Type.extWithoutTag ;
		}

	}
	
	/* From the counting station data I've inferred that the split between 
	 * Class B and C is about 50:50, assuming that `Short' represents Class B, 
	 * and `Medium' and `Long' combined represent Class C vehicles. */
	private final static double fractionClassB = 0.50;
	
	public static double getUtilityOfMoney(final Id vehicleId, final PlanCalcScoreConfigGroup cnScoringGroup) {
		double valueOfTime_hr = 100 ;
		switch( typeOf(vehicleId) ) {
		case carWithTag:
			break ;
		case carWithoutTag:
			break ;
		case commercialClassBWithTag:
			valueOfTime_hr = 200.;
			break ;
		case commercialClassCWithTag:
			valueOfTime_hr = 200.;
			break ;
		case commercialClassBWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case commercialClassCWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case busWithTag:
			valueOfTime_hr = 200.;
			break ;
		case busWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case taxiWithTag:
			valueOfTime_hr = 200.;
			break ;
		case taxiWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		case extWithTag:
			valueOfTime_hr = 200.;
			break ;
		case extWithoutTag:
			valueOfTime_hr = 200.;
			break ;
		}
		final double utilityOfTime_hr = - cnScoringGroup.getPerforming_utils_hr() + cnScoringGroup.getTraveling_utils_hr() ;
		// "performing" is normally positive, but needs to be counted negative
		// "traveling" is normally negative, and needs to be counted negative

		double utilityOfMoney = utilityOfTime_hr / valueOfTime_hr ;
		
		return utilityOfMoney ;
	}

	public static double getTollFactor(final Person person, final Id linkId, final double time) {
		Id vehicleId = person.getId() ;
		// yyyyyy aaarrrrgh ... (assuming vehId = personId).  kai, mar'12
		
		double timeDiscount = getTimeDiscount(time);
		double tagDiscount = 0.00;
		double ptDiscount = 0.00;
		double sizeFactor = 1.00;
		
		switch( typeOf( vehicleId) ) {
		case carWithTag:
			tagDiscount = 0.25;
			break ;
		case carWithoutTag:
			// nothing
			break ;
		case commercialClassBWithTag:
			sizeFactor = 3;
			tagDiscount = 0.25;
			break ;
		case commercialClassCWithTag:
			sizeFactor = 6;
			tagDiscount = 0.25;
			break ;
		case commercialClassBWithoutTag:
			sizeFactor = 3;
			break ;
		case commercialClassCWithoutTag:
			sizeFactor = 6;
			break ;
		case busWithTag:
			sizeFactor = 3;
			tagDiscount = 0.25;
			ptDiscount = 0.55;
			break ;
		case busWithoutTag:
			sizeFactor = 3;
			ptDiscount = 0.55;
			break ;
		case taxiWithTag:
			tagDiscount = 0.25;
			ptDiscount = 0.30;
			break ;
		case taxiWithoutTag:
			ptDiscount = 0.30;
			break ;
		case extWithTag:
			tagDiscount = 0.25;
			break ;
		case extWithoutTag:
			// nothing
			break ;
		}
		return getDiscountEligibility(linkId) ? sizeFactor*(1 - Math.min(1.0, timeDiscount + tagDiscount + ptDiscount)) : sizeFactor;
	}
	
	private static double getTimeDiscount(double time){
		/* First get the real time of day. */
		double timeOfDay = time;
		while(timeOfDay > 86400){
			timeOfDay -= 86400;
		}
		
		/* Weekday discounts */
		if(timeOfDay < 18000){ // 00:00 - 05:00
			return 0.20;
		} else if(timeOfDay < 21600){ // 05:00 - 06:00
			return 0.10;
		} else if(timeOfDay < 64800){ // 06:00 - 18:00
			return 0.00;
		} else if(timeOfDay < 82800){ // 18:00 - 23:00
			return 0.10;
		} else{ // 23:00 - 00:00
			return 0.20;
		}		
	}
	
	/**
	 * Check if the link is an existing toll booth, or a new toll gantry. Only
	 * new gantries are granted discounts. 
	 * @param linkId
	 * @return
	 */
	private static boolean getDiscountEligibility(Id linkId){
		if(NoDiscountLinks.getList().contains(linkId)){
			return false;
		} else{
			return true;
		}
	}
}
