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

import java.util.Set;

import com.ibm.ws.sib.utils.SIBUuid8;

public interface JsBus extends LWMConfig {

	/**
	 * Get the name of this Bus.
	 * 
	 * @return String
	 */
	public String getName();

	/**
	 * Gets the assigned UUID of this Bus.
	 * 
	 * @return SIBUuid8
	 */
	public SIBUuid8 getUuid();

	/* ------------------------------------------------------------------------ */
	/*
	 * getPermittedChains method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns the list of chains that are defined as being
	 * permitted.
	 * 
	 * @return The set of permitted chains.
	 */
	public Set getPermittedChains();

	/* ------------------------------------------------------------------------ */
	/*
	 * getPermittedChainUsage method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns a typesafe enumeration that indicates how the bus
	 * should determine what chains should be permitted.
	 * 
	 * @return the permitted chain usage.
	 */
	public JsPermittedChainUsage getPermittedChainUsage();

	/**
	 * This method returns a custom properties specified in the bus
	 * configuration.
	 * 
	 * @param name
	 *            The name of the custom property.
	 * @return The value of the custom property.
	 */
	public String getCustomProperty(String name);

	/**
	 * Is the bus configured as secure?
	 * 
	 * @return
	 */
	public boolean isSecure();

	/**
	 * Read the WCCM configuration repository and return a
	 * BaseDestinationDefinition containing the configuration for the specified
	 * destination.
	 * 
	 * No resolving of dynamic attributes of the BaseDestinationDefinition, such
	 * as the Exception Destination, is performed; the returned value is that
	 * stored in the configuration.
	 * 
	 * @param busName
	 *            the name of the bus on which the destination resides. This can
	 *            be the current bus, as denoted by null, an empty string or the
	 *            current bus name. For a foreign destination, a name other than
	 *            the current (ie local) bus must be specified.
	 * @param name
	 *            the name, or identifier, of the destination
	 * @return BaseDestinationDefinition
	 */
	public BaseDestinationDefinition getSIBDestination(String busName,
			String name) throws SIBExceptionBase,
			SIBExceptionDestinationNotFound;

	/**
	 * Read the WCCM configuration repository and update a
	 * BaseDestinationDefinition containing the configuration for the specified
	 * destination.
	 * 
	 * No resolving of dynamic attributes of the BaseDestinationDefinition, such
	 * as the Exception Destination, is performed; the returned value is that
	 * stored in the configuration.
	 * 
	 * @param busName
	 *            the name of the bus on which the destination resides. This can
	 *            be the current bus, as denoted by null, an empty string or the
	 *            current bus name. For a foreign destination, a name other than
	 *            the current (ie local) bus must be specified.
	 * @param name
	 *            the name, or identifier, of the destination
	 * @param dd
	 *            the DestinationDefinition to use to return the configuration
	 * @return
	 */
	public void getSIBDestination(String busName, String name,
			DestinationDefinition dd) throws SIBExceptionBase,
			SIBExceptionDestinationNotFound;

	/**
	 * Return a set of UUIDs for the Messaging Engines which localize a
	 * specified destination.
	 * 
	 * @param busName
	 * @param uuid
	 * @return
	 */
	public Set getSIBDestinationLocalitySet(String busName, String uuid)
			throws SIBExceptionBase, SIBExceptionDestinationNotFound;

	/**
	 * Read the specified destination from the WCCM configuration repository and
	 * return a DestinationDefinition containing the configuration of that
	 * destination.
	 * 
	 * @param name
	 * @return
	 */
	// public MediationDefinition getSIBMediation(String busName, String name)
	// throws SIBExceptionBase, SIBExceptionMediationNotFound;

	/**
	 * Read the specified destination from the WCCM configuration repository and
	 * return a DestinationDefinition containing the configuration of that
	 * destination.
	 * 
	 * @param name
	 * @return
	 */
	// public void getSIBMediation(String busName, String name,
	// MediationDefinition dd)
	// throws SIBExceptionBase, SIBExceptionMediationNotFound;

	/**
	 * Return a set of UUIDs for the Messaging Engines which localize a
	 * specified mediation.
	 * 
	 * @param busName
	 * @param uuid
	 * @return
	 */
	// public Set getSIBMediationLocalitySet(String busName, String uuid) throws
	// SIBExceptionBase, SIBExceptionMediationNotFound;

	/**
	 * Retrieve a ForeignBusDefinition for a specified foreign bus.
	 * 
	 * @param name
	 *            The name of the bus
	 * @return The <code>ForeignBusDefinition</code> for that bus
	 * @exception SIBExceptionNotFound
	 *                A foreign bus with the specified name was not found
	 */
	public ForeignBusDefinition getForeignBus(String name);

	/**
	 * Retrieve a ForeignBusDefinition for the foreign bus used by a specified
	 * Link
	 * 
	 * @param uuid
	 *            the UUID of VirtualLink
	 * @return The <code>ForeignBusDefinition</code> for that bus
	 */
	 public ForeignBusDefinition getForeignBusForLink(String uuid);

	/* ------------------------------------------------------------------------ */
	/*
	 * isBootstrapAllowed method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns true if bootstrap is allowed by this application
	 * server. Whether bootstrap is allowed is based on the SIBBoostrapPolicy.
	 * If the policy is SIBSERVICE then this will always return true. If the
	 * SIBBootstrapPolicy is MEMBERS_ONLY then it must be a bus member. If the
	 * policy is MEMBERS_AND_NOMINATED then the server must be a bus member or a
	 * bootstrap member.
	 * 
	 * @return true if bootstrap should be allowed by this server.
	 */
	public boolean isBootstrapAllowed();

	/* ------------------------------------------------------------------------ */
	/*
	 * isBusAuditAllowed method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns true if audit is allowed on the bus.
	 * 
	 * @return true if audit allowed on the bus
	 */
	public boolean isBusAuditAllowed();

	/* ------------------------------------------------------------------------ */
	/*
	 * registerConfigChangeListener method /*
	 * ------------------------------------------------------------------------
	 */

	// public void addConfigChangeListener(BusConfigDocument configDoc,
	// ConfigChangeListener listener);

	/* ------------------------------------------------------------------------ */
	/*
	 * registerConfigChangeListener method /*
	 * ------------------------------------------------------------------------
	 */

	// public void addConfigChangeListener(List<BusConfigDocument> configDocs,
	// ConfigChangeListener listener);
	public JsMEConfig getLWMMEConfig();

}