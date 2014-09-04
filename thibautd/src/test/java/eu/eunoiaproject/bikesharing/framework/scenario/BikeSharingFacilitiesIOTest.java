/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilitiesIOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilitiesReader;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilitiesWriter;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;

/**
 * @author thibautd
 */
public class BikeSharingFacilitiesIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testIO() {
		final BikeSharingFacilities facilities = createFacilities();

		// dump and re-read
		final String file = utils.getOutputDirectory() +"/myDumpedFacilities.xml";
		new BikeSharingFacilitiesWriter( facilities ).write( file );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new BikeSharingFacilitiesReader( scenario ).parse( file );

		final BikeSharingFacilities newFacilities = (BikeSharingFacilities) scenario.getScenarioElement( BikeSharingFacilities.ELEMENT_NAME );

		assertFacilitiesMatch( facilities , newFacilities );
	}

	private void assertFacilitiesMatch(
			final BikeSharingFacilities facilities,
			final BikeSharingFacilities newFacilities) {
		Assert.assertEquals(
				"size changed through IO!",
				facilities.getFacilities().size(),
				newFacilities.getFacilities().size() );

		Assert.assertEquals(
				"Ids changed through IO!",
				facilities.getFacilities().keySet(),
				newFacilities.getFacilities().keySet() );

		for ( BikeSharingFacility f : facilities.getFacilities().values() ) {
			final BikeSharingFacility newF = newFacilities.getFacilities().get( f.getId() );

			Assert.assertEquals(
					"coords do not match",
					f.getCoord(),
					newF.getCoord() );

			Assert.assertEquals(
					"link id do not match",
					f.getLinkId(),
					newF.getLinkId() );

			Assert.assertEquals(
					"capacity do not match",
					f.getCapacity(),
					newF.getCapacity() );

			Assert.assertEquals(
					"initial number of bikes do not match",
					f.getInitialNumberOfBikes(),
					newF.getInitialNumberOfBikes() );
		}

		for ( Map.Entry<String, String> meta : facilities.getMetadata().entrySet() ) {
			final String actualValue = newFacilities.getMetadata().get( meta.getKey() );

			Assert.assertEquals(
					"wrong metadata for attribute "+meta.getKey(),
					meta.getValue(),
					actualValue );
		}
	}

	private static BikeSharingFacilities createFacilities() {
		final BikeSharingFacilities fs = new BikeSharingFacilities();

		final Random r = new Random( 1234 );
		for ( int i=0; i < 100 ; i++ ) {
			fs.addFacility(
					fs.getFactory().createBikeSharingFacility(
						new IdImpl( i ),
						new CoordImpl( r.nextDouble() , r.nextDouble() ),
						new IdImpl( r.nextInt( 20 ) ),
						r.nextInt( 2000 ),
						r.nextInt( 2000 ) ) );
		}

		fs.addMetadata( "some attribute" , "some value" );
		fs.addMetadata( "some other attribute" , "some other value" );

		return fs;
	}
}
