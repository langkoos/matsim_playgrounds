package playground.dhosse.frequencyBasedPt.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class CreateTestPopulation {
	
	public static Population createTestPopulation(Network network, int nPersons){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        PopulationFactory factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		Population population = scenario.getPopulation();
		
		Coord homeCoord = new CoordImpl(-10, 150);
		Coord workCoord = new CoordImpl(610,150);
		
		for(int i=0;i<nPersons;i++){
			
			Person p = factory.createPerson(new IdImpl("p_"+i));
			Plan plan = factory.createPlan();
			
			Activity home = factory.createActivityFromCoord("h", homeCoord);
			home.setEndTime(6*3600);
			plan.addActivity(home);
			Leg leg = factory.createLeg(TransportMode.pt);
			plan.addLeg(leg);
			Activity work = factory.createActivityFromCoord("w", workCoord);
			work.setEndTime(18*3600);
			plan.addActivity(work);
			plan.addLeg(leg);
			plan.addActivity(home);
			
			p.addPlan(plan);
			
			population.addPerson(p);
			
		}
		
		return population;
		
	}

}