package playground.artemc.socialCost;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.scoring.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoring.DisaggregatedScoreAnalyzer;


public class BaseControler {

	private static final Logger log = Logger.getLogger(BaseControler.class);

	private static String input;
	private static String output;

	public static void main(String[] args) {

		input = args[0];
		output = args[1];

		BaseControler runner = new BaseControler();
		runner.runBaseScanerio();
	}

	private void runBaseScanerio() {

		Controler controler = null;
		Scenario scenario = initSampleScenario();
		controler = new Controler(scenario);

		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		// Additional analysis
		ScenarioImpl scnearioImpl = (ScenarioImpl) controler.getScenario();
		controler.setScoringFunctionFactory(new DisaggregatedCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getNetwork()));
		controler.addControlerListener(new DisaggregatedScoreAnalyzer(scnearioImpl));
		controler.addControlerListener(new SimpleAnnealer());
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private static class Initializer implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {

			Controler controler = event.getControler();
			// create a plot containing the mean travel times
			Set<String> transportModes = new HashSet<String>();
			transportModes.add(TransportMode.car);
			transportModes.add(TransportMode.pt);
			transportModes.add(TransportMode.walk);
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);
		}
	}

	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig(input+"config.xml");
		config.controler().setOutputDirectory(output);
		config.network().setInputFile(input+"network.xml");
		config.plans().setInputFile(input+"population.xml");
		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input+"vehicles.xml");

		//config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}


}
