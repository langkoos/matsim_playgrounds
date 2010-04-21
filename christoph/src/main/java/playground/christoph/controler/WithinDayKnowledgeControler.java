/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayKnowledgeControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.controler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.replanning.TravelTimeCollector;
import playground.christoph.router.CloneablePlansCalcRoute;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.router.costcalculators.SubNetworkDijkstraTravelCostWrapper;
import playground.christoph.router.util.SubNetworkDijkstraFactory;
import playground.christoph.withinday.replanning.CurrentLegReplanner;
import playground.christoph.withinday.replanning.InitialReplanner;
import playground.christoph.withinday.replanning.NextLegReplanner;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.identifiers.ActivityEndIdentifier;
import playground.christoph.withinday.replanning.identifiers.InitialIdentifierImpl;
import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifier;

/**
 * This Controler should give an Example what is needed to run
 * Simulations with WithinDayReplanning and respecting the 
 * Knowledge of the Agents.
 * 
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 * 
 * Additional Parameters have to be set in the WithinDayControler
 * Class. Here we just add the Knowledge Component.
 * 
 * By default "test/scenarios/berlin/config.xml" should work.
 * 
 * @author Christoph Dobler
 */

//mysimulations/kt-zurich/configIterative.xml

public class WithinDayKnowledgeControler extends WithinDayControler {
	
	private static final Logger log = Logger.getLogger(WithinDayKnowledgeControler.class);

	/*
	 * Select which size the known Areas should have in the Simulation.
	 * The needed Parameter is the Name of the Table in the MYSQL Database.
	 */
	protected String tableName = "BatchTable1_15";
	
	public WithinDayKnowledgeControler(String[] args)
	{
		super(args);
	}

	// only for Batch Runs
	public WithinDayKnowledgeControler(Config config)
	{
		super(config);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 * 
	 * The used Wrappers check, if an Agents knows a Link / Node or not.
	 * Additionally we have to use a DijkstraWrapperFactory that hands over the
	 * currently replanned Person to the Time- and CostCalculators so they
	 * can decide if the Person knows a Node / Link or not.
	 */
	@Override
	protected void initReplanningRouter() {
		
		/*
		 * Calculate the TravelTime based on the actual load of the links. Use only 
		 * the TravelTime to find the LeastCostPath.
		 */
//		KnowledgeTravelTimeCalculator travelTime = new KnowledgeTravelTimeCalculator(sim.getQNetwork());
//		KnowledgeTravelTimeWrapper travelTimeWrapper = new KnowledgeTravelTimeWrapper(travelTime);
//		travelTimeWrapper.checkNodeKnowledge(true);
//		
//		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTimeWrapper);
//		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
//		travelCostWrapper.checkNodeKnowledge(true);
//		
//		CloneablePlansCalcRoute dijkstraRouter = new CloneablePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, 
//				travelCostWrapper, travelTimeWrapper);

		
//		travelTime = new TravelTimeCollector(network);
//		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
//		foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
//		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector
//		
//		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
//		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
//		travelCostWrapper.checkNodeKnowledge(true);
//		
//		CloneablePlansCalcRoute dijkstraRouter = new CloneablePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, 
//				travelCostWrapper, travelTime);
		
		
		travelTime = new TravelTimeCollector(network);
		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector
				
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
		SubNetworkDijkstraTravelCostWrapper subNetworkDijkstraTravelCostWrapper = new SubNetworkDijkstraTravelCostWrapper(travelCost);
		
		CloneablePlansCalcRoute dijkstraRouter = new CloneablePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, 
				subNetworkDijkstraTravelCostWrapper, travelTime, new SubNetworkDijkstraFactory());		
		
		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		// If we do initial Replanning we don't want to remove the knowledge afterwards!
		((InitialReplanner)this.initialReplanner).setRemoveKnowledge(false);
		this.initialReplanner.setReplanner(dijkstraRouter);
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);
				
		this.duringActivityIdentifier = new ActivityEndIdentifier(this.sim);
		this.duringActivityReplanner = new NextLegReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData, this.events);
		this.duringActivityReplanner.setReplanner(dijkstraRouter);
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.parallelActEndReplanner.addWithinDayReplanner(this.duringActivityReplanner);
		
		this.duringLegIdentifier = new LeaveLinkIdentifier(this.sim);
		this.duringLegReplanner = new CurrentLegReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData, this.events);
		this.duringLegReplanner.setReplanner(dijkstraRouter);
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.parallelLeaveLinkReplanner.addWithinDayReplanner(this.duringLegReplanner);
	}

	/*
	 * Additionally set the KnowledgeStorageHandler.
	 * -> Where to get the Knowledge from?
	 */
	@Override
	protected void runMobSim() 
	{
		setKnowledgeStorageHandler();
		super.runMobSim();
	}
	
	/*
	 * How to store the known Nodes of the Agents?
	 * Currently we store them in a Database.
	 */
	private void setKnowledgeStorageHandler()
	{		
		for(Person person : population.getPersons().values())
		{			
			Map<String, Object> customAttributes = person.getCustomAttributes();
			
			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());
			
			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);
			mapKnowledgeDB.setTableName(tableName);
			
			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
	}
	

	/*
	 * =================================================================== main
	 * ===================================================================
	 */

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final WithinDayKnowledgeControler controler = new WithinDayKnowledgeControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}
