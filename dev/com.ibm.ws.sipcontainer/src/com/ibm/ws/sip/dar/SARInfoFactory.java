/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.dar;

import java.util.StringTokenizer;

import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipApplicationRoutingRegionType;
import javax.servlet.sip.ar.SipRouteModifier;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * The class is factory for SipApplicationRouterInfo
 * 
 * @author Roman Mandeleil
 */
public class SARInfoFactory {

	private static final LogMgr c_logger = Log.get(SARInfoFactory.class);

	/**
	 * Generate object from representing string.
	 * 
	 * @param infoString -
	 *            e.g. ("AppName", "URI location", "Region", "","Route", "0")
	 * @return created SipApplicationRouterInfo
	 */
	public static SipApplicationRouterInfo createSARInfoFactory(
			String infoString) {

		SipApplicationRouterInfo sarInfo = null;
		String appName = "";
		String uriField = "";
		String region = "";
		String routeURI = "";
		String routeType = "";
		String stateInfo = "";

		infoString = infoString.trim();
		infoString = infoString.substring(1, infoString.length() - 1);
		StringTokenizer tokenizer = new StringTokenizer(infoString, ",");

		appName = tokenizer.nextToken().trim();
		appName = appName.substring(1, appName.length() - 1);

		uriField = tokenizer.nextToken().trim();
		uriField = uriField.substring(1, uriField.length() - 1);

		region = tokenizer.nextToken().trim();
		region = region.substring(1, region.length() - 1);

		routeURI = tokenizer.nextToken().trim();
		routeURI = routeURI.substring(1, routeURI.length() - 1);

		routeType = tokenizer.nextToken().trim().toUpperCase();
		routeType = routeType.substring(1, routeType.length() - 1);

		stateInfo = tokenizer.nextToken().trim();
		stateInfo = stateInfo.substring(1, stateInfo.length() - 1);

		SipApplicationRoutingRegionType sipApplicationRoutingRegionType = SipApplicationRoutingRegionType
				.valueOf(region);

		
		String[] routeArray = {routeURI}; 
		
		sarInfo = new SipApplicationRouterInfo(appName,
				new SipApplicationRoutingRegion("",
						sipApplicationRoutingRegionType), uriField, routeArray,
				SipRouteModifier.valueOf(routeType), stateInfo);

		return sarInfo;
	}

}
