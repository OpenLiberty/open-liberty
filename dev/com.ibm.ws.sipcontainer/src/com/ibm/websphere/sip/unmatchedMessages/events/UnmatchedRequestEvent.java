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
package com.ibm.websphere.sip.unmatchedMessages.events;

import java.util.EventObject;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipServletRequest;

import com.ibm.websphere.sip.unmatchedMessages.UnmatchedMessageListener;

/**
 * @ibm-api
 * The application will receive this event as part of {@linkplain UnmatchedMessageListener}
 * when new unmatched request received in the SIP Container.
 * 
 * @author Anat Fradin
 */
public class UnmatchedRequestEvent extends  EventObject{

	private static final long serialVersionUID = 9100908574559212186L;
	
	/**
	 * Reference to Servlet Context
	 */
	private ServletContext _servletContext = null;
	
	/**
	 * Constructor for a new UnmatchedRequestEvent 
	 * @param req - Unmatched incoming request
	 * @param ctxt -  Servlet Context related to the specific Sip Application
	 */
	public UnmatchedRequestEvent(SipServletRequest req, ServletContext ctxt) {
		super(req);
		_servletContext = ctxt;
	}
	
	/**
	 * Returns the unmatched incoming request
	 * @return SipServletRequest
	 */
	public SipServletRequest getRequest() {
		return (SipServletRequest) getSource();	
	}
	
	/**
	 * Returns related Servlet Context
	 * @return ServletContext
	 */
	public ServletContext getServletContext(){
		return _servletContext;
	}

}
