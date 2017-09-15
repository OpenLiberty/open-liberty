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
package com.ibm.ws.sib.admin.internal;


import java.util.HashMap;

import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.SIBLocalizationPoint;
import com.ibm.ws.sib.admin.SIBMessagingEngine;

/**
 *
 */
public class SIBMessagingEngineImpl implements SIBMessagingEngine {

	private String name = JsAdminConstants.DEFAULTMENAME;
	private String uuid = null;
	private long highMessageThreshold = 50000;
	private HashMap<String, SIBLocalizationPoint> sibLocalizationPointList;

	/**
	 *Map to store the destinations.Key is the ID of the destination and value
	 * is SIBDestination
	 */
	private HashMap<String, BaseDestination> destList;

	/** {@inheritDoc} */
	@Override
	public String getUuid() {
		return uuid;
	}

	/** {@inheritDoc} */
	@Override
	public void setUuid(String newUuid) {
		this.uuid = newUuid;

	}

	/** {@inheritDoc} */
	@Override
	public long getHighMessageThreshold() {
		return highMessageThreshold;
	}

	/** {@inheritDoc} */
	@Override
	public void setHighMessageThreshold(long newHighMessageThreshold) {
		this.highMessageThreshold = newHighMessageThreshold;

	}

	/** {@inheritDoc} */
	@Override
	public HashMap<String, BaseDestination> getDestinationList() {
		return destList;
	}

	/** {@inheritDoc} */
	@Override
	public void setDestinationList(
			HashMap<String, BaseDestination> destinationList) {
		this.destList = destinationList;

	}

	/** {@inheritDoc} */
	@Override
	public HashMap<String, SIBLocalizationPoint> getSibLocalizationPointList() {

		return sibLocalizationPointList;
	}

	/** {@inheritDoc} */
	@Override
	public void setSibLocalizationPointList(
			HashMap<String, SIBLocalizationPoint> sibLocalizationPointList) {
		this.sibLocalizationPointList = sibLocalizationPointList;

	}

	@Override
	public void setName(String name) {
		this.name = name;

	}

	@Override
	public String getName() {
		return name;
	}

}
