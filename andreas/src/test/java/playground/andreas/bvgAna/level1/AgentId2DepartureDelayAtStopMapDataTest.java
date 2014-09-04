package playground.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.core.basic.v01.IdImpl;

public class AgentId2DepartureDelayAtStopMapDataTest {
	
		

	@Test
	public void testAgentId2DepartureDelayAtStopMapData() {
		
        Id[] ida= new Id[15];
    	Set<Id> idSet = new TreeSet<Id>();
        for (int ii=0; ii<15; ii++){
        	ida[ii] = new IdImpl(ii); 
            idSet.add(ida[ii]);
        }
        
//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id vehId1 = ida[4];
        Id persId1 = ida[0];
        Id linkId3 = ida[13];

        AgentId2DepartureDelayAtStopMapData data = new AgentId2DepartureDelayAtStopMapData(persId1);
        
//        create Events
        
        PersonDepartureEvent event3 = new PersonDepartureEvent(2.9*3600, persId1, linkId3, TransportMode.pt);
        PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(2.9*3600, persId1, vehId1);
        
        data.addAgentDepartureEvent(event3);
        data.addPersonEntersVehicleEvent(event1);
        
//        run tests for 1 event each, can be expanded later if needed
        
        Assert.assertEquals((Double)event3.getTime(), data.getAgentDepartsPTInteraction().get(0));
		Assert.assertEquals((Double)event1.getTime(), data.getAgentEntersVehicle().get(0));
		
		
		
	}



}