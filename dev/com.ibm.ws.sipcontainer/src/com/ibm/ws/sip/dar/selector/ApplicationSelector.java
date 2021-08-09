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
package com.ibm.ws.sip.dar.selector;

import java.io.Serializable;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

/**
 * 
 * @author Roman Mandeleil
 */
public interface ApplicationSelector {
	
	
	public SipApplicationRouterInfo getNextApplication(
			SipServletRequest initialRequest, 
			SipApplicationRoutingRegion region, 
			SipApplicationRoutingDirective directive, 
			Serializable stateInfo);
	

}
