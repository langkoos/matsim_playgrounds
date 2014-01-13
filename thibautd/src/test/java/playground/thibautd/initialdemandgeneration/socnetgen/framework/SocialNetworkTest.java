/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkTest.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thibautd
 */
public class SocialNetworkTest {
	@Test
	public void testAddTie() {
		final SocialNetwork testee = new SocialNetwork( false );

		final Id id1 = new IdImpl( 1 );
		final Id id2 = new IdImpl( 2 );

		testee.addTie( new Tie( id1 , id2 ) );

		Assert.assertEquals(
				"alter not well added",
				Collections.singleton( id2 ),
				testee.getAlters( id1 ));

		Assert.assertEquals(
				"reciprocal alter not well added",
				Collections.singleton( id1 ),
				testee.getAlters( id2 ));

		Assert.assertEquals(
				"unexpected number of egos in network",
				2,
				testee.getEgos().size());
	}

	@Test
	public void testReciprocity() {
		final Random random = new Random( 9548756 );

		for ( int i=0; i < 100; i++ ) {
			final SocialNetwork net = createRandomNetwork( random );

			for ( Id ego : net.getEgos() ) {
				for ( Id alter : net.getAlters( ego ) ) {
					Assert.assertTrue(
							"found a non-reciprocal relationship!",
							net.getAlters( alter ).contains( ego ));
				}
			}
		}
	}

	private static SocialNetwork createRandomNetwork(final Random random) {
		final List<Id> ids = new ArrayList<Id>();

		for ( int i=0; i < 100 ; i++ ) {
			ids.add( new IdImpl( i ) );
		}

		final SocialNetwork net = new SocialNetwork();
		net.addEgoIds( ids );
		for ( Id ego : ids ) {
			for ( Id alter : ids ) {
				if ( alter == ego ) continue;
				if ( random.nextDouble() < 0.2 ) {
					net.addTie( new Tie( ego , alter ) );
				}
			}
		}

		return net;
	}
}

