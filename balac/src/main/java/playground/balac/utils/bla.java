package playground.balac.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class bla {

	public static void main(String[] args) throws IOException {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
final BufferedWriter outLink = IOUtils.getBufferedWriter("P:/_TEMP/sschmutz/routing_matsim/20140428_stage_car/output/details/outputStatistics_ettapen_7.txt");

		
		for(Person per: sc.getPopulation().getPersons().values()) {
			double time = 0.0;
			Plan p = per.getPlans().get(0);
			Id linkId = null;
			Id linkId2 = null;
			Activity a = null;
			for(PlanElement pe: p.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("leisure")) {
						a = (Activity) pe;
						break;
					}
				}
				else if (pe instanceof Leg) {
					
					time += ((Leg) pe).getTravelTime();
					linkId = ((Leg) pe).getRoute().getStartLinkId();
					linkId2 = ((Leg) pe).getRoute().getEndLinkId();
					
				}
				
			}
			
			outLink.write(per.getId() + " ");
			outLink.write(Double.toString(time) + " ");
			outLink.write(String.valueOf(CoordUtils.calcDistance(((Activity)p.getPlanElements().get(0)).getCoord(), sc.getNetwork().getLinks().get(new IdImpl(linkId.toString())).getCoord())) + " ");
			outLink.write(String.valueOf(CoordUtils.calcDistance(a.getCoord(), sc.getNetwork().getLinks().get(new IdImpl(linkId2.toString())).getCoord())));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();

	}

}