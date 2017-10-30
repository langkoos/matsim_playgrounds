/* *********************************************************************** *
 * project: org.matsim.*
 * KMLVertexId.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.studies.sbsurvey.io;

import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.KMLObjectDetail;

import net.opengis.kml.v_2_2_0.PlacemarkType;

/**
 * @author illenberger
 *
 */
public class KMLVertexId implements KMLObjectDetail {

	@Override
	public void addDetail(PlacemarkType kmlPlacemark, Object object) {
		kmlPlacemark.setName(((SocialVertex) object).getPerson().getId().toString());
	}

}
