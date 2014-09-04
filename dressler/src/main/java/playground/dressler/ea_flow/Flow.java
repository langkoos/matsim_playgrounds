/* *********************************************************************** *
 * project: org.matsim.*
 * Flow.java
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

package playground.dressler.ea_flow;

//java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.dressler.Interval.EdgeFlowI;
import playground.dressler.Interval.EdgeInterval;
import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.HoldoverInterval;
import playground.dressler.Interval.HoldoverIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.ShadowEdgeFlow;
import playground.dressler.Interval.SinkIntervals;
import playground.dressler.Interval.SourceIntervals;
import playground.dressler.control.FlowCalculationSettings;
import playground.dressler.network.IndexedLinkI;
import playground.dressler.network.IndexedNetworkI;
import playground.dressler.network.IndexedNodeI;
import playground.dressler.util.CPUTimer;
/**
 * Class representing a dynamic flow on an network with multiple sources and a single sink
 * @author Daniel Dressler, Manuel Schneider
 *
 */

public class Flow {
////////////////////////////////////////////////////////////////////////////////////////
//--------------------------FIELDS----------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The global settings.
	 */
	private final FlowCalculationSettings _settings;

	/**
	 * The network on which we find routes.
	 * We expect the network not to change between runs!
	 */
	private final IndexedNetworkI _network;
	private int nnodes;
	private int nlinks;

	/**
	 * Edge representation of flow on the network
	 */
//	private HashMap<Link, EdgeIntervals> _flow;
	private EdgeFlowI[] _flow;
	//TODO holdover done declare _holdover
	private EdgeFlowI[] _holdover;

	/**
	 * Source outflow, somewhat like holdover for sources
	 */
	private SourceIntervals[] _sourceoutflow;

	/**
	 * Flow into sinks
	 */
	private SinkIntervals[] _sinkflow;

	/**
	 * TimeExpandedTimeExpandedPath representation of flow on the network
	 */
	private LinkedList<TimeExpandedPath> _TimeExpandedPaths;
	
	/**
	 * stores which path uses an Edge a a given piont of time
	 */
	private  HashMap<Link,TreeMap<Integer,LinkedList<TimeExpandedPath>>> _edgePathMap;
	private  HashMap<Node,TreeMap<Integer,LinkedList<TimeExpandedPath>>> _sourcePathMap;
	private HashMap<Node,TreeMap<Integer,LinkedList<TimeExpandedPath>>>  _sinkPathMap;
	/**
	 * list of all sources
	 */
	private final ArrayList<IndexedNodeI> _sources;

	/**
	 * list of all sinks
	 */
	private final ArrayList<IndexedNodeI> _sinks;

	/**
	 * stores unsatisfied demands for each vertex.
	 * positive means source, negative means sink
	 */
	private int[] _demands;

	/**
	 * the Time Horizon (for easy access)
	 */
	private int _timeHorizon;


	/**
	 * total flow augmented so far
	 */
	private int totalflow;


	/*
	 * enable or disable the touched nodes hashmaps in the TEPs
	 */
	private boolean _checkTouchedNodes;

	private boolean _storepaths;
	private boolean _unfoldpaths;
	CPUTimer  Tunfold= new CPUTimer("unfold time");
	CPUTimer  Topposing = new CPUTimer( "remove time opposing");
	CPUTimer  Tunloop = new CPUTimer("unlooptime");
	CPUTimer Taug= new CPUTimer("actual time in method");
	/**
	 * TODO use debug mode
	 * flag for debug mode
	 */
	
	private static int _debug = 0;


///////////////////////////////////////////////////////////////////////////////////
//-----------------------------Constructors--------------------------------------//
///////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor that initializes a zero flow over time for the specified settings
	 * @param settings
	 */
	public Flow(FlowCalculationSettings settings) {

		this._settings = settings;
		this._network = settings.getNetwork();
		nnodes = _network.getLargestIndexNodes() + 1;
		nlinks = _network.getLargestIndexLinks() + 1;
		
		this._flow = new EdgeFlowI[nlinks];
		this._sourceoutflow = new SourceIntervals[nnodes];
		this._sinkflow = new SinkIntervals[nnodes];

		this._checkTouchedNodes = settings.checkTouchedNodes;

		this._storepaths = settings.keepPaths;
		this._unfoldpaths = settings.unfoldPaths;

		this._TimeExpandedPaths = new LinkedList<TimeExpandedPath>();
		this._demands = new int[nnodes];
		this._sources = new ArrayList<IndexedNodeI>();
		this._sinks = new ArrayList<IndexedNodeI>();
		this._edgePathMap =new HashMap<Link,TreeMap<Integer,LinkedList<TimeExpandedPath>>>();
		this._sinkPathMap =new HashMap<Node,TreeMap<Integer,LinkedList<TimeExpandedPath>>>();
		this._sourcePathMap =new HashMap<Node,TreeMap<Integer,LinkedList<TimeExpandedPath>>>();
		this._holdover =new EdgeFlowI[nnodes];
		
		for(IndexedNodeI node : this._network.getNodes()){
			if (this._settings.isSource(node)) {
				int d = this._settings.getDemand(node);
				this._sources.add(node);
				EdgeInterval temp = new EdgeInterval(0,this._settings.TimeHorizon);
				this._sourceoutflow[node.getIndex()] = new SourceIntervals(temp);
				this._demands[node.getIndex()] = d;
			} else if (this._settings.isSink(node)) {
				int d = this._settings.getDemand(node);
				this._sinks.add(node);
				EdgeInterval temp = new EdgeInterval(0,this._settings.TimeHorizon);
				this._sinkflow[node.getIndex()] =  new SinkIntervals(temp);
				this._demands[node.getIndex()] = d;
			}
			//TODO holdover done initialize _holdover
			if(settings.useHoldover){
				HoldoverInterval temp = new HoldoverInterval(0,settings.TimeHorizon);
				HoldoverIntervals temps = new HoldoverIntervals(temp,Integer.MAX_VALUE);
				this._holdover[node.getIndex()] = temps;
			}
		}

		// initialize EdgeIntervalls or the ShadowEdgeFlows and edgePathMap
		for (IndexedLinkI edge : this._network.getLinks()) {
			if(settings.mapLinksToTEP){
				TreeMap<Integer,LinkedList<TimeExpandedPath>> temp = new TreeMap<Integer,LinkedList<TimeExpandedPath>>();
				this._edgePathMap.put(edge.getMatsimLink(), temp);
			}
			
			int low = 0;
			int high = settings.TimeHorizon;
			
			int availLow = low;
			int availHigh = high;
			// Intervals expects that they start at 0, so we cannot restrict them
			// to just the available interval ...

			Interval available;
			if (this._settings.getWhenAvailable(edge) != null) {
				available = this._settings.getWhenAvailable(edge);
				// could still be null, which means "always"
			} else {
				available = null;
			}

			if (available != null) {
				availLow = Math.max(low, available.getLowBound());
				availHigh = Math.min(high, available.getHighBound());
			}

			available = new Interval(availLow, availHigh);
			
			if (settings.useShadowFlow) {
				Interval temp = new Interval(low, high);

				ShadowEdgeFlow edgeflow = new ShadowEdgeFlow(temp, this._settings.getLength(edge),
						this._settings.getCapacity(edge), available);

				this._flow[edge.getIndex()] = edgeflow;
			} else {

				EdgeInterval temp = new EdgeInterval(low, high);

				EdgeIntervals edgeflow = new EdgeIntervals(temp, this._settings.getLength(edge),
						this._settings.getCapacity(edge), available);

				this._flow[edge.getIndex()] = edgeflow;
			}
			
		}

		this._timeHorizon = settings.TimeHorizon;
		this.totalflow = 0;
		
	}

//////////////////////////////////////////////////////////////////////////////////
//--------------------Flow handling Methods-------------------------------------//
//////////////////////////////////////////////////////////////////////////////////
	/**
	 * Method to determine whether a Node is a Source with positive demand
	 * @param node Node that is checked
	 * @return true iff Node is a Source and has positive demand
	 */
	public boolean isActiveSource(final IndexedNodeI node) {
		return _demands[node.getIndex()] > 0;		
	}

	/**
	 * decides whether a Node is a non-active (depleted) Source
	 * @param node Node to check for
	 * @return true iff node is a Source now with demand 0
	 */
	public boolean isNonActiveSource(final IndexedNodeI node){
		if (this._settings.isSource(node)) {
			return _demands[node.getIndex()] == 0;
		}
		return false;
	}

	/**
	 * Method to determine whether a Node is a Sink with some demand left
	 * @param node Node that is checked
	 * @return true iff Node is a sink and has demand left (i.e. demand < 0)
	 */
	public boolean isActiveSink(final IndexedNodeI node) {
		return _demands[node.getIndex()] < 0;	
	}

