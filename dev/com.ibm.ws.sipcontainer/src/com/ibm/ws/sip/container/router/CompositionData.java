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
package com.ibm.ws.sip.container.router;

import java.io.Serializable;

import javax.servlet.sip.Address;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
//import javax.servlet.sip.Address;
//import javax.servlet.sip.ar.SipApplicationRoutingDirective;

/**
 * The class contains all the data related to composition
 * 
 * @author Chaya Berezin-Chaimson
 */
public class CompositionData {

	private Serializable 					m_stateInfo = null;
	private SipApplicationRoutingDirective 	m_routingDirective = null;
	private URI								m_subscriber_Uri = null;
	private Address							m_initialPoppedRoute = null;
	private SipApplicationRoutingRegion		m_routingRegion = null;
	private String							m_nextApplication = null;
	
	/**
     * Sets the stateInfo variable
     * @param m_stateInfo
     */
	public void setStateInfo(Serializable m_stateInfo) {
		this.m_stateInfo = m_stateInfo;
	}
	
	/**
	 * This method will return the state info
	 * 
	 * @return stateInfo
	 */
	public Serializable getStateInfo() {
		return m_stateInfo;
	}
	
	/**
     * Sets the routing directive variable
     * @param m_routingDirective
     */
	public void setRoutingDirective(SipApplicationRoutingDirective m_routingDirective) {
		this.m_routingDirective = m_routingDirective;
	}
	
	/**
	 * This method will return the routing directive
	 * 
	 * @return routing directive
	 */
	public SipApplicationRoutingDirective getRoutingDirective() {
		return m_routingDirective;
	}
	
	/**
     * Sets the subscriber Uri variable
     * @param m_subscriberUri
     */
	public void setSubscriberUri(URI m_subscriberUri) {
		this.m_subscriber_Uri = m_subscriberUri;
	}
	
	/**
	 * This method will return the subscriber Uri
	 * 
	 * @return subscriber Uri
	 */
	public URI getSubscriberUri() {
		return m_subscriber_Uri;
	}
	
	/**
     * Sets the initial popped route variable
     * @param m_initialPoppedRoute
     */
	public void setInitialPoppedRoute(Address m_initialPoppedRoute) {
		this.m_initialPoppedRoute = m_initialPoppedRoute;
	}
	
	/**
	 * This method will return the initial popped route
	 * 
	 * @return initial popped route
	 */
	public Address getInitialPoppedRoute() {
		return m_initialPoppedRoute;
	}
	
	/**
     * Sets the routing region variable
     * @param m_routingRegion
     */
	public void setRoutingRegion(SipApplicationRoutingRegion m_routingRegion) {
		this.m_routingRegion = m_routingRegion;
	}
	
	/**
	 * This method will return the routing region
	 * 
	 * @return routing region
	 */
	public SipApplicationRoutingRegion getRoutingRegion() {
		return m_routingRegion;
	}
	
	/**
     * Sets the next application variable
     * @param m_nextApplication
     */
	public void setNextApplication(String m_nextApplication) {
		this.m_nextApplication = m_nextApplication;
	}
	
	/**
	 * This method will return the next application
	 * 
	 * @return next application
	 */
	public String getNextApplication() {
		return m_nextApplication;
	}
	
	
}
