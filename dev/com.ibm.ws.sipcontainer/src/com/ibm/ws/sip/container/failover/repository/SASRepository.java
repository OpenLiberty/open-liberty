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
package com.ibm.ws.sip.container.failover.repository;

import java.util.List;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;

public interface  SASRepository extends TransactionSupport 
{
	/**
	 * Adds a new Application session to the repository. 
	 * @param appsession - the Sip App session intance we want to save
	 * @return previous value (if exists) , maybe null if new key inserted.
	 */
	public  SipApplicationSession put(String appSessionId , SipApplicationSession appsession);
	
	/**
	 * Search a Sip application session by giving its app session ID.
	 * @param appSessionId - the replated sip application Session id.
	 */
	public  SipApplicationSession get(String appSessionId );
	
	/**
	 * removes a a SIP Application session from the repository. 
	 * @param appSession - application session 
	 * @return the existing sip application session , maybe null if not found.
	 */
	public  SipApplicationSession remove(SipApplicationSession appSession );

	/**
	 * 
	 * @return a list of all current/active application sessions.
	 */
	public List<SipApplicationSessionImpl> getAll();
	
}