	/**
	 * decides whether a Node is a non-active (full) Sink
	 * @param node Node to check for
	 * @return true iff node is a Sink now with demand 0
	 */
	public boolean isNonActiveSink(final IndexedNodeI node){
		if (this._settings.isSink(node)) {
		  return _demands[node.getIndex()] == 0;
		}
		return false;
	}

	/**
	 * Method for residual bottleneck capacity of the TimeExpandedPath
	 * (also limited by the source)
	 * @param TimeExpandedPath
	 * @return minimum over all unused capacities and the demand in the first node
	 */
	public int bottleNeckCapacity(final TimeExpandedPath TimeExpandedPath){
		//check if first node is a source
		IndexedNodeI temp;
		temp = TimeExpandedPath.getSource();
		int demand = this._demands[temp.getIndex()];
		if (!_settings.isSource(temp)) {
			throw new IllegalArgumentException("Startnode is no source " + TimeExpandedPath);
		}

		int result = demand;

		if (result == 0) {
			// this may actually happen now that many paths are constructed that orginate
			// in the same source (for the forward search).
			return 0;
		}

		// check if the final node is a sink
		temp = TimeExpandedPath.getSink();
		demand = this._demands[temp.getIndex()];
		if (!_settings.isSink(temp)){
			throw new IllegalArgumentException("Endnode is no sink " + TimeExpandedPath);
		}
		result = Math.min(result, -demand);

		if (result == 0) {
			return 0;
		}

		//go through the path edges
		//System.out.println("augmenting path: ");

		for(PathStep step : TimeExpandedPath.getPathSteps()){
			int cap;
			
			// FIXME really bad style ...
			if (step instanceof StepEdge) {
				StepEdge se = (StepEdge) step;
				IndexedLinkI edge = se.getEdge();

				if(se.getForward()){
					cap = this._settings.getCapacity(edge) - this._flow[edge.getIndex()].getFlowAt(se.getStartTime());
					
					// check if the edge exists
					Interval when = this._settings.getWhenAvailable(edge);
					if (when != null && !when.contains(se.getStartTime())) {
							cap = 0;					
					}
				} else {
					cap = this._flow[edge.getIndex()].getFlowAt(se.getArrivalTime());
				}
				if(cap<result ){
					result= cap;
				}
			} else if (step instanceof StepSourceFlow) {
				StepSourceFlow ssf = (StepSourceFlow) step;
				IndexedNodeI node  = ssf.getStartNode().getRealNode();

				if (!ssf.getForward()) {
					SourceIntervals si = this._sourceoutflow[node.getIndex()];
					if (si == null) {
						System.out.println("Weird. Source of StepSourceFlow has no sourceoutflow!");
						return 0;
					} else {
						cap = si.getFlowAt(ssf.getStartTime());
						if (cap < result) {
							result = cap;
						}
					}
				}
				/* no else, because outflow out of a source has no cap.
				   (the demand of the original source is accounted for,
				   demand of sources we pass through does not matter) */
			} else if (step instanceof StepSinkFlow) {
				StepSinkFlow ssf = (StepSinkFlow) step;
				IndexedNodeI sink;

				// the same node anyway ...
				if (ssf.getForward()) {
					sink = ssf.getArrivalNode().getRealNode();
				} else {
					sink = ssf.getStartNode().getRealNode();
				}

				// Flow through a sink is not capped by the demand of the sink.
				// The final destination was already checked in the beginning.
				if (!step.getForward()) {
					SinkIntervals si = this._sinkflow[sink.getIndex()];
					if (si == null) {
						System.out.println("Weird. Sink of StepSinkFlow has no sinkflow!");
						return 0;
					} else {
						cap = si.getFlowAt(ssf.getArrivalTime());
						if (cap < result) {
							result = cap;
						}
					}
					//throw new RuntimeException("BottleNeck for residual StepSinkFlow not supported yet!");
				} // inflow into sink is uncapped
			} else if (step instanceof StepHold) {
				
				StepHold hold = (StepHold) step;
				IndexedNodeI node = hold.getStartNode().getRealNode();
				
				int starttime = hold.getStartTime();
				int stoptime = hold.getArrivalTime();
				
				HoldoverIntervals intervals = (HoldoverIntervals) this._holdover[node.getIndex()];
				
				// this also adjusts the indices appropriately
				cap = intervals.bottleneck(starttime, stoptime, hold.getForward());
				if (cap < result) {
					result = cap;
				}
			} else {
				throw new RuntimeException("Unsupported kind of PathStep!");
			}

			// no need for further scanning, if this path is already useless
			if (result == 0) {
				return 0;
			}

		}
		
		return result;
	}

	/**
	 * Method to add another TimeExpandedPath to the flow. The TimeExpandedPath will be added with flow equal to its bottleneck capacity
	 * @param TimeExpandedPath the TimeExpandedPath on which the maximal possible flow is augmented
	 * @return Amount of flow augmented
	 */
	public int augment(TimeExpandedPath TEP){
		if (this._settings.mapLinksToTEP && _debug > 0) {
			checkPathMap(true);
			//mapAllPaths();
		}
		Taug.onoff();
		int bottleneck = bottleNeckCapacity(TEP);	  
		int result = this.augment(TEP, bottleneck);
		Taug.onoff();
		return result;
	}

	/**
	 * Method to add another TimeExpandedPath to the flow with a given flow value on it
	 * @param TEP The TimeExpandedPath on which the maximal flow possible is augmented
	 * @param gamma Amount of flow to augment
	 * @return Amount of flow that was really augmented. Should be gamma under most circumstances.
	 */
	public int augment(TimeExpandedPath TEP, int gamma){

	  if (TEP.hadToFixSourceLinks()) {
	    System.out.println("TimeExpandedPath should start with PathEdge of type SourceOutflow! Fixed.");
	  }
	  
	  TEP.setFlow(gamma);
	  if (gamma == 0) {
		  if (_debug > 0) {
			  System.out.println("I refuse to augment with 0 flow.");
		  }
		  return 0;
	  }

	  if (this._storepaths) {
		  // We create a fresh copy of the TEP for storage, just to be safe from weird side effects.
		  TimeExpandedPath freshCopy = TimeExpandedPath.clone(TEP);
		  if (this._unfoldpaths) {
			  unfoldandaugment(freshCopy);
		  } else {
			  dumbaugment(freshCopy, gamma);
			  this._TimeExpandedPaths.addFirst(freshCopy);
			  // do not need to map paths if we do not unfold 
			  /*if(this._settings.mapLinksToTEP){
				  mapPath(TEP);
			  }*/
		  }
	  } else {
		  dumbaugment(TEP, gamma);
	  }

	  this.totalflow += gamma;

	  return gamma; // should be correct.
	}

	/**
	 * Method to change just the flow values on the step
	 * @param step The Step
	 * @param gamma Amount of flow to augment, positive or negative!
	 */

	private void augmentStep(PathStep step, int gamma) {

		// FIXME really bad style ...
		if (step instanceof StepEdge) {
			StepEdge se = (StepEdge) step;
			IndexedLinkI edge = se.getEdge();

			if(se.getForward()){
				this._flow[edge.getIndex()].augment(se.getStartTime(), gamma);
			}	else {
				this._flow[edge.getIndex()].augment(se.getArrivalTime(), -gamma);
			}

		} else if (step instanceof StepSourceFlow) {
			StepSourceFlow ssf = (StepSourceFlow) step;
			IndexedNodeI source;

			// doesn't really matter, both retun the source ...
			if (ssf.getForward()) {
				source = ssf.getStartNode().getRealNode();
			} else {
				source = ssf.getArrivalNode().getRealNode();
			}

			// do not adjust the demands! This is only safe at the very first and last step!
//			Integer demand = this._demands.get(source);
//
//			if (demand == null) {
//				throw new IllegalArgumentException("Startnode is no source for StepSourceFlow " + step);
//			}

			if (ssf.getForward()) {
//				demand -= gamma;
//				if (demand < 0) {
//					throw new IllegalArgumentException("too much flow on StepSourceFlow " + step);
//				}
				this._sourceoutflow[source.getIndex()].augment(ssf.getArrivalTime(), gamma, Integer.MAX_VALUE);
//				this._demands.put(source, demand);
			} else {
//				demand += gamma;
//				if (demand > this._settings.getDemand(source)) {
//					throw new IllegalArgumentException("too much flow sent back into source " + step);
//				}
				this._sourceoutflow[source.getIndex()].augment(ssf.getStartTime(), -gamma, Integer.MAX_VALUE);
//				this._demands.put(source, demand);
			}

		} else if (step instanceof StepSinkFlow) {
			StepSinkFlow ssf = (StepSinkFlow) step;
			IndexedNodeI sink;

			// doesn't really matter, both retun the sink ...
			if (ssf.getForward()) {
				sink = ssf.getArrivalNode().getRealNode();
			} else {
				sink = ssf.getStartNode().getRealNode();
			}

			// do not adjust the demands! This is only safe at the very first and last step!
//			Integer demand = this._demands.get(sink);
//
//			if (demand == null) {
//				throw new IllegalArgumentException("Startnode is no sink for StepSinkFlow " + step);
//			}

			if (!step.getForward()) {
//				demand -= gamma;
//				this._demands.put(sink, demand);
				// there isn't any upper capacity on this link
				this._sinkflow[sink.getIndex()].augment(step.getArrivalTime(), -gamma, Integer.MAX_VALUE);
			} else {
//				demand += gamma;
//				if (demand > 0) {
//					throw new IllegalArgumentException("too much flow sent into sink " + step);
//				}
//				this._demands.put(sink, demand);
				// there isn't any upper capacity on this link
				this._sinkflow[sink.getIndex()].augment(step.getStartTime(), gamma, Integer.MAX_VALUE);
			}
		} else if (step instanceof StepHold) {
			StepHold hold = (StepHold) step;
			IndexedNodeI node = hold.getStartNode().getRealNode();
			
			int starttime = hold.getStartTime();
			int stoptime = hold.getArrivalTime();
			
			HoldoverIntervals intervals = (HoldoverIntervals) this._holdover[node.getIndex()];
			if (hold.getForward()) {
				intervals.augment(starttime, stoptime , gamma);
			} else {
				intervals.augment(stoptime, starttime, -gamma);
			}
		} else {
		
			throw new RuntimeException("Unsupported kind of PathStep!");
		}

	}

