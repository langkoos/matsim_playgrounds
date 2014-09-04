package playground.sergioo.facilitiesGenerator2012;

import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class FacilitiesLinkLinker {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader((ScenarioImpl) scenario).readFile(args[0]);
		new MatsimNetworkReader(scenario).readFile(args[1]);
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		NetworkImpl net = NetworkImpl.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		for(ActivityFacility facility:((ScenarioImpl)scenario).getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(net.getNearestLink(facility.getCoord()).getId());
		new FacilitiesWriter(((ScenarioImpl)scenario).getActivityFacilities()).write(args[2]);
	}

}