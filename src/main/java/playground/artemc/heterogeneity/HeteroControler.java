package playground.artemc.heterogeneity;


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
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.scoring.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoring.DisaggregatedScoreAnalyzer;
import playground.artemc.scoring.TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory;
import playground.artemc.socialCost.MeanTravelTimeCalculator;
import playground.artemc.transitRouterEventsBased.TransitRouterHeteroWSImplFactory;
import playground.artemc.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculator;
import playground.artemc.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class HeteroControler {

	private static final Logger log = Logger.getLogger(HeteroControler.class);

	private static String input;
	private static String output;
	private static boolean heteroSwitch = false;
	private static String  simulationType = "homo";


	public static void main(String[] args){

		input = args[0];

		if(args.length>1){
			output = args[1];
		}


		if(args.length>2){
			simulationType = args[2];
			if(args[2].equals("hetero")|| args[2].equals("heteroAlpha") || args[2].equals("heteroGamma"))
				heteroSwitch=true;
		}

		HeteroControler  runner = new HeteroControler();
		runner.run();
	}

	private void run() {

		Controler controler = null;
		Scenario scenario = initSampleScenario();
		System.setProperty("matsim.preferLocalDtds", "true");
		controler = new Controler(scenario);
		

		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		//controler.setMobsimFactory(new QSimFactory());

		log.info("Adding Simple Annealer...");
		controler.addControlerListener(new SimpleAnnealer());

		
		log.info("Simulation type: "+simulationType);
		HeterogeneityConfig heterogeneityConfig = new HeterogeneityConfig(input, scenario, simulationType);
		if(heteroSwitch)
		{
			log.info("Adding Heterogeneity Config...");
			controler.addControlerListener(heterogeneityConfig);
		}

		//Routing Car
		TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory factory = new TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory(heterogeneityConfig);
		controler.setTravelDisutilityFactory(factory);

		//Routing PT
        WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		//VehicleOccupancyCalculator vehicleOccupancyCalculator = new VehicleOccupancyCalculator(controler.getScenario().getTransitSchedule(), controler.getScenario().getVehicles(), controler.getConfig());
		//controler.getEvents().addHandler(vehicleOccupancyCalculator);
		//controler.setTransitRouterFactory(new TransitRouterWSVImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy()));

		//controler.setTransitRouterFactory(new TransitRouterWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		controler.setTransitRouterFactory(new TransitRouterHeteroWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), heterogeneityConfig));

		//Scoring
        controler.setScoringFunctionFactory(new DisaggregatedCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork(), heterogeneityConfig));
		controler.setOverwriteFiles(true);
		
		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedScoreAnalyzer((ScenarioImpl) controler.getScenario(),analysisControlerListener.getTripAnalysisHandler()));
		controler.run();

		//Logger root = Logger.getRootLogger();
		//root.setLevel(Level.ALL);
	}

	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig(input+"config.xml");
		config.network().setInputFile(input+"network.xml");
		
		boolean isPopulationZipped = new File(input+"population.xml.gz").isFile();
		if(isPopulationZipped){
			config.plans().setInputFile(input+"population.xml.gz");
		}else{
			config.plans().setInputFile(input+"population.xml");
		}
		
		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input+"vehicles.xml");

		if(output!=null){
			config.controler().setOutputDirectory(output);
		}

		//config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
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
}