	/**
	 * Method to check the flow on the edge associated with step
	 * @param step The Step to check.
	 * @return true iff 0 <= flow <= capacity at time associated with step
	 */

	@SuppressWarnings("unused")
	private boolean checkStep(PathStep step) {

		// FIXME really bad style ...
		if (step instanceof StepEdge) {
			StepEdge se = (StepEdge) step;
			IndexedLinkI edge = se.getEdge();

			if(se.getForward()){
				return this._flow[edge.getIndex()].checkFlowAt(se.getStartTime(), this._settings.getCapacity(edge));

			}	else {
				return this._flow[edge.getIndex()].checkFlowAt(se.getArrivalTime(), this._settings.getCapacity(edge));
			}
		} else if (step instanceof StepSourceFlow) {
			StepSourceFlow ssf = (StepSourceFlow) step;
			IndexedNodeI source;

			if (ssf.getForward()) {
				source = ssf.getStartNode().getRealNode();
			} else {
				source = ssf.getArrivalNode().getRealNode();
			}

			Integer demand = this._demands[source.getIndex()];

			if (demand == null) {
				throw new IllegalArgumentException("Startnode is no source four StepSourceFlow " + step);
			}

			if (demand < 0 || demand > this._settings.getDemand(source)) {
				return false;
			    //throw new RuntimeException("Demand of source is negative or too large!");
			}

			if (ssf.getForward()) {
				return this._sourceoutflow[source.getIndex()].getIntervalAt(ssf.getArrivalTime()).checkFlow(Integer.MAX_VALUE);
			} else {
				return this._sourceoutflow[source.getIndex()].getIntervalAt(ssf.getStartTime()).checkFlow(Integer.MAX_VALUE);
			}

		} else {
			// TODO check sink steps!
			throw new RuntimeException("Unsupported kind of PathStep!");
		}
	}

	/**
	 * Debug Method to compare the stored collection of TEPs against the stored flow values.
	 * Probably quite badly implemented.
	 */
	public boolean checkTEPsAgainstFlow() {
		boolean everythingOkay = true;
		Flow tempflow = new Flow(this._settings);

		for (TimeExpandedPath TEP : this._TimeExpandedPaths) {
			tempflow.dumbaugment(TEP, TEP.getFlow());
		}

		tempflow.cleanUp();

		for (IndexedNodeI source : this._sources) {
			boolean thisisbad = false;

			int timeToStopChecking;

			if (this._sourceoutflow[source.getIndex()].getLast().getFlow() == 0) {
				timeToStopChecking = this._sourceoutflow[source.getIndex()].getLast().getLowBound();
			} else {
				timeToStopChecking = this._sourceoutflow[source.getIndex()].getLast().getHighBound();
			}

			if (tempflow._sourceoutflow[source.getIndex()].getLast().getFlow() == 0) {
				timeToStopChecking = Math.max(timeToStopChecking, tempflow._sourceoutflow[source.getIndex()].getLast().getLowBound());
			} else {
				timeToStopChecking = Math.max(timeToStopChecking, tempflow._sourceoutflow[source.getIndex()].getLast().getHighBound());
			}

			if (timeToStopChecking > this._timeHorizon) {
				System.out.println("Weird. There is flow beyond the TimeHorizon!");
				thisisbad = true;
			}

			for (int t = 0; t < timeToStopChecking; t++) {
				if (tempflow._sourceoutflow[source.getIndex()].getFlowAt(t) != this._sourceoutflow[source.getIndex()].getFlowAt(t)) {
					thisisbad = true;
					System.out.println("Flows differ on source: " + source.getId());
					break;
				}
			}
			if (thisisbad) {
				System.out.println("Original flow believes: ");
				System.out.println(this._sourceoutflow[source.getIndex()]);
				System.out.println("Reconstructed flow believes: ");
				System.out.println(tempflow._sourceoutflow[source.getIndex()]);
				everythingOkay = false;
			}
		}

		// TODO check sink labels

		for (IndexedLinkI edge : this._network.getLinks()) {
			//System.out.println("Checking edge " + edge.getId() + " ...");
			boolean thisisbad = false;

			int timeToStopChecking;


			timeToStopChecking = this._flow[edge.getIndex()].getLastTime();

			timeToStopChecking = Math.max(timeToStopChecking, tempflow._flow[edge.getIndex()].getLastTime());

			if (timeToStopChecking > this._timeHorizon) {
				System.out.println("Weird. There is flow beyond the TimeHorizon!");
				thisisbad = true;
			}

			for (int t = 0; t < timeToStopChecking; t++) {
				if (tempflow._flow[edge.getIndex()].getFlowAt(t) != this._flow[edge.getIndex()].getFlowAt(t)) {
					thisisbad = true;
					System.out.println("Flows differ on edge: " + edge.getId() + " " + edge.getFromNode().getId() + "-->" + edge.getToNode().getId());
					break;
				}
			}
			if (thisisbad) {
				System.out.println("Original flow believes: ");
				System.out.println(this._flow[edge.getIndex()]);
				System.out.println("Reconstructed flow believes: ");
				System.out.println(tempflow._flow[edge.getIndex()]);
				everythingOkay = false;
			}
		}

		/*if (!everythingOkay) {
			throw new RuntimeException("Flows and stored TEPs disagree!");
		}*/

		return everythingOkay;
	}


	/**
	 * Method to change just the flow values and source/sink demands according to TimeExpandedPath and gamma
	 * @param TEP The TimeExpandedPath on which the flow is augmented
	 * @param gamma Amount of flow to augment
	 */

	private void dumbaugment(TimeExpandedPath TEP, int gamma) {
		// We need to augment the demands at the beginning and at the end manually,
		// because otherwise they might be temporarily exceeded if flow passes through a
		// empty source or full sink!

		// the Source
		{
			StepSourceFlow ssf = (StepSourceFlow) TEP.getPathSteps().getFirst();

			IndexedNodeI source;

			// doesn't really matter, both retun the source ...
			if (ssf.getForward()) {
				source = ssf.getStartNode().getRealNode();
			} else {
				source = ssf.getArrivalNode().getRealNode();
			}

			Integer demand = this._demands[source.getIndex()];

			if (demand == null) {
				throw new IllegalArgumentException("Startnode is no source for StepSourceFlow " + ssf);
			}

			if (ssf.getForward()) {
				demand -= gamma;
				if (demand < 0) {
					throw new IllegalArgumentException("too much flow on StepSourceFlow " + ssf);
				}
			} else {
				demand += gamma;
				if (demand > this._settings.getDemand(source)) {
					throw new IllegalArgumentException("too much flow sent back into source " + ssf);
				}
			}
			this._demands[source.getIndex()] = demand;

		}

		// the Sink
		{
			StepSinkFlow ssf = (StepSinkFlow) TEP.getPathSteps().getLast();
			IndexedNodeI sink;

			// doesn't really matter, both retun the sink ...
			if (ssf.getForward()) {
				sink = ssf.getArrivalNode().getRealNode();
			} else {
				sink = ssf.getStartNode().getRealNode();
			}
			Integer demand = this._demands[sink.getIndex()];

			if (demand == null) {
				throw new IllegalArgumentException("Startnode is no sink for StepSinkFlow " + ssf);
			}

			if (!ssf.getForward()) {
				demand -= gamma;
			} else {
				demand += gamma;
				if (demand > 0) {
					throw new IllegalArgumentException("too much flow sent into sink " + ssf);
				}
			}
			this._demands[sink.getIndex()] = demand;
		}

		// Now do all the steps, including the first and last to fix the actual source/sinkflows.

		for (PathStep step : TEP.getPathSteps()) {
			augmentStep(step, gamma);
		}

	}


