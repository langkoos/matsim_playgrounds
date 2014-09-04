/* *********************************************************************** *
 * project: org.matsim.*
 * Matsim2030Utils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.population.filters.PersonIntersectAreaFilter;

import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.generation.ScenarioMergingConfigGroup;
import playground.ivt.matsim2030.router.TransitRouterNetworkReader;
import playground.ivt.matsim2030.router.TransitRouterWithThinnedNetworkFactory;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.ivt.utils.MapUtils;
import playground.ivt.utils.TripModeShares;

/**
 * @author thibautd
 */
public class Matsim2030Utils {
	private static final Logger log =
		Logger.getLogger(Matsim2030Utils.class);

	
	private static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	private static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "multimodalLegDistanceDistribution.txt";
	private static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";


	public static Config loadConfig( final String file ) {
		final Config config = ConfigUtils.createConfig();
		addDefaultGroups( config );
		ConfigUtils.loadConfig( config , file );
		return config;
	}

	public static void addDefaultGroups( final Config config ) {
		config.addModule( new ScenarioMergingConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
	}

	public static Scenario loadScenario( final Config config ) {
		final Scenario sc = ScenarioUtils.createScenario( config );
		loadScenario( sc );
		return sc;
	}

	public static Scenario loadScenario( final Scenario scenario ) {
		ScenarioUtils.loadScenario( scenario );
		enrichScenario( scenario );

		logPopulationStats( scenario );

		return scenario;
	}

	public static void diluteScenario( final Scenario scenario ) {
		final ScenarioMergingConfigGroup mergingGroup = (ScenarioMergingConfigGroup)
			scenario.getConfig().getModule( ScenarioMergingConfigGroup.GROUP_NAME );

		if ( !mergingGroup.isPerformMerging() ) {
			log.warn( "skipping dilution!" );
			return;
		}

		// now that coordinates are allocated, we can "dilute" the scenario.
		// Note that if no routes are defined in the population(s), straight lines will be used,
		// which may lead to significantly different results in Zurich due to the lake...
		// Particularly for the freight population
		// TODO: check that it is no problem (or route the populations)
		if ( mergingGroup.getPerformDilution() ) {
			log.info( "performing \"dilution\"" );
			diluteScenario( scenario , mergingGroup.getDilutionCenter() , mergingGroup.getDilutionRadiusM() );
		}
	}

	public static void enrichScenario( final Scenario scenario ) {
		final Config config = scenario.getConfig();
		final ScenarioMergingConfigGroup mergingGroup = (ScenarioMergingConfigGroup)
			config.getModule( ScenarioMergingConfigGroup.GROUP_NAME );

		if ( !mergingGroup.isPerformMerging() ) {
			log.warn( "skipping scenario merging!" );
			return;
		}

		final Random random = MatsimRandom.getLocalInstance();

		if ( mergingGroup.getCrossBorderPlansFile() != null ) {
			log.info( "reading cross border plans from "+mergingGroup.getCrossBorderPlansFile() );
			addSubpopulation(
					random,
					mergingGroup.getSamplingRate(),
					mergingGroup.getCrossBorderPopulationId(),
					mergingGroup.getCrossBorderPlansFile(),
					scenario );
		}

		if ( mergingGroup.getCrossBorderFacilitiesFile() != null ) {
			log.info( "reading facilities for cross-border population from "+mergingGroup.getCrossBorderFacilitiesFile() );
			new MatsimFacilitiesReader( scenario ).readFile( mergingGroup.getCrossBorderFacilitiesFile() );
		}

		if ( mergingGroup.getFreightPlansFile() != null ) {
			log.info( "reading freight plans from "+mergingGroup.getFreightPlansFile() );
			addSubpopulation(
					random,
					mergingGroup.getSamplingRate(),
					mergingGroup.getFreightPopulationId(),
					mergingGroup.getFreightPlansFile(),
					scenario );
		}

		if ( mergingGroup.getFreightFacilitiesFile() != null ) {
			log.info( "reading facilities for freight population from "+mergingGroup.getFreightFacilitiesFile() );
			new MatsimFacilitiesReader( scenario ).readFile( mergingGroup.getFreightFacilitiesFile() );
		}

		logPopulationStats( scenario );

		// do it BEFORE importing the PT part of the network.
		log.info( "connecting activities, links and facilities" );
		connectFacilitiesWithLinks( mergingGroup , scenario );

		if ( mergingGroup.getPtSubnetworkFile() != null ) {
			log.info( "reading pt network from "+mergingGroup.getPtSubnetworkFile() );
			new MatsimNetworkReader( scenario ).readFile( mergingGroup.getPtSubnetworkFile() );
		}

	}

	private static void logPopulationStats(final Scenario scenario) {
		final Map<String, AtomicInteger> popcounts = new HashMap<String, AtomicInteger>();

		final String attribute = scenario.getConfig().plans().getSubpopulationAttributeName();
		final MapUtils.Factory<AtomicInteger> factory =
			new MapUtils.Factory<AtomicInteger>() {
				@Override
				public AtomicInteger create() {
					return new AtomicInteger( 0 );
				}
			};
		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			MapUtils.getArbitraryObject(
					(String)
					scenario.getPopulation().getPersonAttributes().getAttribute(
						p.getId().toString(),
						attribute ),
					popcounts,
					factory ).incrementAndGet();
		}

		log.info( "~~~~~~~~~~~~~~~~~~~ Population statistics:" );
		log.info( scenario.getPopulation().getPersons().size()+" persons in total in population" );
		for ( Map.Entry<String, AtomicInteger> e : popcounts.entrySet() ) {
			log.info( e.getValue().intValue()+" persons in subpopulation "+e.getKey() );
		}
	}

