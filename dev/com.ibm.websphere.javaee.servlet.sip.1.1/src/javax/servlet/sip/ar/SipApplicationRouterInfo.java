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
package javax.servlet.sip.ar;

import java.io.Serializable;

/**
 * That class represents value object that is returned 
 * by application router, the class contains information relevant
 * for selected application.
 */
public class SipApplicationRouterInfo {

	 private String nextApplicationName;
	 private String[] routes;
	 private SipApplicationRoutingRegion sipApplicationRoutingRegion;
	 private SipRouteModifier sipRouteModifier;
	 private Serializable stateInfo;
	 private String subscriberURI;

    /**
     * Creates a SipApplicationRouterInfo object containing the information necessary 
     * for the conatiner to perform its routing decision.
     *  
	 * @param nextApplicationName - The name of the application that the application 
	 * 								router selects to service this request. If no further 
	 * 								application is needed in 
	 * @param subscriberURI - The URI that the application is selected to serve
	 * @param routes - array of external routes or an internal route. External routes are 
	 * 				   pushed onto the request by the container, internal route is used by 
	 * @param routingRegion - The Routing region in which the application that is selected will 
	 * 						  serve.
	 * @param mod - An enum modifier which qualifies the routes returned and the router behavior
	 * @param stateInfo
	 */
	public SipApplicationRouterInfo(String nextApplicationName, 
			SipApplicationRoutingRegion routingRegion,
			String subscriberURI, 
			String[] routes, 
			SipRouteModifier mod, 
			Serializable stateInfo){
		this.nextApplicationName = nextApplicationName;
		this.subscriberURI = subscriberURI;
		this.routes = routes;
		this.sipApplicationRoutingRegion = routingRegion;
		this.sipRouteModifier = mod;
		this.stateInfo = stateInfo;
	} 
	

	/**
	 * @return name of next application selected. If the top route is external then this 
	 * 		   returns null.
	 */
	public String getNextApplicationName() {
		return nextApplicationName;
	}

	/**
	 * @return The enum {@link SipRouteModifier} associated with the router
	 *         info.
	 */
	public SipRouteModifier getRouteModifier() {
		return sipRouteModifier;
	}

	/**
	 * @return selected route
	 */
	public String[] getRoutes(){
		return routes;
	}

	/**
	 * @return Routing region in which the next application is selected to
	 *         serve. If the route is external then this returns a null.
	 */
	public SipApplicationRoutingRegion getRoutingRegion() {
		return sipApplicationRoutingRegion;
	}

	/**
	 * @return status of the application chain
	 */
	public Serializable getStateInfo() {
		return stateInfo;
	}
	
	/**
	 * @return URI of the subscriber whom the next application is selected to serve. 
	 * 		   If the top route is external then this returns a null.
	 */
	public String getSubscriberURI() {
		return subscriberURI;
	}

	/**
	 * @return string for log purposes 
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("; Data: nextApplicationName="); 
		sb.append(nextApplicationName);
		sb.append(", sipRouteModifier=");
		sb.append(sipRouteModifier);
		sb.append(", sipApplicationRoutingRegion = ");
		sb.append(sipApplicationRoutingRegion);
		sb.append(", stateInfo = ");
		sb.append(stateInfo);
		sb.append(", subscriberURI = ");
		sb.append(subscriberURI);
		return sb.toString(); 
	}
}
