/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip.ar;

/**
 * The enum identifies SipApplicationRouterInfo.getRoute()
 * validness.
 * 
 * @author Roman Mandeleil
 */
public enum SipRouteModifier {


	/**
	 * indicates that application router is not returning any route and 
	 * the SipApplicationRouterInfo.getRoute() value if any should 
	 * be disregarded.
	 */
	NO_ROUTE, 

	/**
	 * modifier indicates to the container to remove the popped top route 
	 * associated with the request. Specifically the subsequent invocation 
	 * of SipServletRequest.getPoppedRoute() MUST now return null 
	 */
	ROUTE, 
	
	/**
	 * Tells the container to push a route back to itself before pusing the external 
	 * routes specified by SipApplicationRouterInfo.getRoutes().  
	 */
	ROUTE_BACK 
	
}
