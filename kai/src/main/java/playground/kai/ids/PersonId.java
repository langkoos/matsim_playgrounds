/* *********************************************************************** *
 * project: kai
 * PersonId.java
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

package playground.kai.ids;

import org.matsim.api.core.v01.Id;

/**The necessary steps seem to be:<ul>
 * <li> Generate interface PersonId.
 * <li> Make IdImpl implement PersonId.
 * <li> Make PersonImpl internally store PersonId instead of Id, and return PersonId.
 * <li> Generate PopulationIdFactory.createId(...) that creates PersonId.
 * <li> Set Scenario.createId(...) to deprecated.
 * <li> Set new IdImpl(...) to deprecated.
 * <li> As a perspective, I would like to make new IdImpl(...) protected so that it cannot be used any longer. 
 * </ul>
 * @author nagel
 */
public interface PersonId extends Id {

}