	/**
	 * Method to resolve opposing edges in a TEP
	 * It is assumed that the edges of an opposing pair only occur once in the path!
	 * (other edges can occur more than once)
	 * This does not adjust the flow!
	 * @param TEP The TimeExpandedPath on which loops should be removed. It is no longer to be used afterwards!
	 * @return a TimeExpandedPath without loops, but it might be new or the original TEP
	 */
	public TimeExpandedPath removeOpposing(TimeExpandedPath TEP) {

		TimeExpandedPath newTEP = new TimeExpandedPath();
		newTEP.setFlow(TEP.getFlow());

		Iterator<PathStep> iter, search;

		iter = TEP.getPathSteps().listIterator();

		boolean didsomething = false;

		while (iter.hasNext()) {
			PathStep step = iter.next();

			// is there an opposing step?
			// if so, we continue strictly after it
			search = TEP.getPathSteps().listIterator();

			//System.out.println("Looking for opposing of " + step);
			while (search.hasNext()) {
				PathStep other = search.next();
				// do they oppose each other?
				if (step.getForward() != other.getForward() &&  step.equalsNoCheckForward(other)) {
					if (_debug > 0) {
						System.out.println("Found opposing!");
						System.out.println("Original step: " + step);
					}
					// jump to the step after this.
					iter = search;
					if (iter.hasNext()) {
					  step = iter.next();
					} else {
					  step = null;
					}
					didsomething = true;
					if (_debug > 0) {
						System.out.println("Opposing step: " + other);
					}
					break;
				}
			}

			if (step != null) { // this only happens if the opposing step was the very last step
			   newTEP.append(step);
			}
		}

		if (_debug > 0) {
			if (didsomething) {
				System.out.println("Before removeOpposing: ");
				System.out.println(TEP);
				System.out.println("After removeOpposing: ");
				System.out.println(newTEP);
			}
		}
		return newTEP;
	}


	/**
	 * Method to remove loops in a TEP that does not contain opposing edges!
	 * (opposing edges would be removed as well, but that might cause havoc!)
	 * This does not adjust the flow!
	 * @param TEP The TimeExpandedPath on which loops should be removed. It is no longer to be used afterwards!
	 * @return a TimeExpandedPath without loops, but it might be new or the original TEP
	 */

	public TimeExpandedPath unloopNoOpposing(TimeExpandedPath TEP) {
		/* check for loops in the path!
		 * any time-expanded node visited twice qualifies!
		 */

		boolean hadtofixloop;
		do {
			PathStep startLoop = null, endLoop = null;
			hadtofixloop = false;

			for (PathStep step1 : TEP.getPathSteps()) {
				startLoop = step1;
				boolean afterwards = false;
				// TODO just start searching at step1 ... would be much nicer
				for (PathStep step2 : TEP.getPathSteps()) {
					if (step1 == step2) { // not step.equals(step2), that would be counterproductive
						afterwards = true;
						continue;
					} else if (afterwards) {
						if (step1.getStartNode().equals(step2.getStartNode())) {
							// loop!
							//System.out.println("Loop! " + step1 + " and " + step2);
							endLoop = step2;
						}
					}
				}
				if (endLoop != null) {
					// This is the loop starting the earliest.
					// Among those, it is the longest.
					// But further loops could happen afterwards.
					break;
				}
			}

			if (endLoop != null) {
				hadtofixloop = true;

				if (_debug > 0) {
					System.out.println("with loops: " + TEP);
				}
				TimeExpandedPath newTEP = new TimeExpandedPath();
				newTEP.setFlow(TEP.getFlow());
				boolean inloop = false;
				for (PathStep step : TEP.getPathSteps()) {
					if (step == startLoop) {
						inloop = true;
					} else if (step == endLoop) {
						inloop = false;
					}
					if (!inloop) {
						newTEP.append(step);
					}
				}

				if (_debug > 0) {
					System.out.println("checking path with less loops: " + newTEP);
					if (!newTEP.check()) {
						System.out.println("Bad unlooped new TEP!");
						throw new RuntimeException("Bad unlooped new TEP!");
					}
				}

				TEP = newTEP;
			}
		} while (hadtofixloop); // maybe we have to fix multiple loops, so try again.

		return TEP;
	}


