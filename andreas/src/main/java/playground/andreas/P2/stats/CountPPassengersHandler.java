package playground.andreas.P2.stats;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * Counts the number of passenger of paratransit vehicles per link
 * 
 * @author aneumann
 *
 */
public class CountPPassengersHandler implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private static final Logger log = Logger.getLogger(CountPPassengersHandler.class);
	
	private HashMap<Id, Integer> linkId2CountsTable;
	private HashMap<Id, Integer> vehId2CountsMap;

	public CountPPassengersHandler() {
		this.linkId2CountsTable = new HashMap<Id, Integer>();
		this.vehId2CountsMap =  new HashMap<Id, Integer>();
	}

	public int getCountForLinkId(Id linkId){
		Integer count = this.linkId2CountsTable.get(linkId);
		if(count == null){
			return 0;
		} else {
			return count.intValue();
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2CountsTable = new HashMap<Id, Integer>();
		for (Integer count : this.vehId2CountsMap.values()) {
			if(count != 0){
				log.warn("Should not have a count different zero " + count);
			}
		}
		this.vehId2CountsMap = new HashMap<Id, Integer>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add the number of passengers of the vehicle to the total amount of that link. ignore every non paratransit vehicle
		if(event.getVehicleId().toString().contains("p_")){
			if(this.linkId2CountsTable.get(event.getLinkId()) == null){
				this.linkId2CountsTable.put(event.getLinkId(), new Integer(0));
			}
			
			if(this.vehId2CountsMap.get(event.getVehicleId()) != null){
				int oldValue = this.linkId2CountsTable.get(event.getLinkId());
				int additionalValue = this.vehId2CountsMap.get(event.getVehicleId()).intValue();
				this.linkId2CountsTable.put(event.getLinkId(), new Integer(oldValue + additionalValue));
			}
		}		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// add a passenger to the vehicle counts data, but ignore every non paratransit vehicle and every driver
		if(event.getVehicleId().toString().contains("p_")){
			if(!event.getPersonId().toString().contains("p_")){
				if(this.vehId2CountsMap.get(event.getVehicleId()) == null){
					this.vehId2CountsMap.put(event.getVehicleId(), new Integer(0));
				}
				int oldValue = this.vehId2CountsMap.get(event.getVehicleId()).intValue();
				this.vehId2CountsMap.put(event.getVehicleId(), new Integer(oldValue + 1));
			}
		}		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// subtract a passenger to the vehicle counts data, but ignore every non paratransit vehicle and every driver
		if(event.getVehicleId().toString().contains("p_")){
			if(!event.getPersonId().toString().contains("p_")){
				this.vehId2CountsMap.put(event.getVehicleId(), this.vehId2CountsMap.get(event.getVehicleId()).intValue() - 1);
			}
		}		
	}
}