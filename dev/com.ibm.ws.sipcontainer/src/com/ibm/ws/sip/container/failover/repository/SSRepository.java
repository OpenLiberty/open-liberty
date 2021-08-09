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

import javax.servlet.sip.SipSession;

/**
 * This class defines a storage for Sip Session implementation instances
 * @author mordechai
 *
 */
public interface SSRepository extends TransactionSupport 
{
	
	/**
	 * Add a Sip Session instance to the repository of Sip Sessions.
	 * @param sessionId - the SipSession id.
	 * @param session - a SipSession instance
	 * @return the previous value in the repository (if such value exsists ).
	 */
	public SipSession put(String sessionId , SipSession session);

	/**
	 * Search a Sip session by giving its session ID.
	 * @param sessionId - the replated SipSession id.
	 */
	public SipSession get(String sessionId );
	
	/**
	 * Remove a Sip session from the repository
	 * @param session - the sip session to be removed.
	 * @return previous value (if exists)
	 */
	public SipSession remove(SipSession session );

}