	/**
	 * Method to add a TEP to the stored TEP collection and adjust the flow according to TEP
	 * All TEPs will be unlooped and unfolded, and the flow is augmented accordingly to TEP.getFlow()
	 * @param TEPtoAdd The TimeExpandedPath to add.
	 */
	private void unfoldandaugment(TimeExpandedPath TEPtoAdd){
		if(this._settings.mapLinksToTEP&& _debug>0){
			if(checkPathMap(true)){
				//mapAllPaths();
			}
		}
		// this function could be quite slow ...
		/*
		 * This is built on the following assumptions:
		 * Paths in flow._timeexpandedpaths are always good, i.e. use just forward steps.
		 * Paths in flow._timeexpandedpaths have no loops.
		 * The flow on the edges is always equal to the sum of flow._timeexpandedpath.
		 * However, TEPtoAdd and all other unprocessed paths are not counted,
		 * neither are any goodpaths not yet added to flow._timeexpandedpath.
		 *
		 * I.e., this function always keeps the flow and the TEP collection in sync!
		 */

		// TEPs that need processing
		LinkedList<TimeExpandedPath> unfinishedTEPs = new LinkedList<TimeExpandedPath>();
		
		// BIG DEBUG FIXME
		// only augment a single flow unit, but multiple times
		/* if (_debug > 0) {
			while (TEPtoAdd.getFlow() > 1) {
				TimeExpandedPath newTEP = new TimeExpandedPath(TEPtoAdd);
				newTEP.setFlow(1);
				TEPtoAdd.setFlow(TEPtoAdd.getFlow() - 1);
			}
		}*/
		
		unfinishedTEPs.add(TEPtoAdd);

		if (_debug > 0) {
			System.out.println("Starting unfoldandaugment() ...");
		}

		// processed TEPs that we want to add later on
		LinkedList<TimeExpandedPath> goodTEPs = new LinkedList<TimeExpandedPath>();

		// processed TEPs that now have 0 flow and we want to remove them later
		LinkedList<TimeExpandedPath> zeroTEPs = new LinkedList<TimeExpandedPath>();

		while (!unfinishedTEPs.isEmpty()) {
			TimeExpandedPath TEP = unfinishedTEPs.poll();

			if (_debug > 0) {
			  System.out.println("Unfolding: " + TEP);
			}

			if (_debug > 0) {
               System.out.println("Edges etc along the path: ");
               for (PathStep step : TEP.getPathSteps()) {
            	   if (step instanceof StepEdge) {
 					  System.out.println("Flow on edge for: " + step);
 					  System.out.println(this._flow[((StepEdge) step).getEdge().getIndex()]);
 					} else if (step instanceof StepSourceFlow) {
 						System.out.println("Flow for source: " + step);
   					    System.out.println(this._sourceoutflow[((StepSourceFlow) step).getStartNode().getRealNode().getIndex()]);
 					} else if (step instanceof StepSinkFlow) {
 						System.out.println("Step sink flow " + step + " ...");
 					} else {
 						System.out.println("Unknown kind of PathStep! " + step);
 					}
               }
			}


		    if (_debug > 0) {
				if (!TEP.check()) {
					System.out.println("Bad unprocessed TEP!");
					System.out.println(TEP);
					throw new RuntimeException("Bad unprocessed TEP!");
				}
			}

			/* remove loops
			 */
		    //TODO may need remapping
		    this.Topposing.onoff();
			TEP = removeOpposing(TEP);
			this.Topposing.onoff();
			this.Tunloop.onoff();
			TEP = unloopNoOpposing(TEP);
			this.Tunloop.onoff();
			boolean onlyForward = true;

			// traverse in order
			// be careful not to rearrange it and expect things to continue
			for (PathStep step : TEP.getPathSteps()) {
			   if (!step.getForward()) {
				   onlyForward = false;
				   if (_debug > 0) {
				     System.out.println("Found backwards step: " + step);
				   }

				   int flowToUndo = TEP.getFlow();

				   if (flowToUndo == 0) {
					   System.out.println("flowToUndo == 0");
					   System.out.println("processing path: " + TEP);
					   System.out.println("good paths:");
					   for (TimeExpandedPath tempTEP : goodTEPs) {
							System.out.println(tempTEP);
						}
					   System.out.println("unfinished paths:");
					   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
							System.out.println(tempTEP);
						}
					   throw new RuntimeException("flowToUndo == 0");
				   }
				   LinkedList<TimeExpandedPath> unfoldPaths= findReversePath(step);
				   Tunfold.onoff();
  				   // search for an appropriate TEP to unfold with
				   for (TimeExpandedPath otherTEP : unfoldPaths) {

					   // FIXME this seems to slow down rather than speed up the augmenting!
					   // That is somewhat surprising, though. Needs more testing.
					   if (this._checkTouchedNodes) {
						   if (!otherTEP.doesTouch(step.getStartNode()) || !otherTEP.doesTouch(step.getArrivalNode())) {
							   // this path cannot contain the reverse of step
							   //System.out.println("Skipped checking a path!");
							   continue;
						   }
					   }

 					   // DEBUG
					   if (otherTEP.getFlow() == 0) {
						   throw new RuntimeException("OtherTEP.getFlow() == 0 even earlier");
					   }


					   for (PathStep otherStep : otherTEP.getPathSteps()) {
						   if (step.isResidualVersionOf(otherStep)) {

							   if (_debug > 0) {
							     System.out.println("Found step to unfold with: " + otherStep);
							     System.out.println("It's in otherTEP: " + otherTEP);
							   }

 							   // must do this everytime so we get true fresh copies of myHead and myTail!
							   // note that splitPathAtStep splits at the first possible point.
							   LinkedList<TimeExpandedPath> myParts = TEP.splitPathAtStep(step, true);
							   TimeExpandedPath myHead = myParts.getFirst();
							   TimeExpandedPath myTail = myParts.getLast();

							   // split the found path
							   // since otherTEP is a good path, it only has forward edges
							   // we make a safe check anyway
							   LinkedList<TimeExpandedPath> otherParts = otherTEP.splitPathAtStep(otherStep, true);
							   TimeExpandedPath otherHead = otherParts.getFirst();
							   TimeExpandedPath otherTail = otherParts.getLast();

							   int augment = Math.min(flowToUndo, otherTEP.getFlow());

							   if (_debug > 0) {
							     System.out.println("myHead " + myHead);
							     System.out.println("myTail " + myTail);
							     System.out.println("otherHead " + otherHead);
							     System.out.println("otherTail " + otherTail);
							   }

							   if (otherTEP.getFlow() == 0) {
								   System.out.println("OtherTEP.getFlow() == 0");
								   System.out.println("processing path: " + TEP);
								   System.out.println("other path: " + otherTEP);
								   System.out.println("good paths:");
								   for (TimeExpandedPath tempTEP : goodTEPs) {
										System.out.println(tempTEP);
									}
								   System.out.println("unfinished paths:");
								   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
										System.out.println(tempTEP);
									}
								   throw new RuntimeException("OtherTEP.getFlow() == 0");
							   }



							   myHead.addTailToPath(otherTail);
							   myHead.setFlow(augment);
							   goodTEPs.add(myHead);

							   otherHead.addTailToPath(myTail);
							   otherHead.setFlow(augment);							   
							   //  process things in DFS, not BFS order. (Can bug with BFS.)
							   // BIG DEBUG
							   // only do DFS if DEBUG is set
							   //if (_debug > 0) {
							     unfinishedTEPs.addFirst(otherHead); // DFS, good
							   //} else {
								// unfinishedTEPs.addLast(otherHead); // BFS
							   //}

							   // adjust the flow on the edges and the flow on the (still) stored TEP
							   dumbaugment(otherTEP, -augment);
							   otherTEP.setFlow(otherTEP.getFlow() - augment);

							   flowToUndo -= augment;

							   if (_debug > 0) {
							     System.out.println("What's left of otherTEP: ");
							     System.out.println(otherTEP);
							   }

							   // flow might be zero, should be deleted!
							   if (otherTEP.getFlow() == 0) {
								   if(!zeroTEPs.contains(otherTEP)){
									   zeroTEPs.add(otherTEP);
								   }
							   }

							   // We found the forward step we were looking for.
							   // Now look for another TEP to unfold with.
							   break;
						   }
					   }

					   if (flowToUndo == 0) break;
				   }
				   Tunfold.onoff();

				   
				   if (flowToUndo > 0) {
					   
					   
					   System.out.println("problem path: " + TEP);
					   System.out.println("good paths:");
					   for (TimeExpandedPath tempTEP : goodTEPs) {
							System.out.println(tempTEP);
						}
					   System.out.println("unfinished paths:");
					   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
							System.out.println(tempTEP);
					   }
					   System.out.println("zero paths:");
					   for (TimeExpandedPath tempTEP : zeroTEPs) {
							System.out.println(tempTEP);
					   }
					   /*System.out.println("All paths so far:");
						for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
							System.out.println(tempTEP);
						}*/

					   throw new RuntimeException("Could not undo all flow on backwards step!");
				   }

				   // this TEP is broken up ... don't bother with it anymore
				   break;
			   }
			}

			// was everything alright?
			if (onlyForward) {
				goodTEPs.add(TEP);
			}

			// deleting paths with 0 flow does not change the flow
			
			if(this._settings.mapLinksToTEP){
				  unMapPaths(zeroTEPs);
			}
			this._TimeExpandedPaths.removeAll(zeroTEPs);
			
			zeroTEPs.clear();

			/*// DEBUG
			for (TimeExpandedPath tempTEP : goodTEPs) {
			  if (tempTEP.getFlow() == 0) {
				 System.out.println("Bad goodPath.getFlow() == 0... " + tempTEP);
				 throw new RuntimeException("Bad goodPath.getFlow() == 0");
			  }
			}*/

			// add the good flows:
			// check for loops and adjust the flow
			for (TimeExpandedPath good : goodTEPs) {
				this.Tunloop.onoff();
				good = unloopNoOpposing(good);
				this.Tunloop.onoff();
				// paths are probably (it's a for loop) read from the beginning again
				// addFirst is better than addLast: interact with recently added TEPs
				this._TimeExpandedPaths.addFirst(good);
				dumbaugment(good, good.getFlow());

				if(this._settings.mapLinksToTEP){
					mapPath(good);
				}
			}
			goodTEPs.clear();

			/*if (_debug > 0) {
				System.out.println("All paths so far:");
				for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
					System.out.println(tempTEP);
				}
			}*/

			// BIG DEBUG
			if (_debug > 0) {
				System.out.println("Checking consistency at end of UnfoldAndAugment");
				if (!this.checkTEPsAgainstFlow()) {
					System.out.println("calling path: " + TEPtoAdd);
					System.out.println("last processed path: " + TEP);

					/*System.out.println("All paths so far:");
				for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
					System.out.println(tempTEP);
				}*/

					throw new RuntimeException("Flow and stored TEPs disagree!");
				}
			}
		}
	}
	
	private LinkedList<TimeExpandedPath> findReversePath(PathStep step){
		if(!this._settings.mapLinksToTEP){
			return this._TimeExpandedPaths;
		}
		TreeMap<Integer,LinkedList<TimeExpandedPath>> tree=null;
		int time=-1;
		if (step instanceof StepEdge){
			StepEdge edge =(StepEdge) step;
			time = edge.getArrivalTime();
			Link link = edge.getEdge().getMatsimLink();
			tree =this._edgePathMap.get(link);
		}else if(step instanceof StepSourceFlow){
			StepSourceFlow sourcestep = (StepSourceFlow)step;
			Node source =sourcestep.getArrivalNode().getRealNode().getMatsimNode();
			time = sourcestep.getStartTime();
			tree =this._sourcePathMap.get(source);
		}else if(step instanceof StepSinkFlow){
			StepSinkFlow sinkstep = (StepSinkFlow)step;
			Node sink =sinkstep.getArrivalNode().getRealNode().getMatsimNode();
			time = sinkstep.getArrivalTime();
			tree =this._sinkPathMap.get(sink);
		}
		//System.out.println("timttimetime: "+ time);
		return tree.get(time);
	}
	private boolean checkPathMap(boolean complete){
		
	
		return pathsAreMapped(complete)&& mappedPathsExist(complete);
	}
	
	
	private boolean mappedPathsExist(boolean complete){
		boolean error = false;
		for(IndexedLinkI iedge : _network.getLinks()){
			Link edge = iedge.getMatsimLink();
			for(LinkedList<TimeExpandedPath> list : this._edgePathMap.get(edge).values()){
				for(TimeExpandedPath path :list){
					if(!this._TimeExpandedPaths.contains(path)){
						System.out.println("WARNING  Path still mapped: \n"+path.toString());
						error=true;
					}else{
						if (_debug>0){
							System.out.println("exists");
						}
					}
				}
			}
		}
		for(Node node : this._sourcePathMap.keySet()){
			for(LinkedList<TimeExpandedPath> list : this._sourcePathMap.get(node).values()){
				for(TimeExpandedPath path :list){
					if(!this._TimeExpandedPaths.contains(path)){
						System.out.println("WARNING  Path still mapped: \n"+path.toString());
						error=true;
					}else{
						if (_debug>0){
							System.out.println("exists");
						}
					}
				}
			}
		}for(Node node : this._sinkPathMap.keySet()){
			for(LinkedList<TimeExpandedPath> list : this._sinkPathMap.get(node).values()){
				for(TimeExpandedPath path :list){
					if(!this._TimeExpandedPaths.contains(path)){
						System.out.println("WARNING  Path still mapped: \n"+path.toString());
						error=true;
					}else{
						if (_debug>0){
							System.out.println("exists");
						}
					}
				}
			}
		}
		return !error;
	}
	
	
	
	private boolean pathsAreMapped(boolean complete){
		System.out.println( "begin pathmapcheck");
		boolean error=false;
		//check if all paths are mapped 
		for(TimeExpandedPath path : this._TimeExpandedPaths)
			for (PathStep step : path.getPathSteps()){
				TreeMap<Integer,LinkedList<TimeExpandedPath>> tree=null;
				int time=-1;
				if (step instanceof StepEdge){
					StepEdge edge =(StepEdge) step;
					time = edge.getStartTime();
					Link link = edge.getEdge().getMatsimLink();
					tree =this._edgePathMap.get(link);
				}else if(step instanceof StepSourceFlow){
					StepSourceFlow sourcestep = (StepSourceFlow)step;
					Node source =sourcestep.getArrivalNode().getRealNode().getMatsimNode();
					time = sourcestep.getArrivalTime();
					tree =this._sourcePathMap.get(source);
				}else if(step instanceof StepSinkFlow){
					StepSinkFlow sinkstep = (StepSinkFlow)step;
					Node sink =sinkstep.getArrivalNode().getRealNode().getMatsimNode();
					time = sinkstep.getStartTime();
					tree =this._sinkPathMap.get(sink);
				}
				if(tree==null){
					System.out.println("no tree on"+ step.toString());
					if(!complete){
						return false;
					}
					error=true;
				}
				LinkedList<TimeExpandedPath> list =tree.get(time);
				if(list==null){
					System.out.println("no list for: "+step.toString() +" at time :" +time);
					if(!complete){
						return false;
					}
					error=true;
				}
				if(!list.contains(path)){
					System.out.println("Path not set on: "+step.toString()+" at time :" +time+ "\n Path: "+ path.toString());
					if(!complete){
						return false;
					}
					error=true;
				}
			}
		
		System.out.println("end pathmapcheck");
		return !error;
	}
	
	@SuppressWarnings("unused")
	private void mapAllPaths(){
		for(TimeExpandedPath path : this._TimeExpandedPaths){
			mapPath(path);
		}
	}

	private void mapPath(TimeExpandedPath path){
		for(PathStep step : path.getPathSteps()){
			TreeMap<Integer,LinkedList<TimeExpandedPath>> tree=null;
			int time=-1;
			if (step instanceof StepEdge){
				StepEdge edge =(StepEdge) step;
				time = edge.getStartTime();
				Link link = edge.getEdge().getMatsimLink();
				tree =this._edgePathMap.get(link);
				if(tree == null){
					tree = new TreeMap<Integer,LinkedList<TimeExpandedPath>>();
					this._edgePathMap.put(link, tree);
				}
			}else if(step instanceof StepSourceFlow){
				StepSourceFlow sourcestep = (StepSourceFlow)step;
				Node source =sourcestep.getArrivalNode().getRealNode().getMatsimNode();
				time = sourcestep.getArrivalTime();
				tree =this._sourcePathMap.get(source);
				if(tree == null){
					tree = new TreeMap<Integer,LinkedList<TimeExpandedPath>>();
					this._sourcePathMap.put(source, tree);
				}
			}else if(step instanceof StepSinkFlow){
				StepSinkFlow sinkstep = (StepSinkFlow)step;
				Node sink =sinkstep.getArrivalNode().getRealNode().getMatsimNode();
				time = sinkstep.getStartTime();
				tree =this._sinkPathMap.get(sink);
				if(tree == null){
					tree = new TreeMap<Integer,LinkedList<TimeExpandedPath>>();
					this._sinkPathMap.put(sink, tree);
				}
			}
			LinkedList<TimeExpandedPath> list =tree.get(time);
			if(list==null){
				list = new LinkedList<TimeExpandedPath>();
				tree.put(time, list);
			}
			if(!list.contains(path)){
				list.addFirst(path);
			}
		}
	}
	
	private void unMapPaths(LinkedList<TimeExpandedPath> paths){
		for (TimeExpandedPath path : paths){
			for (PathStep step : path.getPathSteps()){
				int time = -1;
				TreeMap<Integer,LinkedList<TimeExpandedPath>> tree =null;
				if (step instanceof StepEdge){
					StepEdge edge =(StepEdge) step;
					time = edge.getStartTime();
					Link link = edge.getEdge().getMatsimLink();
					tree =this._edgePathMap.get(link);
				}else if(step instanceof StepSourceFlow){
					StepSourceFlow sourcestep = (StepSourceFlow)step;
					Node source = sourcestep.getArrivalNode().getRealNode().getMatsimNode();
					time = sourcestep.getArrivalTime();
					tree =this._sourcePathMap.get(source);
				}else if(step instanceof StepSinkFlow){
					StepSinkFlow sinkstep = (StepSinkFlow)step;
					Node sink =sinkstep.getArrivalNode().getRealNode().getMatsimNode();
					time = sinkstep.getStartTime();
					tree =this._sinkPathMap.get(sink);
				}
				if(tree == null){
					System.out.println("NO PATH TREE!!!!!");
				}else{
					LinkedList<TimeExpandedPath> list =tree.get(time);
					if(list==null){
						System.out.println("NO LIST WHERE IT SOULD BE!!!!!!");
					}else{
						if(!list.remove(path)){
							if(this._TimeExpandedPaths.contains(path)){
								System.out.println("NO PATH ENTRY!!!!!!");
							}
						}else if(_debug>0){System.out.println("found: \n"+path);}
					}
				}
				
			}
		}
	}

	/**
	 * method to resolve residual edges in TEP
	 * @param TEP A TimeExpandedPath
	 */

	/*private void unfold(TimeExpandedPath TEPtoAdd){
		// FIXME this is broken! Do not use!
		System.out.println("flow.unfold() is broken. Do not use it!");

		// this function could be quite slow ...

		 * Paths in flow._timeexpandedpaths are always good, i.e. use just forward steps.
		 * The current path may contain loops! (Not from the BellmanFord,
		 *   but from unfolding with other paths.) These must be removed first.
		 * Other unprocessed paths can also contain loops.
		 * Unprocessed paths cannot contain forward steps for residual steps from the
		 * current path (or any other unprocessed path), because that would imply a loop
		 * in some path that caused these unprocessed paths. (use induction)
		 *
		 * FIXME removing loops changes the flow on the edges!
		 * make sure that this function always keeps the flow and the TEP collection in sync!


		// TEPs that need processing
		LinkedList<TimeExpandedPath> unfinishedTEPs = new LinkedList<TimeExpandedPath>();
		unfinishedTEPs.add(TEPtoAdd);

		// processed TEPs that we want to add later on
		LinkedList<TimeExpandedPath> goodTEPs = new LinkedList<TimeExpandedPath>();

		// processed TEPs that now have 0 flow and we want to remove them later
		LinkedList<TimeExpandedPath> zeroTEPs = new LinkedList<TimeExpandedPath>();

		ArrayList<PathStep> wasinvolvedinloop = new ArrayList<PathStep>();

		while (!unfinishedTEPs.isEmpty()) {
			TimeExpandedPath TEP = unfinishedTEPs.poll();
			TimeExpandedPath TEPtoProcess = TEP; // DEBUG

			if (_debug > 0) {
			  System.out.println("Unfolding: " + TEP);
			}

			if (_debug > 0) {
               System.out.println("Edges etc along the path: ");
               for (PathStep step : TEP.getPathSteps()) {
            	   if (step instanceof StepEdge) {
 					  System.out.println("Flow on edge for: " + step);
 					  System.out.println(this._flow.get(((StepEdge) step).getEdge()));
 					} else if (step instanceof StepSourceFlow) {
 						System.out.println("Flow for source: " + step);
   					    System.out.println(this._sourceoutflow.get(((StepSourceFlow) step).getStartNode().getRealNode()));
 					} else if (step instanceof StepSinkFlow) {
 						System.out.println("Step sink flow " + step + " ...");
 					} else {
 						System.out.println("Unknown kind of PathStep! " + step);
 					}
               }
			}

			// DEBUG
			//if (_debug > 0) {
				if (!TEP.check()) {
					System.out.println("Bad unprocessed TEP!");
					System.out.println(TEP);
					throw new RuntimeException("Bad unprocessed TEP!");
				}
			//}

			 check for loops in the path!
			 * these can be pairwise opposing residual edges
			 * or visiting the same time-expanded node twice
			 * be careful with sourceoutflow-nodes ...


			boolean hadtofixloop;
			do {
				PathStep startLoop = null, endLoop = null;
				hadtofixloop = false;

				for (PathStep step1 : TEP.getPathSteps()) {
					startLoop = step1;
					boolean afterwards = false;
					// TODO just start searching at step1 ... would be much nicer
					for (PathStep step2 : TEP.getPathSteps()) {
						if (step1 == step2) { // not step.equals(step2), that would be counterproductive
							afterwards = true;
							continue;
						} else if (afterwards) {
							if (step1.getStartNode().equals(step2.getStartNode())) {
								// loop!
								//System.out.println("Loop! " + step1 + " and " + step2);
								endLoop = step2;
							}
						}
					}
					if (endLoop != null) {
						// this is the loop starting the earliest and ending the latest!
						// but further loops could happen afterwards or
						break;
					}
				}

				if (endLoop != null) {
					hadtofixloop = true;

					if (_debug > 0) {
						System.out.println("with loops: " + TEP);
					}
					TimeExpandedPath newTEP = new TimeExpandedPath();
					newTEP.setFlow(TEP.getFlow());
					boolean inloop = false;
					for (PathStep step : TEP.getPathSteps()) {
						if (step == startLoop) {
							inloop = true;
						} else if (step == endLoop) {
							inloop = false;
						}
						if (!inloop) {
							newTEP.append(step);
						} else {
							 FIXME adjust flow on edges to be in sync with TEP collection!
						   However, removing the flow might temporarily exceed [0, u] boundaries
						   if the TEP could unfold with itself
							augmentStepUnsafe(step, -TEP.getFlow());
							wasinvolvedinloop.add(step);
						}
					}



					if (_debug > 0) {
						System.out.println("checking path with loops: " + TEP);
						if (!newTEP.check()) {
							System.out.println("Bad unlooped new TEP:");
							System.out.println(newTEP);
							throw new RuntimeException("Bad unlooped TEP!");
						}
					}

					TEP = newTEP;
				}
			} while (hadtofixloop); // maybe we have to fix multiple loops, so try again.


			boolean onlyForward = true;

			// traverse in order
			// be careful not to rearrange it and expect things to continue
			for (PathStep step : TEP.getPathSteps()) {
			   if (!step.getForward()) {
				   onlyForward = false;
				   if (_debug > 0) {
				     System.out.println("Found backwards step: " + step);
				   }

				   int flowToUndo = TEP.getFlow();

				   if (flowToUndo == 0) {
					   System.out.println("flowToUndo == 0");
					   System.out.println("processing path: " + TEP);
					   System.out.println("good paths:");
					   for (TimeExpandedPath tempTEP : goodTEPs) {
							System.out.println(tempTEP);
						}
					   System.out.println("unfinished paths:");
					   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
							System.out.println(tempTEP);
						}
					   throw new RuntimeException("flowToUndo == 0");
				   }

  				   // search for an appropriate TEP to unfold with
				   for (TimeExpandedPath otherTEP : this._TimeExpandedPaths) {

					   // FIXME this seems to slow down rather than speed up the augmenting!
					   // That is somewhat surprising, though. Needs more testing.
					   if (!otherTEP.doesTouch(step.getStartNode()) || !otherTEP.doesTouch(step.getArrivalNode())) {
						   // this path cannot contain the reverse of step
						   //System.out.println("Skipped checking a path!");
						   continue;
					   }

 					   // DEBUG
					   if (otherTEP.getFlow() == 0) {
						   throw new RuntimeException("OtherTEP.getFlow() == 0 even earlier");
					   }

					   if (flowToUndo == 0) break;
					   for (PathStep otherStep : otherTEP.getPathSteps()) {
						   if (step.isResidualVersionOf(otherStep)) {

							   // DEBUG
							   if (otherTEP.getFlow() == 0) {
								   throw new RuntimeException("OtherTEP.getFlow() == 0 already");
							   }

							   if (_debug > 0) {
							     System.out.println("Found step to unfold with: " + otherStep);
							     System.out.println("It's in otherTEP: " + otherTEP);
							   }

 							   // must do this everytime so we get true fresh copies of myHead and myTail!
							   // note that splitPathAtStep splits at the first possible point.
							   // since TEP should not unfold with itself (after the unlooping)
							   // this should find the right step ...
							   // FIXME ... but i have doubts ...
							   // could just use splitPathAtStep(step, true)
							   //LinkedList<TimeExpandedPath> myParts = TEP.splitPathAtStep(step, false);
							   LinkedList<TimeExpandedPath> myParts = TEP.splitPathAtStep(step, true);
							   TimeExpandedPath myHead = myParts.getFirst();
							   TimeExpandedPath myTail = myParts.getLast();

							   // split the found path
							   // since otherTEP is a good path, it only has forward edges ...
							   // but maybe, just maybe, it might contain loops because those are not removed
							   // before adding good paths
							   // that could have caused splitPathAtStep to fail.
 							   // could just use splitPathAtStep(otherstep, true)
							   //LinkedList<TimeExpandedPath> otherParts = otherTEP.splitPathAtStep(step, false);
							   LinkedList<TimeExpandedPath> otherParts = otherTEP.splitPathAtStep(otherStep, true);
							   TimeExpandedPath otherHead = otherParts.getFirst();
							   TimeExpandedPath otherTail = otherParts.getLast();

							   int augment = Math.min(flowToUndo, otherTEP.getFlow());

							   if (_debug > 0) {
							     System.out.println("myHead " + myHead);
							     System.out.println("myTail " + myTail);
							     System.out.println("otherHead " + otherHead);
							     System.out.println("otherTail " + otherTail);
							   }

							   if (otherTEP.getFlow() == 0) {
								   System.out.println("OtherTEP.getFlow() == 0");
								   System.out.println("processing path: " + TEP);
								   System.out.println("other path: " + otherTEP);
								   System.out.println("good paths:");
								   for (TimeExpandedPath tempTEP : goodTEPs) {
										System.out.println(tempTEP);
									}
								   System.out.println("unfinished paths:");
								   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
										System.out.println(tempTEP);
									}
								   throw new RuntimeException("OtherTEP.getFlow() == 0");
							   }

							   myHead.addTailToPath(otherTail);
							   myHead.setFlow(augment);
							   goodTEPs.add(myHead);

							   otherHead.addTailToPath(myTail);
							   otherHead.setFlow(augment);
							   unfinishedTEPs.add(otherHead);

							   otherTEP.setFlow(otherTEP.getFlow() - augment);

							   flowToUndo -= augment;

							   if (_debug > 0) {
							     System.out.println("What's left of otherTEP: ");
							     System.out.println(otherTEP);
							   }

							   // flow might be zero, should be deleted!
							   if (otherTEP.getFlow() == 0) {
							     zeroTEPs.add(otherTEP);
							   }

							   // we found the forward step we were looking for
							   // look for another TEP to unfold with
							   break;
						   }
					   }
				   }

				   if (flowToUndo > 0) {
					   System.out.println("problem path: " + TEP);
					   System.out.println("good paths:");
					   for (TimeExpandedPath tempTEP : goodTEPs) {
							System.out.println(tempTEP);
						}
					   System.out.println("unfinished paths:");
					   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
							System.out.println(tempTEP);
					   }
					   System.out.println("zero paths:");
					   for (TimeExpandedPath tempTEP : zeroTEPs) {
							System.out.println(tempTEP);
					   }
					   System.out.println("All paths so far:");
						for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
							System.out.println(tempTEP);
						}

					   throw new RuntimeException("Could not undo all flow on backwards step!");
				   }

				   // this TEP is broken up ... don't bother with it anymore
				   break;
			   }
			}
			if (onlyForward) {
				goodTEPs.add(TEP);
			}

			// DEBUG
			int vorher = this._TimeExpandedPaths.size();
			int zero = zeroTEPs.size();

			this._TimeExpandedPaths.removeAll(zeroTEPs);
			zeroTEPs.clear();

			int nachher = this._TimeExpandedPaths.size();
			if (nachher + zero != vorher) {
				throw new RuntimeException("Did not delete all paths with zero flow!");
			}


			// DEBUG
			for (TimeExpandedPath tempTEP : goodTEPs) {
			  if (tempTEP.getFlow() == 0) {
				 System.out.println("Bad goodPath.getFlow() == 0... " + tempTEP);
				 throw new RuntimeException("Bad goodPath.getFlow() == 0");
			  }
			}

			this._TimeExpandedPaths.addAll(goodTEPs);
			goodTEPs.clear();

			if (_debug > 0) {
				System.out.println("All paths so far:");
				for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
					System.out.println(tempTEP);
				}
			}
		}

		// just to be sure
		// if (_debug > 0) {
		boolean somethingwrong = false;
		for (PathStep step : wasinvolvedinloop) {
			if (!checkStep(step)) {
				somethingwrong = true;
				System.out.println("Path to add: " + TEPtoAdd);
				System.out.println("good paths:");
				for (TimeExpandedPath tempTEP : goodTEPs) {
					System.out.println(tempTEP);
				}
				System.out.println("unfinished paths:");
				for (TimeExpandedPath tempTEP : unfinishedTEPs) {
					System.out.println(tempTEP);
				}

				System.out.println("All paths so far:");
				for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
					System.out.println(tempTEP);
				}

				System.out.println("with all loops: " + TEPtoProcess);
				//System.out.println("before removing the last loop: " + TEP);
				//System.out.println("Bad unlooped new TEP: " + newTEP);
				System.out.println("Bad unlooped new TEP: " + TEP);
				System.out.println("problem step: " + step);

				if (step instanceof StepEdge) {
				  System.out.println("Flow on edge of the problem step: ");
				  System.out.println(this._flow.get(((StepEdge) step).getEdge()));
				}


			}
		}
		if (somethingwrong) {
			throw new RuntimeException("Unsafe unlooping violated bounds on flow!");
		}

		// }

		// BIG DEBUG
		System.out.println("Checking consistency at end of Unfold");
		if (!this.checkTEPsAgainstFlow()) {
			System.out.println("problem path: " + TEPtoAdd);

			System.out.println("All paths so far:");
			for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
				System.out.println(tempTEP);
			}

			throw new RuntimeException("Flow and stored TEPs disagree!");
		}
	}*/



