/* *********************************************************************** *
 * project: org.matsim.*
 * SignalControlDataConsistencyChecker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.data.consistency;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;

import playground.dgrether.designdrafts.consistency.ConsistencyChecker;


/**
 * @author dgrether
 *
 */
public class SignalControlDataConsistencyChecker implements ConsistencyChecker {
	
	private static final Logger log = Logger.getLogger(SignalControlDataConsistencyChecker.class);
	
	private SignalsData signalsData;

	public SignalControlDataConsistencyChecker(Scenario scenario) {
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

	}

	/**
	 * @see playground.dgrether.designdrafts.consistency.ConsistencyChecker#checkConsistency()
	 */
	@Override
	public void checkConsistency() {
		log.info("Checking consistency of SignalControlData...");
		this.checkSettingsToGroupMatching();
		log.info("Checked consistency of SignalControlData.");
	}

	private void checkSettingsToGroupMatching() {
		SignalGroupsData siganlGroupsData = this.signalsData.getSignalGroupsData();
		SignalControlData control = this.signalsData.getSignalControlData();
		
		for (SignalSystemControllerData  controller : control.getSignalSystemControllerDataBySystemId().values()) {
			Map<Id, SignalGroupData> signalGroups = siganlGroupsData.getSignalGroupDataBySystemId(controller.getSignalSystemId());
			if (null == signalGroups) {
				log.error("Error: No SignalGroups for SignalSystemController:");
				log.error("\t\tSignalGroups have no entry for SignalSystem Id: " + controller.getSignalSystemId() + " specified in " + 
				" the SignalControl");
			}

			for (SignalPlanData plan : controller.getSignalPlanData().values()) {
				for (SignalGroupSettingsData settings : plan.getSignalGroupSettingsDataByGroupId().values()) {
					if (! signalGroups.containsKey(settings.getSignalGroupId())){
						log.error("Error: No SignalGroup for SignalGroupSettings:");
						log.error("\t\t SignalGroupSettings have no entry for SignalGroup Id: " + settings.getSignalGroupId() + 
								" of SignalSystem Id: " + controller.getSignalSystemId() + " and SignalPlan Id: " + plan.getId());
					}
				}
			}
			
		}
		
	}
	
}