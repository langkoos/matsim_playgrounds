package playground.jbischoff.networkChange;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class ChangeableNetworkCreatorTest {

	private Id id1, id2;

	@Before
	public void setUp() throws Exception {
		
		
	}

	/**
	 * Creates a Network with one 600 m link fs 6 -> fs_tt 100 s and flow_cap = 1 veh/s
	 * 1 ---------- 2
	 */
	private Scenario createScenario(){
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.id1 = sc.createId("1");
		this.id2 = sc.createId("2");
		Network net = sc.getNetwork();
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n1 = nf.createNode(id1, sc.createCoord(0, 0));
		net.addNode(n1);
		Node n2 = nf.createNode(id2, sc.createCoord(500, 0));
		net.addNode(n2);
	
		Link l = nf.createLink(id1, n1, n2);
		net.addLink(l);
		l.setLength(600.0);
		l.setCapacity(3600);
		l.setFreespeed(6.0);
		return sc;
	}
	
	@Test
	public void test() {
		
		Scenario sc = createScenario();
		TravelTimeCalculatorConfigGroup ttccg = new TravelTimeCalculatorConfigGroup();
		TravelTimeCalculator calc = new TravelTimeCalculator(sc.getNetwork(), ttccg);
		LinkEnterEvent ev1 = new LinkEnterEvent(0, id1, id1, id1);
		LinkLeaveEvent ev2 = new LinkLeaveEvent(200, id1, id1, id1);
		calc.handleEvent(ev1);
		calc.handleEvent(ev2);
		double newTt=calc.getLinkTravelTimes().getLinkTravelTime(sc.getNetwork().getLinks().get(id1), 100, null, null);

		Assert.assertEquals(200, newTt,1);
		ChangeableNetworkCreator ncc = new ChangeableNetworkCreator();
		ncc.createNetworkChangeEvents(sc.getNetwork(), calc);
		Assert.assertEquals(2,(ncc.getNetworkChangeEvents().size()));
				
		NetworkChangeEvent nce0 = ncc.getNetworkChangeEvents().get(0);
		Assert.assertEquals(0, nce0.getStartTime(),1);
		Assert.assertEquals(ChangeType.ABSOLUTE, nce0.getFreespeedChange().getType());
		Assert.assertEquals(3.0,nce0.getFreespeedChange().getValue(),1);
		
		NetworkChangeEvent nce1 = ncc.getNetworkChangeEvents().get(1);
		Assert.assertEquals(900, nce1.getStartTime(),1);
		Assert.assertEquals(ChangeType.ABSOLUTE, nce1.getFreespeedChange().getType());
		Assert.assertEquals(6.0,nce1.getFreespeedChange().getValue(),1);
	}

}