////////////////////////////////////////////////////////////////////////////////////
//-----------evaluation methods---------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////

	
	/**
	 * gives back an array containing the amount of flow into the sink for all time steps from 0 to time horizon
	 */
	public int[] arrivals(){
		int maxtime = 0;
		
		for (IndexedNodeI sink : this._sinks) {
		   SinkIntervals si = this._sinkflow[sink.getIndex()];
		   int t = 0;
		   if (si.getLast().getFlow() > 0) {
			   t = si.getLast().getHighBound() - 1;
		   } else {
			   t = si.getLast().getLowBound() - 1;
		   }
		   maxtime = Math.max(maxtime, t);
		}
		
		int[] arrivals = new int[maxtime + 1];

		for (IndexedNodeI sink : this._sinks) {
			SinkIntervals si = this._sinkflow[sink.getIndex()];
			for (int t = 0; t <= maxtime; t++) {
			   arrivals[t] += si.getFlowAt(t); 
			}
		}

		return arrivals;
	}

	/**
	 * gives back an array containing the total amount of flow into the sink by a given time
	 * for all time steps from 0 to time horizon
	 */
	public int[] arrivalPattern(){
		int[] result = this.arrivals();
		int sum = 0;
		for (int i=0;i<result.length; i++){
			sum+=result[i];
			result[i]=sum;
		}
		return result;
	}
	/**
	 * String representation of the arrivals specifying the amount of flow into the sink
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrivals
	 */
	public String arrivalsToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("  arrivals:");
		int[] a =this.arrivals();
		for (int i=0; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}

	/**
	 * a STring specifying the total amount of flow into the sink by a given time
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrival pattern
	 */
	public String arrivalPatternToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("arrival pattern:");
		int[] a =this.arrivalPattern();
		for (int i=0; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}

