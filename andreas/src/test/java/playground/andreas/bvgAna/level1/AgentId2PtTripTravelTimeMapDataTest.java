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

package playground.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.basic.v01.IdImpl;

public class AgentId2PtTripTravelTimeMapDataTest {

	@Test
	public void testAgentId2PtTripTravelTimeMapData() {
		
		
	       Id[] ida= new Id[15];
	    	Set<Id> idSet = new TreeSet<Id>();
	        for (int ii=0; ii<15; ii++){
	        	ida[ii] = new IdImpl(ii); 
	            idSet.add(ida[ii]);
	        }
	        
//	        assign Ids to routes, vehicles and agents to be used in Test
	        
	        Id linkId1 = ida[1];
	        Id linkId2 = ida[2];
	        Id linkId3 = ida[3];
	        Id agentId1 = ida[4];
	        Id facilId1 = ida[5];

        ActivityEndEvent event = new ActivityEndEvent(1.2*3600, agentId1, linkId1, facilId1, "w");	
        PersonDepartureEvent event3 = new PersonDepartureEvent(1.2*3600, agentId1, linkId2, "pt");        
        PersonArrivalEvent event4 = new PersonArrivalEvent(1.9*3600, agentId1, linkId3, "pt");
        PersonDepartureEvent event5 = new PersonDepartureEvent(2.1*3600, agentId1, linkId3, "pt");        
        PersonArrivalEvent event6 = new PersonArrivalEvent(2.5*3600, agentId1, linkId2, "pt");
		
		AgentId2PtTripTravelTimeMapData test = new AgentId2PtTripTravelTimeMapData(event);
		
		test.handle(event3);
		test.handle(event4);
		test.handle(event5);
		test.handle(event6);
				
//		test, this works
		
		System.out.println("Number of Transfers should be: 1 and are: "+test.getNumberOfTransfers());	
		System.out.println("Total travel time should be: "+(event6.getTime()-event5.getTime()+event4.getTime()-event3.getTime())+" and is: "+test.getTotalTripTravelTime()); 
				
		Assert.assertEquals(event6.getTime()-event5.getTime()+event4.getTime()-event3.getTime(), test.getTotalTripTravelTime(), 0.);
		
		Assert.assertEquals((long)1, (long)test.getNumberOfTransfers());
		

		
	}

}