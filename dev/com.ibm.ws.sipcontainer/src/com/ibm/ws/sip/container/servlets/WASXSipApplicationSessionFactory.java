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
package com.ibm.ws.sip.container.servlets;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.ws.sip.container.converged.servlet.session.ConvergedAppUtils;
import com.ibm.ws.sip.container.session.SipApplicationSessionCreator;
//TODO Liberty import com.ibm.ws.webcontainer.httpsession.ApplicationSessionCreator;

/**
 * Creates a SipApplicationSessionImpl. This implementation supports converged applications
 * 
 * @author Nitzan Nissim
 */
public class WASXSipApplicationSessionFactory {
	
	/** Singleton */
    private static WASXSipApplicationSessionFactory s_instance = new WASXSipApplicationSessionFactory(); 

	private WASXSipApplicationSessionFactory() {
		// the SipAppSessionCreator will be used by web container to create ApplicationSession.
		// the AppSession is create request come from non sip protocol
		// for example - web services - jsr109
		ConvergedAppUtils.setAppSessCreator(new SipApplicationSessionCreator());
	}
	
	public static WASXSipApplicationSessionFactory getInstance() {
		return s_instance;
	}

	/**
	 * @see com.ibm.ws.sip.container.servlets.SipApplicationSessionFactory#createSipApplicationSession(java.lang.String)
	 */
	public SipApplicationSession createSipApplicationSession(String id) {
		return new WASXSipApplicationSessionImpl(id);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.servlets.SipApplicationSessionFactory#createSipApplicationSession()
	 */
	public SipApplicationSession createSipApplicationSession() {
		return new WASXSipApplicationSessionImpl();
	}

}