//////////////////////////////////////////////////////////////////////////////////////
//------------------- Clean Up---------------------------------------//
//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Call the cleanup-method for each edge
	 */
	public int cleanUp() {

		int gain = 0;
		
		for (EdgeFlowI EI : _flow) {
		  gain += EI.cleanup();
		}
		
		for (IndexedNodeI source : this._sources) {
			SourceIntervals si = this._sourceoutflow[source.getIndex()];
			 if (si != null)
			   gain += si.cleanup();
		}
		
		for (IndexedNodeI sink : this._sinks) {
			SinkIntervals si = this._sinkflow[sink.getIndex()];
			 if (si != null)
			   gain += si.cleanup();
		}
		return gain;
	}

//////////////////////////////////////////////////////////////////////////////////////
//-------------------Getters Setters toString---------------------------------------//
//////////////////////////////////////////////////////////////////////////////////////


	/**
	 * returns a String representation of the entire flows
	 */
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		for(IndexedLinkI link : _network.getLinks()){
			EdgeFlowI edge =_flow[link.getIndex()];
			strb.append(link.getId().toString()+ ": " + edge.toString()+ "\n");
		}
		return strb.toString();
	}


	/**
	 * returns a path representation of a flow as a String
	 */
	public String writePathflow(boolean forward) {
		StringBuilder sb = new StringBuilder();
		if(forward){
			for(TimeExpandedPath path :_TimeExpandedPaths){
				sb.append(path.print());
				sb.append("\n");
			}
		}else{
			ListIterator<TimeExpandedPath> iterator = this._TimeExpandedPaths.listIterator(this._TimeExpandedPaths.size());
			while(iterator.hasPrevious()){
				TimeExpandedPath path = iterator.previous();
				sb.append(path.print());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * @return the _demands
	 */
	public int[] getDemands() {
		return this._demands;
	}

	/**
	 * @return the _flow
	 */
	public EdgeFlowI getFlow(IndexedLinkI edge) {
		return this._flow[edge.getIndex()];
	}
	
	public EdgeFlowI getHoldover(IndexedNodeI node){
		return this._holdover[node.getIndex()];
	}
	//TODO holdover done implement getHoldover
	

	public SourceIntervals getSourceOutflow(IndexedNodeI node) {
		return this._sourceoutflow[node.getIndex()];
	}

	public SinkIntervals getSinkFlow(IndexedNodeI node) {
		return this._sinkflow[node.getIndex()];
	}

	/**
	 * @return the _sink
	 */
	public ArrayList<IndexedNodeI> getSinks() {
		return _sinks;
	}

	/**
	 * @return the _sources
	 */
	public ArrayList<IndexedNodeI> getSources() {
		return this._sources;
	}

	/**
	 * @return the network
	 */
	public IndexedNetworkI getNetwork() {
		return this._network;
	}

	/**
	 * @return the total flow so far
	 */
	public int getTotalFlow() {
		return this.totalflow;
	}

    /** @return the paths
	*/
	public LinkedList<TimeExpandedPath> getPaths() {
		return this._TimeExpandedPaths;
	}

	/**

	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		Flow._debug=debug;
	}

	public int getStartTime()
	{
		return 0;
	}

	public int getEndTime()
	{
		return this.arrivals().length-1;
	}

	public String measure() {
		String result;
		// statistics for EdgeIntervalls and SourceIntervalls
		// min, max, avg size
		int min = Integer.MAX_VALUE;
		int max = 0;
		long sum = 0;
		for (EdgeFlowI i : this._flow) {
			if (i == null) continue;
			int size = i.getMeasure();
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result = "  Size of EdgeIntervalls (min/avg/max): " + min + " / " + sum / (double) this._network.getNodes().size() + " / " + max + "\n";

		sum = 0;
		max = 0;
		min = Integer.MAX_VALUE;
		for (SourceIntervals i : this._sourceoutflow) {
			if (i == null) continue;
			int size = i.getSize();
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result += "  Size of SourceIntervalls (min/avg/max): " + min + " / " + sum / (double) this.getSources().size() + " / " + max + "\n";

		sum = 0;
		max = 0;
		min = Integer.MAX_VALUE;
		for (SinkIntervals i : this._sinkflow) {
			if (i == null) continue;
			int size = i.getSize();
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result += "  Size of SinkIntervalls (min/avg/max): " + min + " / " + sum / (double) this.getSinks().size() + " / " + max + "\n";

		return result;
	}

	/** @return newly constructed paths, but completely destroys this flow object!
	*/
	public LinkedList<TimeExpandedPath> doPathDecomposition() {
		// FIXME destroys the flow!
		Decomposer decomp = new Decomposer(this, _settings);
				
		this._TimeExpandedPaths = decomp.decompose(); 
		return this._TimeExpandedPaths;
	}
	
}