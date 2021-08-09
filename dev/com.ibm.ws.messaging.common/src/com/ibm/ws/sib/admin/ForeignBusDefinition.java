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

import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * @author philip
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface ForeignBusDefinition {

	// Accessor methods for configuration attributes

	/**
	 * Get the name
	 * 
	 * @return string containing the name of the foreign bus
	 */
	public String getName();

	/**
	 * Get the description
	 * 
	 * @return string containing the textual description of the foreign bus
	 */
	public String getDescription();

	/**
	 * Get the UUID
	 * 
	 * @return string containing the UUID of the foreign bus
	 */
	public SIBUuid12 getUuid();

	/**
	 * Get the sendAllowed
	 * 
	 * @return boolean Send allowed
	 */
	public boolean getSendAllowed();

	// Methods to return relationships

	/**
	 * Return whether the foreign bus has a Link associated with it. Use this
	 * method to test if a link exists prior to retrieving the LinkDefinition
	 * using the getLink() method.
	 * 
	 * @return boolean value indicating whether a Link exists or not.
	 */
	public boolean hasLink();

	/**
	 * Retrieve the LinkDefinition of this foreign bus. This method should only
	 * be invoked after using the hasLink() method to check whether a link
	 * exists. If a link does not exist, then the exception
	 * SIBExceptionNoLinkExists is thrown.
	 * 
	 * @return The <code>LinkDefinition</code> on that foreign bus
	 * @throws SIBExceptionNoLinkExists
	 *             No link exists on the foreign bus
	 */
	public VirtualLinkDefinition getLink() throws SIBExceptionNoLinkExists;

	/**
	 * Retrieve the ForeignBusDefinition of the bus which represents the next
	 * routing point as configured by the Administrator. If this bus has a link
	 * defined, then this method returns the same ForeignBusDefinition.
	 * 
	 * @return The <code>ForeignBusDefinition</code> on that foreign bus
	 * @throws SIBExceptionBusNotFound
	 *             The referenced foreign bus definition was not found
	 */
	public ForeignBusDefinition getNextHop() throws SIBExceptionBusNotFound;

	/**
	 * Retrieve the LinkDefinition for the designated next hop.
	 * 
	 * @return
	 * @throws _SIBExceptionBusNotFound
	 *             The referenced foreign bus definition was not found
	 * @throws _SIBExceptionNoLinkExists
	 *             No link exists on the foreign bus
	 */
	public VirtualLinkDefinition getLinkForNextHop()
			throws SIBExceptionBusNotFound, SIBExceptionNoLinkExists;

	/**
	 * Retrieve the ForeignDestinationDefault. This method should only be
	 * invoked after using the hasDestinationDefault() method to check whether
	 * it exists. If it does not exist, then the exception
	 * SIBExceptionObjectNotFound is thrown.
	 * 
	 * @return The <code>ForeignDestinationDefault</code>
	 * @throws SIBExceptionObjectNotFound
	 *             No link exists on the foreign bus
	 */
	public ForeignDestinationDefault getDestinationDefault()
			throws SIBExceptionObjectNotFound;
}
