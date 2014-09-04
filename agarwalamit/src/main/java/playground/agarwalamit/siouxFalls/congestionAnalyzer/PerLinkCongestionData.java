/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.agarwalamit.siouxFalls.congestionAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.congestion.CongestionLinkAnalyzer;

/**
 * @author amit
 *
 */
public class PerLinkCongestionData {
	private Logger logger = Logger.getLogger(PerLinkCongestionData.class);
	private static String outputDir = "/Users/aagarwal/Desktop/ils/agarwal/siouxFalls/output/run1/";/*"./output/run2/";*/
	private static String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private static String configFile = outputDir+"/output_config.xml";//"/config.xml";//
	private static String eventFile = outputDir+"/ITERS/it.100/100.events.xml.gz";//"/events.xml";//

	 Network network;

	public static void main(String[] args) throws IOException {
		PerLinkCongestionData data = new PerLinkCongestionData();
		data.run();
	}

	private void run() throws IOException {
		
		BufferedWriter writer1 = IOUtils.getBufferedWriter(outputDir+"/ITERS/it.100/100.timeLinkIdTotalCongestion.txt");//

		Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		this.network = scenario.getNetwork();
		double simulationEndTime = LoadMyScenarios.getEndTime(configFile);
		CongestionLinkAnalyzer linkAnalyzer = new CongestionLinkAnalyzer(simulationEndTime, eventFile,1);
		linkAnalyzer.init(scenario);
		linkAnalyzer.preProcessData();
		linkAnalyzer.postProcessData();
		linkAnalyzer.checkTotalDelayUsingAlternativeMethod();
		Map<Double, Map<Id, Double>> time2linkIdDelays = linkAnalyzer.getCongestionPerLinkTimeInterval();
		
		writer1.write("time \t linkId \t delay(in sec) \n");
		for(double time : time2linkIdDelays.keySet()){
			for(Link link : network.getLinks().values()){
				double delay;
				if(time2linkIdDelays.get(time).get(link.getId())==null) delay = 0.0;
				else delay = time2linkIdDelays.get(time).get(link.getId());
				writer1.write(time+"\t"+link.getId().toString()+"\t"+delay);
				writer1.write("\n");
			}
		} 
		writer1.close();
		logger.info("Finished Writing files.");
	}
}