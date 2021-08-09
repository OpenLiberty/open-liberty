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
package com.ibm.ws.sip.dar.ext;

import java.io.Serializable;

import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;

import com.ibm.ws.sip.dar.repository.impl.StartOrderApplicationRepository;

/**
 * This class extends SipApplicationRouterInfo defined by JSR289
 * for additional information
 * @author Anat Fradin
 */
public class SipApplicationRouterInfoStrartOrder extends SipApplicationRouterInfo {
	
	private static String[] routeArray = {""}; 
	
	/**
	 * 
	 * @param nextApplicationName
	 * @param subscriberURI
	 * @param route
	 * @param region
	 * @param mod
	 * @param stateInfo
	 * @param weight
	 */
	public SipApplicationRouterInfoStrartOrder(String nextApplicationName, 
			String subscriberURI, 
			String[] routeArray, 
			SipApplicationRoutingRegion region, 
			SipRouteModifier mod, 
			Serializable stateInfo){
		
		super(nextApplicationName, region, subscriberURI, routeArray, mod, stateInfo);
	}
	
	/**
	 * 
	 * @param nextApplicationName
	 * @param weight
	 */
	public SipApplicationRouterInfoStrartOrder(String nextApplicationName){
		super(nextApplicationName, 
				StartOrderApplicationRepository.DEAFULT_ROUTING_REGION,
				StartOrderApplicationRepository.SUBSCRIBER_URI, 
				routeArray, 
				StartOrderApplicationRepository.DEAFULT_ROUTE_MODIFIER, 
				null);

	}
		
}
