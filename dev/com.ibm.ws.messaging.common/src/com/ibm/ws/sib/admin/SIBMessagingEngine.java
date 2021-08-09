/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

import java.util.HashMap;

/**
 * Interface representing the messaging engine construct
 * @author chetan
 *
 */
public interface SIBMessagingEngine extends LWMConfig{

	/**
	 * Set the name of the messaging engine.The ID from the <messagingEngine> tag is set as the name
	 * @param name
	 */
    public void setName(String name);

    /**
     * Get the name of the messaging engine
     * @return String
     */
    public String getName();

    /**
     * Get the high message threshold
     * @return
     */
    public long getHighMessageThreshold();

    /**
     * Set the high message threshold
     * @param newHighMessageThreshold
     */
    public void setHighMessageThreshold(long newHighMessageThreshold);

    /**
     * Getter for sibLocalizationPointList
     * @return HashMap<String, SIBLocalizationPoint>
     */
    public HashMap<String, SIBLocalizationPoint> getSibLocalizationPointList();

    /**
     * sets the localization points for each and every destination of type QUEUE and TOPIC
     * 
     * @param sibLocalizationPointList
     */
    public void setSibLocalizationPointList(
                                            HashMap<String, SIBLocalizationPoint> sibLocalizationPointList);

    /**
     * Returns the map which contains all the destination of type QUEUE and TOPIC
     * 
     * @return HashMap<String, BaseDestination>
     */
    public HashMap<String, BaseDestination> getDestinationList();

    /**
     * Contains all the destinations of type QUEUE and TOPIC.
     * Sets the hashmap where key is the ID of the destination and value is the
     * SIBDestination.
     * 
     * @param destinationList
     */

    public void setDestinationList(
                                   HashMap<String, BaseDestination> destinationList);

    /**
     * Set the UUID of the messaging engine
     * @return String
     */
    public String getUuid();

    /**
     * Set the messaging engine UUID
     * @param newUuid
     */
    public void setUuid(String newUuid);

}
