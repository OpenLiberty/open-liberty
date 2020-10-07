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
package com.ibm.websphere.sip.resolver;

import java.util.EventListener;

import com.ibm.websphere.sip.resolver.events.SipURILookupErrorEvent;
import com.ibm.websphere.sip.resolver.events.SipURILookupEvent;

/**
 * Implementations of this interface are notified when {@linkplain DomainResolver}
 * has completed its DNS query.
 * To receive those events, application should call the asynchronous API {@linkplain DomainResolver#locate(javax.servlet.sip.SipURI, DomainResolverListener, javax.servlet.sip.SipSession)}
 *  
 * @author Noam Almog
 * @ibm-api
 */
public interface DomainResolverListener extends EventListener {

	/**
	 * Notification that query was successful and application can handle results.
	 * 
	 * @param event
	 */
	public void handleResults(SipURILookupEvent event);
	
	/**
	 * Notification that query was unsuccessful.
	 * 
	 * @param event
	 */
	public void error(SipURILookupErrorEvent event);
}