	private static void diluteScenario(
			final Scenario scenario,
			final Coord center,
			final double radius) {
		// TODO Auto-generated method stub
		final Map<Id, Link> areaOfInterest = new HashMap<Id, Link>();

		for (Link link : scenario.getNetwork().getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= radius) || (CoordUtils.calcDistance(to.getCoord(), center) <= radius)) {
				areaOfInterest.put(link.getId(),link);
			}
		}

		final Set<Id> idsToKeep = new HashSet<Id>();
		final PersonIntersectAreaFilter filter =
			new PersonIntersectAreaFilter(
					new PersonAlgorithm() {
						@Override
						public void run(final Person person) {
							idsToKeep.add( person.getId() );
						}
					},
					areaOfInterest,
					scenario.getNetwork());
		filter.setAlternativeAOI(center,radius);

		filter.run( scenario.getPopulation() );

		// XXX this should actually not be allowed. Should we add a remove method to population?
		final Iterator<Id> it = scenario.getPopulation().getPersons().keySet().iterator();
		final int initialSize = scenario.getPopulation().getPersons().size();
		while ( it.hasNext() ) {
			final Id current = it.next();
			if ( !idsToKeep.contains( current ) ) {
				it.remove();
				scenario.getPopulation().getPersonAttributes().removeAllAttributes( current.toString() );
			}
		}
		assert scenario.getPopulation().getPersons().size() == idsToKeep.size();
		final int finalSize = scenario.getPopulation().getPersons().size();

		log.info( finalSize+" persons out of "+initialSize+" retained in dilution ("+(100d * finalSize / initialSize)+"%)" );
	}

	private static void addSubpopulation(
			final Random random,
			final double samplingRate,
			final String subpopulationName,
			final String subpopulationFile,
			final Scenario scenario ) {
		// we could read directly the population in the global scenario,
		// but this would make creation of object attributes tricky.
		// This also makes sampling easier
		final Scenario tempSc = ScenarioUtils.createScenario( scenario.getConfig() );
		new MatsimPopulationReader( tempSc ).readFile( subpopulationFile );

		final String attribute = scenario.getConfig().plans().getSubpopulationAttributeName();
		int inputCount = 0;
		int acceptCount = 0;
		for ( Person p : tempSc.getPopulation().getPersons().values() ) {
			inputCount++;
			if ( random.nextDouble() > samplingRate ) continue;
			acceptCount++;

			scenario.getPopulation().addPerson( p );
			scenario.getPopulation().getPersonAttributes().putAttribute(
					p.getId().toString(),
					attribute,
					subpopulationName );
		}

		log.info( acceptCount+" persons out of "+inputCount+" were retained in the population (effective sampling rate "+
				( ((double) acceptCount) / inputCount )+")" );
	}

	private static void connectFacilitiesWithLinks(
			final ScenarioMergingConfigGroup mergingGroup,
			final Scenario sc ) {
		final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );
		// ignore facilities, as otherwise the following happens:
		// - if there is a facility, the activity link id is set to the one of the facility
		// - thus, if the facility has no link, the coordinate of the activity is overriden
		// - if the activity has no link, a new one is computed
		// This caused problems when I had routes in the population: activities were
		// moved to a close link, and routes became wrong.
		final PersonAlgorithm xy2Links =
			new XY2Links(
					filterLinksWithAllModes(
						sc.getNetwork(),
						mergingGroup.getModesOfFacilityLinks() ),
					null );

		// first: if there are links indicated in the activities, use them
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			// allocate links to activities which have none
			xy2Links.run( person );

			// use links of activities to locate facilities
			for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan(), stages ) ) {
				final Id linkId = act.getLinkId();
				if ( !sc.getNetwork().getLinks().containsKey( linkId ) ) {
					throw new RuntimeException( "There is no link "+linkId+
							" in the car part of the network, but activity "+act+
							" for "+person+
							" is there. Might be a PT link, which is wrong. Check your initial plans!" );
				}
				final Id facilityId = act.getFacilityId();

				final ActivityFacility fac = sc.getActivityFacilities().getFacilities().get( facilityId );
				if ( fac.getLinkId() == null ) ((ActivityFacilityImpl) fac).setLinkId( linkId );
				else if ( !fac.getLinkId().equals( linkId ) ) throw new RuntimeException( "inconsistent links for facility "+facilityId );
			}
		}

		// there might still be some facilities without a link (ie facilities not used by anybody):
		// allocate a link here (before loading the PT part of the network)
		for ( ActivityFacility fac : sc.getActivityFacilities().getFacilities().values() ) {
			if ( fac.getLinkId() != null ) {
				final NetworkImpl net = (NetworkImpl) sc.getNetwork();
				((ActivityFacilityImpl) fac).setLinkId( net.getNearestLink( fac.getCoord() ).getId() );
			}
		}

		// now hopefully everything is nice and consistent...
	}

	public static Network filterLinksWithAllModes(final Network fullNetwork, final Set<String> modes) {
		final Network subNetwork = NetworkImpl.createNetwork();
		final NetworkFactory factory = subNetwork.getFactory();

		for (Link link : fullNetwork.getLinks().values()) {
			if ( link.getAllowedModes().containsAll( modes ) ) {
				final Id fromId = link.getFromNode().getId();
				final Id toId = link.getToNode().getId();

				Node fromNode2 = subNetwork.getNodes().get(fromId);
				Node toNode2 = subNetwork.getNodes().get(toId);

				if (fromNode2 == null) {
					fromNode2 = factory.createNode(fromId, link.getFromNode().getCoord());
					subNetwork.addNode(fromNode2);
					if (fromId == toId) {
						toNode2 = fromNode2;
					}
				}

				if (toNode2 == null) {
					toNode2 = factory.createNode(toId, link.getToNode().getCoord());
					subNetwork.addNode(toNode2);
				}

				final Link link2 = factory.createLink(link.getId(), fromNode2, toNode2);
				link2.setAllowedModes( link.getAllowedModes() );
				link2.setCapacity(link.getCapacity());
				link2.setFreespeed(link.getFreespeed());
				link2.setLength(link.getLength());
				link2.setNumberOfLanes(link.getNumberOfLanes());
				subNetwork.addLink(link2);
			}
		}

		return subNetwork;
	}

	public static void initializeLocationChoice( final Controler controler ) {
		final Scenario scenario = controler.getScenario();
		final DestinationChoiceBestResponseContext lcContext =
			new DestinationChoiceBestResponseContext( scenario );
		lcContext.init();

		// XXX this thing is awful. I think one can (and should) avoid using it...
		// There does not seem to be a reason not to do all the notify startup
		// method does before calling run().
		controler.addControlerListener(
				new DestinationChoiceInitializer(
					lcContext));
	}

	public static void initializeScoring( final Controler controler ) {
 		final MATSim2010ScoringFunctionFactory scoringFunctionFactory =
			new MATSim2010ScoringFunctionFactory(
					controler.getScenario(),
					new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ) ); 	
		controler.setScoringFunctionFactory( scoringFunctionFactory );
	}

	public static TripRouterFactory createTripRouterFactory( final Scenario scenario ) {
		final TransitRouterConfig conf = new TransitRouterConfig( scenario.getConfig() );

		final ScenarioMergingConfigGroup matsim2030conf =
			(ScenarioMergingConfigGroup)
			scenario.getConfig().getModule(
					ScenarioMergingConfigGroup.GROUP_NAME );
		
		final TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		
		if ( matsim2030conf.getThinnedTransitRouterNetworkFile() != null ) {
			log.info( "using thinned transit router network from "+matsim2030conf.getThinnedTransitRouterNetworkFile() );
			final TransitRouterNetwork transitRouterNetwork = new TransitRouterNetwork();
			new TransitRouterNetworkReader(
					scenario,
					scenario.getTransitSchedule(),
					transitRouterNetwork ).parse(
						matsim2030conf.getThinnedTransitRouterNetworkFile() );
	
			final TransitRouterWithThinnedNetworkFactory transitRouterFactory =
				new TransitRouterWithThinnedNetworkFactory(
						scenario.getTransitSchedule(),
						conf,
						transitRouterNetwork );
	
			builder.setTransitRouterFactory( transitRouterFactory );
		}
		else {
			log.warn( "using no pre-processed transit router network --- This would be more efficient!" );
		}

		return builder.build( scenario );
	}

	public static void loadControlerListeners( final Controler controler ) {
		controler.addControlerListener(
				new CalcLegTimesHerbieListener(
					CALC_LEG_TIMES_FILE_NAME,
					LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		controler.addControlerListener(
				new LegDistanceDistributionWriter(
					LEG_DISTANCE_DISTRIBUTION_FILE_NAME,
					controler.getScenario().getNetwork()));
		controler.addControlerListener(
				new IterationStartsListener() {
					boolean wasInit = false;
					@Override
					public void notifyIterationStarts(IterationStartsEvent event) {
						if ( wasInit ) return;
						wasInit = true;

						// This is awful, but this is the only way to make sure
						// all the elements of the Controler are initialized...
						// We need to do it at the first iteration, because we need
						// to be able to initialize the trip router, which requires
						// the TravelTimeCalculator to be initialized, which is done
						// at "prepareForSim", ie AFTER the startup event has been fired.
						// What a mess.
						final TripRouter router = controler.getTripRouterFactory().instantiateAndConfigureTripRouter();
						controler.addControlerListener(
							new TripModeShares(
								25, // write interval. TODO: pass by config
								controler.getControlerIO(),
								controler.getScenario(),
								router.getMainModeIdentifier(),
								router.getStageActivityTypes() ) );
					}
				} );
	}

	public static void createEmptyDirectoryOrFailIfExists(final String directory) {
		final File file = new File( directory +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
		}
		file.mkdirs();
	}
}
