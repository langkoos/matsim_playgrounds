/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package playground.sergioo.Singapore;


import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.router.TransitRouterConfig;

import playground.sergioo.Singapore.ScoringFunction.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.sergioo.Singapore.TransitRouterVariable.TransitRouterVariableImplFactory;
import playground.sergioo.Singapore.TransitRouterVariable.WaitTimeCalculator;


/**
 * A run Controler for a transit router that depends on the travel times and wait times
 * 
 * @author sergioo
 */

public class ControlerWW implements StartupListener {

	private Controler controler;
	
	public ControlerWW(Controler controler) {
		this.controler = controler;
	}

	public static void main(String[] args) {
		Controler controler = new Controler(args);
		controler.setOverwriteFiles(true);
		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
		controler.addControlerListener(new ControlerWW(controler));
		controler.run();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(controler.getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), 30*3600);
		controler.getEvents().addHandler(waitTimeCalculator);
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(controler.getScenario().getConfig().planCalcScore(),
				controler.getScenario().getConfig().plansCalcRoute(), controler.getScenario().getConfig().transitRouter(),
				controler.getScenario().getConfig().vspExperimental());
		controler.setTransitRouterFactory(new TransitRouterVariableImplFactory(controler, transitRouterConfig, waitTimeCalculator));
	}
	
}
