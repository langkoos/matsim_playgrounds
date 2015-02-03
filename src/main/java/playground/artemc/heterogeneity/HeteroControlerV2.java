package playground.artemc.heterogeneity;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.scoring.DisaggregatedHeterogeneousScoreAnalyzer;
import playground.artemc.socialCost.MeanTravelTimeCalculator;
import playground.artemc.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculator;
import playground.artemc.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class HeteroControlerV2 {

	private static final Logger log = Logger.getLogger(HeteroControlerV2.class);

	private static String input;
	private static String output;
	private static boolean heteroSwitch = false;
	private static String simulationType = "homo";
	private static Double heterogeneityFactor = 1.0;


	public static void main(String[] args){

		input = args[0];
		if(args.length>1){
			output = args[1];
		}
		if(args.length>2){
			simulationType = args[2];
			if(args[2].equals("hetero")|| args[2].equals("heteroAlpha") || args[2].equals("heteroGamma") || args[2].equals("heteroGammaProp") || args[2].equals("heteroAlphaProp"))
				heteroSwitch=true;
		}
		if(args.length>3){
			heterogeneityFactor = Double.valueOf(args[3]);			
		}
		
		HeteroControlerV2 runner = new HeteroControlerV2();
		runner.run();
	}

	private void run() {

		Scenario scenario = initScenario();
		//System.setProperty("matsim.preferLocalDtds", "true");

		System.out.println(scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getName());
		System.out.println(ConfigUtils.addOrGetModule(scenario.getConfig(), HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class).getLambdaIncomeTravelcost());

		//Adjust the heterogeneity parameter
		Double newIncomeLambda = heterogeneityFactor * Double.parseDouble(ConfigUtils.addOrGetModule(scenario.getConfig(), HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class).getLambdaIncomeTravelcost());

		ConfigUtils.addOrGetModule(scenario.getConfig(),HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).setLambdaIncomeTravelcost(Double.toString(newIncomeLambda));
		ConfigUtils.addOrGetModule(scenario.getConfig(),HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).setIncomeOnTravelCostType(this.simulationType);

		Controler controler = new Controler(scenario);

		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		log.info("Adding Simple Annealer...");
		controler.addControlerListener(new SimpleAnnealer());

		controler.setModules(new IncomeHeterogeneityModule(input));
//		controler.setModules(new AbstractModule() {
//			                     @Override
//			                     public void install() {
//				                     // Include some things from ControlerDefaultsModule.java,
//				                     // but leave out TravelTimeCalculator.
//				                     // You can just comment out these lines if you don't want them,
//				                     // these modules are optional.
//				                     include(new TripRouterModule());
//				                     include(new StrategyManagerModule());
//				                     include(new LinkStatsModule());
//				                     include(new VolumesAnalyzerModule());
//				                     include(new LegHistogramModule());
//				                     include(new TravelDisutilityModule());
//				                     include(new IncomeHeterogeneityModule(input));
//
//				                     // Because TravelTimeCalculatorModule is left out,
//				                     // we have to provide a TravelTime.
//				                     // This line says: Use this thing here as the TravelTime implementation.
//				                     // Try removing this line: You will get an error because there is no
//				                     // TravelTime and someone needs it.
//				                     bindToInstance(TravelTime.class, new FreeSpeedTravelTime());
//			                     }
//		                     });


		log.info("Simulation type: "+simulationType);
		//HeterogeneityConfig heterogeneityConfig = new HeterogeneityConfig(input, scenario, simulationType, heterogeneityFactor);
		if(heteroSwitch)
		{
//			log.info("Adding Heterogeneity Config...");
//			controler.addControlerListener(heterogeneityConfig);
			
			log.info("Heterogeneityfactor: "+heterogeneityFactor);
			
			log.info("Setting TravelDisutilityFactory for heterogeneous population...");
//			//Routing Car
//			TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory factory = new TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory(heterogeneityConfig);
//			controler.setTravelDisutilityFactory(factory);
		}
		else{
			TravelTimeAndDistanceBasedTravelDisutilityFactory factory = new TravelTimeAndDistanceBasedTravelDisutilityFactory();
			controler.setTravelDisutilityFactory(factory);
		}


		//Routing PT
        WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		
		//VehicleOccupancyCalculator vehicleOccupancyCalculator = new VehicleOccupancyCalculator(controler.getScenario().getTransitSchedule(), controler.getScenario().getVehicles(), controler.getConfig());
		//controler.getEvents().addHandler(vehicleOccupancyCalculator);
		//controler.setTransitRouterFactory(new TransitRouterWSVImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy()));

		//controler.setTransitRouterFactory(new TransitRouterWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		//controler.setTransitRouterFactory(new TransitRouterHeteroWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), heterogeneityConfig));

		//Scoring
        controler.setScoringFunctionFactory(new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()));
		controler.setOverwriteFiles(true);
		
		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedHeterogeneousScoreAnalyzer((ScenarioImpl) controler.getScenario(),analysisControlerListener.getTripAnalysisHandler()));
		controler.run();

		//Logger root = Logger.getRootLogger();
		//root.setLevel(Level.ALL);
	}

	private static Scenario initScenario() {

		Config config = ConfigUtils.loadConfig(input+"config.xml", new HeterogeneityConfigGroup());

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
