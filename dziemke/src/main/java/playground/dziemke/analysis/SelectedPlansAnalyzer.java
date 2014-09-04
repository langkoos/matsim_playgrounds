package playground.dziemke.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * NOTE: Only yields useful results if agents cannot add activities to their plans over the course of the simulation
 */
public class SelectedPlansAnalyzer {
	// Parameters
	static String runId = "run_145f";
	static int numberOfIterations = 150;
	static int plansFileInterval = 50;
	static boolean useInterimPlans = true;
	
	// Output file
	static String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/analysis/";

	static Map<Integer, Integer> stayHomePlansMap = new HashMap<Integer, Integer>();
	static Map<Integer, Integer> otherPlansMap = new HashMap<Integer, Integer>();
	
	public static void main(String[] args) {
		if (useInterimPlans == true) {
			for (int i = 1; i<= numberOfIterations/plansFileInterval; i++) {
				String plansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + i * plansFileInterval
						+ "/" + runId + "." + i * plansFileInterval + ".plans.xml.gz";
				
				Config config = ConfigUtils.createConfig();
				Scenario scenario = ScenarioUtils.createScenario(config);
				PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
				reader.readFile(plansFile);
				Population population = scenario.getPopulation();
	
				countSelectedPlanTypes(population, i * plansFileInterval);
			}
		}
		
		String plansFileOutput = "D:/Workspace/container/demand/output/" + runId + "/" + runId + ".output_plans.xml.gz";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(plansFileOutput);
		Population population = scenario.getPopulation();

		countSelectedPlanTypes (population, 99999);
		
		writeFile();
	}
	
	
	static void countSelectedPlanTypes (Population population, int iteration) {
		int counterStayHomePlans = 0;
		int counterOtherPlans = 0;		
		
		// iterate over persons
		for (Person person : population.getPersons().values()) {			
			Plan selectedPlan = person.getSelectedPlan();
			int numberOfPlanElements = selectedPlan.getPlanElements().size();
			
			if (numberOfPlanElements == 1) {
				counterStayHomePlans++;
			} else if (numberOfPlanElements > 1) {
				counterOtherPlans++;
			} else {
				System.err.println("Plan may not have less than one element.");
			}		
		}
		stayHomePlansMap.put(iteration, counterStayHomePlans);
		otherPlansMap.put(iteration, counterOtherPlans);
	}

	
	static void writeFile() {
		new File(outputDirectory).mkdir();
		BufferedWriter bufferedWriter = null;
			
		try {
			File output = new File(outputDirectory + "selectedPlans.txt");
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
			
			for (int key : stayHomePlansMap.keySet()) {
    			bufferedWriter.write(key + "\t" + stayHomePlansMap.get(key) + "\t" + otherPlansMap.get(key));
    			bufferedWriter.newLine();
    		}    		
	    } catch (FileNotFoundException ex) {
	        ex.printStackTrace();
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        try {
	            if (bufferedWriter != null) {
	                bufferedWriter.flush();
	                bufferedWriter.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		System.out.println("Analysis file " + outputDirectory + "selectedPlans.txt" + " written.");
	}
}