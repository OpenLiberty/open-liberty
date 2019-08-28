/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover;

import java.io.Serializable;

/**
 * Interface for container elements that are meant to be replicated across a cluster of servers. 
 * The elements share common properties when related to the same SipApplicationSession (SAS). 
 * @author Nitzan Nissim
 *
 */
public interface Replicatable extends Serializable{
	/**
     * Separates the different factors that construct the ID of replicatable elements
     */
	static public final String ID_INTERNAL_SEPERATOR = "_";
	
	/**
	 * @return The ID that is shared between different replicatable elements of an ApplicationSessio: TU, SS, SAS and Timers
	 */
	public String getSharedId();
	/**
	 * Sets the shared ID
	 * @param id
	 */
	public void setSharedId(String id);
	
	/**
	 * Indicates whether this replicatable was modified since last stored
	 */
	public boolean isDirty();
	
	/**
	 * Storing to replicatable to its repository
	 */
	public void store();
	/**
	 * Removing this replicatable from its storage
	 */
	public void removeFromStorage();
}
