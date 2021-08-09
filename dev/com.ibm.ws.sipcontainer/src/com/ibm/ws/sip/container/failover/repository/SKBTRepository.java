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
package com.ibm.ws.sip.container.failover.repository;

import java.util.Collection;

import com.ibm.ws.sip.container.tu.SessionKeyBase;

public interface SKBTRepository extends TransactionSupport {
	/**
	 * Adds a new Application session to the repository. 
	 * 
	 * @param keyBaseSession - key base session key
	 * @param appsession - the Sip App session id intance we want to save
	 * @return previous value (if exists) , maybe null if new key inserted.
	 */
	public SessionKeyBase put(String keyBaseSession, SessionKeyBase appSessionId);
	
	/**
	 * Search a Sip application session id by giving its app key base session.
	 * 
	 * @param keyBaseSession - the replated sip application key base Session id.
	 */
	public SessionKeyBase get(String keyBaseSession);
	
	/**
	 * removes a a SIP Application session from the repository.
	 *  
	 * @param keyBaseSession - the replated sip application key base Session id. 
	 * @return the existing sip application session id, null if not found.
	 */
	public SessionKeyBase remove(String keyBaseSession);

	/**
	 * 
	 * @return a list of all current/active application sessions id.
	 */
	public Collection<SessionKeyBase> getAll();


}
